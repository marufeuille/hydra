package dev.marufeuille.hydra.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
class RecordScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun plusAndMinusChangeDraftWithoutChangingToday() {
        val state = mutableStateOf(readyRecordState())
        composeRule.setContent {
            HydraTheme {
                RecordContent(
                    state = state.value,
                    onOpenSettings = {},
                    onMinus = { state.value = state.value.copy(draftMl = (state.value.draftMl - 50).coerceAtLeast(0)) },
                    onPlus = { state.value = state.value.copy(draftMl = state.value.draftMl + 50) },
                    onSubmit = {},
                )
            }
        }

        composeRule.onNodeWithTag(HydraTestTags.RECORD_TODAY)
            .assertTextEquals("800 / 2000 ml")
        composeRule.onNodeWithTag(HydraTestTags.RECORD_DRAFT).assertTextEquals("0")

        composeRule.onNodeWithTag(HydraTestTags.RECORD_PLUS).performClick()
        composeRule.onNodeWithTag(HydraTestTags.RECORD_PLUS).performClick()

        composeRule.onNodeWithTag(HydraTestTags.RECORD_DRAFT).assertTextEquals("100")
        composeRule.onNodeWithTag(HydraTestTags.RECORD_TODAY)
            .assertTextEquals("900 / 2000 ml")

        composeRule.onNodeWithTag(HydraTestTags.RECORD_MINUS).performClick()
        composeRule.onNodeWithTag(HydraTestTags.RECORD_DRAFT).assertTextEquals("50")
        composeRule.onNodeWithTag(HydraTestTags.RECORD_TODAY)
            .assertTextEquals("850 / 2000 ml")
    }

    @Test
    fun submitSuccessResetsDraftAndUpdatesToday() {
        val state = mutableStateOf(readyRecordState().copy(draftMl = 250))
        composeRule.setContent {
            HydraTheme {
                RecordContent(
                    state = state.value,
                    onOpenSettings = {},
                    onMinus = {},
                    onPlus = {},
                    onSubmit = {
                        state.value = state.value.copy(todayMl = 1050, draftMl = 0)
                    },
                )
            }
        }

        composeRule.onNodeWithTag(HydraTestTags.RECORD_SUBMIT).assertIsEnabled()
        composeRule.onNodeWithTag(HydraTestTags.RECORD_SUBMIT).performClick()

        composeRule.onNodeWithTag(HydraTestTags.RECORD_TODAY)
            .assertTextEquals("1050 / 2000 ml")
        composeRule.onNodeWithTag(HydraTestTags.RECORD_DRAFT).assertTextEquals("0")
    }

    @Test
    fun submitIsDisabledWhenDraftIsZero() {
        val state = mutableStateOf(readyRecordState().copy(draftMl = 0))
        composeRule.setContent {
            HydraTheme {
                RecordContent(
                    state = state.value,
                    onOpenSettings = {},
                    onMinus = {},
                    onPlus = {},
                    onSubmit = {},
                )
            }
        }

        composeRule.onNodeWithTag(HydraTestTags.RECORD_SUBMIT).assertIsNotEnabled()
    }

    private fun readyRecordState() = RecordUiState(
        todayMl = 800,
        goalMl = 2000,
        draftMl = 0,
        status = HealthStatus.Ready,
    )
}
