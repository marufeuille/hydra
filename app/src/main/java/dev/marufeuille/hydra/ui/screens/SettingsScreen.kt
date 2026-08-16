package dev.marufeuille.hydra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Text
import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.ui.components.StepperButtons
import dev.marufeuille.hydra.ui.theme.ButtonDark
import dev.marufeuille.hydra.ui.theme.TextPrimary
import dev.marufeuille.hydra.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        onMinus = viewModel::decrementGoal,
        onPlus = viewModel::incrementGoal,
        onBack = onBack,
    )
}

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onBack: () -> Unit,
) {
    val statusText = when (state.status) {
        HealthStatus.Ready -> "スマホと連携済み"
        HealthStatus.NeedsPermission -> "スマホの Hydra で許可"
        HealthStatus.Unavailable -> "スマホの Hydra を開く"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("1日の目標", fontSize = 11.sp, color = TextSecondary)
        Text(
            text = "${state.goalMl} ml",
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
        )
        StepperButtons(
            minusEnabled = state.minusEnabled,
            plusEnabled = state.plusEnabled,
            onMinus = onMinus,
            onPlus = onPlus,
        )
        Text(
            text = statusText,
            fontSize = 11.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        CompactButton(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("戻る", fontSize = 12.sp, color = TextPrimary)
        }
    }
}
