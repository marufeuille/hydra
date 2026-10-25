package dev.marufeuille.hydra.complication

import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.domain.Macro
import java.text.NumberFormat
import java.util.Locale

data class ComplicationUiModel(
    val value: Int,
    val goal: Int,
    /** RANGED_VALUE 用。目標で頭打ちにする（GOAL_PROGRESS は超えてよい）。 */
    val rangedValue: Int,
    val text: String,
    val title: String,
    val contentDescription: String,
)

private fun format(n: Int): String = NumberFormat.getIntegerInstance(Locale.JAPAN).format(n)

/**
 * ウォッチフェイスに出す今日の水分。
 * スマホ側が使えない・権限が無いときは量を出さず「—」にする（タイルと同じ）。
 */
fun complicationUiModel(todayMl: Int, goalMl: Int, status: HealthStatus): ComplicationUiModel {
    if (status != HealthStatus.Ready) {
        return ComplicationUiModel(0, goalMl, 0, "—", "水分", "水分 未取得")
    }
    val today = format(todayMl)
    return ComplicationUiModel(
        value = todayMl,
        goal = goalMl,
        rangedValue = todayMl.coerceIn(0, goalMl),
        text = "${today}ml",
        title = "水分",
        contentDescription = "水分 ${today}ml / 目標 ${goalMl}ml",
    )
}

/**
 * 今日のカロリーと P / F / C。栄養の読み取りを許可していないときも「—」。
 * 水分の権限とは別なので、水分だけ使う場合は許可しなくてよい。
 */
fun nutritionComplicationUiModel(
    macro: Macro,
    amount: Int,
    status: HealthStatus,
    permitted: Boolean,
): ComplicationUiModel {
    if (status != HealthStatus.Ready || !permitted) {
        return ComplicationUiModel(0, macro.goal, 0, "—", macro.title, "${macro.label} 未取得")
    }
    val text = "${format(amount)}${macro.unit}"
    return ComplicationUiModel(
        value = amount,
        goal = macro.goal,
        rangedValue = amount.coerceIn(0, macro.goal),
        text = text,
        title = macro.title,
        contentDescription = "${macro.label} $text / 目標 ${format(macro.goal)}${macro.unit}",
    )
}
