package dev.marufeuille.hydra.tile

import dev.marufeuille.hydra.domain.HealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileModelTest {

    @Test
    fun `準備完了なら記録画面を開く`() {
        val model = tileUiModel(1000, 2000, HealthStatus.Ready)
        assertEquals(1000, model.todayMl)
        assertEquals(2000, model.goalMl)
        assertFalse(model.opensSettings)
        assertEquals(90f, model.sweepDegrees, 0.01f)
    }

    @Test
    fun `権限が無いときは設定画面を開く`() {
        val model = tileUiModel(0, 2000, HealthStatus.NeedsPermission)
        assertTrue(model.opensSettings)
    }

    @Test
    fun `使えないときも設定画面を開く`() {
        val model = tileUiModel(0, 2000, HealthStatus.Unavailable)
        assertTrue(model.opensSettings)
    }
}
