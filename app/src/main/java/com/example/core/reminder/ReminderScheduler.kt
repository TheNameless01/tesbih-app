package com.example.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val reminderType = inputData.getString("REMINDER_TYPE") ?: "GENERAL"
        showNotification(reminderType)
        return Result.success()
    }

    private fun showNotification(reminderType: String) {
        val channelId = "musabbih_reminders_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تذكير الأذكار والتسبيح",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات ورد الأذكار اليومي والتسبيح"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = when (reminderType) {
            "MORNING" -> "أذكار الصباح ☀️"
            "EVENING" -> "أذكار المساء 🌙"
            else -> "ورد الذكر والتسبيح 📿"
        }

        val message = when (reminderType) {
            "MORNING" -> "أصبحنا وأصبح الملك لله.. حان وقت وردك من أذكار الصباح والتسبيح"
            "EVENING" -> "أمسينا وأمسى الملك لله.. حان وقت وردك من أذكار المساء والاستغفار"
            else -> "ألا بذكر الله تطمئن القلوب.. افتح مُسَبِّح لوردك اليومي"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (reminderType == "MORNING") 1001 else 1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(if (reminderType == "MORNING") 101 else 102, notification)
    }
}

object ReminderScheduler {

    private const val WORK_MORNING = "musabbih_morning_reminder"
    private const val WORK_EVENING = "musabbih_evening_reminder"

    fun scheduleMorningReminder(context: Context, hour: Int, minute: Int, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_MORNING)
            return
        }

        val initialDelay = calculateInitialDelayMinutes(hour, minute)
        val data = androidx.work.workDataOf("REMINDER_TYPE" to "MORNING")

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .setInputData(data)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_MORNING,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun scheduleEveningReminder(context: Context, hour: Int, minute: Int, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_EVENING)
            return
        }

        val initialDelay = calculateInitialDelayMinutes(hour, minute)
        val data = androidx.work.workDataOf("REMINDER_TYPE" to "EVENING")

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .setInputData(data)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_EVENING,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun calculateInitialDelayMinutes(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val diffMillis = target.timeInMillis - now.timeInMillis
        return (diffMillis / (1000 * 60)).coerceAtLeast(1)
    }
}
