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
import de.kurvenflug.core.model.DuelCode
import de.kurvenflug.app.ui.modes.BadgesScreen
import de.kurvenflug.app.ui.modes.BossScreen
import de.kurvenflug.app.ui.modes.DuelScreen
import de.kurvenflug.app.ui.modes.NewBadgesOverlay
import de.kurvenflug.app.ui.modes.StormScreen
import de.kurvenflug.core.model.Daily
import de.kurvenflug.core.model.Levels
import de.kurvenflug.core.model.MusicMood
import androidx.navigation.compose.currentBackStackEntryAsState

object Routes {
    const val HOME = "start"
    const val LEVEL = "level/{id}"
    const val SETTINGS = "einstellungen"
    const val BOSS = "boss/{chapter}"
    const val STORM = "sturm"
    const val DUEL = "duell"
    const val BADGES = "abzeichen"
    fun boss(chapter: Int) = "boss/$chapter"
    const val BUILD_HOME = "baukasten"
    const val BUILDER = "bauen"
    const val PLAY = "spielen"
    const val CHALLENGE_NEW = "challenge-neu"
    const val CHALLENGE = "challenge"
    const val DETECTIVE = "detektiv"
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
    /** Duell-Code, der per „Teilen“ (z. B. aus WhatsApp) hereinkam */
    private val incomingDuel = androidx.compose.runtime.mutableStateOf<String?>(null)

    private fun readShared(intent: android.content.Intent?) {
        val text = intent?.takeIf { it.action == android.content.Intent.ACTION_SEND }?.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: return
        if (de.kurvenflug.core.model.Codec.detect(text) != null) incomingDuel.value = text
    }

    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); readShared(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as KurvenflugApp
        if (savedInstanceState == null) readShared(intent)
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
        // Duell-Code von außen → direkt ins Duell
        val shared by incomingDuel
        fun openCode(text: String) {
            when (de.kurvenflug.core.model.Codec.detect(text)) {
                de.kurvenflug.core.model.Codec.Type.DUEL -> { incomingDuel.value = text; nav.navigate(Routes.DUEL) { launchSingleTop = true }; return }
                de.kurvenflug.core.model.Codec.Type.LEVEL -> de.kurvenflug.core.model.Codec.decodeLevel(text)?.toLevel("geteilt")?.let {
                    vm.playLevel = it; nav.navigate(Routes.PLAY) }
                de.kurvenflug.core.model.Codec.Type.CHALLENGE -> { vm.openCode = text; nav.navigate(Routes.CHALLENGE) }
                null -> {}
            }
            incomingDuel.value = null
        }
        LaunchedEffect(shared) { shared?.let { openCode(it) } }
        val stats by vm.stats.collectAsStateWithLifecycle()
        var celebrate by remember { mutableStateOf<List<de.kurvenflug.core.model.Badge>>(emptyList()) }
        LaunchedEffect(stats) { val n = vm.newBadges(stats); if (n.isNotEmpty()) celebrate = n }
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
                    bossStars = vm.bossStars.collectAsStateWithLifecycle().value,
                    onBoss = { nav.goOnce(Routes.boss(it)) },
                    badges = de.kurvenflug.core.model.Badges.earned(stats).size,
                    stormBest = stats.stormBest,
                    onDuel = { nav.goOnce(Routes.DUEL) }, onStorm = { nav.goOnce(Routes.STORM) }, onBadges = { nav.goOnce(Routes.BADGES) },
                    onBuild = { nav.goOnce(Routes.BUILD_HOME) }, onDetective = { nav.goOnce(Routes.DETECTIVE) },
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
            composable(Routes.BOSS, arguments = listOf(navArgument("chapter") { type = NavType.IntType })) { e ->
                val boss = de.kurvenflug.core.model.Bosses.forChapter(e.arguments?.getInt("chapter") ?: 0) ?: return@composable
                BossScreen(boss, probeColor, onWon = { s, m -> vm.record(boss.id, s, emptySet(), m + boss.stages.size) }, onBack = { nav.popSafe() })
            }
            composable(Routes.STORM) {
                StormScreen(stats.stormBest, probeColor, onFinished = { vm.settings.recordStorm(it) }, onBack = { nav.popSafe() })
            }
            composable(Routes.DUEL) {
                val code = remember { incomingDuel.value.also { incomingDuel.value = null } }
                DuelScreen(probeColor, code, onResult = { vm.settings.recordDuel(it) }, onBack = { nav.popSafe() })
            }
            composable(Routes.BADGES) { BadgesScreen(stats) { nav.popSafe() } }
            composable(Routes.BUILD_HOME) {
                val mine by vm.settings.myLevels.collectAsStateWithLifecycle()
                de.kurvenflug.app.ui.build.BuilderHome(
                    mine,
                    onNew = { vm.openCode = null; nav.goOnce(Routes.BUILDER) },
                    onEdit = { vm.openCode = it; nav.goOnce(Routes.BUILDER) },
                    onPlay = { c -> de.kurvenflug.core.model.Codec.decodeLevel(c)?.toLevel("eigenes")?.let { vm.playLevel = it; nav.goOnce(Routes.PLAY) } },
                    onDelete = vm.settings::deleteMyLevel,
                    onCode = { openCode(it) },
                    onChallenge = { nav.goOnce(Routes.CHALLENGE_NEW) },
                    onBack = { nav.popSafe() },
                )
            }
            composable(Routes.BUILDER) {
                val editing = remember { vm.openCode }
                val initial = remember { editing?.let { de.kurvenflug.core.model.Codec.decodeLevel(it) } }
                de.kurvenflug.app.ui.build.BuilderScreen(initial, probeColor,
                    onSave = { vm.settings.saveMyLevel(it, replace = editing) },
                    onTest = { vm.playLevel = it; nav.goOnce(Routes.PLAY) },
                    onBack = { nav.popSafe() })
            }
            composable(Routes.PLAY) {
                val l = remember { vm.playLevel } ?: run { LaunchedEffect(Unit) { nav.popSafe() }; return@composable }
                LevelScreen(l, Chapter(0, "Eigenes Level", "", "🛠️", emptyList(), listOf(l)), probeColor, false, {},
                    onSolved = { _, _, _ -> }, onNext = null, onBack = { nav.popSafe() })
            }
            composable(Routes.CHALLENGE_NEW) {
                val mine by vm.settings.myLevels.collectAsStateWithLifecycle()
                de.kurvenflug.app.ui.build.ChallengeComposer(mine) { nav.popSafe() }
            }
            composable(Routes.CHALLENGE) {
                val spec = remember { vm.openCode?.let { de.kurvenflug.core.model.Codec.decodeChallenge(it) } }
                if (spec == null) LaunchedEffect(Unit) { nav.popSafe() }
                else de.kurvenflug.app.ui.build.ChallengeRun(spec, probeColor) { nav.popSafe() }
            }
            composable(Routes.DETECTIVE) {
                de.kurvenflug.app.ui.build.DetectiveScreen(vm.settings.detectiveBest, probeColor,
                    onFinished = { if (it > vm.settings.detectiveBest) vm.settings.detectiveBest = it }, onBack = { nav.popSafe() })
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
        // Neue Abzeichen auf der Startseite feiern (nicht mitten im Level)
        if (celebrate.isNotEmpty() && route == Routes.HOME) NewBadgesOverlay(celebrate) {
            vm.markBadgesSeen(celebrate.map { it.id }); celebrate = emptyList()
        }
    }
}
