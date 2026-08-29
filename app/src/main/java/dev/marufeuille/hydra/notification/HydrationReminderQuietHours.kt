package dev.marufeuille.hydra.notification

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

internal object HydrationReminderQuietHours {
    const val QUIET_START_HOUR = 21
    const val QUIET_END_HOUR = 6

    private val quietStart = LocalTime.of(QUIET_START_HOUR, 0)
    private val quietEnd = LocalTime.of(QUIET_END_HOUR, 0)

    fun isQuietTime(atMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val localTime = Instant.ofEpochMilli(atMillis).atZone(zone).toLocalTime()
        return isQuietTime(localTime)
    }

    fun deferIfQuietTime(
        triggerAtMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val dateTime = Instant.ofEpochMilli(triggerAtMillis).atZone(zone)
        val localTime = dateTime.toLocalTime()
        if (!isQuietTime(localTime)) {
            return triggerAtMillis
        }

        val quietEndDate = if (localTime >= quietStart) {
            dateTime.toLocalDate().plusDays(1)
        } else {
            dateTime.toLocalDate()
        }
        return quietEndDate
            .atTime(quietEnd)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    fun nextReminderAt(
        nowMillis: Long,
        intervalMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val candidate = if (isQuietTime(nowMillis, zone)) {
            nowMillis
        } else {
            nowMillis + intervalMillis
        }
        return deferIfQuietTime(candidate, zone)
    }

    private fun isQuietTime(localTime: LocalTime): Boolean =
        localTime >= quietStart || localTime < quietEnd
}
