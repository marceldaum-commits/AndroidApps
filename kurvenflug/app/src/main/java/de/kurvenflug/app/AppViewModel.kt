package de.kurvenflug.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.kurvenflug.core.data.LevelProgress
import de.kurvenflug.core.model.AppRelease
import de.kurvenflug.core.model.Levels
import de.kurvenflug.core.model.Updates
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as KurvenflugApp).container
    val settings = c.settings

    val progress: StateFlow<Map<String, LevelProgress>> = c.progress.all.map { l -> l.associateBy { it.levelId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val stars: StateFlow<Map<String, Int>> = progress.map { m -> m.mapValues { it.value.stars } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val totalCells: StateFlow<Int> = progress.map { m -> m.values.sumOf { Integer.bitCount(it.cellMask) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val maxStars = Levels.all.size * 3

    private val _skin = MutableStateFlow(settings.skin)
    val skin: StateFlow<Int> = _skin
    fun setSkin(i: Int) { settings.skin = i; _skin.value = i }

    fun record(levelId: String, stars: Int, cells: Set<Int>, attempts: Int) {
        viewModelScope.launch { c.progress.record(levelId, stars, cells, attempts) }
    }

    // ------------------------------------------------------------ App-Update
    private val _update = MutableStateFlow<AppRelease?>(null)
    val availableUpdate: StateFlow<AppRelease?> = _update

    /** Beim Start: höchstens alle 20 Stunden bei GitHub nachsehen (abschaltbar). */
    fun checkForUpdateIfDue(installed: String) {
        if (!BuildConfig.SELF_UPDATE || !settings.autoUpdate.value) return
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheck < 20 * 3_600_000L) return
        viewModelScope.launch {
            de.kurvenflug.app.update.AppUpdater.check().onSuccess { r ->
                val newer = r?.takeIf { Updates.isNewer(it.version, installed) }
                if (newer == null) settings.lastUpdateCheck = now
                _update.value = newer
            }
        }
    }
}
