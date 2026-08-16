package dev.marufeuille.hydra.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.marufeuille.hydra.data.HydrationRepository
import dev.marufeuille.hydra.data.HydrationSnapshot
import dev.marufeuille.hydra.data.SubmitResult
import dev.marufeuille.hydra.domain.DRAFT_DEFAULT_ML
import dev.marufeuille.hydra.domain.DRAFT_MAX_ML
import dev.marufeuille.hydra.domain.DRAFT_MIN_ML
import dev.marufeuille.hydra.domain.GOAL_DEFAULT_ML
import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.domain.canSubmit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordUiState(
    val todayMl: Int = 0,
    val goalMl: Int = GOAL_DEFAULT_ML,
    val draftMl: Int = DRAFT_DEFAULT_ML,
    val status: HealthStatus = HealthStatus.NeedsPermission,
    val submitting: Boolean = false,
    val message: String? = null,
) {
    val submitEnabled: Boolean = canSubmit(draftMl) && !submitting
    val minusEnabled: Boolean = draftMl > DRAFT_MIN_ML
    val plusEnabled: Boolean = draftMl < DRAFT_MAX_ML
}

class RecordViewModel(private val repository: HydrationRepository) : ViewModel() {

    private val _ui = MutableStateFlow(RecordUiState())
    val ui: StateFlow<RecordUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshFromCompanion()
            show(repository.snapshot())
        }
    }

    fun incrementDraft() = adjustDraft(1)

    fun decrementDraft() = adjustDraft(-1)

    fun submit() {
        if (!_ui.value.submitEnabled) return
        viewModelScope.launch {
            _ui.update { it.copy(submitting = true, message = null) }
            val result = repository.submit()
            val message = (result as? SubmitResult.Failed)?.let { submitErrorMessage(it.snapshot.status) }
            show(result.snapshot, submitting = false, message = message)
        }
    }

    private fun adjustDraft(deltaSteps: Int) {
        viewModelScope.launch { show(repository.adjustDraft(deltaSteps)) }
    }

    private fun show(
        snapshot: HydrationSnapshot,
        submitting: Boolean = false,
        message: String? = null,
    ) {
        _ui.value = RecordUiState(
            todayMl = snapshot.todayMl,
            goalMl = snapshot.goalMl,
            draftMl = snapshot.draftMl,
            status = snapshot.status,
            submitting = submitting,
            message = message,
        )
    }
}

private fun submitErrorMessage(status: HealthStatus): String = when (status) {
    HealthStatus.Unavailable -> "スマホの Hydra を開く"
    else -> "スマホの Hydra で許可"
}
