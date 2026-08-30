package dev.marufeuille.hydra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Text
import dev.marufeuille.hydra.ui.theme.ButtonDark
import dev.marufeuille.hydra.ui.theme.TextPrimary
import dev.marufeuille.hydra.ui.theme.TextSecondary

@Composable
fun StepperButtons(
    minusEnabled: Boolean,
    plusEnabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusTestTag: String? = null,
    plusTestTag: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
    ) {
        StepButton(
            label = "−",
            enabled = minusEnabled,
            onClick = onMinus,
            testTag = minusTestTag,
        )
        StepButton(
            label = "+",
            enabled = plusEnabled,
            onClick = onPlus,
            testTag = plusTestTag,
        )
    }
}

@Composable
private fun StepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String?,
) {
    CompactButton(
        onClick = onClick,
        enabled = enabled,
        modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ButtonDark,
            contentColor = TextPrimary,
            disabledBackgroundColor = ButtonDark,
            disabledContentColor = TextSecondary.copy(alpha = 0.35f),
        ),
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
