package de.wissensgarten.app

import android.app.Application
import de.wissensgarten.core.data.AppContainer

class WissensgartenApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
