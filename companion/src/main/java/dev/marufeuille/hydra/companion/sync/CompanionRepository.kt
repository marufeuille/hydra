package dev.marufeuille.hydra.companion.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dev.marufeuille.hydra.companion.data.PendingSipStore
import dev.marufeuille.hydra.companion.health.HealthConnectHydration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CompanionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val health = HealthConnectHydration(appContext)
    private val pending = PendingSipStore(appContext)
    private val dataClient by lazy { Wearable.getDataClient(appContext) }

    val healthConnectAvailable: Boolean get() = health.isAvailable

    suspend fun healthConnectPermitted(): Boolean = health.hasPermissions()

    suspend fun todayMl(): Int = health.todayTotalMl().getOrDefault(0)

    suspend fun receive(sip: HydrationSip) {
        pending.add(sip)
        flushPending()
        pushStatus()
    }

    suspend fun onPermissionChanged() {
        flushPending()
        pushStatus()
    }

    suspend fun refreshStatus() {
        pushStatus()
    }

    suspend fun flushPending(): Int {
        if (!health.hasPermissions()) return 0
        var written = 0
        pending.all().forEach { sip ->
            val result = health.insertMl(sip.volumeMl, sip.recordedAtMillis, sip.id)
            if (result.isSuccess) {
                pending.remove(sip.id)
                written += 1
            }
        }
        return written
    }

    suspend fun pushStatus() {
        val available = health.isAvailable
        val permitted = runCatching { health.hasPermissions() }.getOrDefault(false)
        val todayMl = if (permitted) health.todayTotalMl().getOrDefault(0) else 0
        val nutritionPermitted = runCatching { health.hasNutritionPermission() }.getOrDefault(false)
        val nutrition = if (nutritionPermitted) health.todayNutrition().getOrNull() else null
        val request = PutDataMapRequest.create(HydrationSync.STATUS_PATH).apply {
            dataMap.putInt(HydrationSync.KEY_TODAY_ML, todayMl)
            dataMap.putBoolean(HydrationSync.KEY_PERMITTED, permitted)
            dataMap.putBoolean(HydrationSync.KEY_AVAILABLE, available)
            dataMap.putBoolean(HydrationSync.KEY_NUTRITION_PERMITTED, nutritionPermitted)
            dataMap.putInt(HydrationSync.KEY_PROTEIN_G, nutrition?.proteinG ?: 0)
            dataMap.putInt(HydrationSync.KEY_FAT_G, nutrition?.fatG ?: 0)
            dataMap.putInt(HydrationSync.KEY_CARBS_G, nutrition?.carbsG ?: 0)
            dataMap.putLong(HydrationSync.KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        runCatching { dataClient.putDataItem(request).awaitTask() }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
