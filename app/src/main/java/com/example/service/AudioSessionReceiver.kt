package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.HoregApplication

class AudioSessionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AudioSessionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)

        Log.d(TAG, "AudioSession broadcast received: action=$action, sessionId=$sessionId, pkg=$packageName")

        if (sessionId == -1) return

        // 1. Direct memory dispatch for zero-latency effect attachment
        try {
            if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) {
                HoregApplication.instance.audioEffectManager.openSession(sessionId, packageName)
            } else if (action == AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION) {
                HoregApplication.instance.audioEffectManager.closeSession(sessionId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct call to AudioEffectManager failed", e)
        }

        // 2. Pass through foreground service
        try {
            val serviceIntent = Intent(context, EqualizerService::class.java).apply {
                this.action = if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
                    EqualizerService.ACTION_OPEN_SESSION
                else
                    EqualizerService.ACTION_CLOSE_SESSION
                putExtra(EqualizerService.EXTRA_SESSION_ID, sessionId)
                putExtra(EqualizerService.EXTRA_PACKAGE_NAME, packageName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start service from receiver", e)
        }
    }
}
