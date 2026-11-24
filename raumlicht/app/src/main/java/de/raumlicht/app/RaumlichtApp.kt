package de.raumlicht.app

import android.app.Application
import de.raumlicht.app.ui.fx.Sfx
import de.raumlicht.core.data.AppContainer

class RaumlichtApp : Application() {
    lateinit var container: AppContainer
        private set
    val music: de.raumlicht.app.ui.fx.BackgroundMusic by lazy { de.raumlicht.app.ui.fx.BackgroundMusic(this) }
    val voice: de.raumlicht.app.ui.fx.LuxVoice by lazy { de.raumlicht.app.ui.fx.LuxVoice(this) { speaking -> music.duck(speaking) }.also { it.enabled = container.settings.voice.value; it.use(container.settings.voiceName, container.settings.voiceBright) } }
    /** Vorlesen ist aus (Vorlesen.ENABLED). Kommt es mit den fertigen Tonstudio-Dateien wieder, wird der Sprecher hier angeschlossen; bis dahin nur die Stimme des Geräts. */
    val speaker: de.raumlicht.app.ui.fx.Speaker get() = voice
    val voiceStatus = de.raumlicht.app.ui.fx.VoiceStatus()
    val sfx: Sfx by lazy { Sfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        Diagnose.install(this)
        de.raumlicht.app.ui.stage.stageNote = { Diagnose.note(this, it) }
        container = AppContainer(this)
        // Aufräumen im Hintergrund: das frühere Stimm-Paket (63 MB, bis 2.16) und die früher gehörten Sprachdateien löschen
        kotlin.concurrent.thread(name = "aufraeumen", isDaemon = true) { runCatching { java.io.File(filesDir, "stimme").deleteRecursively(); java.io.File(cacheDir, "nova").deleteRecursively(); java.io.File(filesDir, "nova").deleteRecursively() } }
    }
}
