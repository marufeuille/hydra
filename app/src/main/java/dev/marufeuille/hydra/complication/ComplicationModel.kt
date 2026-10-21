package dev.marufeuille.hydra.complication

import dev.marufeuille.hydra.domain.HealthStatus
import java.text.NumberFormat
import java.util.Locale

data class ComplicationUiModel(
    val todayMl: Int,
    val goalMl: Int,
    /** RANGED_VALUE 用。目標で頭打ちにする（GOAL_PROGRESS は超えてよい）。 */
    val rangedMl: Int,
    val text: String,
    val contentDescription: String,
)

/**
 * ウォッチフェイスに出す今日の摂取量。
 * スマホ側が使えない・権限が無いときは量を出さず「—」にする（タイルと同じ）。
 */
fun complicationUiModel(todayMl: Int, goalMl: Int, status: HealthStatus): ComplicationUiModel {
    if (status != HealthStatus.Ready) {
        return ComplicationUiModel(0, goalMl, 0, "—", "水分 未取得")
    }
    val today = NumberFormat.getIntegerInstance(Locale.JAPAN).format(todayMl)
    return ComplicationUiModel(
        todayMl = todayMl,
        goalMl = goalMl,
        rangedMl = todayMl.coerceIn(0, goalMl),
        text = "${today}ml",
        contentDescription = "水分 ${today}ml / 目標 ${goalMl}ml",
    )
}
