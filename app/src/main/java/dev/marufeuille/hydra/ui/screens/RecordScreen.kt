package dev.marufeuille.hydra.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.ui.components.SemicircleGauge
import dev.marufeuille.hydra.ui.components.StepperButtons
import dev.marufeuille.hydra.ui.theme.Accent
import dev.marufeuille.hydra.ui.theme.ButtonDark
import dev.marufeuille.hydra.ui.theme.OnAccent
import dev.marufeuille.hydra.ui.theme.TextPrimary
import dev.marufeuille.hydra.ui.theme.TextSecondary

@Composable
fun RecordScreen(
    viewModel: RecordViewModel,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    RecordContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onMinus = viewModel::decrementDraft,
        onPlus = viewModel::incrementDraft,
        onSubmit = viewModel::submit,
    )
}

@Composable
internal fun RecordContent(
    state: RecordUiState,
    onOpenSettings: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSubmit: () -> Unit,
) {
    val header = when (state.status) {
        HealthStatus.NeedsPermission -> "許可が必要"
        HealthStatus.Unavailable -> "スマホが必要"
        HealthStatus.Ready -> "${state.todayMl} / ${state.goalMl} ml"
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SemicircleGauge(
            todayMl = state.todayMl,
            goalMl = state.goalMl,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = header,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(onClick = onOpenSettings)
                    .padding(4.dp),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${state.draftMl}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    text = " ml",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            StepperButtons(
                minusEnabled = state.minusEnabled,
                plusEnabled = state.plusEnabled,
                onMinus = onMinus,
                onPlus = onPlus,
                modifier = Modifier.padding(top = 6.dp),
            )
            Button(
                onClick = onSubmit,
                enabled = state.submitEnabled,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(width = 96.dp, height = 36.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Accent,
                    contentColor = OnAccent,
                    disabledBackgroundColor = ButtonDark,
                    disabledContentColor = TextSecondary,
                ),
            ) {
                Text("Submit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            state.message?.let {
                Text(it, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
