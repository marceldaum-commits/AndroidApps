package de.kurvenflug.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.kurvenflug.core.data.LevelProgress
import de.kurvenflug.core.model.AppRelease
import de.kurvenflug.core.model.Daily
import de.kurvenflug.core.model.Levels
import de.kurvenflug.core.model.Updates
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as KurvenflugApp).container
    val settings = c.settings

    val progress: StateFlow<Map<String, LevelProgress>> = c.progress.all.map { l -> l.associateBy { it.levelId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Sterne der Kapitel-Level (Tagesmissionen zählen extra) */
    val stars: StateFlow<Map<String, Int>> = progress.map { m -> m.filterKeys { !Daily.isDaily(it) }.mapValues { it.value.stars } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val totalCells: StateFlow<Int> = progress.map { m -> m.values.sumOf { Integer.bitCount(it.cellMask) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val maxStars = Levels.all.size * 3

    /** Heutiger Tag (Ortszeit) – daraus wird die Tagesmission gebaut */
    fun today(): Long = java.time.LocalDate.now().toEpochDay()
    fun daily() = Daily.forDay(today())

    /** Wie viele Tagesmissionen schon geschafft wurden */
    val dailiesDone: StateFlow<Int> = progress.map { m -> m.keys.count { Daily.isDaily(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _skin = MutableStateFlow(settings.skin)
    val skin: StateFlow<Int> = _skin
    fun setSkin(i: Int) { settings.skin = i; _skin.value = i }

    fun record(levelId: String, stars: Int, cells: Set<Int>, attempts: Int) {
        viewModelScope.launch { c.progress.record(levelId, stars, cells, attempts) }
    }

    // ------------------------------------------------------------ App-Update
    private val _update = MutableStateFlow<AppRelease?>(null)
    private companion object { const val CHECK_EVERY_MS = 30 * 60_000L }
    val availableUpdate: StateFlow<AppRelease?> = _update

    /** Bei jedem Öffnen der App: höchstens alle 30 Minuten bei GitHub nachsehen (abschaltbar). */
    fun checkForUpdateIfDue(installed: String) {
        if (!BuildConfig.SELF_UPDATE || !settings.autoUpdate.value) return
        val now = System.currentTimeMillis()
        if (_update.value == null && now - settings.lastUpdateCheck < CHECK_EVERY_MS) return
        viewModelScope.launch {
            de.kurvenflug.app.update.AppUpdater.check().onSuccess { r ->
                val newer = r?.takeIf { Updates.isNewer(it.version, installed) }
                if (newer == null) settings.lastUpdateCheck = now
                _update.value = newer
            }
        }
    }
}
