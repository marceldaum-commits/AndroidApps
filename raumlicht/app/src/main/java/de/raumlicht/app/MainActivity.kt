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
import de.raumlicht.app.ui.home.BadgesScreen
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as RaumlichtApp
        app.music.prepare(MusicMood.MENU, MusicMood.FLIGHT)
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
        vm.checkForUpdateIfDue(BuildConfig.VERSION_NAME)
    }
    override fun onStop() { (application as RaumlichtApp).music.onForeground(false); super.onStop() }

    @Composable
    private fun Nav() {
        val nav = rememberNavController()
        // Musik passend zum Bildschirm: ruhig im Tourplan, treibend auf der Bühne
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        LaunchedEffect(route) { (application as RaumlichtApp).music.setMood(if (route == Routes.LEVEL || route == Routes.JAM || route == Routes.DAILY) MusicMood.FLIGHT else MusicMood.MENU) }
        val stars by vm.stars.collectAsStateWithLifecycle()
        val update by vm.availableUpdate.collectAsStateWithLifecycle()
        val jamBest by vm.settings.jamBest.collectAsStateWithLifecycle()
        val badges = remember(stars, jamBest) { Badges.all(stars, jamBest) }

        NavHost(nav, Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(Levels.chapters, stars, { nav.goOnce(Routes.level(it)) }, { nav.goOnce(Routes.SETTINGS) },
                    updateVersion = update?.version, onUpdate = { nav.goOnce(Routes.SETTINGS) },
                    dailyDone = (stars[Daily.id(vm.today())] ?: 0) > 0, jamBest = jamBest, badges = badges.count { it.earned }, badgeCount = badges.size,
                    onDaily = { nav.goOnce(Routes.DAILY) }, onJam = { nav.goOnce(Routes.JAM_HOME) }, onSheet = { nav.goOnce(Routes.SHEET) }, onBadges = { nav.goOnce(Routes.BADGES) })
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
            composable(Routes.SHEET) { SheetScreen(Sheet.cards) { nav.popSafe() } }
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
