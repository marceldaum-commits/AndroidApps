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
    val stars: StateFlow<Map<String, Int>> = progress.map { m -> m.filterKeys { Levels.byId(it) != null }.mapValues { it.value.stars } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Boss-Sterne je Kapitel */
    val bossStars: StateFlow<Map<Int, Int>> = progress.map { m ->
        de.kurvenflug.core.model.Bosses.all.associate { b -> b.chapter to (m[b.id]?.stars ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Alles für die Abzeichen */
    val stats: StateFlow<de.kurvenflug.core.model.Stats> = combine(progress, settings.stormBest, settings.duelWins, settings.duelsPlayed) { m, storm, wins, played ->
        val lv = m.filterKeys { Levels.byId(it) != null }
        de.kurvenflug.core.model.Stats(
            stars = lv.values.sumOf { it.stars },
            perfect = lv.values.count { it.stars == 3 },
            chaptersDone = Levels.chapters.count { c -> c.levels.all { (lv[it.id]?.stars ?: 0) > 0 } },
            cells = m.values.sumOf { Integer.bitCount(it.cellMask) },
            dailies = m.keys.count { Daily.isDaily(it) },
            bosses = m.keys.count { it.startsWith("boss-") },
            stormBest = storm, duelWins = wins, duelsPlayed = played,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, de.kurvenflug.core.model.Stats())

    /** Neu verdiente, noch nicht gefeierte Abzeichen */
    fun newBadges(s: de.kurvenflug.core.model.Stats) = de.kurvenflug.core.model.Badges.earned(s).filter { it.id !in settings.badgesSeen }
    fun markBadgesSeen(ids: Collection<String>) { settings.badgesSeen = settings.badgesSeen + ids }

    val totalCells: StateFlow<Int> = progress.map { m -> m.values.sumOf { Integer.bitCount(it.cellMask) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val maxStars = Levels.all.size * 3

    /** Übergabe zwischen Bildschirmen: zu spielendes Level (Testflug, geteilter Code) bzw. Code */
    var playLevel: de.kurvenflug.core.model.Level? = null
    var openCode: String? = null

    /** Spielstand für den Lernstand: beste Sterne und Zeitpunkt je Level bzw. Auffrischung */
    fun played(m: Map<String, LevelProgress>) = m.mapValues { de.kurvenflug.core.model.Played(it.value.stars, it.value.solvedAt) }

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
        viewModelScope.launch {
            c.progress.record(levelId, stars, cells, attempts)
            de.kurvenflug.app.widget.KurvenflugWidget.refresh(getApplication())
        }
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
