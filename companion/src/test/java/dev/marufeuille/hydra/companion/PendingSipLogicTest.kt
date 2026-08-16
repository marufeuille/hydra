package dev.marufeuille.hydra.companion

import dev.marufeuille.hydra.companion.sync.HydrationSip
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingSipLogicTest {

    @Test
    fun `同じ id の sip は 1 件にまとめる`() {
        val first = HydrationSip("a", 200, 1L)
        val retry = HydrationSip("a", 200, 1L)
        val other = HydrationSip("b", 300, 2L)
        val merged = (listOf(first) + retry + other).distinctBy { it.id }
        assertEquals(listOf(first, other), merged)
    }
}
