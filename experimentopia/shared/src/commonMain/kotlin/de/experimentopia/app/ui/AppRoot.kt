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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
    data object Topics : Route
    data class SubjectR(val subject: Subject) : Route
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
    /** Forscher-Pass: alter Schalter oder gültiger Code von der Website */
    fun pro() = prefs.pro.value || Pro.valid(prefs.proCode.value, today())
    fun todayDay() = today()
    fun locked(e: Experiment) = Pro.locked(e, pro(), done = stars.value.keys)
    fun locked(l: Lab) = Pro.locked(l, pro())
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
    val audio by state.prefs.audio.collectAsState()
    val sound by state.prefs.sound.collectAsState()
    // Forscher-Pass freigeschaltet? (beobachtet Code und alten Schalter, damit das Abzeichen sofort erscheint)
    val proCode by state.prefs.proCode.collectAsState(); val proFlag by state.prefs.pro.collectAsState()
    val pass = remember(proCode, proFlag) { state.pro() }
    val music = LocalMusic.current; val reader = LocalReader.current
    // Musik und Stimme folgen den Einstellungen; Musik erst nach der ersten Berührung
    var touched by remember { mutableStateOf(music?.needsTouch == false) }
    LaunchedEffect(audio) { music?.configure(audio); reader?.configure(audio) }
    LaunchedEffect(touched) { if (touched && audio.music) music?.jingle() }
    LaunchedEffect(nav.top, touched) {
        if (!touched) return@LaunchedEffect
        music?.world(when (val r = nav.top) {
            is Route.IslandR -> r.island.world(); is Route.SubjectR -> r.subject.world()
            is Route.Exp, is Route.FreeRun -> null
            else -> World.LABOR
        })
    }
    LaunchedEffect(reader?.speaking) { music?.duck(reader?.speaking == true) }
    var celebrate by remember { mutableStateOf<List<Badge>>(emptyList()) }
    LaunchedEffect(stats) { val n = state.newBadges(stats); if (n.isNotEmpty()) celebrate = n }
    LaunchedEffect(nav.top) { onRoute(nav.top) }
    // Eltern-Kopplung (nur, wo die Plattform sie anbietet)
    val parentSync = LocalParentSync.current
    val parents = remember(parentSync) { parentSync?.let { de.experimentopia.app.ui.settings.ParentCoupling(it) } }
    de.experimentopia.app.ui.settings.ParentAutoUpload(parents, stars, journal, grade)
    fun open(e: Experiment, daily: Boolean = false) { if (!daily && state.locked(e)) nav.go(Route.ProR(e.title)) else nav.go(Route.Exp(e.id, daily)) }

    CompositionLocalProvider(LocalReadOn provides (audio.read && de.experimentopia.core.model.Nova.ENABLED), LocalAutoRead provides (audio.autoRead && de.experimentopia.core.model.Nova.ENABLED)) {
    // Große Bildschirme (Laptop, Tablet quer): Hintergrund füllt alles, der Inhalt bleibt in angenehmer Lesebreite in der Mitte
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(E.Deep, E.Night))), contentAlignment = Alignment.TopCenter) {
    Box(Modifier.widthIn(max = 720.dp).fillMaxSize().pointerInput(Unit) {
        // erste Berührung merken (ohne sie zu verbrauchen) – erst dann darf Musik spielen
        awaitPointerEventScope { while (!touched) { val ev = awaitPointerEvent(PointerEventPass.Initial); if (ev.changes.any { it.pressed }) touched = true } }
    }) {
        AnimatedContent(nav.top, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { r ->
            when (r) {
                Route.Home -> {
                    var welcome by remember { mutableStateOf(!state.prefs.welcomed) }
                    HomeScreen(
                        results = stars, stats = stats, daily = state.daily(), dailyDone = state.dailyDoneToday(journal), grade = grade, welcome = welcome,
                        onIsland = { nav.go(Route.IslandR(it)) }, onDaily = { open(state.daily(), daily = true) },
                        onFreeLab = { nav.go(Route.Free) }, onTopics = { nav.go(Route.Topics) }, onSubject = { nav.go(Route.SubjectR(it)) }, onJournal = { nav.go(Route.Journal) }, onBadges = { nav.go(Route.Badges) },
                        onSettings = { nav.go(Route.Settings) }, onGrade = state.prefs::setGrade,
                        onWelcomed = { state.prefs.welcomed = true; welcome = false },
                        banner = banner?.let { b -> { b { nav.go(Route.Settings) } } }, pass = pass,
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
                is Route.SubjectR -> de.experimentopia.app.ui.home.SubjectScreen(r.subject, stars, grade, state::locked, { open(it) }, state.prefs::setGrade) { nav.back() }
                Route.Topics -> de.experimentopia.app.ui.home.TopicScreen(stars, grade, state::locked, { open(it) }, { nav.back() })
                Route.Free -> FreeLabScreen(state::locked, { l -> if (state.locked(l)) nav.go(Route.ProR(l.title)) else { state.prefs.countFreeLab(); nav.go(Route.FreeRun(l)) } }, { nav.back() })
                is Route.FreeRun -> FreeLabRun(r.lab) { nav.back() }
                Route.Journal -> JournalScreen(journal, { nav.back() }, onOpen = { open(it) })
                Route.Badges -> BadgesScreen(stats) { nav.back() }
                is Route.ProR -> ProScreen(r.what, state) { nav.back() }
                Route.Settings -> {
                    val vibration by state.prefs.vibration.collectAsState()
                    de.experimentopia.app.ui.settings.SettingsScreen(
                        sound = sound, onSound = state.prefs::setSound, vibration = vibration, onVibration = state.prefs::setVibration,
                        grade = grade, onGrade = state.prefs::setGrade, installed = versionName, versionDate = versionDate,
                        onReset = state::reset, onPro = { nav.go(Route.ProR("Alles")) }, onBack = { nav.back() },
                        top = settingsTop, privacy = privacy ?: de.experimentopia.app.ui.settings.DEFAULT_PRIVACY,
                        audio = audio, onAudio = state.prefs::setAudio, pass = pass,
                        parents = { de.experimentopia.app.ui.settings.ParentCard(parents, state.todayDay()) { parents?.upload(stars, journal, grade) } },
                    )
                }
            }
        }
        // Einmal je Sitzung ein dezenter Hinweis, wenn Nova nicht erreichbar war
        var novaHint by remember { mutableStateOf(false) }
        LaunchedEffect(reader?.novaDown) { if (reader?.novaDown == true) { novaHint = true; kotlinx.coroutines.delay(4500); novaHint = false } }
        androidx.compose.animation.AnimatedVisibility(novaHint, Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            Text("📶 Nova gerade nicht erreichbar – Blubb liest kurz mit der Stimme des Geräts.", color = E.Text, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(E.Panel).border(1.dp, E.Line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp).testTag("nova_hinweis"))
        }
        // Neue Abzeichen auf der Startseite feiern (nicht mitten im Versuch)
        if (celebrate.isNotEmpty() && nav.top == Route.Home) NewBadges(celebrate) { state.prefs.badgesSeen = state.prefs.badgesSeen + celebrate.map { it.id }; celebrate = emptyList() }
    }
    }
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
