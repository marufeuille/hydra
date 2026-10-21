package dev.marufeuille.hydra.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.marufeuille.hydra.HydraApplication
import dev.marufeuille.hydra.MainActivity
import dev.marufeuille.hydra.R
import dev.marufeuille.hydra.domain.HealthStatus

/**
 * 今日の摂取量 / 目標をウォッチフェイスに出す。
 * リング向けに GOAL_PROGRESS と RANGED_VALUE、文字だけのスロット向けに SHORT_TEXT。
 * タップで記録画面。
 */
class HydrationComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(type, complicationUiModel(1250, 2000, HealthStatus.Ready))

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val snapshot = (application as HydraApplication).container.repository.snapshot()
        return build(request.complicationType, complicationUiModel(snapshot.todayMl, snapshot.goalMl, snapshot.status))
    }

    private fun build(type: ComplicationType, model: ComplicationUiModel): ComplicationData? {
        val text = PlainComplicationText.Builder(model.text).build()
        val description = PlainComplicationText.Builder(model.contentDescription).build()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_notification)).build()
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return when (type) {
            ComplicationType.GOAL_PROGRESS ->
                GoalProgressComplicationData.Builder(model.todayMl.toFloat(), model.goalMl.toFloat(), description)
                    .setText(text)
                    .setMonochromaticImage(icon)
                    .setTapAction(tap)
                    .build()

            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(model.rangedMl.toFloat(), 0f, model.goalMl.toFloat(), description)
                    .setText(text)
                    .setMonochromaticImage(icon)
                    .setTapAction(tap)
                    .build()

            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(text, description)
                    .setMonochromaticImage(icon)
                    .setTapAction(tap)
                    .build()

            else -> null
        }
    }
}
