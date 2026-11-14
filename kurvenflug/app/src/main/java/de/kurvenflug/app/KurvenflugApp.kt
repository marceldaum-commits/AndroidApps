package de.kurvenflug.app

import android.app.Application
import de.kurvenflug.app.ui.fx.Sfx
import de.kurvenflug.core.data.AppContainer

class KurvenflugApp : Application() {
    lateinit var container: AppContainer
        private set
    val sfx: Sfx by lazy { Sfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
