package dev.marufeuille.hydra.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dev.marufeuille.hydra.HydraApplication
import dev.marufeuille.hydra.ui.screens.RecordScreen
import dev.marufeuille.hydra.ui.screens.RecordViewModel
import dev.marufeuille.hydra.ui.screens.SettingsScreen
import dev.marufeuille.hydra.ui.screens.SettingsViewModel

object Routes {
    const val RECORD = "record"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(openSettings: Boolean = false) {
    val navController = rememberSwipeDismissableNavController()
    val repository = (LocalContext.current.applicationContext as HydraApplication).container.repository

    LaunchedEffect(openSettings) {
        if (openSettings) {
            navController.navigate(Routes.SETTINGS)
        }
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Routes.RECORD,
    ) {
        composable(Routes.RECORD) {
            val recordViewModel: RecordViewModel = viewModel { RecordViewModel(repository) }
            LaunchedEffect(Unit) { recordViewModel.refresh() }
            RecordScreen(
                viewModel = recordViewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel(repository) }
            LaunchedEffect(Unit) { settingsViewModel.refresh() }
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
