package dev.marufeuille.hydra.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HydrationReminderQuietHoursTest {
    private val zone = ZoneId.of("Asia/Tokyo")
    private val date = LocalDate.of(2026, 8, 16)

    @Test
    fun `21時から翌朝6時までは通知を遅らせる`() {
        val at = millisAt(date, 21)
        val expected = millisAt(date.plusDays(1), 6)

        assertTrue(HydrationReminderQuietHours.isQuietTime(at, zone))
        assertEquals(expected, HydrationReminderQuietHours.deferIfQuietTime(at, zone))
    }

    @Test
    fun `朝6時前の通知は当日6時に遅らせる`() {
        val at = millisAt(date, 5, 59)
        val expected = millisAt(date, 6)

        assertTrue(HydrationReminderQuietHours.isQuietTime(at, zone))
        assertEquals(expected, HydrationReminderQuietHours.deferIfQuietTime(at, zone))
    }

    @Test
    fun `朝6時以降は通知時刻を変更しない`() {
        val at = millisAt(date, 6)

        assertFalse(HydrationReminderQuietHours.isQuietTime(at, zone))
        assertEquals(at, HydrationReminderQuietHours.deferIfQuietTime(at, zone))
    }

    @Test
    fun `非通知期間中に発火した通知は6時に再予約する`() {
        val at = millisAt(date, 5, 59)
        val expected = millisAt(date, 6)

        assertEquals(
            expected,
            HydrationReminderQuietHours.nextReminderAt(
                nowMillis = at,
                intervalMillis = 60 * 60 * 1000L,
                zone = zone,
            ),
        )
    }

    private fun millisAt(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
}
