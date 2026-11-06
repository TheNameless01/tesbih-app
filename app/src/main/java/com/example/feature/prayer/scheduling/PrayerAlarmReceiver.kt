package com.example.feature.prayer.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.MusabbihApplication
import com.example.feature.prayer.audio.AdhanPlaybackService
import com.example.feature.prayer.domain.model.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerTypeName = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: return
        val prayerType = try { PrayerType.valueOf(prayerTypeName) } catch (_: Exception) { return }
        val prayerArabicName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: prayerType.arabicName
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val locationName = intent.getStringExtra(EXTRA_LOCATION_NAME) ?: ""

        val app = context.applicationContext as? MusabbihApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.prayerPreferencesRepository.prayerSettingsFlow.first()

                // Master switch check
                if (!settings.masterAdhanEnabled) {
                    return@launch
                }

                // Individual prayer switch check
                if (!settings.individualAdhanSwitches.isEnabledFor(prayerType)) {
                    return@launch
                }

                // Launch foreground playback service
                val serviceIntent = Intent(context, AdhanPlaybackService::class.java).apply {
                    action = AdhanPlaybackService.ACTION_START
                    putExtra(AdhanPlaybackService.EXTRA_PRAYER_NAME, prayerArabicName)
                    putExtra(AdhanPlaybackService.EXTRA_PRAYER_TIME, prayerTime)
                    putExtra(AdhanPlaybackService.EXTRA_LOCATION_NAME, locationName)
                }

                ContextCompat.startForegroundService(context, serviceIntent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
    }
}
