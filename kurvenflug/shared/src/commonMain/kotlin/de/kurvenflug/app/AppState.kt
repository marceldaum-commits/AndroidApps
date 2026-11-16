package de.kurvenflug.app

import de.kurvenflug.core.data.LevelProgress
import de.kurvenflug.core.data.ProgressStore
import de.kurvenflug.core.data.Settings
import de.kurvenflug.core.model.AppRelease
import de.kurvenflug.core.model.Daily
import de.kurvenflug.core.model.Levels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Zustand der ganzen App – auf Android im ViewModel, im Browser einmal beim Start angelegt.
 * [onChanged]: nach Änderungen am Spielstand (Android aktualisiert dann das Startbildschirm-Widget).
 */
class AppState(
    val settings: Settings,
    private val store: ProgressStore,
    private val scope: CoroutineScope,
    private val onChanged: suspend () -> Unit = {},
) {

    val progress: StateFlow<Map<String, LevelProgress>> = store.all.map { l -> l.associateBy { it.levelId } }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Sterne der Kapitel-Level (Tagesmissionen zählen extra) */
    val stars: StateFlow<Map<String, Int>> = progress.map { m -> m.filterKeys { Levels.byId(it) != null }.mapValues { it.value.stars } }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Boss-Sterne je Kapitel */
    val bossStars: StateFlow<Map<Int, Int>> = progress.map { m ->
        de.kurvenflug.core.model.Bosses.all.associate { b -> b.chapter to (m[b.id]?.stars ?: 0) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Alles für die Abzeichen */
    val stats: StateFlow<de.kurvenflug.core.model.Stats> = combine(progress, settings.stormBest, settings.duelWins, settings.duelsPlayed) { m, storm, wins, played ->
        val lv = m.filterKeys { Levels.byId(it) != null }
        de.kurvenflug.core.model.Stats(
            stars = lv.values.sumOf { it.stars },
            perfect = lv.values.count { it.stars == 3 },
            chaptersDone = Levels.chapters.count { c -> c.levels.all { (lv[it.id]?.stars ?: 0) > 0 } },
            cells = m.values.sumOf { de.kurvenflug.core.model.countBits(it.cellMask) },
            dailies = m.keys.count { Daily.isDaily(it) },
            bosses = m.keys.count { it.startsWith("boss-") },
            stormBest = storm, duelWins = wins, duelsPlayed = played,
        )
    }.stateIn(scope, SharingStarted.Eagerly, de.kurvenflug.core.model.Stats())

    /** Neu verdiente, noch nicht gefeierte Abzeichen */
    fun newBadges(s: de.kurvenflug.core.model.Stats) = de.kurvenflug.core.model.Badges.earned(s).filter { it.id !in settings.badgesSeen }
    fun markBadgesSeen(ids: Collection<String>) { settings.badgesSeen = settings.badgesSeen + ids }

    val totalCells: StateFlow<Int> = progress.map { m -> m.values.sumOf { de.kurvenflug.core.model.countBits(it.cellMask) } }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    val maxStars = Levels.all.size * 3

    /** Übergabe zwischen Bildschirmen: zu spielendes Level (Testflug, geteilter Code) bzw. Code */
    var playLevel: de.kurvenflug.core.model.Level? = null
    var openCode: String? = null

    /** Spielstand für den Lernstand: beste Sterne und Zeitpunkt je Level bzw. Auffrischung */
    fun played(m: Map<String, LevelProgress>) = m.mapValues { de.kurvenflug.core.model.Played(it.value.stars, it.value.solvedAt) }

    /** Heutiger Tag (Ortszeit) – daraus wird die Tagesmission gebaut */
    fun today(): Long = de.kurvenflug.core.model.CalDate.today().toEpochDay()
    fun daily() = Daily.forDay(today())

    /** Wie viele Tagesmissionen schon geschafft wurden */
    val dailiesDone: StateFlow<Int> = progress.map { m -> m.keys.count { Daily.isDaily(it) } }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    private val _skin = MutableStateFlow(settings.skin)
    val skin: StateFlow<Int> = _skin
    fun setSkin(i: Int) { settings.skin = i; _skin.value = i }

    fun record(levelId: String, stars: Int, cells: Set<Int>, attempts: Int, seconds: Int = 0) {
        scope.launch {
            store.record(levelId, stars, cells, attempts, seconds)
            onChanged()
        }
    }

    /** Rechen-Stern: Sterne eines geschafften Levels nachträglich erhöhen */
    fun upgradeStars(levelId: String, stars: Int) { scope.launch { store.upgrade(levelId, stars); onChanged() } }

    /** Nach 3 Fehlversuchen: fürs Eltern-Protokoll merken, wo es hakt */
    fun stuck(levelId: String, attempts: Int, seconds: Int) { scope.launch { store.stuck(levelId, attempts, seconds) } }

    // ------------------------------------------------------------ Spieler (Profile)
    val profiles = settings.profiles
    val profile = settings.profile
    fun switchProfile(id: Int) {
        settings.switchProfile(id); _skin.value = settings.skin
        scope.launch { onChanged() }
    }
    fun addProfile(name: String, avatar: String) { settings.addProfile(name, avatar)?.let { switchProfile(it.id) } }
    fun deleteProfile(id: Int) {
        scope.launch { store.clear(id); settings.deleteProfile(id); _skin.value = settings.skin
            onChanged() }
    }

    /** Eltern-Ansicht: Durchgänge der letzten 14 Tage */
    fun weekPlays(since: Long) = store.plays(since)

    /** Tage seit dem letzten Öffnen – einmal beim Start gelesen */
    val daysAway: Int = settings.daysSinceLastOpen()

    /** Nach einem geschafften Level vielleicht ein Sticker (kommt sofort ins Album) */
    fun rollSticker(stars: Int): de.kurvenflug.core.model.StickerDrop? =
        de.kurvenflug.core.model.Stickers.roll(kotlin.random.Random.Default, stars, settings.stickers.value)
            ?.also { settings.addSticker(de.kurvenflug.core.model.Stickers.key(it.sticker, it.glitter)) }

    /** Neu anfangen: Spielstand und Spiel-Einstellungen löschen (Ton & eigene Level bleiben), Widget nachziehen. */
    fun resetAll() {
        scope.launch {
            store.clear()
            settings.resetGame()
            _skin.value = 0
            onChanged()
        }
    }

    /** Neue App-Version (nur Android mit Update-Knopf) */
    val availableUpdate = MutableStateFlow<AppRelease?>(null)
}
