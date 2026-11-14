package de.kurvenflug.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.kurvenflug.app.ui.flight.LevelScreen
import de.kurvenflug.app.ui.fx.LocalSfx
import de.kurvenflug.app.ui.fx.LocalVibration
import de.kurvenflug.app.ui.home.HomeScreen
import de.kurvenflug.app.ui.home.Skins
import de.kurvenflug.app.ui.settings.SettingsScreen
import de.kurvenflug.app.ui.settings.UpdateBanner
import de.kurvenflug.app.ui.settings.UpdateState
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.theme.KurvenflugTheme
import de.kurvenflug.core.model.Chapter
import de.kurvenflug.core.model.Daily
import de.kurvenflug.core.model.Levels
import de.kurvenflug.core.model.MusicMood
import androidx.navigation.compose.currentBackStackEntryAsState

object Routes {
    const val HOME = "start"
    const val LEVEL = "level/{id}"
    const val SETTINGS = "einstellungen"
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
        val app = application as KurvenflugApp
        (application as KurvenflugApp).music.prepare(MusicMood.MENU, MusicMood.FLIGHT)
        setContent {
            val sound by vm.settings.sound.collectAsStateWithLifecycle()
            val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
            val music by vm.settings.music.collectAsStateWithLifecycle()
            LaunchedEffect(sound) { app.sfx.enabled = sound }
            LaunchedEffect(music) { app.music.setEnabled(music) }
            KurvenflugTheme {
                CompositionLocalProvider(LocalSfx provides app.sfx, LocalVibration provides vibration) {
                    Surface(Modifier.fillMaxSize(), color = Q.Space) { Nav() }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as KurvenflugApp).music.onForeground(true)
        // Auch beim Zurückkehren aus dem Hintergrund nach neuen Versionen schauen
        vm.checkForUpdateIfDue(BuildConfig.VERSION_NAME)
    }
    override fun onStop() { (application as KurvenflugApp).music.onForeground(false); super.onStop() }

    @Composable
    private fun Nav() {
        val nav = rememberNavController()
        // Musik passend zum Bildschirm: ruhig im Menü, treibend im Level
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        LaunchedEffect(route) {
            (application as KurvenflugApp).music.setMood(if (route == Routes.LEVEL) MusicMood.FLIGHT else MusicMood.MENU)
        }
        val stars by vm.stars.collectAsStateWithLifecycle()
        val cells by vm.totalCells.collectAsStateWithLifecycle()
        val skin by vm.skin.collectAsStateWithLifecycle()
        val probeColor = Skins.getOrElse(skin) { Skins[0] }.color
        NavHost(nav, Routes.HOME) {
            composable(Routes.HOME) {
                val upd by vm.availableUpdate.collectAsStateWithLifecycle()
                val progress by vm.progress.collectAsStateWithLifecycle()
                val dailiesDone by vm.dailiesDone.collectAsStateWithLifecycle()
                val daily = remember(progress) { vm.daily() }
                HomeScreen(
                    stars = stars.values.sum(), maxStars = vm.maxStars, cells = cells, skin = skin,
                    onSkin = vm::setSkin,
                    chapters = Levels.chapters, levelStars = stars,
                    onLevel = { nav.goOnce(Routes.level(it.id)) },
                    onSettings = { nav.goOnce(Routes.SETTINGS) },
                    daily = daily, dailyStars = progress[daily.id]?.stars ?: 0, dailiesDone = dailiesDone,
                    banner = upd?.let { u -> { UpdateBanner(u.version) { nav.goOnce(Routes.SETTINGS) } } },
                )
            }
            composable(Routes.LEVEL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                val level = remember(id) {
                    if (Daily.isDaily(id)) id.removePrefix(Daily.PREFIX).toLongOrNull()?.let { Daily.forDay(it) } else Levels.byId(id)
                } ?: return@composable
                val chapter = Levels.chapters.firstOrNull { it.number == level.chapter }
                    ?: Chapter(0, "Tagesmission", "Jeden Tag neu", "🛰️", emptyList(), listOf(level))
                val next = Levels.next(level.id)?.takeIf { it.chapter == level.chapter }
                LevelScreen(
                    level = level, chapter = chapter, probeColor = probeColor,
                    showIntro = level.number == 1 && chapter.intro.isNotEmpty() && !vm.settings.introSeen(chapter.number),
                    onIntroSeen = { vm.settings.markIntroSeen(chapter.number) },
                    onSolved = { s, c, a -> vm.record(level.id, s, c, a) },
                    onNext = next?.let { n -> {
                        nav.navigate(Routes.level(n.id)) { popUpTo(Routes.LEVEL) { inclusive = true } }
                    } },
                    onBack = { nav.popSafe() },
                )
            }
            composable(Routes.SETTINGS) {
                val upd by vm.availableUpdate.collectAsStateWithLifecycle()
                val autoUpd by vm.settings.autoUpdate.collectAsStateWithLifecycle()
                val sound by vm.settings.sound.collectAsStateWithLifecycle()
                val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
                val music by vm.settings.music.collectAsStateWithLifecycle()
                SettingsScreen(
                    music = music, onMusic = vm.settings::setMusic,
                    sound = sound, onSound = vm.settings::setSound,
                    vibration = vibration, onVibration = vm.settings::setVibration,
                    autoUpdate = autoUpd, onAutoUpdate = vm.settings::setAutoUpdate,
                    installed = BuildConfig.VERSION_NAME,
                    update = upd?.let { UpdateState.Available(it) } ?: UpdateState.Idle,
                    onBack = { nav.popSafe() },
                )
            }
        }
    }
}
