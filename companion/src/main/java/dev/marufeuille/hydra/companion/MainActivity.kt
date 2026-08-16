package dev.marufeuille.hydra.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.marufeuille.hydra.companion.ui.CompanionTheme
import dev.marufeuille.hydra.companion.ui.HomeScreen
import dev.marufeuille.hydra.companion.ui.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as CompanionApplication).container.repository
        setContent {
            CompanionTheme {
                val vm: HomeViewModel = viewModel { HomeViewModel(repository) }
                HomeScreen(viewModel = vm)
            }
        }
    }
}
