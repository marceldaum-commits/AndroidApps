package de.quanta.app

import android.app.Application
import de.quanta.app.ui.fx.Sfx
import de.quanta.core.data.AppContainer

class QuantaApp : Application() {
    lateinit var container: AppContainer
        private set
    val sfx: Sfx by lazy { Sfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
