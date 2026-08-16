package dev.marufeuille.hydra.sync

interface HydrationSender {
    suspend fun sendSip(volumeMl: Int, recordedAtMillis: Long): Result<Unit>
    suspend fun requestStatus(): Result<Unit>
}
