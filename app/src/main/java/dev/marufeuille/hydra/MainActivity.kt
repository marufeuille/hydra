package dev.marufeuille.hydra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.marufeuille.hydra.ui.navigation.AppNavigation
import dev.marufeuille.hydra.ui.theme.HydraTheme

const val EXTRA_OPEN_SETTINGS = "dev.marufeuille.hydra.extra.OPEN_SETTINGS"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openSettings = intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true
        setContent {
            HydraTheme {
                AppNavigation(openSettings = openSettings)
            }
        }
    }
}
