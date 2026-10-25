package dev.marufeuille.hydra.data

import dev.marufeuille.hydra.domain.NutritionToday
import java.time.LocalDate

data class StoredPrefs(
    val goalMl: Int,
    val draftMl: Int,
    val draftDate: LocalDate?,
    val cachedTodayMl: Int,
    val cachedTodayDate: LocalDate?,
    val companionAvailable: Boolean,
    val companionPermitted: Boolean,
    val nutritionPermitted: Boolean = false,
    val cachedNutrition: NutritionToday = NutritionToday(),
    val cachedNutritionDate: LocalDate? = null,
    val statusReceivedAtMillis: Long? = null,
)

interface GoalDraftStore {
    suspend fun load(): StoredPrefs
    suspend fun saveGoal(goalMl: Int)
    suspend fun saveDraft(draftMl: Int, date: LocalDate)
    suspend fun saveCompanionStatus(available: Boolean, permitted: Boolean, todayMl: Int, date: LocalDate)
    suspend fun saveNutritionStatus(permitted: Boolean, nutrition: NutritionToday, date: LocalDate, receivedAtMillis: Long)
}
