package de.experimentopia.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.blubb.Blubb
import de.experimentopia.app.ui.blubb.Mood
import de.experimentopia.app.ui.common.Panel
import de.experimentopia.app.ui.experiment.ExperimentScreen
import de.experimentopia.app.ui.free.FreeLabRun
import de.experimentopia.app.ui.free.FreeLabScreen
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.home.HomeScreen
import de.experimentopia.app.ui.home.IslandScreen
import de.experimentopia.app.ui.journal.BadgesScreen
import de.experimentopia.app.ui.journal.JournalScreen
import de.experimentopia.app.ui.settings.ProScreen
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Ein Bildschirm in der App */
sealed interface Route {
    data object Home : Route
    data class IslandR(val island: Island) : Route
    data class Exp(val id: String, val daily: Boolean = false) : Route
    data object Free : Route
    data class FreeRun(val lab: Lab) : Route
    data object Journal : Route
    data object Badges : Route
    data object Settings : Route
    data class ProR(val what: String) : Route
}

/** Einfacher Stapel von Bildschirmen – gleich für Android und Browser (dort hängt die Zurück-Taste des Browsers daran). */
class Navigator {
    val stack = mutableStateListOf<Route>(Route.Home)
    val top get() = stack.last()
    val canBack get() = stack.size > 1
    fun go(r: Route) { if (stack.last() != r) stack += r }
    /** ersetzt den obersten Bildschirm (z. B. „Nächster Versuch“) */
    fun replace(r: Route) { stack[stack.lastIndex] = r }
    fun back(): Boolean { if (stack.size <= 1) return false; stack.removeAt(stack.lastIndex); return true }
}

/** Alles, was die Bildschirme an Zustand brauchen – aus Speicher und Einstellungen berechnet. */
class AppState(private val store: LabStore, val prefs: Prefs, private val scope: CoroutineScope, private val today: () -> Long, private val dayStart: () -> Long) {
    val stars: StateFlow<Map<String, Int>> = store.results.map { l -> l.filter { Catalog.byId(it.experimentId) != null }.associate { it.experimentId to it.stars } }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())
    val journal: StateFlow<List<NoteRow>> = store.journal.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val stats: StateFlow<Stats> = combine(store.results, store.journal, prefs.freeLabRuns) { res, j, runs -> statsOf(res, j, runs) }
        .stateIn(scope, SharingStarted.Eagerly, Stats())

    fun daily() = Daily.forDay(today())
    fun dailyDoneToday(j: List<NoteRow>) = j.any { it.daily && it.at >= dayStart() && it.experimentId == daily().id }
    fun record(e: Experiment, o: de.experimentopia.app.ui.experiment.Outcome, daily: Boolean) {
        scope.launch { store.record(Finished(e.id, o.stars, o.guess, o.result, o.right, o.sure.ordinal, o.explainedFirstTry, daily)) }
    }
    fun reset() { scope.launch { store.reset(); prefs.resetProgress() } }
    fun locked(e: Experiment) = Pro.locked(e, prefs.pro.value)
    fun locked(l: Lab) = Pro.locked(l, prefs.pro.value)
    fun newBadges(s: Stats) = Badges.earned(s).filter { it.id !in prefs.badgesSeen }

    companion object {
        fun statsOf(res: List<ResultRow>, j: List<NoteRow>, runs: Int): Stats {
            val r = res.filter { Catalog.byId(it.experimentId) != null }
            val ids = r.map { it.experimentId }.toSet()
            return Stats(
                done = r.size, stars = r.sumOf { it.stars },
                hits = r.count { it.firstGuessRight }, ahas = r.count { !it.firstGuessRight },
                explained = j.filter { it.explainedFirstTry }.map { it.experimentId }.toSet().size,
                islandsDone = Island.entries.count { i -> Catalog.of(i).all { it.id in ids } },
                freeLabRuns = runs,
                dailies = j.filter { it.daily }.map { it.at / 86_400_000L }.toSet().size,
                calibrated = j.count { Calibration.matches(Sure.entries.getOrElse(it.sure) { Sure.ETWAS }, it.right) },
            )
        }
    }
}

/**
 * Die ganze App – derselbe Code für Android und Browser.
 * [settingsTop]: oben in den Einstellungen (Android: App-Update; Web: Impressum/Datenschutz). [banner]: Hinweis auf der Startseite (z. B. neue Version).
 */
