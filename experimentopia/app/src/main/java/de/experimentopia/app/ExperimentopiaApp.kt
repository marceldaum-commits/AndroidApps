package de.experimentopia.app

import android.app.Application
import de.experimentopia.app.AndroidSfx
import de.experimentopia.core.data.AppContainer

class ExperimentopiaApp : Application() {
    lateinit var container: AppContainer
        private set
    val sfx: AndroidSfx by lazy { AndroidSfx(this).also { it.enabled = container.settings.sound.value } }
    val music: AndroidMusic by lazy { AndroidMusic(this) }
    val reader: AndroidReader by lazy { AndroidReader(this) }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
