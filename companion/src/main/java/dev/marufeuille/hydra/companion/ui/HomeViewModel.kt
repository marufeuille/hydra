package dev.marufeuille.hydra.companion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.marufeuille.hydra.companion.sync.CompanionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val available: Boolean,
    val permitted: Boolean = false,
    val todayMl: Int = 0,
    val status: String,
)

class HomeViewModel(private val repository: CompanionRepository) : ViewModel() {

    private val _ui = MutableStateFlow(
        HomeUiState(
            available = repository.healthConnectAvailable,
            status = if (repository.healthConnectAvailable) {
                "ウォッチの Submit を Health Connect に書きます"
            } else {
                "この端末では Health Connect が使えません"
            },
        )
    )
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val permitted = runCatching { repository.healthConnectPermitted() }.getOrDefault(false)
            val todayMl = if (permitted) runCatching { repository.todayMl() }.getOrDefault(0) else 0
            _ui.value = HomeUiState(
                available = repository.healthConnectAvailable,
                permitted = permitted,
                todayMl = todayMl,
                status = when {
                    !repository.healthConnectAvailable -> "この端末では Health Connect が使えません"
                    !permitted -> "水分の読み取り・書き込みを許可してください"
                    else -> "今日 $todayMl ml。ウォッチと連携済み"
                },
            )
            repository.refreshStatus()
        }
    }

    fun onPermissionResult() {
        viewModelScope.launch {
            repository.onPermissionChanged()
            refresh()
        }
    }
}
