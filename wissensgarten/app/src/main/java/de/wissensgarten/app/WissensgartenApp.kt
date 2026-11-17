package de.wissensgarten.app

import android.app.Application
import de.wissensgarten.app.platform.AndroidMusic
import de.wissensgarten.app.platform.AndroidSfx
import de.wissensgarten.app.platform.AndroidVoice
import de.wissensgarten.app.notify.Reminders
import de.wissensgarten.core.data.AppContainer

class WissensgartenApp : Application() {
    lateinit var container: AppContainer
        private set

    val voice: AndroidVoice by lazy { AndroidVoice(this) }
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
