package dev.marufeuille.hydra.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class HydrationReminderScheduler(context: Context) : HydrationReminder {
    private val appContext = context.applicationContext
    private val reminderPrefs = appContext
        .createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun scheduleAfterSip(recordedAtMillis: Long) {
        reminderPrefs.edit()
            .putLong(LAST_ACTIVITY_AT, recordedAtMillis)
            .apply()
        HydrationReminderReceiver.cancelNotification(appContext)
        val nextReminderAt = (recordedAtMillis + REMINDER_INTERVAL_MILLIS)
            .coerceAtLeast(System.currentTimeMillis() + MINIMUM_ALARM_DELAY_MILLIS)
        scheduleAt(nextReminderAt)
    }

    fun ensureScheduled() {
        val now = System.currentTimeMillis()
        val existingReminderAt = reminderPrefs.getLong(NEXT_REMINDER_AT, NO_TIMESTAMP)
        if (existingReminderAt > now) {
            scheduleAt(existingReminderAt)
            return
        }
        val lastActivityAt = reminderPrefs.getLong(LAST_ACTIVITY_AT, NO_TIMESTAMP)
        val baseline = if (lastActivityAt == NO_TIMESTAMP) {
            now.also {
                reminderPrefs.edit().putLong(LAST_ACTIVITY_AT, it).apply()
            }
        } else {
            lastActivityAt
        }
        scheduleAt(
            (baseline + REMINDER_INTERVAL_MILLIS)
                .coerceAtLeast(now + MINIMUM_ALARM_DELAY_MILLIS)
        )
    }

    fun rescheduleFromSavedState() {
        val now = System.currentTimeMillis()
        val lastActivityAt = reminderPrefs.getLong(LAST_ACTIVITY_AT, NO_TIMESTAMP)
        val baseline = if (lastActivityAt == NO_TIMESTAMP) {
            now.also {
                reminderPrefs.edit().putLong(LAST_ACTIVITY_AT, it).apply()
            }
        } else {
            lastActivityAt
        }
        scheduleAt(
            (baseline + REMINDER_INTERVAL_MILLIS)
                .coerceAtLeast(now + MINIMUM_ALARM_DELAY_MILLIS)
        )
    }

    fun scheduleNextFromNow() {
        scheduleAt(System.currentTimeMillis() + REMINDER_INTERVAL_MILLIS)
    }

    fun cancel() {
        reminderPendingIntent(PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
        reminderPrefs.edit().remove(NEXT_REMINDER_AT).apply()
        NotificationManagerCompat.from(appContext).cancel(HydrationReminderReceiver.NOTIFICATION_ID)
    }

    private fun scheduleAt(triggerAtMillis: Long) {
        reminderPendingIntent(PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
        val pendingIntent = reminderPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        reminderPrefs.edit().putLong(NEXT_REMINDER_AT, triggerAtMillis).apply()
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }

    private fun reminderPendingIntent(extraFlags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            Intent(appContext, HydrationReminderReceiver::class.java),
            extraFlags or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val REMINDER_INTERVAL_MILLIS = 60 * 60 * 1000L
        private const val PREFERENCES_NAME = "hydration_reminder"
        private const val LAST_ACTIVITY_AT = "last_activity_at"
        private const val NEXT_REMINDER_AT = "next_reminder_at"
        private const val NO_TIMESTAMP = -1L
        private const val MINIMUM_ALARM_DELAY_MILLIS = 1_000L
        private const val REQUEST_CODE = 1001
    }
}
