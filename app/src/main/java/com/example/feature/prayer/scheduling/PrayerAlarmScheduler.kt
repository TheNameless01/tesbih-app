package com.example.feature.prayer.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MusabbihApplication
import com.example.feature.prayer.data.repository.PrayerTimesRepository
import com.example.feature.prayer.domain.model.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

object PrayerAlarmScheduler {

    fun reconcileAlarms(context: Context) {
        val app = context.applicationContext as? MusabbihApplication ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.prayerPreferencesRepository.prayerSettingsFlow.first()
                if (!settings.isConfigured || !settings.masterAdhanEnabled) {
                    cancelAllAlarms(context)
                    return@launch
                }

                val now = LocalDate.now()
                val upcomingEntities = app.database.prayerDao().getUpcomingPrayerTimes(
                    startDateString = now.toString(),
                    limit = 7
                )

                if (upcomingEntities.isEmpty()) {
                    return@launch
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val nowMillis = System.currentTimeMillis()

                for (entity in upcomingEntities) {
                    val date = try { LocalDate.parse(entity.dateString) } catch (_: Exception) { continue }
                    val schedule = app.prayerTimesRepository.mapEntityToDailySchedule(entity, settings, date)
                    val zoneId = try { ZoneId.of(entity.timezone) } catch (_: Exception) { ZoneId.systemDefault() }

                    for (item in schedule.prayers) {
                        // Skip sunrise (not a prayer)
                        if (!item.type.isPrayer) continue

                        // Check individual switch
                        if (!settings.individualAdhanSwitches.isEnabledFor(item.type)) {
                            cancelSingleAlarm(context, entity.dateString, item.type)
                            continue
                        }

                        val triggerMillis = item.epochMillis

                        // Only schedule future alarms (buffer 5 seconds)
                        if (triggerMillis > (nowMillis + 5000L)) {
                            scheduleSingleAlarm(
                                context = context,
                                alarmManager = alarmManager,
                                triggerMillis = triggerMillis,
                                dateString = entity.dateString,
                                prayerType = item.type,
                                prayerTime = item.adjustedTime,
                                locationName = schedule.locationName
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun scheduleSingleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerMillis: Long,
        dateString: String,
        prayerType: PrayerType,
        prayerTime: String,
        locationName: String
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.arabicName)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayerTime)
            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, locationName)
        }

        val requestCode = getUniqueRequestCode(dateString, prayerType)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun cancelSingleAlarm(context: Context, dateString: String, prayerType: PrayerType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val requestCode = getUniqueRequestCode(dateString, prayerType)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAllAlarms(context: Context) {
        val now = LocalDate.now()
        for (i in 0..14) {
            val dateStr = now.plusDays(i.toLong()).toString()
            for (type in PrayerType.prayersOnly) {
                cancelSingleAlarm(context, dateStr, type)
            }
        }
    }

    private fun getUniqueRequestCode(dateString: String, prayerType: PrayerType): Int {
        return abs(dateString.hashCode() * 31 + prayerType.ordinal)
    }
}
