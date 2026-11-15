package de.raumlicht.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.raumlicht.app.ui.fx.LocalSfx
import de.raumlicht.app.ui.fx.LocalVibration
import de.raumlicht.app.ui.home.HomeScreen
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.settings.SettingsScreen
import de.raumlicht.app.ui.settings.UpdateState
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.Levels
import de.raumlicht.core.model.Badges
import de.raumlicht.core.model.Chapter
import de.raumlicht.core.model.Daily
import de.raumlicht.core.model.Gen
import de.raumlicht.core.model.JamTopic
import de.raumlicht.core.model.Sheet
import de.raumlicht.app.ui.home.*
import de.raumlicht.core.model.*
import de.raumlicht.app.ui.home.JamPicker
import de.raumlicht.app.ui.home.SheetScreen
import androidx.compose.runtime.saveable.rememberSaveable
import de.raumlicht.core.model.MusicMood

object Routes {
    const val HOME = "start"
    const val LEVEL = "level/{id}"
    const val SETTINGS = "einstellungen"
    const val DAILY = "tagesgig"
    const val JAM_HOME = "jam"
    const val JAM = "jam/{topic}"
    const val SHEET = "spickzettel"
    const val BADGES = "abzeichen"
    const val BACKSTAGE = "backstage"
    const val EXAMS = "abi"
    const val EXAM = "abi/{id}/{zeit}"
    const val LAGE = "lage"
    const val DUEL = "duell"
    const val TRAINING = "zweite-chance"
    const val WORKSHOP = "werkstatt"
    const val BUILDER = "baukasten"
    fun exam(id: String, timed: Boolean) = "abi/$id/${if (timed) 1 else 0}"
    fun jam(topic: String) = "jam/$topic"
    fun level(id: String) = "level/$id"
}

