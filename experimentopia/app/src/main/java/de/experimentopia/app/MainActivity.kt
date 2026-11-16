package de.experimentopia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import de.experimentopia.app.ui.settings.SettingsScreen
import de.experimentopia.app.ui.settings.UpdateBanner
import de.experimentopia.app.ui.settings.UpdateState
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.ExperimentopiaTheme
import de.experimentopia.core.model.*

object Routes {
    const val HOME = "start"
    const val ISLAND = "insel/{island}"
    const val EXPERIMENT = "versuch/{id}?daily={daily}"
    const val FREE = "labor"
    const val FREE_RUN = "labor/{lab}"
    const val JOURNAL = "forscherbuch"
    const val BADGES = "abzeichen"
    const val SETTINGS = "einstellungen"
    const val PRO = "pro/{what}"
    fun island(i: Island) = "insel/${i.name}"
    fun experiment(id: String, daily: Boolean = false) = "versuch/$id?daily=$daily"
    fun freeRun(l: Lab) = "labor/${l.name}"
    fun pro(what: String) = "pro/${java.net.URLEncoder.encode(what, "UTF-8")}"
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
        val app = application as ExperimentopiaApp
        setContent {
            val sound by vm.settings.sound.collectAsStateWithLifecycle()
            val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
            LaunchedEffect(sound) { app.sfx.enabled = sound }
            ExperimentopiaTheme {
                CompositionLocalProvider(LocalSfx provides app.sfx, LocalVibration provides vibration) {
                    Surface(Modifier.fillMaxSize(), color = E.Night) { Nav() }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.checkForUpdateIfDue(BuildConfig.VERSION_NAME)
    }

    @Composable
    private fun Nav() {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val stars by vm.stars.collectAsStateWithLifecycle()
        val stats by vm.stats.collectAsStateWithLifecycle()
        val journal by vm.journal.collectAsStateWithLifecycle()
        val grade by vm.settings.grade.collectAsStateWithLifecycle()
        var celebrate by remember { mutableStateOf<List<Badge>>(emptyList()) }
        LaunchedEffect(stats) { val n = vm.newBadges(stats); if (n.isNotEmpty()) celebrate = n }
        fun open(e: Experiment, daily: Boolean = false) {
            if (!daily && vm.locked(e)) nav.goOnce(Routes.pro(e.title)) else nav.goOnce(Routes.experiment(e.id, daily))
        }
        NavHost(nav, Routes.HOME) {
            composable(Routes.HOME) {
                val upd by vm.availableUpdate.collectAsStateWithLifecycle()
                var welcome by remember { mutableStateOf(!vm.settings.welcomed) }
                HomeScreen(
                    results = stars, stats = stats, daily = vm.daily(), dailyDone = vm.dailyDoneToday(journal), grade = grade, welcome = welcome,
                    onIsland = { nav.goOnce(Routes.island(it)) }, onDaily = { open(vm.daily(), daily = true) },
                    onFreeLab = { nav.goOnce(Routes.FREE) }, onJournal = { nav.goOnce(Routes.JOURNAL) }, onBadges = { nav.goOnce(Routes.BADGES) },
                    onSettings = { nav.goOnce(Routes.SETTINGS) }, onGrade = vm.settings::setGrade,
                    onWelcomed = { vm.settings.welcomed = true; welcome = false },
                    banner = upd?.let { u -> { UpdateBanner(u.version) { nav.goOnce(Routes.SETTINGS) } } },
                )
            }
            composable(Routes.ISLAND, arguments = listOf(navArgument("island") { type = NavType.StringType })) { b ->
                val island = Island.entries.firstOrNull { it.name == b.arguments?.getString("island") } ?: return@composable
                IslandScreen(island, stars, grade, vm::locked, { open(it) }, { nav.popSafe() })
            }
            composable(Routes.EXPERIMENT, arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("daily") { type = NavType.BoolType; defaultValue = false })) { b ->
                val e = Catalog.byId(b.arguments?.getString("id").orEmpty()) ?: return@composable
                val daily = b.arguments?.getBoolean("daily") ?: false
                val next = if (daily) null else Catalog.next(e.id)
                ExperimentScreen(e, onDone = { vm.record(e, it, daily) },
                    onNext = next?.let { n -> {
                        if (vm.locked(n)) nav.navigate(Routes.pro(n.title)) { popUpTo(Routes.EXPERIMENT) { inclusive = true } }
                        else nav.navigate(Routes.experiment(n.id)) { popUpTo(Routes.EXPERIMENT) { inclusive = true } }
                    } },
                    onBack = { nav.popSafe() }, daily = daily)
            }
            composable(Routes.FREE) { FreeLabScreen(vm::locked, { l -> if (vm.locked(l)) nav.goOnce(Routes.pro(l.title)) else { vm.settings.countFreeLab(); nav.goOnce(Routes.freeRun(l)) } }, { nav.popSafe() }) }
            composable(Routes.FREE_RUN, arguments = listOf(navArgument("lab") { type = NavType.StringType })) { b ->
                val lab = Lab.entries.firstOrNull { it.name == b.arguments?.getString("lab") } ?: return@composable
                FreeLabRun(lab) { nav.popSafe() }
            }
            composable(Routes.JOURNAL) { JournalScreen(journal, { nav.popSafe() }) }
            composable(Routes.BADGES) { BadgesScreen(stats) { nav.popSafe() } }
            composable(Routes.PRO, arguments = listOf(navArgument("what") { type = NavType.StringType })) { b ->
                ProScreen(java.net.URLDecoder.decode(b.arguments?.getString("what").orEmpty(), "UTF-8")) { nav.popSafe() }
            }
            composable(Routes.SETTINGS) {
                val upd by vm.availableUpdate.collectAsStateWithLifecycle()
                val autoUpd by vm.settings.autoUpdate.collectAsStateWithLifecycle()
                val sound by vm.settings.sound.collectAsStateWithLifecycle()
                val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
                SettingsScreen(
                    sound = sound, onSound = vm.settings::setSound,
                    vibration = vibration, onVibration = vm.settings::setVibration,
                    autoUpdate = autoUpd, onAutoUpdate = vm.settings::setAutoUpdate,
                    grade = grade, onGrade = vm.settings::setGrade,
                    installed = BuildConfig.VERSION_NAME, versionDate = BuildConfig.VERSION_DATE,
                    update = upd?.let { UpdateState.Available(it) } ?: UpdateState.Idle,
                    onReset = vm::reset, onPro = { nav.goOnce(Routes.pro("Alles")) },
                    onBack = { nav.popSafe() },
                )
            }
        }
        // Neue Abzeichen auf der Startseite feiern (nicht mitten im Versuch)
        if (celebrate.isNotEmpty() && route == Routes.HOME) NewBadges(celebrate) { vm.markBadgesSeen(celebrate.map { it.id }); celebrate = emptyList() }
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
