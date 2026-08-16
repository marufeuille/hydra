package dev.marufeuille.hydra.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dev.marufeuille.hydra.HydraApplication
import kotlinx.coroutines.runBlocking

class HydrationStatusListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val updates = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == HydrationSync.STATUS_PATH }
            .map { DataMapItem.fromDataItem(it.dataItem).dataMap }
        if (updates.isEmpty()) return
        val latest = updates.last()
        val repository = (application as HydraApplication).container.repository
        runBlocking {
            repository.applyCompanionStatus(
                available = latest.getBoolean(HydrationSync.KEY_AVAILABLE),
                permitted = latest.getBoolean(HydrationSync.KEY_PERMITTED),
                todayMl = latest.getInt(HydrationSync.KEY_TODAY_ML),
            )
        }
    }
}
