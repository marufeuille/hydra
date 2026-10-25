package dev.marufeuille.hydra.domain

/** カロミルなどが Health Connect に書いた今日の栄養の合計（g、カロリーは kcal）。スマホの companion が集計して送ってくる。 */
data class NutritionToday(
    val proteinG: Int = 0,
    val fatG: Int = 0,
    val carbsG: Int = 0,
    val kcal: Int = 0,
) {
    fun amount(macro: Macro): Int = when (macro) {
        Macro.CALORIES -> kcal
        Macro.PROTEIN -> proteinG
        Macro.FAT -> fatG
        Macro.CARBS -> carbsG
    }
}

/** 目標は今はここで固定（カロミルの目標に合わせている）。設定画面はまだ無い。 */
enum class Macro(val label: String, val title: String, val goal: Int, val unit: String) {
    // カロリーの目標は P / F / C の目標から計算（150×4 + 55×9 + 310×4）
    CALORIES("カロリー", "kcal", 2335, "kcal"),
    PROTEIN("たんぱく質", "P", 150, "g"),
    FAT("脂質", "F", 55, "g"),
    CARBS("炭水化物", "C", 310, "g"),
}

/**
 * スマホに最新値を問い合わせる間隔。
 * 食事の記録はスマホ側から通知されないので、コンプリケーションの更新のついでに取りに行く。
 * 間隔を空けないと「問い合わせ → 受信 → 更新 → 問い合わせ」が止まらない。
 */
const val STATUS_REFRESH_INTERVAL_MILLIS = 10 * 60 * 1000L

fun needsStatusRefresh(lastReceivedAtMillis: Long?, nowMillis: Long): Boolean =
    lastReceivedAtMillis == null || nowMillis - lastReceivedAtMillis >= STATUS_REFRESH_INTERVAL_MILLIS
