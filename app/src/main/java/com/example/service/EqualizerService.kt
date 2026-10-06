package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.HoregApplication
import com.example.MainActivity
import com.example.R

class EqualizerService : Service() {
    companion object {
        const val CHANNEL_ID = "horeg_eq_audio_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TOGGLE_EQ = "com.example.service.ACTION_TOGGLE_EQ"
        const val ACTION_OPEN_SESSION = "com.example.service.ACTION_OPEN_SESSION"
        const val ACTION_CLOSE_SESSION = "com.example.service.ACTION_CLOSE_SESSION"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_EQ -> {
                val manager = HoregApplication.instance.audioEffectManager
                manager.setEnabled(!manager.isEnabled.value)
                updateNotification()
            }
            ACTION_OPEN_SESSION -> {
                val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)
                val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                if (sessionId != -1) {
                    HoregApplication.instance.audioEffectManager.openSession(sessionId, pkg)
                }
            }
            ACTION_CLOSE_SESSION -> {
                val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)
                if (sessionId != -1) {
                    HoregApplication.instance.audioEffectManager.closeSession(sessionId)
                }
            }
            else -> {
                // ACTION_START or standard start
                startAsForeground()
            }
        }

        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val manager = HoregApplication.instance.audioEffectManager
        val isEnabled = manager.isEnabled.value
        val btStatus = manager.bluetoothStatus.value

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, EqualizerService::class.java).apply {
            action = ACTION_TOGGLE_EQ
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionText = if (isEnabled) "Matikan EQ" else "Aktifkan EQ"

        val statusText = if (isEnabled) {
            "Aktif • Sub 60Hz Horeg • ${btStatus.deviceName}"
        } else {
            "Nonaktif (Bypass)"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Horeg EQ Audio Studio")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_play,
                toggleActionText,
                togglePendingIntent
            )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Horeg EQ Audio Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status background equalizer untuk YouTube dan TWS"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
