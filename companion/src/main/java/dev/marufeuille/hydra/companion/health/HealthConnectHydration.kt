package dev.marufeuille.hydra.companion.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

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
            val today = LocalDate.now(zone)
            val start = today.atStartOfDay(zone).toInstant()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant()
            val result = client.aggregate(
                AggregateRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            result[HydrationRecord.VOLUME_TOTAL]?.inMilliliters?.roundToInt() ?: 0
        }
    }

    /** 栄養の読み取りは水分とは別に許可を取る。許可しなくても水分は使える。 */
    suspend fun hasNutritionPermission(): Boolean {
        if (!isAvailable) return false
        return runCatching {
            client.permissionController.getGrantedPermissions().containsAll(NUTRITION_PERMISSIONS)
        }.getOrDefault(false)
    }

    /** 今日の P / F / C の合計（g）。カロミルなど他アプリが書いた記録も含む。 */
    suspend fun todayNutrition(zone: ZoneId = ZoneId.systemDefault()): Result<NutritionTotals> {
        if (!hasNutritionPermission()) return Result.failure(IllegalStateException("no permission"))
        return runCatching {
            val today = LocalDate.now(zone)
            val start = today.atStartOfDay(zone).toInstant()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant()
            val result = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        NutritionRecord.PROTEIN_TOTAL,
                        NutritionRecord.TOTAL_FAT_TOTAL,
                        NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            NutritionTotals(
                proteinG = result[NutritionRecord.PROTEIN_TOTAL]?.inGrams?.roundToInt() ?: 0,
                fatG = result[NutritionRecord.TOTAL_FAT_TOTAL]?.inGrams?.roundToInt() ?: 0,
                carbsG = result[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.inGrams?.roundToInt() ?: 0,
            )
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
        val NUTRITION_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(NutritionRecord::class),
        )
    }
}

data class NutritionTotals(
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
)
