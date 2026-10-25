package dev.marufeuille.hydra

import android.app.Application
import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import dev.marufeuille.hydra.complication.CaloriesComplicationService
import dev.marufeuille.hydra.complication.CarbsComplicationService
import dev.marufeuille.hydra.complication.FatComplicationService
import dev.marufeuille.hydra.complication.HydrationComplicationService
import dev.marufeuille.hydra.complication.ProteinComplicationService
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
            COMPLICATION_SERVICES.forEach { service ->
                ComplicationDataSourceUpdateRequester
                    .create(application, ComponentName(application, service))
                    .requestUpdateAll()
            }
        },
    )
}

private val COMPLICATION_SERVICES = listOf(
    HydrationComplicationService::class.java,
    CaloriesComplicationService::class.java,
    ProteinComplicationService::class.java,
    FatComplicationService::class.java,
    CarbsComplicationService::class.java,
)
