package com.example.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class McWakeService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceTts: TextToSpeech? = null
    private var ttsReady = false
    private var currentLanguage = AppLanguage.ENGLISH
    private var videoTalkActive = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        serviceTts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val langCode = intent?.getStringExtra(EXTRA_LANGUAGE)
        if (langCode != null) {
            currentLanguage = if (langCode == AppLanguage.BANGLA.code) AppLanguage.BANGLA else AppLanguage.ENGLISH
        }
        videoTalkActive = intent?.getBooleanExtra(EXTRA_VIDEO_TALK, videoTalkActive) ?: videoTalkActive

        if (action == ACTION_TRIGGER_WAKE) {
            respondFastToBoss()
            return START_STICKY
        }

        if (action == ACTION_STOP) {
            releaseWakeLock()
            _isRunning.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasMic = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            val hasCam = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            val fgsType = when {
                hasMic && videoTalkActive && hasCam ->
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                hasMic -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                videoTalkActive && hasCam -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                else -> 0
            }
            if (fgsType != 0) {
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isRunning.value = true
        return START_STICKY
    }

    private fun respondFastToBoss() {
        val greeting = McLocalization.get(currentLanguage).mcGreetingWake
        if (ttsReady) {
            val locale = if (currentLanguage == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.US
            serviceTts?.setLanguage(locale)
            serviceTts?.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "mc_fast_wake")
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(statusText = greeting))
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "McAssistant::ScreenOffWakeLock"
                )
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(60 * 60 * 1000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.mc_wake_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.mc_wake_channel_desc)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String? = null): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FROM_WAKE_SERVICE, true)
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callMcIntent = Intent(this, McWakeService::class.java).apply {
            action = ACTION_TRIGGER_WAKE
            putExtra(EXTRA_LANGUAGE, currentLanguage.code)
        }
        val callMcPending = PendingIntent.getService(
            this,
            1,
            callMcIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeInfo = buildString {
            append(if (currentLanguage == AppLanguage.BANGLA) "স্ক্রিন-অফ ফাস্ট ওয়েক সক্রিয়" else "Screen-off fast wake active")
            if (videoTalkActive) {
                append(" • Video Talk ON")
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MC Personal AI Core")
            .setContentText(statusText ?: modeInfo)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)
            .addAction(0, "Call MC", callMcPending)
            .build()
    }

    override fun onDestroy() {
        releaseWakeLock()
        serviceTts?.stop()
        serviceTts?.shutdown()
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "mc_wake_channel"
        const val NOTIFICATION_ID = 7001
        const val ACTION_START = "com.example.MC_WAKE_START"
        const val ACTION_STOP = "com.example.MC_WAKE_STOP"
        const val ACTION_TRIGGER_WAKE = "com.example.MC_TRIGGER_WAKE"
        const val EXTRA_LANGUAGE = "extra_language"
        const val EXTRA_VIDEO_TALK = "extra_video_talk"
        const val EXTRA_FROM_WAKE_SERVICE = "extra_from_wake_service"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context, language: AppLanguage, videoTalk: Boolean) {
            val intent = Intent(context, McWakeService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LANGUAGE, language.code)
                putExtra(EXTRA_VIDEO_TALK, videoTalk)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, McWakeService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
