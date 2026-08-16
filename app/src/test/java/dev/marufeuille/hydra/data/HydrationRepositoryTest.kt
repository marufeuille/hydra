package dev.marufeuille.hydra.data

import dev.marufeuille.hydra.domain.DRAFT_DEFAULT_ML
import dev.marufeuille.hydra.domain.GOAL_DEFAULT_ML
import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.sync.HydrationSender
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class HydrationRepositoryTest {

    private val zone = ZoneId.of("Asia/Tokyo")
    private val today = LocalDate.of(2026, 8, 16)
    private val clock = Clock.fixed(today.atTime(15, 0).atZone(zone).toInstant(), zone)

    @Test
    fun `プラスは送信せずドラフトだけ増やす`() = runBlocking {
        val sender = FakeSender()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(800)), sender, clock)
        repo.adjustDraft(2)
        val snap = repo.snapshot()
        assertEquals(300, snap.draftMl)
        assertEquals(800, snap.todayMl)
        assertEquals(emptyList<Int>(), sender.sips)
    }

    @Test
    fun `Submit でドラフト量が 1 件送られ今日の合計が増える`() = runBlocking {
        val sender = FakeSender()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(800)), sender, clock)
        repo.adjustDraft(2)
        val result = repo.submit()
        assertTrue(result is SubmitResult.Written)
        assertEquals(listOf(300), sender.sips)
        assertEquals(1100, result.snapshot.todayMl)
        assertEquals(DRAFT_DEFAULT_ML, result.snapshot.draftMl)
    }

    @Test
    fun `マイナスしても過去の送信は消えない`() = runBlocking {
        val sender = FakeSender()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(800)), sender, clock)
        repo.adjustDraft(2)
        repo.submit()
        repo.adjustDraft(-1)
        assertEquals(listOf(300), sender.sips)
        assertEquals(1100, repo.snapshot().todayMl)
    }

    @Test
    fun `ドラフト 0 では Submit できない`() = runBlocking {
        val sender = FakeSender()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(0)), sender, clock)
        repo.adjustDraft(-1)
        val result = repo.submit()
        assertTrue(result is SubmitResult.Rejected)
        assertEquals(emptyList<Int>(), sender.sips)
        assertEquals(0, result.snapshot.draftMl)
    }

    @Test
    fun `権限なしの Submit は送らずドラフトを維持する`() = runBlocking {
        val sender = FakeSender()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(0).copy(companionPermitted = false)), sender, clock)
        repo.adjustDraft(2)
        val result = repo.submit()
        assertTrue(result is SubmitResult.Failed)
        assertEquals(emptyList<Int>(), sender.sips)
        assertEquals(300, result.snapshot.draftMl)
        assertEquals(HealthStatus.NeedsPermission, result.snapshot.status)
    }

    @Test
    fun `日付をまたぐとドラフトは 100ml に戻る`() = runBlocking {
        val prefs = InMemoryPrefs(
            StoredPrefs(
                goalMl = GOAL_DEFAULT_ML,
                draftMl = 700,
                draftDate = today.minusDays(1),
                cachedTodayMl = 500,
                cachedTodayDate = today.minusDays(1),
                companionAvailable = true,
                companionPermitted = true,
            )
        )
        val repo = HydrationRepository(prefs, FakeSender(), clock)
        val snap = repo.snapshot()
        assertEquals(100, snap.draftMl)
        assertEquals(0, snap.todayMl)
    }

    @Test
    fun `目標変更は端末内に残り摂取量は変えない`() = runBlocking {
        val sender = FakeSender()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(800)), sender, clock)
        val snap = repo.adjustGoal(1)
        assertEquals(2100, snap.goalMl)
        assertEquals(800, snap.todayMl)
        assertEquals(emptyList<Int>(), sender.sips)
    }

    @Test
    fun `companion の状態が来ると今日の合計と権限が更新される`() = runBlocking {
        val repo = HydrationRepository(InMemoryPrefs(readyStored(0).copy(companionAvailable = false)), FakeSender(), clock)
        assertEquals(HealthStatus.Unavailable, repo.snapshot().status)
        repo.applyCompanionStatus(available = true, permitted = true, todayMl = 900)
        val snap = repo.snapshot()
        assertEquals(HealthStatus.Ready, snap.status)
        assertEquals(900, snap.todayMl)
    }

    private fun readyStored(todayMl: Int) = StoredPrefs(
        goalMl = GOAL_DEFAULT_ML,
        draftMl = DRAFT_DEFAULT_ML,
        draftDate = today,
        cachedTodayMl = todayMl,
        cachedTodayDate = today,
        companionAvailable = true,
        companionPermitted = true,
    )

    private class InMemoryPrefs(initial: StoredPrefs) : GoalDraftStore {
        private var value = initial
        override suspend fun load(): StoredPrefs = value
        override suspend fun saveGoal(goalMl: Int) {
            value = value.copy(goalMl = goalMl)
        }
        override suspend fun saveDraft(draftMl: Int, date: LocalDate) {
            value = value.copy(draftMl = draftMl, draftDate = date)
        }
        override suspend fun saveCompanionStatus(
            available: Boolean,
            permitted: Boolean,
            todayMl: Int,
            date: LocalDate,
        ) {
            value = value.copy(
                companionAvailable = available,
                companionPermitted = permitted,
                cachedTodayMl = todayMl,
                cachedTodayDate = date,
            )
        }
    }

    private class FakeSender : HydrationSender {
        val sips = mutableListOf<Int>()
        override suspend fun sendSip(volumeMl: Int, recordedAtMillis: Long): Result<Unit> {
            sips += volumeMl
            return Result.success(Unit)
        }
        override suspend fun requestStatus(): Result<Unit> = Result.success(Unit)
    }
}
