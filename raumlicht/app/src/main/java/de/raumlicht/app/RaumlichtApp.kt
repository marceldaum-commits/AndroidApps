package de.raumlicht.app

import android.app.Application
import de.raumlicht.app.ui.fx.Sfx
import de.raumlicht.core.data.AppContainer

class RaumlichtApp : Application() {
    lateinit var container: AppContainer
        private set
    val music: de.raumlicht.app.ui.fx.BackgroundMusic by lazy { de.raumlicht.app.ui.fx.BackgroundMusic(this) }
    val voice: de.raumlicht.app.ui.fx.LuxVoice by lazy { de.raumlicht.app.ui.fx.LuxVoice(this) { speaking -> music.duck(speaking) }.also { it.enabled = container.settings.voice.value; it.use(container.settings.voiceName, container.settings.voiceBright) } }
    /** Natürliche Stimme (Paket „Thorsten“, freiwilliger Download) – spricht statt der Handy-Stimme, sobald sie geladen und eingeschaltet ist */
    val natural: de.raumlicht.app.ui.fx.NaturalVoice by lazy { de.raumlicht.app.ui.fx.NaturalVoice(this) { speaking -> music.duck(speaking) }.also { it.enabled = container.settings.voice.value; if (container.settings.voiceNatural) it.load() } }
    val voicePack: de.raumlicht.app.ui.fx.VoicePackLoader by lazy { de.raumlicht.app.ui.fx.VoicePackLoader(this, onReady = { container.settings.voiceNatural = true; natural.load() }) }
    val speaker: de.raumlicht.app.ui.fx.Speaker by lazy { de.raumlicht.app.ui.fx.VoiceRouter(voice, natural) { container.settings.voiceNatural && natural.ready } }
    val sfx: Sfx by lazy { Sfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
