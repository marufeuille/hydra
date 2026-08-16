package dev.marufeuille.hydra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.marufeuille.hydra.domain.GAUGE_MIN_VISIBLE_SWEEP_DEGREES
import dev.marufeuille.hydra.domain.GAUGE_SWEEP_DEGREES
import dev.marufeuille.hydra.domain.gaugeSweepDegrees
import dev.marufeuille.hydra.ui.theme.GaugeFill
import dev.marufeuille.hydra.ui.theme.GaugeTrack

/** Android Canvas では 0° が 3 時、正方向が時計回り。180° から 180° 掃引すると上側半円（9時→12時→3時）。 */
private const val GAUGE_START_DEGREES = 180f

@Composable
fun SemicircleGauge(
    todayMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
) {
    val sweep = gaugeSweepDegrees(todayMl, goalMl)
    Canvas(modifier = modifier) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2f + 8.dp.toPx()
        val diameter = size.minDimension - inset * 2f
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f,
        )
        val arcSize = Size(diameter, diameter)
        val style = Stroke(width = stroke, cap = StrokeCap.Round)
        fun drawGauge(color: Color, sweepAngle: Float) {
            drawArc(
                color = color,
                startAngle = GAUGE_START_DEGREES,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style,
            )
        }
        drawGauge(GaugeTrack, GAUGE_SWEEP_DEGREES)
        if (sweep > GAUGE_MIN_VISIBLE_SWEEP_DEGREES) {
            drawGauge(GaugeFill, sweep)
        }
    }
}
