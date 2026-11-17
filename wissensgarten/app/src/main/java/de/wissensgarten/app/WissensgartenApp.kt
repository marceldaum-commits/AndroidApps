package de.wissensgarten.app

import android.app.Application
import de.wissensgarten.app.platform.AndroidMusic
import de.wissensgarten.app.platform.AndroidSfx
import de.wissensgarten.app.platform.AndroidNova
import de.wissensgarten.app.ui.fx.NovaVoice
import de.wissensgarten.app.notify.Reminders
import de.wissensgarten.core.data.AppContainer

class WissensgartenApp : Application() {
    lateinit var container: AppContainer
        private set

    /** Brummis Stimme „Nova“ (mit der Android-Sprachausgabe als stiller Notlösung) */
    val voice: NovaVoice by lazy {
        val scope = kotlinx.coroutines.MainScope()
        val nova = AndroidNova(this)
        // „Alle Nova-Texte offline laden“: Liste und Server-Stand des Live-Hosts (dieselben Texte und Namen wie der Server)
        val host = de.wissensgarten.core.model.NovaOffline.LIVE_HOST
        NovaVoice(nova, scope, de.wissensgarten.app.ui.fx.NovaOfflineManager(nova, scope, de.wissensgarten.core.model.NovaOffline.listeUrl(host), host))
    }
    val sync: de.wissensgarten.app.sync.DriveSync by lazy { de.wissensgarten.app.sync.DriveSync(this, container) }
    val music: AndroidMusic by lazy { AndroidMusic(this).also { m -> voice.onSpeaking = { m.duck(it) } } }
    val sfx: AndroidSfx by lazy { AndroidSfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, plusConfig(purchaseAllowed = BuildConfig.SELF_UPDATE))
        Reminders.createChannel(this)
        // Erinnerung sicherstellen (z. B. nach App-Update), ohne die Uhrzeit zu verschieben
        if (container.settings.reminderOn.value) Reminders.schedule(this, container.settings.reminderHour.value, replace = false)
    }
}
