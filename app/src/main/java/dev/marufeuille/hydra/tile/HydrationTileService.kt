package dev.marufeuille.hydra.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.ARC_ANCHOR_START
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.STROKE_CAP_ROUND
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import dev.marufeuille.hydra.EXTRA_OPEN_SETTINGS
import dev.marufeuille.hydra.HydraApplication
import dev.marufeuille.hydra.MainActivity
import dev.marufeuille.hydra.domain.GAUGE_MIN_VISIBLE_SWEEP_DEGREES
import dev.marufeuille.hydra.domain.GAUGE_SWEEP_DEGREES
import dev.marufeuille.hydra.domain.HealthStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

private const val RESOURCES_VERSION = "1"
private const val TRACK = 0xFF3F3F46.toInt()
private const val FILL = 0xFF6EA8FF.toInt()
private const val TEXT_PRIMARY = 0xFFE8E8EA.toInt()
private const val TEXT_SECONDARY = 0xFF8B8B90.toInt()

/**
 * 今日の摂取量 / 目標と上側半円ゲージを出すタイル。
 * タップで記録画面。権限が無いときは設定画面。
 */
class HydrationTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> = scope.future {
        val snapshot = (application as HydraApplication).container.repository.snapshot()
        val model = tileUiModel(snapshot.todayMl, snapshot.goalMl, snapshot.status)
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(tileLayout(this@HydrationTileService, model))
            )
            .build()
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = scope.future {
        ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    }

    private fun tileLayout(context: Context, model: TileUiModel): LayoutElement {
        val column = Column.Builder()
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .addContent(caption(context, statusLabel(model)))
            .addContent(
                Text.Builder(context, if (model.status == HealthStatus.Ready) "${model.todayMl}" else "—")
                    .setTypography(Typography.TYPOGRAPHY_DISPLAY3)
                    .setColor(argb(TEXT_PRIMARY))
                    .build()
            )
            .addContent(caption(context, "/ ${model.goalMl} ml"))
            .build()

        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(launchClickable(model.opensSettings))
                    .build()
            )
            .addContent(gaugeArc(GAUGE_SWEEP_DEGREES, TRACK))
            .apply {
                if (model.sweepDegrees > GAUGE_MIN_VISIBLE_SWEEP_DEGREES && model.status == HealthStatus.Ready) {
                    addContent(gaugeArc(model.sweepDegrees, FILL))
                }
            }
            .addContent(column)
            .build()
    }

    private fun statusLabel(model: TileUiModel): String = when (model.status) {
        HealthStatus.Ready -> "今日"
        HealthStatus.NeedsPermission -> "許可が必要"
        HealthStatus.Unavailable -> "スマホが必要"
    }

    private fun caption(context: Context, text: String): LayoutElement =
        Text.Builder(context, text)
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setColor(argb(TEXT_SECONDARY))
            .build()

    /**
     * ProtoLayout の Arc は 0° が 12 時・時計回り。
     * 270° 起点で 180° 掃引すると 9時→12時→3時の上側半円になる。
     */
    private fun gaugeArc(sweep: Float, color: Int): LayoutElement =
        Arc.Builder()
            .setAnchorAngle(degrees(270f))
            .setAnchorType(ARC_ANCHOR_START)
            .setVerticalAlign(VERTICAL_ALIGN_CENTER)
            .addContent(
                ArcLine.Builder()
                    .setLength(degrees(sweep))
                    .setThickness(dp(10f))
                    .setColor(argb(color))
                    .setStrokeCap(STROKE_CAP_ROUND)
                    .build()
            )
            .build()

    private fun launchClickable(openSettings: Boolean): ModifiersBuilders.Clickable {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(packageName)
            .setClassName(MainActivity::class.java.name)
            .addKeyToExtraMapping(
                EXTRA_OPEN_SETTINGS,
                ActionBuilders.AndroidBooleanExtra.Builder().setValue(openSettings).build(),
            )
            .build()
        return ModifiersBuilders.Clickable.Builder()
            .setId(if (openSettings) "settings" else "record")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity).build())
            .build()
    }
}
