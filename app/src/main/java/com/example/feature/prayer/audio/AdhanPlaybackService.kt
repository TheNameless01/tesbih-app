package com.example.feature.prayer.audio

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.MusabbihApplication
import com.example.feature.prayer.domain.model.AudioSourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdhanPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var audioManager: AdhanAudioManager
    private lateinit var notificationManager: AdhanNotificationManager

    override fun onCreate() {
        super.onCreate()
        audioManager = AdhanAudioManager(this)
        notificationManager = AdhanNotificationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            stopPlaybackAndService()
            return START_NOT_STICKY
        }

        val prayerArabicName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        val timeFormatted = intent?.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val locationName = intent?.getStringExtra(EXTRA_LOCATION_NAME) ?: ""

        val notification = notificationManager.buildAdhanNotification(
            prayerArabicName = prayerArabicName,
            timeFormatted = timeFormatted,
            locationName = locationName
        )

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            AdhanNotificationManager.NOTIFICATION_ID_ADHAN,
            notification,
            foregroundType
        )

        // Read settings and start playback
        serviceScope.launch {
            val app = applicationContext as? MusabbihApplication
            val prefsRepo = app?.prayerPreferencesRepository
            val settings = prefsRepo?.prayerSettingsFlow?.first()

            val source = settings?.audioSource ?: AudioSourceType.BUNDLED
            val customUri = settings?.customAudioUri
            val duration = settings?.playbackDurationSeconds ?: 30

            audioManager.playAdhan(
                audioSource = source,
                customUriString = customUri,
                durationSeconds = duration,
                onComplete = {
                    stopPlaybackAndService()
                }
            )
        }

        return START_NOT_STICKY
    }

    private fun stopPlaybackAndService() {
        audioManager.stopAdhan()
        notificationManager.cancelAdhanNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        audioManager.stopAdhan()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.musabbih.action.START_ADHAN"
        const val ACTION_STOP = "com.example.musabbih.action.STOP_ADHAN"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
    }
}
