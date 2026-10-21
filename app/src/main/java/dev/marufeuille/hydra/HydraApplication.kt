package dev.marufeuille.hydra

import android.app.Application
import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import dev.marufeuille.hydra.complication.HydrationComplicationService
import dev.marufeuille.hydra.data.HydrationRepository
import dev.marufeuille.hydra.data.PreferencesStore
import dev.marufeuille.hydra.notification.HydrationReminderScheduler
import dev.marufeuille.hydra.sync.WearHydrationSender
import dev.marufeuille.hydra.tile.HydrationTileService

class HydraApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.reminderScheduler.ensureScheduled()
    }
}

class AppContainer(application: Application) {
    val reminderScheduler = HydrationReminderScheduler(application)
    val repository: HydrationRepository = HydrationRepository(
        prefs = PreferencesStore(application),
        sender = WearHydrationSender(application),
        reminder = reminderScheduler,
        onChanged = {
            TileService.getUpdater(application).requestUpdate(HydrationTileService::class.java)
            ComplicationDataSourceUpdateRequester
                .create(application, ComponentName(application, HydrationComplicationService::class.java))
                .requestUpdateAll()
        },
    )
}
