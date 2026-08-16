package dev.marufeuille.hydra.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class WearHydrationSender(context: Context) : HydrationSender {
    private val dataClient = Wearable.getDataClient(context.applicationContext)

    override suspend fun sendSip(volumeMl: Int, recordedAtMillis: Long): Result<Unit> = runCatching {
        val id = UUID.randomUUID().toString()
        val request = PutDataMapRequest.create("${HydrationSync.SIP_PATH_PREFIX}/$id").apply {
            dataMap.putString(HydrationSync.KEY_ID, id)
            dataMap.putInt(HydrationSync.KEY_VOLUME_ML, volumeMl)
            dataMap.putLong(HydrationSync.KEY_RECORDED_AT, recordedAtMillis)
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).awaitTask()
        Unit
    }

    override suspend fun requestStatus(): Result<Unit> = runCatching {
        val request = PutDataMapRequest.create(HydrationSync.REQUEST_PATH).apply {
            dataMap.putLong(HydrationSync.KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).awaitTask()
        Unit
    }
}

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
