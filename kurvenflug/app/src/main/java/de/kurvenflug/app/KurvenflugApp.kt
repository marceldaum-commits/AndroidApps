package de.kurvenflug.app

import android.app.Application
import de.kurvenflug.app.ui.fx.AndroidSfx
import de.kurvenflug.core.data.AppContainer

class KurvenflugApp : Application() {
    lateinit var container: AppContainer
        private set
    val music: de.kurvenflug.app.ui.fx.BackgroundMusic by lazy { de.kurvenflug.app.ui.fx.BackgroundMusic(this) }
    /** Vorlesen: Gerätestimme bzw. „Thorsten“ (in beiden Fassungen ladbar, nur auf Wunsch) */
    val reader by lazy { de.kurvenflug.app.ui.fx.AndroidReader(this, canLoadPack = true) }
    val sfx: AndroidSfx by lazy { AndroidSfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
