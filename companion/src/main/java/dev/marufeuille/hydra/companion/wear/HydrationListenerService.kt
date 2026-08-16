package dev.marufeuille.hydra.companion.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dev.marufeuille.hydra.companion.CompanionApplication
import dev.marufeuille.hydra.companion.sync.HydrationSip
import dev.marufeuille.hydra.companion.sync.HydrationSync
import kotlinx.coroutines.runBlocking

class HydrationListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val repository = (application as CompanionApplication).container.repository
        val sips = mutableListOf<Pair<android.net.Uri, HydrationSip>>()
        var requested = false

        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val path = event.dataItem.uri.path.orEmpty()
            when {
                path.startsWith(HydrationSync.SIP_PATH_PREFIX) -> {
                    val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val sip = HydrationSip(
                        id = map.getString(HydrationSync.KEY_ID).orEmpty(),
                        volumeMl = map.getInt(HydrationSync.KEY_VOLUME_ML),
                        recordedAtMillis = map.getLong(HydrationSync.KEY_RECORDED_AT),
                    )
                    if (sip.id.isNotBlank() && sip.volumeMl > 0) {
                        sips += event.dataItem.uri to sip
                    }
                }
                path == HydrationSync.REQUEST_PATH -> requested = true
            }
        }

        if (sips.isEmpty() && !requested) return

        runBlocking {
            sips.forEach { (uri, sip) ->
                repository.receive(sip)
                runCatching { Wearable.getDataClient(applicationContext).deleteDataItems(uri) }
            }
            if (requested || sips.isEmpty()) {
                repository.refreshStatus()
            }
        }
    }
}
