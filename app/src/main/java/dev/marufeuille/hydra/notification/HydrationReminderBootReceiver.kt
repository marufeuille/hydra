package dev.marufeuille.hydra.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HydrationReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        HydrationReminderScheduler(context).rescheduleFromSavedState()
    }
}
