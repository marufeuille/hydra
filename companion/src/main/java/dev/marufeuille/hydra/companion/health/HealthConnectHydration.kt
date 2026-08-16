package dev.marufeuille.hydra.companion.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.ZoneId

class HealthConnectHydration(context: Context) {

    private val appContext = context.applicationContext

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(appContext) }

    suspend fun hasPermissions(): Boolean {
        if (!isAvailable) return false
        return runCatching {
            client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
        }.getOrDefault(false)
    }

    suspend fun todayTotalMl(zone: ZoneId = ZoneId.systemDefault()): Result<Int> {
        if (!hasPermissions()) return Result.failure(IllegalStateException("no permission"))
        return runCatching {
            val today = java.time.LocalDate.now(zone)
            val start = today.atStartOfDay(zone).toInstant()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant()
            val result = client.aggregate(
                AggregateRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            result[HydrationRecord.VOLUME_TOTAL]?.inMilliliters?.toInt() ?: 0
        }
    }

    suspend fun insertMl(volumeMl: Int, recordedAtMillis: Long, clientRecordId: String): Result<Unit> {
        if (!hasPermissions()) return Result.failure(IllegalStateException("no permission"))
        return runCatching {
            val start = Instant.ofEpochMilli(recordedAtMillis)
            val zone = ZoneId.systemDefault().rules.getOffset(start)
            val record = HydrationRecord(
                startTime = start,
                startZoneOffset = zone,
                endTime = start.plusSeconds(1),
                endZoneOffset = zone,
                volume = Volume.milliliters(volumeMl.toDouble()),
                metadata = Metadata.manualEntry(
                    clientRecordId = clientRecordId,
                    device = Device(type = Device.TYPE_WATCH),
                ),
            )
            client.insertRecords(listOf(record))
            Unit
        }
    }

    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class),
        )
    }
}
