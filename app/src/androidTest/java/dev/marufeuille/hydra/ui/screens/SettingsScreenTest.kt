package dev.marufeuille.hydra.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.marufeuille.hydra.domain.HealthStatus
import dev.marufeuille.hydra.ui.HydraTestTags
import dev.marufeuille.hydra.ui.theme.HydraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun goalChangesIn100MlStepsAndBackIsAvailable() {
        val goal = mutableStateOf(2000)
        composeRule.setContent {
            HydraTheme {
                SettingsContent(
                    state = SettingsUiState(goalMl = goal.value, status = HealthStatus.Ready),
                    onMinus = { goal.value = (goal.value - 100).coerceAtLeast(100) },
                    onPlus = { goal.value = (goal.value + 100).coerceAtMost(5000) },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(HydraTestTags.SETTINGS_GOAL)
            .assertTextEquals("2000 ml")
        composeRule.onNodeWithTag(HydraTestTags.SETTINGS_PLUS).assertIsEnabled()
        composeRule.onNodeWithTag(HydraTestTags.SETTINGS_PLUS).performClick()
        composeRule.onNodeWithTag(HydraTestTags.SETTINGS_GOAL)
            .assertTextEquals("2100 ml")
        composeRule.onNodeWithTag(HydraTestTags.SETTINGS_MINUS).performClick()
        composeRule.onNodeWithTag(HydraTestTags.SETTINGS_GOAL)
            .assertTextEquals("2000 ml")
    }
}
