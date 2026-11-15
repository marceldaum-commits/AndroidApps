package de.raumlicht.app

import android.app.Application
import de.raumlicht.app.ui.fx.Sfx
import de.raumlicht.core.data.AppContainer

class RaumlichtApp : Application() {
    lateinit var container: AppContainer
        private set
    val music: de.raumlicht.app.ui.fx.BackgroundMusic by lazy { de.raumlicht.app.ui.fx.BackgroundMusic(this) }
    val voice: de.raumlicht.app.ui.fx.LuxVoice by lazy { de.raumlicht.app.ui.fx.LuxVoice(this) { speaking -> music.duck(speaking) }.also { it.enabled = container.settings.voice.value } }
    val sfx: Sfx by lazy { Sfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
