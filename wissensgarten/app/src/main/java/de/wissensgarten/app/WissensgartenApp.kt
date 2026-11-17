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
    val voice: NovaVoice by lazy { NovaVoice(AndroidNova(this), kotlinx.coroutines.MainScope()) }
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
