package dev.marufeuille.hydra.companion.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permittedHealthConnectIsShownAndPermissionButtonIsEnabled() {
        composeRule.setContent {
            CompanionTheme {
                HomeContent(
                    state = HomeUiState(
                        available = true,
                        permitted = true,
                        todayMl = 800,
                        status = "今日 800 ml。ウォッチと連携済み",
                    ),
                    onRequestPermission = {},
                )
            }
        }

        composeRule.onNodeWithTag(CompanionTestTags.HEALTH_CONNECT_STATUS)
            .assertTextEquals("連携済み")
        composeRule.onNodeWithTag(CompanionTestTags.HEALTH_CONNECT_PERMISSION)
            .assertIsEnabled()
    }

    @Test
    fun unavailableHealthConnectDisablesPermissionButton() {
        composeRule.setContent {
            CompanionTheme {
                HomeContent(
                    state = HomeUiState(
                        available = false,
                        permitted = false,
                        status = "この端末では Health Connect が使えません",
                    ),
                    onRequestPermission = {},
                )
            }
        }

        composeRule.onNodeWithTag(CompanionTestTags.HEALTH_CONNECT_STATUS)
            .assertTextEquals("未連携")
        composeRule.onNodeWithTag(CompanionTestTags.HEALTH_CONNECT_PERMISSION)
            .assertIsNotEnabled()
    }
}
