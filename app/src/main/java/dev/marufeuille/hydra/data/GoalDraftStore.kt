package dev.marufeuille.hydra.data

import java.time.LocalDate

data class StoredPrefs(
    val goalMl: Int,
    val draftMl: Int,
    val draftDate: LocalDate?,
    val cachedTodayMl: Int,
    val cachedTodayDate: LocalDate?,
    val companionAvailable: Boolean,
    val companionPermitted: Boolean,
)

interface GoalDraftStore {
    suspend fun load(): StoredPrefs
    suspend fun saveGoal(goalMl: Int)
    suspend fun saveDraft(draftMl: Int, date: LocalDate)
    suspend fun saveCompanionStatus(available: Boolean, permitted: Boolean, todayMl: Int, date: LocalDate)
}
