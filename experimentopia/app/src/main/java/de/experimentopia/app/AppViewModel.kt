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
    // Ein gefundenes Update wird gemerkt, damit der Hinweis auf der Startseite bleibt, bis es installiert ist
    private val _update = MutableStateFlow(settings.foundUpdate?.let { (v, url) -> AppRelease(v, "", url, 0) }
        ?.takeIf { Updates.isNewer(it.version, BuildConfig.VERSION_NAME) })
    val availableUpdate: StateFlow<AppRelease?> = _update
    /** Höchstens alle paar Minuten fragen (Drehen des Handys, schnelles Hin und Her) */
    private companion object { const val CHECK_EVERY_MS = 5 * 60_000L }

    /** Bei jedem Start bei GitHub nachsehen (abschaltbar) – ein neues Update erscheint gleich als Hinweis auf der Startseite. */
    fun checkForUpdateIfDue(installed: String) {
        if (!BuildConfig.SELF_UPDATE || !settings.autoUpdate.value) return
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheck < CHECK_EVERY_MS) return
        viewModelScope.launch {
            de.experimentopia.app.update.AppUpdater.check().onSuccess { r ->
                settings.lastUpdateCheck = now
                val newer = r?.takeIf { Updates.isNewer(it.version, installed) }
                _update.value = newer
                settings.foundUpdate = newer?.let { it.version to it.apkUrl }
            }
        }
    }
}
