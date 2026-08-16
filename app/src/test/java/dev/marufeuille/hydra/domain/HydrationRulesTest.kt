package dev.marufeuille.hydra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HydrationRulesTest {

    @Test
    fun `目標の初期値は 2000ml`() {
        assertEquals(2000, GOAL_DEFAULT_ML)
    }

    @Test
    fun `ドラフトの初期値は 100ml`() {
        assertEquals(100, DRAFT_DEFAULT_ML)
    }

    @Test
    fun `プラスマイナスは 100ml 刻みでドラフトだけを動かす`() {
        assertEquals(200, stepDraft(100, 1))
        assertEquals(300, stepDraft(100, 2))
        assertEquals(0, stepDraft(100, -1))
        assertEquals(0, stepDraft(0, -1))
    }

    @Test
    fun `ドラフトは 0 から 2000 に収める`() {
        assertEquals(0, stepDraft(0, -1))
        assertEquals(2000, stepDraft(2000, 1))
        assertEquals(2000, clampDraft(9999))
    }

    @Test
    fun `ドラフト 0ml では Submit できない`() {
        assertFalse(canSubmit(0))
        assertTrue(canSubmit(100))
        assertTrue(canSubmit(300))
    }

    @Test
    fun `目標は 100ml 刻みで 100 から 5000`() {
        assertEquals(2100, stepGoal(2000, 1))
        assertEquals(1900, stepGoal(2000, -1))
        assertEquals(100, stepGoal(100, -1))
        assertEquals(5000, stepGoal(5000, 1))
    }

    @Test
    fun `摂取量が目標の半分なら弧は 12 時まで`() {
        assertEquals(0.5f, progress(1000, 2000))
        assertEquals(90f, gaugeSweepDegrees(1000, 2000))
    }

    @Test
    fun `目標到達で弧は右端まで伸びる`() {
        assertEquals(1.0f, progress(2000, 2000))
        assertEquals(180f, gaugeSweepDegrees(2000, 2000))
    }

    @Test
    fun `超過しても弧は右端のまま数字は超過を出してよい`() {
        assertEquals(1.0f, progress(2200, 2000))
        assertEquals(180f, gaugeSweepDegrees(2200, 2000))
    }

    @Test
    fun `目標 0 なら弧は 0 パーセントのまま`() {
        assertEquals(0f, progress(800, 0))
        assertEquals(0f, gaugeSweepDegrees(800, 0))
    }

    @Test
    fun `日付をまたいだらドラフトは 100ml に戻す`() {
        val today = LocalDate.of(2026, 8, 16)
        assertEquals(100, resolveDraft(700, LocalDate.of(2026, 8, 15), today))
        assertEquals(700, resolveDraft(700, today, today))
        assertEquals(100, resolveDraft(700, null, today))
    }

    @Test
    fun `権限と可用性からタイルの状態を決める`() {
        assertEquals(HealthStatus.Ready, healthStatus(available = true, permitted = true))
        assertEquals(HealthStatus.NeedsPermission, healthStatus(available = true, permitted = false))
        assertEquals(HealthStatus.Unavailable, healthStatus(available = false, permitted = false))
    }
}
