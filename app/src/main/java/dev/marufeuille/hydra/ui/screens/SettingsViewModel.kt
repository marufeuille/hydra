package dev.marufeuille.hydra.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.marufeuille.hydra.data.HydrationRepository
import dev.marufeuille.hydra.data.HydrationSnapshot
import dev.marufeuille.hydra.domain.GOAL_DEFAULT_ML
import dev.marufeuille.hydra.domain.GOAL_MAX_ML
import dev.marufeuille.hydra.domain.GOAL_MIN_ML
import dev.marufeuille.hydra.domain.HealthStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val goalMl: Int = GOAL_DEFAULT_ML,
    val status: HealthStatus = HealthStatus.NeedsPermission,
) {
    val minusEnabled: Boolean = goalMl > GOAL_MIN_ML
    val plusEnabled: Boolean = goalMl < GOAL_MAX_ML
}

class SettingsViewModel(private val repository: HydrationRepository) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { show(repository.snapshot()) }
    }

    fun incrementGoal() = adjustGoal(1)

    fun decrementGoal() = adjustGoal(-1)

    private fun adjustGoal(deltaSteps: Int) {
        viewModelScope.launch { show(repository.adjustGoal(deltaSteps)) }
    }

    private fun show(snapshot: HydrationSnapshot) {
        _ui.value = SettingsUiState(goalMl = snapshot.goalMl, status = snapshot.status)
    }
}
