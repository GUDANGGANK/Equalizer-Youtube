package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.audio.AudioEffectManager
import com.example.data.AppDatabase
import com.example.service.EqualizerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HoregApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val audioEffectManager by lazy { AudioEffectManager(this) }

    companion object {
        lateinit var instance: HoregApplication
            private set

        fun startEqualizerService(context: Context) {
            val intent = Intent(context, EqualizerService::class.java).apply {
                action = EqualizerService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopEqualizerService(context: Context) {
            val intent = Intent(context, EqualizerService::class.java).apply {
                action = EqualizerService.ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Pre-initialize audio manager
        audioEffectManager
        startEqualizerService(this)
    }

    override fun onTerminate() {
        audioEffectManager.release()
        super.onTerminate()
    }
}
