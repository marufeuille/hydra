package dev.marufeuille.hydra.domain

import java.time.LocalDate

const val STEP_ML = 100
const val DRAFT_MIN_ML = 0
const val DRAFT_MAX_ML = 2000
const val DRAFT_DEFAULT_ML = 100
const val GOAL_MIN_ML = 100
const val GOAL_MAX_ML = 5000
const val GOAL_DEFAULT_ML = 2000

/** 半円ゲージの軌道。左端（9時）が 0%、右端（3時）が 100%。 */
const val GAUGE_SWEEP_DEGREES = 180f

/** 丸端の点が残らないよう、これ以下の掃引は塗らない。 */
const val GAUGE_MIN_VISIBLE_SWEEP_DEGREES = 0.7f

fun clampDraft(ml: Int): Int = ml.coerceIn(DRAFT_MIN_ML, DRAFT_MAX_ML)

fun clampGoal(ml: Int): Int = ml.coerceIn(GOAL_MIN_ML, GOAL_MAX_ML)

fun stepDraft(currentMl: Int, deltaSteps: Int): Int =
    clampDraft(currentMl + deltaSteps * STEP_ML)

fun stepGoal(currentMl: Int, deltaSteps: Int): Int =
    clampGoal(currentMl + deltaSteps * STEP_ML)

fun canSubmit(draftMl: Int): Boolean = draftMl >= STEP_ML

/**
 * 今日の摂取量 ÷ 目標。0.0〜1.0 にクランプする。
 * 目標 0 は設定上起きないが、弧は 0% のままにする。
 */
fun progress(todayMl: Int, goalMl: Int): Float {
    if (goalMl <= 0) return 0f
    return (todayMl.toFloat() / goalMl).coerceIn(0f, 1f)
}

/** 進捗に対応する上側半円の掃引角。半分で 90°（12時）、到達で 180°（3時）。 */
fun gaugeSweepDegrees(todayMl: Int, goalMl: Int): Float =
    GAUGE_SWEEP_DEGREES * progress(todayMl, goalMl)

/**
 * 日付をまたいだらドラフトを初期値に戻す。同じ暦日なら保存値を使う。
 */
fun resolveDraft(storedMl: Int, storedDate: LocalDate?, today: LocalDate): Int {
    if (storedDate == null || storedDate != today) return DRAFT_DEFAULT_ML
    return clampDraft(storedMl)
}

enum class HealthStatus {
    Ready,
    NeedsPermission,
    Unavailable,
}

fun healthStatus(available: Boolean, permitted: Boolean): HealthStatus = when {
    !available -> HealthStatus.Unavailable
    !permitted -> HealthStatus.NeedsPermission
    else -> HealthStatus.Ready
}