/** Nur zurück, wenn der Bildschirm noch aktiv ist – verhindert, dass ein Doppeltipp zwei Ebenen schließt. */
fun NavController.popSafe(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true && previousBackStackEntry != null && popBackStack()

fun NavController.goOnce(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return
    navigate(route) { launchSingleTop = true }
}

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    /** Wunsch-Ziel von außen (Widget): wird einmal angesteuert und dann gelöscht */
    private val openRoute = mutableStateOf<String?>(null)

    private fun readIntent(intent: android.content.Intent?) {
        if (intent?.getStringExtra(de.raumlicht.app.widget.DailyWidget.EXTRA_OPEN) == de.raumlicht.app.widget.DailyWidget.OPEN_DAILY) openRoute.value = Routes.DAILY
    }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); readIntent(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as RaumlichtApp
        app.music.prepare(MusicMood.MENU, MusicMood.FLIGHT)
        if (savedInstanceState == null) readIntent(intent)
        setContent {
            val sound by vm.settings.sound.collectAsStateWithLifecycle()
            val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
            val music by vm.settings.music.collectAsStateWithLifecycle()
            LaunchedEffect(sound) { app.sfx.enabled = sound }
            LaunchedEffect(music) { app.music.setEnabled(music) }
            RaumlichtTheme {
                CompositionLocalProvider(LocalSfx provides app.sfx, LocalVibration provides vibration) {
                    Surface(Modifier.fillMaxSize(), color = Q.Space) { Nav() }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as RaumlichtApp).music.onForeground(true)
        de.raumlicht.app.widget.DailyWidget.refresh(this)   // z. B. nach Mitternacht: neuer Tagesgig
        vm.checkForUpdateIfDue(BuildConfig.VERSION_NAME)
    }
    override fun onStop() { (application as RaumlichtApp).music.onForeground(false); super.onStop() }

    @Composable
    private fun Nav() {
        val nav = rememberNavController()
        // Musik passend zum Bildschirm: ruhig im Tourplan, treibend auf der Bühne
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        LaunchedEffect(route) { (application as RaumlichtApp).music.setMood(if (route in setOf(Routes.LEVEL, Routes.JAM, Routes.DAILY, Routes.EXAM, Routes.TRAINING, Routes.DUEL, Routes.LAGE)) MusicMood.FLIGHT else MusicMood.MENU) }
        val skin by vm.settings.skin.collectAsStateWithLifecycle()
        val exams by vm.settings.exams.collectAsStateWithLifecycle()
        val lageBest by vm.settings.lageBest.collectAsStateWithLifecycle()
        val stars by vm.stars.collectAsStateWithLifecycle()
        val update by vm.availableUpdate.collectAsStateWithLifecycle()
        val wanted by openRoute
        LaunchedEffect(wanted) { wanted?.let { r -> nav.navigate(r) { popUpTo(Routes.HOME); launchSingleTop = true }; openRoute.value = null } }
        val jamBest by vm.settings.jamBest.collectAsStateWithLifecycle()
        val badges = remember(stars, jamBest) { Badges.all(stars, jamBest) }
        val tourStars = remember(stars) { Levels.all.sumOf { stars[it.id] ?: 0 } }

        CompositionLocalProvider(de.raumlicht.app.ui.stage.LocalLuxSkin provides skin) {
        NavHost(nav, Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(Levels.chapters, stars, { nav.goOnce(Routes.level(it)) }, { nav.goOnce(Routes.SETTINGS) },
                    updateVersion = update?.version, onUpdate = { nav.goOnce(Routes.SETTINGS) },
                    dailyDone = (stars[Daily.id(vm.today())] ?: 0) > 0, jamBest = jamBest,
                    onDaily = { nav.goOnce(Routes.DAILY) }, onJam = { nav.goOnce(Routes.JAM_HOME) }, onExam = { nav.goOnce(Routes.EXAMS) }, onMore = { nav.goOnce(Routes.BACKSTAGE) })
            }
            composable(Routes.LEVEL) { e ->
                val level = Levels.byId(e.arguments?.getString("id").orEmpty())
                if (level == null) { LaunchedEffect(Unit) { nav.popSafe() }; return@composable }
                val chapter = Levels.chapters.first { it.number == level.chapter }
                val next = Levels.next(level.id)
                LevelScreen(level, chapter,
                    showIntro = remember(level.id) { level.id.endsWith("-1") && !vm.settings.introSeen(level.chapter) },
                    onIntroSeen = { vm.settings.markIntroSeen(level.chapter) },
                    onSolved = { s, a -> vm.record(level.id, s, a) },
                    onNext = next?.let { n -> { nav.navigate(Routes.level(n.id)) { popUpTo(Routes.HOME); launchSingleTop = true } } },
                    onBack = { nav.popSafe() })
            }
            composable(Routes.DAILY) {
                val day = remember { vm.today() }
                val level = remember(day) { Daily.forDay(day) }
                LevelScreen(level, Chapter(0, "Tagesgig", level.title, "Tagesgig", "📅", "orange", emptyList(), emptyList()), false, {},
                    onSolved = { s, a -> vm.record(level.id, s, a) }, onNext = null, onBack = { nav.popSafe() },
                    caption = "📅 Tagesgig · ${level.title}", doneLabel = "Fertig für heute ✓")
            }
            composable(Routes.JAM_HOME) { JamPicker(jamBest, { nav.goOnce(Routes.jam(it.name)) }, { nav.popSafe() }) }
            composable(Routes.JAM) { e ->
                val topic = JamTopic.entries.firstOrNull { it.name == e.arguments?.getString("topic") } ?: JamTopic.MIX
                val session = rememberSaveable { System.currentTimeMillis() % 1_000_000 }
                var n by rememberSaveable { mutableIntStateOf(0) }
                var streak by rememberSaveable { mutableIntStateOf(0) }
                val level = remember(session, n) { Gen.jam(topic, session, n) }
                LevelScreen(level, Chapter(0, topic.title, topic.title, "Jam-Session", topic.emoji, topic.color, emptyList(), emptyList()), false, {},
                    // Serie: nur Treffer beim ersten Versuch zählen
                    onSolved = { s, _ -> streak = if (s == 3) streak + 1 else 0; vm.settings.recordJam(streak) },
                    onNext = { n++ }, onBack = { nav.popSafe() },
                    caption = "${topic.emoji} Jam · ${level.title} · Nr. ${n + 1}", badge = if (streak > 0) "🔥 $streak" else null)
            }
            composable(Routes.SHEET) {
                SheetScreen(Sheet.cards,
                    // direkt in den ersten noch offenen Gig des Kapitels
                    onChapter = { n -> Levels.chapters.firstOrNull { it.number == n }?.let { c -> nav.goOnce(Routes.level((c.levels.firstOrNull { (stars[it.id] ?: 0) == 0 } ?: c.levels.first()).id)) } },
                    onJam = { nav.goOnce(Routes.jam(it.name)) }, onBack = { nav.popSafe() })
            }
            composable(Routes.BACKSTAGE) {
                val weak = remember(stars) { Training.levels(stars).size }
                BackstageScreen(listOf(
                    BackstageEntry("🧭", "Lage-Check", "parallel, identisch, schneidend, windschief? · Rekord $lageBest/${LageCheck.ROUNDS}", Q.Yellow) { nav.goOnce(Routes.LAGE) },
                    BackstageEntry("⚔️", "Duell", "5 Aufgaben per Code an Freunde schicken", Q.Pink) { nav.goOnce(Routes.DUEL) },
                    BackstageEntry("🔁", "Zweite Chance", if (weak == 0) "nichts offen" else "$weak Gigs, die nicht beim ersten Versuch saßen", Q.Orange) { nav.goOnce(Routes.TRAINING) },
                    BackstageEntry("🎇", "Show-Baukasten", "eigene Lichtshow aus Formeln bauen und teilen", Q.Violet) { nav.goOnce(Routes.BUILDER) },
                    BackstageEntry("🎨", "Lux-Werkstatt", "${Skins.unlocked(tourStars).size} von ${Skins.all.size} Farben frei", Q.Cyan) { nav.goOnce(Routes.WORKSHOP) },
                    BackstageEntry("📓", "Spickzettel", "alle Formeln – mit Sprung zu den Aufgaben", Q.Green) { nav.goOnce(Routes.SHEET) },
                    BackstageEntry("🏅", "Abzeichen", "${badges.count { it.earned }} von ${badges.size} verdient", Q.Yellow) { nav.goOnce(Routes.BADGES) },
                )) { nav.popSafe() }
            }
            composable(Routes.EXAMS) { ExamPicker(Exams.all, exams, { e, timed -> nav.goOnce(Routes.exam(e.id, timed)) }) { nav.popSafe() } }
            composable(Routes.EXAM) { e ->
                val exam = Exams.byId(e.arguments?.getString("id").orEmpty())
                if (exam == null) { LaunchedEffect(Unit) { nav.popSafe() }; return@composable }
                ExamPlay(exam, onFinished = { vm.settings.recordExam(exam.id, it) }, onBack = { nav.popSafe() }, timed = e.arguments?.getString("zeit") == "1")
            }
            composable(Routes.LAGE) { LageScreen(lageBest, vm.settings::recordLage, { nav.popSafe() }) }
            composable(Routes.DUEL) { DuelScreen({ nav.popSafe() }) }
            composable(Routes.TRAINING) {
                val list = remember { Training.levels(vm.stars.value) }
                TrainingPlay(list, { l, s, a -> vm.record(l.id, s, a) }) { nav.popSafe() }
            }
            composable(Routes.WORKSHOP) { WorkshopScreen(tourStars, skin, vm.settings::setSkin) { nav.popSafe() } }
            composable(Routes.BUILDER) {
                BuilderScreen(remember { Show.decode(vm.settings.show) ?: Show.demo }, { vm.settings.show = Show.encode(it) }, { nav.popSafe() })
            }
            composable(Routes.BADGES) { BadgesScreen(badges) { nav.popSafe() } }
            composable(Routes.SETTINGS) {
                val sound by vm.settings.sound.collectAsStateWithLifecycle()
                val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
                val music by vm.settings.music.collectAsStateWithLifecycle()
                val auto by vm.settings.autoUpdate.collectAsStateWithLifecycle()
                SettingsScreen(music, vm.settings::setMusic, sound, vm.settings::setSound, vibration, vm.settings::setVibration, auto, vm.settings::setAutoUpdate,
                    BuildConfig.VERSION_NAME, update?.let { UpdateState.Available(it) } ?: UpdateState.Idle, { nav.popSafe() })
            }
        }
        }
    }
}
