package de.experimentopia.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.experimentopia.app.ui.AppState
import de.experimentopia.core.model.AppRelease
import de.experimentopia.core.model.Updates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as ExperimentopiaApp).container
    val settings = c.settings
    val nav = de.experimentopia.app.ui.Navigator()
    val state = AppState(c.progress, c.settings, viewModelScope,
        today = { LocalDate.now().toEpochDay() },
        dayStart = { LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() })

    // ------------------------------------------------------------ App-Update (nur GitHub-Fassung)
    private val _update = MutableStateFlow<AppRelease?>(null)
    val availableUpdate: StateFlow<AppRelease?> = _update
    private companion object { const val CHECK_EVERY_MS = 20 * 60 * 60_000L }

    /** Beim Start höchstens alle 20 Stunden bei GitHub nachsehen (abschaltbar). */
    fun checkForUpdateIfDue(installed: String) {
        if (!BuildConfig.SELF_UPDATE || !settings.autoUpdate.value) return
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheck < CHECK_EVERY_MS) return
        viewModelScope.launch {
            de.experimentopia.app.update.AppUpdater.check().onSuccess { r ->
                settings.lastUpdateCheck = now
                _update.value = r?.takeIf { Updates.isNewer(it.version, installed) }
            }
        }
    }
}
