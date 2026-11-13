package de.wissensgarten.app

import android.app.Application
import de.wissensgarten.app.ui.fx.Sfx
import de.wissensgarten.core.data.AppContainer

class WissensgartenApp : Application() {
    lateinit var container: AppContainer
        private set

    val sfx: Sfx by lazy { Sfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
