package dev.marufeuille.hydra.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.marufeuille.hydra.domain.DRAFT_DEFAULT_ML
import dev.marufeuille.hydra.domain.GOAL_DEFAULT_ML
import dev.marufeuille.hydra.domain.NutritionToday
import dev.marufeuille.hydra.domain.clampDraft
import dev.marufeuille.hydra.domain.clampGoal
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.hydraDataStore: DataStore<Preferences> by preferencesDataStore(name = "hydra")

class PreferencesStore(context: Context) : GoalDraftStore {

    private val dataStore = context.applicationContext.hydraDataStore

    override suspend fun load(): StoredPrefs {
        val prefs = dataStore.data.first()
        return StoredPrefs(
            goalMl = clampGoal(prefs[GOAL_ML] ?: GOAL_DEFAULT_ML),
            draftMl = clampDraft(prefs[DRAFT_ML] ?: DRAFT_DEFAULT_ML),
            draftDate = parseDate(prefs[DRAFT_DATE]),
            cachedTodayMl = (prefs[CACHED_TODAY_ML] ?: 0).coerceAtLeast(0),
            cachedTodayDate = parseDate(prefs[CACHED_TODAY_DATE]),
            companionAvailable = prefs[COMPANION_AVAILABLE] ?: false,
            companionPermitted = prefs[COMPANION_PERMITTED] ?: false,
            nutritionPermitted = prefs[NUTRITION_PERMITTED] ?: false,
            cachedNutrition = NutritionToday(
                proteinG = (prefs[CACHED_PROTEIN_G] ?: 0).coerceAtLeast(0),
                fatG = (prefs[CACHED_FAT_G] ?: 0).coerceAtLeast(0),
                carbsG = (prefs[CACHED_CARBS_G] ?: 0).coerceAtLeast(0),
                kcal = (prefs[CACHED_KCAL] ?: 0).coerceAtLeast(0),
            ),
            cachedNutritionDate = parseDate(prefs[CACHED_NUTRITION_DATE]),
            statusReceivedAtMillis = prefs[STATUS_RECEIVED_AT],
        )
    }

    override suspend fun saveGoal(goalMl: Int) {
        dataStore.edit { it[GOAL_ML] = clampGoal(goalMl) }
    }

    override suspend fun saveDraft(draftMl: Int, date: LocalDate) {
        dataStore.edit {
            it[DRAFT_ML] = clampDraft(draftMl)
            it[DRAFT_DATE] = date.toString()
        }
    }

    override suspend fun saveCompanionStatus(
        available: Boolean,
        permitted: Boolean,
        todayMl: Int,
        date: LocalDate,
    ) {
        dataStore.edit {
            it[COMPANION_AVAILABLE] = available
            it[COMPANION_PERMITTED] = permitted
            it[CACHED_TODAY_ML] = todayMl.coerceAtLeast(0)
            it[CACHED_TODAY_DATE] = date.toString()
        }
    }

    override suspend fun saveNutritionStatus(
        permitted: Boolean,
        nutrition: NutritionToday,
        date: LocalDate,
        receivedAtMillis: Long,
    ) {
        dataStore.edit {
            it[NUTRITION_PERMITTED] = permitted
            it[CACHED_PROTEIN_G] = nutrition.proteinG.coerceAtLeast(0)
            it[CACHED_FAT_G] = nutrition.fatG.coerceAtLeast(0)
            it[CACHED_CARBS_G] = nutrition.carbsG.coerceAtLeast(0)
            it[CACHED_KCAL] = nutrition.kcal.coerceAtLeast(0)
            it[CACHED_NUTRITION_DATE] = date.toString()
            it[STATUS_RECEIVED_AT] = receivedAtMillis
        }
    }

    private fun parseDate(raw: String?): LocalDate? =
        raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private companion object {
        val GOAL_ML = intPreferencesKey("goal_ml")
        val DRAFT_ML = intPreferencesKey("draft_ml")
        val DRAFT_DATE = stringPreferencesKey("draft_date")
        val CACHED_TODAY_ML = intPreferencesKey("cached_today_ml")
        val CACHED_TODAY_DATE = stringPreferencesKey("cached_today_date")
        val COMPANION_AVAILABLE = booleanPreferencesKey("companion_available")
        val COMPANION_PERMITTED = booleanPreferencesKey("companion_permitted")
        val NUTRITION_PERMITTED = booleanPreferencesKey("nutrition_permitted")
        val CACHED_PROTEIN_G = intPreferencesKey("cached_protein_g")
        val CACHED_FAT_G = intPreferencesKey("cached_fat_g")
        val CACHED_CARBS_G = intPreferencesKey("cached_carbs_g")
        val CACHED_KCAL = intPreferencesKey("cached_kcal")
        val CACHED_NUTRITION_DATE = stringPreferencesKey("cached_nutrition_date")
        val STATUS_RECEIVED_AT = longPreferencesKey("status_received_at")
    }
}
