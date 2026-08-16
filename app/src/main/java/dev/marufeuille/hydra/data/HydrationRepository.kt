package dev.marufeuille.hydra.data

import dev.marufeuille.hydra.domain.DRAFT_DEFAULT_ML
import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.domain.canSubmit
import dev.marufeuille.hydra.domain.healthStatus
import dev.marufeuille.hydra.domain.resolveDraft
import dev.marufeuille.hydra.domain.stepDraft
import dev.marufeuille.hydra.domain.stepGoal
import dev.marufeuille.hydra.sync.HydrationSender
import java.time.Clock
import java.time.LocalDate

data class HydrationSnapshot(
    val todayMl: Int,
    val goalMl: Int,
    val draftMl: Int,
    val status: HealthStatus,
)

class HydrationRepository(
    private val prefs: GoalDraftStore,
    private val sender: HydrationSender,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val onChanged: () -> Unit = {},
) {
    suspend fun snapshot(): HydrationSnapshot {
        val today = today()
        val stored = prefs.load()
        val draftMl = resolveDraft(stored.draftMl, stored.draftDate, today)
        if (draftMl != stored.draftMl || stored.draftDate != today) {
            prefs.saveDraft(draftMl, today)
        }
        return stored.toSnapshot(today, draftMl)
    }

    suspend fun refreshFromCompanion() {
        sender.requestStatus()
    }

    suspend fun applyCompanionStatus(available: Boolean, permitted: Boolean, todayMl: Int) {
        prefs.saveCompanionStatus(available, permitted, todayMl, today())
        onChanged()
    }

    suspend fun adjustDraft(deltaSteps: Int): HydrationSnapshot {
        val current = snapshot()
        val next = stepDraft(current.draftMl, deltaSteps)
        prefs.saveDraft(next, today())
        return current.copy(draftMl = next)
    }

    suspend fun adjustGoal(deltaSteps: Int): HydrationSnapshot {
        val current = snapshot()
        val next = stepGoal(current.goalMl, deltaSteps)
        prefs.saveGoal(next)
        onChanged()
        return current.copy(goalMl = next)
    }

    /**
     * ドラフトをスマホへ送る。Health Connect への書き込みは companion 側。
     * キューイングに成功したらドラフトを 100ml に戻し、今日の表示を足す。
     * 権限なし・送信失敗では送らず、ドラフトは維持する。
     */
    suspend fun submit(): SubmitResult {
        val before = snapshot()
        if (!canSubmit(before.draftMl)) {
            return SubmitResult.Rejected(before)
        }
        if (before.status == HealthStatus.NeedsPermission) {
            return SubmitResult.Failed(before)
        }
        val sent = sender.sendSip(before.draftMl, clock.millis())
        if (sent.isFailure) {
            return SubmitResult.Failed(before)
        }
        val today = today()
        prefs.saveDraft(DRAFT_DEFAULT_ML, today)
        val stored = prefs.load()
        prefs.saveCompanionStatus(
            available = stored.companionAvailable,
            permitted = stored.companionPermitted,
            todayMl = before.todayMl + before.draftMl,
            date = today,
        )
        onChanged()
        sender.requestStatus()
        return SubmitResult.Written(snapshot())
    }

    private fun StoredPrefs.toSnapshot(today: LocalDate, draftMl: Int) = HydrationSnapshot(
        todayMl = if (cachedTodayDate == today) cachedTodayMl else 0,
        goalMl = goalMl,
        draftMl = draftMl,
        status = healthStatus(companionAvailable, companionPermitted),
    )

    private fun today(): LocalDate = LocalDate.now(clock)
}

sealed class SubmitResult {
    abstract val snapshot: HydrationSnapshot

    data class Written(override val snapshot: HydrationSnapshot) : SubmitResult()
    data class Failed(override val snapshot: HydrationSnapshot) : SubmitResult()
    data class Rejected(override val snapshot: HydrationSnapshot) : SubmitResult()
}
