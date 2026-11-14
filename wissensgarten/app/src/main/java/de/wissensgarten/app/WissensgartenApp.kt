package de.wissensgarten.app

import android.app.Application
import de.wissensgarten.app.ui.fx.Sfx
import de.wissensgarten.app.notify.Reminders
import de.wissensgarten.core.data.AppContainer

class WissensgartenApp : Application() {
    lateinit var container: AppContainer
        private set

    val voice: de.wissensgarten.app.ui.fx.Voice by lazy { de.wissensgarten.app.ui.fx.Voice(this) }
    val sync: de.wissensgarten.app.sync.DriveSync by lazy { de.wissensgarten.app.sync.DriveSync(this, container) }
    val music: de.wissensgarten.app.ui.fx.BackgroundMusic by lazy {
        de.wissensgarten.app.ui.fx.BackgroundMusic(this).also { m -> voice.onSpeaking = { m.duck(it) } }
    }
    val sfx: Sfx by lazy { Sfx(this).also { it.enabled = container.settings.sound.value } }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.createChannel(this)
        // Erinnerung sicherstellen (z. B. nach App-Update), ohne die Uhrzeit zu verschieben
        if (container.settings.reminderOn.value) Reminders.schedule(this, container.settings.reminderHour.value, replace = false)
    }
}
