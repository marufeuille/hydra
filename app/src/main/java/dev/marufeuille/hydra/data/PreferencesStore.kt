package dev.marufeuille.hydra.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.marufeuille.hydra.domain.DRAFT_DEFAULT_ML
import dev.marufeuille.hydra.domain.GOAL_DEFAULT_ML
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
    }
}
