package dev.marufeuille.hydra.complication

import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.domain.Macro
import org.junit.Assert.assertEquals
import org.junit.Test

class ComplicationModelTest {

    @Test
    fun `準備完了なら量を桁区切りで出す`() {
        val model = complicationUiModel(1250, 2000, HealthStatus.Ready)
        assertEquals(1250, model.value)
        assertEquals(2000, model.goal)
        assertEquals("1,250ml", model.text)
        assertEquals("水分", model.title)
    }

    @Test
    fun `目標を超えたら RANGED_VALUE だけ頭打ちにする`() {
        val model = complicationUiModel(2600, 2000, HealthStatus.Ready)
        assertEquals(2600, model.value)
        assertEquals(2000, model.rangedValue)
    }

    @Test
    fun `権限が無いときは量を出さない`() {
        val model = complicationUiModel(800, 2000, HealthStatus.NeedsPermission)
        assertEquals(0, model.value)
        assertEquals("—", model.text)
    }

    @Test
    fun `使えないときも量を出さない`() {
        val model = complicationUiModel(800, 2000, HealthStatus.Unavailable)
        assertEquals(0, model.rangedValue)
        assertEquals("—", model.text)
    }

    @Test
    fun `栄養はグラムと P F C の見出しで出す`() {
        val model = nutritionComplicationUiModel(Macro.PROTEIN, 62, HealthStatus.Ready, permitted = true)
        assertEquals(62, model.value)
        assertEquals(Macro.PROTEIN.goal, model.goal)
        assertEquals("62g", model.text)
        assertEquals("P", model.title)
    }

    @Test
    fun `カロリーは kcal で桁区切りにする`() {
        val model = nutritionComplicationUiModel(Macro.CALORIES, 1480, HealthStatus.Ready, permitted = true)
        assertEquals(1480, model.value)
        assertEquals(2335, model.goal)
        assertEquals("1,480kcal", model.text)
        assertEquals("kcal", model.title)
    }

    @Test
    fun `栄養の読み取りを許可していなければ出さない`() {
        val model = nutritionComplicationUiModel(Macro.CARBS, 180, HealthStatus.Ready, permitted = false)
        assertEquals(0, model.value)
        assertEquals("—", model.text)
        assertEquals("C", model.title)
    }
}
