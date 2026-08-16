package dev.marufeuille.hydra.companion.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.marufeuille.hydra.companion.sync.HydrationSip
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.pendingSipStore by preferencesDataStore(name = "hydra_pending_sips")

class PendingSipStore(context: Context) {
    private val dataStore = context.applicationContext.pendingSipStore

    suspend fun all(): List<HydrationSip> {
        val raw = dataStore.data.first()[PENDING] ?: "[]"
        return runCatching { parse(raw) }.getOrDefault(emptyList())
    }

    suspend fun add(sip: HydrationSip) {
        val next = (all() + sip).distinctBy { it.id }
        write(next)
    }

    suspend fun remove(id: String) {
        write(all().filterNot { it.id == id })
    }

    private suspend fun write(sips: List<HydrationSip>) {
        val array = JSONArray()
        sips.forEach { sip ->
            array.put(
                JSONObject()
                    .put("id", sip.id)
                    .put("volume_ml", sip.volumeMl)
                    .put("recorded_at", sip.recordedAtMillis),
            )
        }
        dataStore.edit { it[PENDING] = array.toString() }
    }

    private fun parse(raw: String): List<HydrationSip> {
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val id = obj.optString("id")
            val volume = obj.optInt("volume_ml")
            if (id.isBlank() || volume <= 0) null
            else HydrationSip(id, volume, obj.optLong("recorded_at"))
        }
    }

    private companion object {
        val PENDING = stringPreferencesKey("pending_sips")
    }
}
