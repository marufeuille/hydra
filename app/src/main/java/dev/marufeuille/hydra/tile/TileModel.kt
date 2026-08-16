package dev.marufeuille.hydra.tile

import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.domain.gaugeSweepDegrees

data class TileUiModel(
    val todayMl: Int,
    val goalMl: Int,
    val sweepDegrees: Float,
    val status: HealthStatus,
) {
    val opensSettings: Boolean = status != HealthStatus.Ready
}

fun tileUiModel(todayMl: Int, goalMl: Int, status: HealthStatus): TileUiModel =
    TileUiModel(
        todayMl = todayMl,
        goalMl = goalMl,
        sweepDegrees = gaugeSweepDegrees(todayMl, goalMl),
        status = status,
    )
