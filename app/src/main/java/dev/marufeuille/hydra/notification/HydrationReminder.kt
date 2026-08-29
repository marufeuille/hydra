package dev.marufeuille.hydra.notification

/**
 * 水分を記録したあと、次のリマインダーを予約するための境界。
 *
 * Repository が Android の AlarmManager に直接依存しないようにする。
 */
interface HydrationReminder {
    fun scheduleAfterSip(recordedAtMillis: Long)
}

object NoOpHydrationReminder : HydrationReminder {
    override fun scheduleAfterSip(recordedAtMillis: Long) = Unit
}
