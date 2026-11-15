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
    const val CODEX = "lexikon"
    const val WORKSHOP = "werkstatt"
    const val LEARN = "lernstand"
    const val HANGAR = "hangar"
    const val STICKERS = "sticker"
    const val PROFILES = "spieler"
    const val PARENT = "eltern"
    const val RESULTS = "ergebnisse"
    const val CREW = "crew"
    const val RESCUE = "rettung/{n}"
    fun rescue(n: Int) = "rettung/$n"
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

    /** Level, das ein Widget-Knopf öffnen soll (kurvenflug://level/<id>) */
    private val widgetLevel = androidx.compose.runtime.mutableStateOf<String?>(null)

    private fun readWidget(intent: android.content.Intent?) {
        val uri = intent?.data?.takeIf { it.scheme == "kurvenflug" && it.host == "level" } ?: return
        val id = uri.lastPathSegment ?: return
        val known = Levels.byId(id) != null || Daily.isDaily(id) || de.kurvenflug.core.model.Learning.levelFor(id) != null
        if (known) widgetLevel.value = id
    }

    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); readShared(intent); readWidget(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as KurvenflugApp
        if (savedInstanceState == null) { readShared(intent); readWidget(intent) }
        (application as KurvenflugApp).music.prepare(MusicMood.MENU, MusicMood.FLIGHT)
        setContent {
            val sound by vm.settings.sound.collectAsStateWithLifecycle()
            val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
            val music by vm.settings.music.collectAsStateWithLifecycle()
            LaunchedEffect(sound) { app.sfx.enabled = sound }
            LaunchedEffect(music) { app.music.setEnabled(music) }
            KurvenflugTheme {
                val look by vm.settings.look.collectAsStateWithLifecycle()
                val voltVoice by vm.settings.voltVoice.collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalSfx provides app.sfx, LocalVibration provides vibration,
                    de.kurvenflug.app.ui.flight.LocalLook provides look, de.kurvenflug.app.ui.volt.LocalVoltHat provides look.third,
                    de.kurvenflug.app.ui.volt.LocalVoltVoice provides voltVoice) {
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
        // Vom Widget: direkt ins Level
        val fromWidget by widgetLevel
        LaunchedEffect(fromWidget) {
            fromWidget?.let { id -> nav.navigate(Routes.level(id)) { launchSingleTop = true }; widgetLevel.value = null }
        }
        val allProgress by vm.progress.collectAsStateWithLifecycle()
        val rescued = remember(allProgress) { allProgress.keys.filter { de.kurvenflug.core.model.Season2.isRescue(it) }.mapNotNull { it.removePrefix(de.kurvenflug.core.model.Season2.PREFIX).toIntOrNull() }.toSet() }
        val stats by vm.stats.collectAsStateWithLifecycle()
        var celebrate by remember { mutableStateOf<List<de.kurvenflug.core.model.Badge>>(emptyList()) }
        LaunchedEffect(stats) { val n = vm.newBadges(stats); if (n.isNotEmpty()) celebrate = n }
        NavHost(nav, Routes.HOME) {
            composable(Routes.HOME) {
                val upd by vm.availableUpdate.collectAsStateWithLifecycle()
                val progress by vm.progress.collectAsStateWithLifecycle()
                val dailiesDone by vm.dailiesDone.collectAsStateWithLifecycle()
                val daily = remember(progress) { vm.daily() }
                val grade by vm.settings.grade.collectAsStateWithLifecycle()
                val gradeAsked by vm.settings.gradeAsked.collectAsStateWithLifecycle()
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
                    onCodex = { nav.goOnce(Routes.CODEX) }, onWorkshop = { nav.goOnce(Routes.WORKSHOP) },
                    banner = upd?.let { u -> { UpdateBanner(u.version) { nav.goOnce(Routes.SETTINGS) } } },
                    grade = grade, onGrade = vm.settings::setGrade, askGrade = !gradeAsked,
                    continueLevel = remember(progress, grade) { de.kurvenflug.core.model.Plan.next(Levels.chapters, grade, vm.played(progress)) },
                    onLearn = { nav.goOnce(Routes.LEARN) },
                    partsFound = remember(stars) { de.kurvenflug.core.model.Story.found(Levels.chapters, stars).size },
                    onHangar = { nav.goOnce(Routes.HANGAR) },
                    greeting = remember(stars) {
                        val (f, text) = de.kurvenflug.core.model.Greeting.home(java.time.LocalTime.now().hour, vm.daysAway, stars.values.sum())
                        when (f) {
                            de.kurvenflug.core.model.Feeling.SLEEPY -> de.kurvenflug.app.ui.volt.VoltMood.SLEEP
                            de.kurvenflug.core.model.Feeling.MISSED_YOU -> de.kurvenflug.app.ui.volt.VoltMood.DANCE
                            de.kurvenflug.core.model.Feeling.IDLE -> de.kurvenflug.app.ui.volt.VoltMood.IDLE
                            else -> de.kurvenflug.app.ui.volt.VoltMood.HAPPY
                        } to text
                    },
                    stickers = vm.settings.stickers.collectAsStateWithLifecycle().value.map { it.removeSuffix("*") }.toSet().size,
                    onStickers = { nav.goOnce(Routes.STICKERS) },
                    player = vm.profiles.collectAsStateWithLifecycle().value.let { ps -> val id = vm.profile.collectAsStateWithLifecycle().value
                        ps.firstOrNull { it.id == id }?.takeIf { ps.size > 1 || it.name != "Spieler 1" }?.let { "${it.avatar} ${it.name}" } },
                    onProfiles = { nav.goOnce(Routes.PROFILES) },
                    onParent = { nav.goOnce(Routes.PARENT) },
                    crewLine = remember(rescued) {
                        // jeden Tag sagt ein anderes Crew-Mitglied etwas (nur gerettete)
                        val saved = de.kurvenflug.core.model.Season2.crew.filter { it.n in rescued }
                        val day = vm.today()
                        saved.takeIf { it.isNotEmpty() }?.let { l -> val m = l[Math.floorMod(day, l.size.toLong()).toInt()]; m to m.lines[Math.floorMod(day / l.size, m.lines.size.toLong()).toInt()] }
                    },
                    rescueCall = remember(rescued, stars) {
                        val parts = de.kurvenflug.core.model.Story.found(Levels.chapters, stars).size
                        val gold = de.kurvenflug.core.model.Season2.gold(Levels.chapters, stars).size
                        de.kurvenflug.core.model.Season2.crew.firstOrNull { it.n !in rescued && de.kurvenflug.core.model.Season2.lock(it.n, parts, gold) == null }
                    },
                    crewCount = rescued.size,
                    onCrew = { nav.goOnce(Routes.CREW) },
                    learning = {
                        val now = remember(progress) { System.currentTimeMillis() }
                        val played = remember(progress) { vm.played(progress) }
                        de.kurvenflug.app.ui.learning.LearningCard(
                            remember(played) { de.kurvenflug.core.model.Learning.status(played, now) },
                            remember(played) { de.kurvenflug.core.model.Learning.review(played, now, vm.today()) },
                        ) { nav.goOnce(Routes.LEARN) }
                    },
                )
            }
            composable(Routes.LEVEL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                val level = remember(id) {
                    if (Daily.isDaily(id)) id.removePrefix(Daily.PREFIX).toLongOrNull()?.let { Daily.forDay(it) }
                    else if (de.kurvenflug.core.model.Learning.isReview(id)) de.kurvenflug.core.model.Learning.levelFor(id)
                    else Levels.byId(id)
                } ?: return@composable
                val chapter = Levels.chapters.firstOrNull { it.number == level.chapter }
                    ?: Chapter(0, "Tagesmission", "Jeden Tag neu", "🛰️", emptyList(), listOf(level))
                val next = Levels.next(level.id)?.takeIf { it.chapter == level.chapter }
                val opened = remember(id) { System.currentTimeMillis() }
                LevelScreen(
                    level = level, chapter = chapter, probeColor = probeColor,
                    showIntro = level.number == 1 && chapter.intro.isNotEmpty() && !vm.settings.introSeen(chapter.number),
                    onIntroSeen = { vm.settings.markIntroSeen(chapter.number) },
                    onSolved = { s, c, a -> vm.record(level.id, s, c, a, ((System.currentTimeMillis() - opened) / 1000).toInt()) },
                    onMiss = { a -> if (a == 3) vm.stuck(level.id, a, ((System.currentTimeMillis() - opened) / 1000).toInt()) },
                    onNext = next?.let { n -> {
                        nav.navigate(Routes.level(n.id)) { popUpTo(Routes.LEVEL) { inclusive = true } }
                    } },
                    onBack = { nav.popSafe() },
                    header = if (de.kurvenflug.core.model.Learning.isReview(level.id)) "🔁 Auffrischen · ${chapter.emoji} ${chapter.title}" else null,
                    voltSolves = if (vm.settings.walkthroughsSeen(level.kind.name) == 0) 1 else 0,
                    onWalkthrough = { vm.settings.markWalkthrough(level.kind.name) },
                    onReward = { s -> vm.rollSticker(s) },
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
            composable(Routes.PROFILES) {
                val ps by vm.profiles.collectAsStateWithLifecycle()
                val cur by vm.profile.collectAsStateWithLifecycle()
                de.kurvenflug.app.ui.together.ProfileScreen(ps, cur, onSwitch = { vm.switchProfile(it) }, onAdd = vm::addProfile,
                    onRename = { id, n, a -> vm.settings.renameProfile(id, n, a) }, onDelete = vm::deleteProfile, onBack = { nav.popSafe() })
            }
            composable(Routes.PARENT) {
                val today = remember { java.time.LocalDate.now() }
                val todayStart = remember { today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
                val plays by remember { vm.weekPlays(todayStart - 13 * de.kurvenflug.core.model.Weekly.DAY) }.collectAsStateWithLifecycle(emptyList())
                val ps by vm.profiles.collectAsStateWithLifecycle()
                val cur by vm.profile.collectAsStateWithLifecycle()
                val week = remember(plays) {
                    de.kurvenflug.core.model.Weekly.summary(plays.map { de.kurvenflug.core.model.PlayRecord(it.levelId, it.stars, it.attempts, it.seconds, it.at) },
                        System.currentTimeMillis(), todayStart)
                }
                val labels = remember { (6 downTo 0).map { today.minusDays(it.toLong()).dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.GERMAN).take(2) } }
                de.kurvenflug.app.ui.together.ParentScreen(ps.first { it.id == cur }, week, labels) { nav.popSafe() }
            }
            composable(Routes.RESULTS) { de.kurvenflug.app.ui.together.ResultsBoard({ nav.popSafe() }) }
            composable(Routes.STICKERS) {
                val owned by vm.settings.stickers.collectAsStateWithLifecycle()
                de.kurvenflug.app.ui.collection.StickerAlbum(owned) { nav.popSafe() }
            }
            composable(Routes.HANGAR) {
                de.kurvenflug.app.ui.story.HangarScreen(remember(stars) { de.kurvenflug.core.model.Story.found(Levels.chapters, stars) }, { nav.popSafe() },
                    gold = remember(stars) { de.kurvenflug.core.model.Season2.gold(Levels.chapters, stars) }, onCrew = { nav.goOnce(Routes.CREW) })
            }
            composable(Routes.CREW) {
                de.kurvenflug.app.ui.story.CrewScreen(rescued, remember(stars) { de.kurvenflug.core.model.Story.found(Levels.chapters, stars).size },
                    remember(stars) { de.kurvenflug.core.model.Season2.gold(Levels.chapters, stars).size },
                    onRescue = { nav.goOnce(Routes.rescue(it)) }, onBack = { nav.popSafe() })
            }
            composable(Routes.RESCUE, arguments = listOf(navArgument("n") { type = NavType.IntType })) { e ->
                val n = e.arguments?.getInt("n") ?: return@composable
                de.kurvenflug.app.ui.story.RescueScreen(n, probeColor, onWon = { s, m -> vm.record(de.kurvenflug.core.model.Season2.id(n), s, emptySet(), m + 5) },
                    onBack = { nav.popSafe() }, reward = { s -> vm.rollSticker(s) })
            }
            composable(Routes.LEARN) {
                val progress by vm.progress.collectAsStateWithLifecycle()
                val now = remember(progress) { System.currentTimeMillis() }
                val played = remember(progress) { vm.played(progress) }
                de.kurvenflug.app.ui.learning.LearningScreen(
                    status = remember(played) { de.kurvenflug.core.model.Learning.status(played, now) },
                    review = remember(played) { de.kurvenflug.core.model.Learning.review(played, now, vm.today()) },
                    doneToday = de.kurvenflug.core.model.Learning.doneToday(played, vm.today()),
                    now = now,
                    onReview = { nav.goOnce(Routes.level(it.id)) },
                    onBack = { nav.popSafe() },
                )
            }
            composable(Routes.BADGES) { BadgesScreen(stats) { nav.popSafe() } }
            composable(Routes.CODEX) { de.kurvenflug.app.ui.collection.CodexScreen(stats.stars) { nav.popSafe() } }
            composable(Routes.WORKSHOP) {
                val look by vm.settings.look.collectAsStateWithLifecycle()
                de.kurvenflug.app.ui.collection.WorkshopScreen(stats.stars, stats.cells, skin, look, vm::setSkin,
                    onLook = { a, b, c -> vm.settings.setLook(a, b, c) }, onBack = { nav.popSafe() })
            }
            composable(Routes.BUILD_HOME) {
                val mine by vm.settings.myLevels.collectAsStateWithLifecycle()
                de.kurvenflug.app.ui.maker.BuilderHome(
                    mine,
                    onNew = { vm.openCode = null; nav.goOnce(Routes.BUILDER) },
                    onEdit = { vm.openCode = it; nav.goOnce(Routes.BUILDER) },
                    onPlay = { c -> de.kurvenflug.core.model.Codec.decodeLevel(c)?.toLevel("eigenes")?.let { vm.playLevel = it; nav.goOnce(Routes.PLAY) } },
                    onDelete = vm.settings::deleteMyLevel,
                    onCode = { openCode(it) },
                    onChallenge = { nav.goOnce(Routes.CHALLENGE_NEW) },
                    onResults = { nav.goOnce(Routes.RESULTS) },
                    onBack = { nav.popSafe() },
                )
            }
            composable(Routes.BUILDER) {
                val editing = remember { vm.openCode }
                val initial = remember { editing?.let { de.kurvenflug.core.model.Codec.decodeLevel(it) } }
                de.kurvenflug.app.ui.maker.BuilderScreen(initial, probeColor,
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
                de.kurvenflug.app.ui.maker.ChallengeComposer(mine) { nav.popSafe() }
            }
            composable(Routes.CHALLENGE) {
                val spec = remember { vm.openCode?.let { de.kurvenflug.core.model.Codec.decodeChallenge(it) } }
                if (spec == null) LaunchedEffect(Unit) { nav.popSafe() }
                else de.kurvenflug.app.ui.maker.ChallengeRun(spec, probeColor) { nav.popSafe() }
            }
            composable(Routes.DETECTIVE) {
                de.kurvenflug.app.ui.maker.DetectiveScreen(vm.settings.detectiveBest, probeColor,
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
                    onReset = vm::resetAll,
                    voltVoice = vm.settings.voltVoice.collectAsStateWithLifecycle().value, onVoltVoice = vm.settings::setVoltVoice,
                )
            }
        }
        // Neue Abzeichen auf der Startseite feiern (nicht mitten im Level)
        if (celebrate.isNotEmpty() && route == Routes.HOME) NewBadgesOverlay(celebrate) {
            vm.markBadgesSeen(celebrate.map { it.id }); celebrate = emptyList()
        }
        // Story: einmal erzählen (nach der Klassen-Wahl), dann jedes neu gefundene Bauteil feiern
        val storySeen by vm.settings.storySeen.collectAsStateWithLifecycle()
        val gradeAskedNow by vm.settings.gradeAsked.collectAsStateWithLifecycle()
        val foundParts = remember(stars) { de.kurvenflug.core.model.Story.found(Levels.chapters, stars) }
        var newParts by remember { mutableStateOf(emptyList<de.kurvenflug.core.model.Part>()) }
        LaunchedEffect(foundParts) { newParts = foundParts.filter { it.chapter.toString() !in vm.settings.partsSeen } }
        val goldParts = remember(stars) { de.kurvenflug.core.model.Season2.gold(Levels.chapters, stars) }
        var newGold by remember { mutableStateOf(emptyList<de.kurvenflug.core.model.Part>()) }
        LaunchedEffect(goldParts) { newGold = goldParts.filter { it.chapter.toString() !in vm.settings.goldSeen } }
        if (route == Routes.HOME && celebrate.isEmpty()) {
            if (gradeAskedNow && !storySeen) de.kurvenflug.app.ui.story.StoryIntroOverlay { vm.settings.markStorySeen() }
            else if (storySeen && newParts.isNotEmpty()) de.kurvenflug.app.ui.story.PartFoundOverlay(newParts, foundParts.map { it.chapter }.toSet(), {
                vm.settings.partsSeen = vm.settings.partsSeen + newParts.map { it.chapter.toString() }
                newParts = emptyList()
                nav.goOnce(Routes.HANGAR)
            }, gold = goldParts.map { it.chapter }.toSet())
            else if (storySeen && newGold.isNotEmpty()) de.kurvenflug.app.ui.story.PartFoundOverlay(newGold, foundParts.map { it.chapter }.toSet(), {
                vm.settings.goldSeen = vm.settings.goldSeen + newGold.map { it.chapter.toString() }
                newGold = emptyList()
                nav.goOnce(Routes.HANGAR)
            }, gold = goldParts.map { it.chapter }.toSet(), isGold = true)
        }
    }
}
