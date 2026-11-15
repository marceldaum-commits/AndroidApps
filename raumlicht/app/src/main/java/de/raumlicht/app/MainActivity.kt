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
import de.raumlicht.core.model.MusicMood

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
        LaunchedEffect(route) { (application as RaumlichtApp).music.setMood(if (route == Routes.LEVEL) MusicMood.FLIGHT else MusicMood.MENU) }
        val stars by vm.stars.collectAsStateWithLifecycle()
        val update by vm.availableUpdate.collectAsStateWithLifecycle()

        NavHost(nav, Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(Levels.chapters, stars, { nav.goOnce(Routes.level(it)) }, { nav.goOnce(Routes.SETTINGS) },
                    updateVersion = update?.version, onUpdate = { nav.goOnce(Routes.SETTINGS) })
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
