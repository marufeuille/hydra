package dev.marufeuille.hydra.complication

import dev.marufeuille.hydra.domain.HealthStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ComplicationModelTest {

    @Test
    fun `準備完了なら量を桁区切りで出す`() {
        val model = complicationUiModel(1250, 2000, HealthStatus.Ready)
        assertEquals(1250, model.todayMl)
        assertEquals(2000, model.goalMl)
        assertEquals("1,250ml", model.text)
    }

    @Test
    fun `目標を超えたら RANGED_VALUE だけ頭打ちにする`() {
        val model = complicationUiModel(2600, 2000, HealthStatus.Ready)
        assertEquals(2600, model.todayMl)
        assertEquals(2000, model.rangedMl)
    }

    @Test
    fun `権限が無いときは量を出さない`() {
        val model = complicationUiModel(800, 2000, HealthStatus.NeedsPermission)
        assertEquals(0, model.todayMl)
        assertEquals("—", model.text)
    }

    @Test
    fun `使えないときも量を出さない`() {
        val model = complicationUiModel(800, 2000, HealthStatus.Unavailable)
        assertEquals(0, model.rangedMl)
        assertEquals("—", model.text)
    }
}