@Composable
fun AppRoot(
    nav: Navigator, state: AppState,
    versionName: String, versionDate: String,
    settingsTop: @Composable () -> Unit = {},
    privacy: String? = null,
    banner: (@Composable (onOpenSettings: () -> Unit) -> Unit)? = null,
    onRoute: (Route) -> Unit = {},
) {
    val stars by state.stars.collectAsState()
    val stats by state.stats.collectAsState()
    val journal by state.journal.collectAsState()
    val grade by state.prefs.grade.collectAsState()
    var celebrate by remember { mutableStateOf<List<Badge>>(emptyList()) }
    LaunchedEffect(stats) { val n = state.newBadges(stats); if (n.isNotEmpty()) celebrate = n }
    LaunchedEffect(nav.top) { onRoute(nav.top) }
    fun open(e: Experiment, daily: Boolean = false) { if (!daily && state.locked(e)) nav.go(Route.ProR(e.title)) else nav.go(Route.Exp(e.id, daily)) }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(nav.top, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { r ->
            when (r) {
                Route.Home -> {
                    var welcome by remember { mutableStateOf(!state.prefs.welcomed) }
                    HomeScreen(
                        results = stars, stats = stats, daily = state.daily(), dailyDone = state.dailyDoneToday(journal), grade = grade, welcome = welcome,
                        onIsland = { nav.go(Route.IslandR(it)) }, onDaily = { open(state.daily(), daily = true) },
                        onFreeLab = { nav.go(Route.Free) }, onJournal = { nav.go(Route.Journal) }, onBadges = { nav.go(Route.Badges) },
                        onSettings = { nav.go(Route.Settings) }, onGrade = state.prefs::setGrade,
                        onWelcomed = { state.prefs.welcomed = true; welcome = false },
                        banner = banner?.let { b -> { b { nav.go(Route.Settings) } } },
                    )
                }
                is Route.IslandR -> IslandScreen(r.island, stars, grade, state::locked, { open(it) }, { nav.back() })
                is Route.Exp -> {
                    val e = Catalog.byId(r.id)
                    if (e == null) LaunchedEffect(Unit) { nav.back() } else {
                        val next = if (r.daily) null else Catalog.next(e.id)
                        ExperimentScreen(e, onDone = { state.record(e, it, r.daily) },
                            onNext = next?.let { n -> { if (state.locked(n)) nav.replace(Route.ProR(n.title)) else nav.replace(Route.Exp(n.id)) } },
                            onBack = { nav.back() }, daily = r.daily)
                    }
                }
                Route.Free -> FreeLabScreen(state::locked, { l -> if (state.locked(l)) nav.go(Route.ProR(l.title)) else { state.prefs.countFreeLab(); nav.go(Route.FreeRun(l)) } }, { nav.back() })
                is Route.FreeRun -> FreeLabRun(r.lab) { nav.back() }
                Route.Journal -> JournalScreen(journal, { nav.back() })
                Route.Badges -> BadgesScreen(stats) { nav.back() }
                is Route.ProR -> ProScreen(r.what) { nav.back() }
                Route.Settings -> {
                    val sound by state.prefs.sound.collectAsState()
                    val vibration by state.prefs.vibration.collectAsState()
                    de.experimentopia.app.ui.settings.SettingsScreen(
                        sound = sound, onSound = state.prefs::setSound, vibration = vibration, onVibration = state.prefs::setVibration,
                        grade = grade, onGrade = state.prefs::setGrade, installed = versionName, versionDate = versionDate,
                        onReset = state::reset, onPro = { nav.go(Route.ProR("Alles")) }, onBack = { nav.back() },
                        top = settingsTop, privacy = privacy ?: de.experimentopia.app.ui.settings.DEFAULT_PRIVACY,
                    )
                }
            }
        }
        // Neue Abzeichen auf der Startseite feiern (nicht mitten im Versuch)
        if (celebrate.isNotEmpty() && nav.top == Route.Home) NewBadges(celebrate) { state.prefs.badgesSeen = state.prefs.badgesSeen + celebrate.map { it.id }; celebrate = emptyList() }
    }
}

@Composable
fun NewBadges(badges: List<Badge>, onClose: () -> Unit) {
    val sfx = LocalSfx.current
    LaunchedEffect(badges) { sfx?.play(Sound.WIN) }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Panel(Modifier.fillMaxWidth().popIn(), border = E.Sun) {
            Row(verticalAlignment = Alignment.CenterVertically) { Blubb(Mood.CHEER, size = 64.dp); Text("Neues Abzeichen!", color = E.Sun, fontWeight = FontWeight.Black, fontSize = 22.sp) }
            badges.forEach { Text("${it.emoji} ${it.title} – ${it.text}", color = E.Text, fontSize = 16.sp) }
            GlowButton(onClose, Modifier.fillMaxWidth(), color = E.Sun) { Text("Juhu!", fontWeight = FontWeight.Bold) }
        }
        Confetti(1)
    }
}
