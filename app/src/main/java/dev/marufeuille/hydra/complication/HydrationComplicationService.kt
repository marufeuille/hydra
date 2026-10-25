package dev.marufeuille.hydra.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
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
import dev.marufeuille.hydra.data.HydrationSnapshot
import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.domain.Macro

/**
 * 今日の値 / 目標をウォッチフェイスに出す共通部分。
 * リング向けに GOAL_PROGRESS と RANGED_VALUE、文字だけのスロット向けに SHORT_TEXT。
 * タップで記録画面。
 */
abstract class MetricComplicationService : SuspendingComplicationDataSourceService() {

    @get:DrawableRes
    protected abstract val iconRes: Int
    protected abstract val previewModel: ComplicationUiModel
    protected abstract fun model(snapshot: HydrationSnapshot): ComplicationUiModel

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, previewModel)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val repository = (application as HydraApplication).container.repository
        repository.refreshIfStale()
        return build(request.complicationType, model(repository.snapshot()))
    }

    private fun build(type: ComplicationType, model: ComplicationUiModel): ComplicationData? {
        val text = PlainComplicationText.Builder(model.text).build()
        val title = PlainComplicationText.Builder(model.title).build()
        val description = PlainComplicationText.Builder(model.contentDescription).build()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, iconRes)).build()
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return when (type) {
            ComplicationType.GOAL_PROGRESS ->
                GoalProgressComplicationData.Builder(model.value.toFloat(), model.goal.toFloat(), description)
                    .setText(text)
                    .setTitle(title)
                    .setMonochromaticImage(icon)
                    .setTapAction(tap)
                    .build()

            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(model.rangedValue.toFloat(), 0f, model.goal.toFloat(), description)
                    .setText(text)
                    .setTitle(title)
                    .setMonochromaticImage(icon)
                    .setTapAction(tap)
                    .build()

            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(text, description)
                    .setTitle(title)
                    .setMonochromaticImage(icon)
                    .setTapAction(tap)
                    .build()

            else -> null
        }
    }
}

class HydrationComplicationService : MetricComplicationService() {
    override val iconRes = R.drawable.ic_notification
    override val previewModel = complicationUiModel(1250, 2000, HealthStatus.Ready)
    override fun model(snapshot: HydrationSnapshot) =
        complicationUiModel(snapshot.todayMl, snapshot.goalMl, snapshot.status)
}

abstract class NutritionComplicationService(private val macro: Macro) : MetricComplicationService() {
    override val iconRes = R.drawable.ic_nutrition
    override val previewModel = nutritionComplicationUiModel(macro, macro.goal * 2 / 3, HealthStatus.Ready, true)
    override fun model(snapshot: HydrationSnapshot) = nutritionComplicationUiModel(
        macro = macro,
        amount = snapshot.nutrition.amount(macro),
        status = snapshot.status,
        permitted = snapshot.nutritionPermitted,
    )
}

class CaloriesComplicationService : NutritionComplicationService(Macro.CALORIES)

class ProteinComplicationService : NutritionComplicationService(Macro.PROTEIN)

class FatComplicationService : NutritionComplicationService(Macro.FAT)

class CarbsComplicationService : NutritionComplicationService(Macro.CARBS)
