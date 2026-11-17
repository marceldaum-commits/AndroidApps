package de.raumlicht.app

import android.app.Application
import de.raumlicht.app.ui.fx.Sfx
import de.raumlicht.core.data.AppContainer

class RaumlichtApp : Application() {
    lateinit var container: AppContainer
        private set
    val music: de.raumlicht.app.ui.fx.BackgroundMusic by lazy { de.raumlicht.app.ui.fx.BackgroundMusic(this) }
    val voice: de.raumlicht.app.ui.fx.LuxVoice by lazy { de.raumlicht.app.ui.fx.LuxVoice(this) { speaking -> music.duck(speaking) }.also { it.enabled = container.settings.voice.value; it.use(container.settings.voiceName, container.settings.voiceBright) } }
    /** Lux spricht mit „Nova“ (KI-generierte Stimme, fertige Dateien von unserem Server); kommt ein Satz nicht an, liest ihn still die Stimme des Geräts */
    val nova: de.raumlicht.app.ui.fx.NovaVoice by lazy { de.raumlicht.app.ui.fx.NovaVoice(this, voice, onSpeaking = { speaking -> music.duck(speaking) }).also { it.enabled = container.settings.voice.value } }
    val speaker: de.raumlicht.app.ui.fx.Speaker get() = nova
    /** Alle festen Nova-Texte auf einmal laden (Einstellungen) */
    val novaOffline: de.raumlicht.app.ui.fx.NovaOffline by lazy { de.raumlicht.app.ui.fx.NovaOffline(this) }
    val sfx: Sfx by lazy { Sfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        Diagnose.install(this)
        de.raumlicht.app.ui.stage.stageNote = { Diagnose.note(this, it) }
        container = AppContainer(this)
        // Aufräumen im Hintergrund: die frühere Stimme „Thorsten“ (63 MB) löschen, gespeicherte Nova-Sätze begrenzen
        kotlin.concurrent.thread(name = "aufraeumen", isDaemon = true) { runCatching { java.io.File(filesDir, "stimme").deleteRecursively(); de.raumlicht.app.ui.fx.NovaVoice.trim(this) } }
    }
}
