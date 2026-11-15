package de.raumlicht.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.raumlicht.core.model.AppRelease
import de.raumlicht.core.model.Updates
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as RaumlichtApp).container
    val settings = c.settings

    /** Sterne je Level */
    val stars: StateFlow<Map<String, Int>> = c.progress.all.map { l -> l.associate { it.levelId to it.stars } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Heutiger Tag (Ortszeit) – daraus entsteht der Tagesaufgabe */
    fun today(): Long = java.time.LocalDate.now().toEpochDay()

    fun record(levelId: String, stars: Int, attempts: Int) {
        viewModelScope.launch { c.progress.record(levelId, stars, attempts); de.raumlicht.app.widget.DailyWidget.refresh(getApplication()) }
    }

    // ------------------------------------------------------------ App-Update
    private val _update = MutableStateFlow<AppRelease?>(null)
    val availableUpdate: StateFlow<AppRelease?> = _update

    /** Bei jedem Öffnen der App live bei GitHub nachsehen – ohne Wartezeit (abschaltbar). */
    fun checkForUpdateIfDue(installed: String) {
        if (!BuildConfig.SELF_UPDATE || !settings.autoUpdate.value) return
        viewModelScope.launch {
            de.raumlicht.app.update.AppUpdater.check().onSuccess { r ->
                val newer = r?.takeIf { Updates.isNewer(it.version, installed) }
                _update.value = newer
            }
        }
    }
}
