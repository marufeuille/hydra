package dev.marufeuille.hydra.data

import dev.marufeuille.hydra.domain.DRAFT_DEFAULT_ML
import dev.marufeuille.hydra.domain.GOAL_DEFAULT_ML
import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.domain.NutritionToday
import dev.marufeuille.hydra.notification.HydrationReminder
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
        repo.adjustDraft(4)
        val snap = repo.snapshot()
        assertEquals(200, snap.draftMl)
        assertEquals(800, snap.todayMl)
        assertEquals(emptyList<Int>(), sender.sips)
    }

    @Test
    fun `Submit でドラフト量が 1 件送られ今日の合計が増える`() = runBlocking {
        val sender = FakeSender()
        val reminder = FakeReminder()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(800)), sender, clock, reminder = reminder)
        repo.adjustDraft(5)
        val result = repo.submit()
        assertTrue(result is SubmitResult.Written)
        assertEquals(listOf(250), sender.sips)
        assertEquals(listOf(clock.millis()), reminder.recordedAtMillis)
        assertEquals(1050, result.snapshot.todayMl)
        assertEquals(DRAFT_DEFAULT_ML, result.snapshot.draftMl)
    }

    @Test
    fun `マイナスしても過去の送信は消えない`() = runBlocking {
        val sender = FakeSender()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(800)), sender, clock)
        repo.adjustDraft(5)
        repo.submit()
        repo.adjustDraft(-1)
        assertEquals(listOf(250), sender.sips)
        assertEquals(1050, repo.snapshot().todayMl)
    }

    @Test
    fun `ドラフト 50ml は Submit できる`() = runBlocking {
        val sender = FakeSender()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(800)), sender, clock)
        repo.adjustDraft(1)
        val result = repo.submit()
        assertTrue(result is SubmitResult.Written)
        assertEquals(listOf(50), sender.sips)
        assertEquals(850, result.snapshot.todayMl)
        assertEquals(DRAFT_DEFAULT_ML, result.snapshot.draftMl)
    }

    @Test
    fun `ドラフト 0 では Submit できない`() = runBlocking {
        val sender = FakeSender()
        val repo = HydrationRepository(InMemoryPrefs(readyStored(0)), sender, clock)
        val result = repo.submit()
        assertTrue(result is SubmitResult.Rejected)
        assertEquals(emptyList<Int>(), sender.sips)
        assertEquals(0, result.snapshot.draftMl)
    }

    @Test
    fun `権限なしの Submit は送らずドラフトを維持する`() = runBlocking {
        val sender = FakeSender()
        val reminder = FakeReminder()
        val repo = HydrationRepository(
            InMemoryPrefs(readyStored(0).copy(companionPermitted = false)),
            sender,
            clock,
            reminder = reminder,
        )
        repo.adjustDraft(5)
        val result = repo.submit()
        assertTrue(result is SubmitResult.Failed)
        assertEquals(emptyList<Int>(), sender.sips)
        assertEquals(emptyList<Long>(), reminder.recordedAtMillis)
        assertEquals(250, result.snapshot.draftMl)
        assertEquals(HealthStatus.NeedsPermission, result.snapshot.status)
    }

    @Test
    fun `日付をまたぐとドラフトは 0ml に戻る`() = runBlocking {
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
        assertEquals(0, snap.draftMl)
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

    @Test
    fun `companion から届いた栄養を今日の分として持つ`() = runBlocking {
        val repo = HydrationRepository(InMemoryPrefs(readyStored(800)), FakeSender(), clock)
        repo.applyCompanionStatus(
            available = true,
            permitted = true,
            todayMl = 900,
            nutrition = NutritionStatus(permitted = true, today = NutritionToday(62, 38, 180, 1480)),
        )
        val snap = repo.snapshot()
        assertTrue(snap.nutritionPermitted)
        assertEquals(NutritionToday(62, 38, 180, 1480), snap.nutrition)
    }

    @Test
    fun `古い companion から栄養が来なければ未許可として扱う`() = runBlocking {
        val repo = HydrationRepository(InMemoryPrefs(readyStored(800)), FakeSender(), clock)
        repo.applyCompanionStatus(available = true, permitted = true, todayMl = 900)
        val snap = repo.snapshot()
        assertEquals(false, snap.nutritionPermitted)
        assertEquals(NutritionToday(), snap.nutrition)
    }

    @Test
    fun `日付が変わったら栄養は 0 に戻す`() = runBlocking {
        val prefs = InMemoryPrefs(
            readyStored(800).copy(
                nutritionPermitted = true,
                cachedNutrition = NutritionToday(62, 38, 180),
                cachedNutritionDate = today.minusDays(1),
            )
        )
        val repo = HydrationRepository(prefs, FakeSender(), clock)
        assertEquals(NutritionToday(), repo.snapshot().nutrition)
    }

    @Test
    fun `最新値の問い合わせは前回の受信から 10 分空ける`() = runBlocking {
        val sender = FakeSender()
        val prefs = InMemoryPrefs(readyStored(800).copy(statusReceivedAtMillis = clock.millis() - 5 * 60 * 1000))
        HydrationRepository(prefs, sender, clock).refreshIfStale()
        assertEquals(0, sender.statusRequests)

        val stale = InMemoryPrefs(readyStored(800).copy(statusReceivedAtMillis = clock.millis() - 11 * 60 * 1000))
        HydrationRepository(stale, sender, clock).refreshIfStale()
        assertEquals(1, sender.statusRequests)
    }

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
        override suspend fun saveNutritionStatus(
            permitted: Boolean,
            nutrition: NutritionToday,
            date: LocalDate,
            receivedAtMillis: Long,
        ) {
            value = value.copy(
                nutritionPermitted = permitted,
                cachedNutrition = nutrition,
                cachedNutritionDate = date,
                statusReceivedAtMillis = receivedAtMillis,
            )
        }
    }

    private class FakeSender : HydrationSender {
        val sips = mutableListOf<Int>()
        var statusRequests = 0
        override suspend fun sendSip(volumeMl: Int, recordedAtMillis: Long): Result<Unit> {
            sips += volumeMl
            return Result.success(Unit)
        }
        override suspend fun requestStatus(): Result<Unit> {
            statusRequests += 1
            return Result.success(Unit)
        }
    }

    private class FakeReminder : HydrationReminder {
        val recordedAtMillis = mutableListOf<Long>()

        override fun scheduleAfterSip(recordedAtMillis: Long) {
            this.recordedAtMillis += recordedAtMillis
        }
    }
}
