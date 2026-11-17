package de.kurvenflug.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.kurvenflug.core.model.Updates
import kotlinx.coroutines.launch

/** Android-Hülle um den gemeinsamen [AppState]: Widget nachziehen, nach App-Updates schauen. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as KurvenflugApp).container
    val state = AppState(c.settings, c.progress, viewModelScope) { de.kurvenflug.app.widget.KurvenflugWidget.refresh(getApplication()) }
    private val settings = c.settings

    private companion object { const val CHECK_EVERY_MS = 20 * 60 * 60_000L }

    /** Bei jedem Öffnen der App: höchstens alle 20 Stunden bei GitHub nachsehen (abschaltbar). */
    fun checkForUpdateIfDue(installed: String) {
        if (!BuildConfig.SELF_UPDATE || !settings.autoUpdate.value) return
        val now = System.currentTimeMillis()
        if (state.availableUpdate.value == null && now - settings.lastUpdateCheck < CHECK_EVERY_MS) return
        viewModelScope.launch {
            de.kurvenflug.app.update.AppUpdater.check().onSuccess { r ->
                val newer = r?.takeIf { Updates.isNewer(it.version, installed) }
                if (newer == null) settings.lastUpdateCheck = now
                state.availableUpdate.value = newer
            }
        }
    }
}
