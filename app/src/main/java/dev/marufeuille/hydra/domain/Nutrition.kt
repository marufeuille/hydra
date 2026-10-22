package dev.marufeuille.hydra.domain

/** カロミルなどが Health Connect に書いた今日の栄養の合計（g）。スマホの companion が集計して送ってくる。 */
data class NutritionToday(
    val proteinG: Int = 0,
    val fatG: Int = 0,
    val carbsG: Int = 0,
) {
    fun grams(macro: Macro): Int = when (macro) {
        Macro.PROTEIN -> proteinG
        Macro.FAT -> fatG
        Macro.CARBS -> carbsG
    }
}

/** 目標は今はここで固定。設定画面はまだ無い。 */
enum class Macro(val label: String, val title: String, val goalG: Int) {
    PROTEIN("たんぱく質", "P", 90),
    FAT("脂質", "F", 60),
    CARBS("炭水化物", "C", 250),
}

/**
 * スマホに最新値を問い合わせる間隔。
 * 食事の記録はスマホ側から通知されないので、コンプリケーションの更新のついでに取りに行く。
 * 間隔を空けないと「問い合わせ → 受信 → 更新 → 問い合わせ」が止まらない。
 */
const val STATUS_REFRESH_INTERVAL_MILLIS = 10 * 60 * 1000L

fun needsStatusRefresh(lastReceivedAtMillis: Long?, nowMillis: Long): Boolean =
    lastReceivedAtMillis == null || nowMillis - lastReceivedAtMillis >= STATUS_REFRESH_INTERVAL_MILLIS
