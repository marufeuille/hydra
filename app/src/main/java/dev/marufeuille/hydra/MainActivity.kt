package dev.marufeuille.hydra

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dev.marufeuille.hydra.ui.navigation.AppNavigation
import dev.marufeuille.hydra.ui.theme.HydraTheme

const val EXTRA_OPEN_SETTINGS = "dev.marufeuille.hydra.extra.OPEN_SETTINGS"

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        val openSettings = intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true
        setContent {
            HydraTheme {
                AppNavigation(openSettings = openSettings)
            }
        }
    }

    private fun requestNotificationPermission() {
        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || permissionGranted) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
