package dev.marufeuille.hydra.companion

import android.app.Application
import dev.marufeuille.hydra.companion.sync.CompanionRepository

class CompanionApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val repository: CompanionRepository = CompanionRepository(application)
}
