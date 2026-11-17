package de.kurvenflug.app

import androidx.compose.material3.Surface
import androidx.savedstate.read
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
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
    const val ANDROMEDA = "andromeda"
    const val STATION = "station"
    const val SEASON = "gastplanet"
    fun level(id: String) = "level/$id"
    const val PRO = "pro"
}

/** Nur zurück, wenn der Bildschirm noch aktiv ist – verhindert, dass ein Doppeltipp zwei Ebenen schließt. */
fun NavController.popSafe(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true && previousBackStackEntry != null && popBackStack()

fun NavController.goOnce(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return
    navigate(route) { launchSingleTop = true }
}

/**
 * Die ganze App – gemeinsam für Android und Browser. [incomingDuel]: per „Teilen“ hereingekommener Code,
 * [widgetLevel]: vom Widget gewünschtes Level (nur Android).
 */
@Composable
fun KurvenflugRoot(
    vm: AppState,
    platform: Platform,
    incomingDuel: MutableState<String?> = remember { mutableStateOf(null) },
    widgetLevel: MutableState<String?> = remember { mutableStateOf(null) },
) {
    val sound by vm.settings.sound.collectAsStateWithLifecycle()
    val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
    val music by vm.settings.music.collectAsStateWithLifecycle()
    LaunchedEffect(sound) { platform.sfx?.enabled = sound }
    LaunchedEffect(music) { platform.setMusicEnabled(music) }
    val musicVol by vm.settings.musicVolume.collectAsStateWithLifecycle()
    val sfxVol by vm.settings.sfxVolume.collectAsStateWithLifecycle()
    LaunchedEffect(sfxVol) { platform.sfx?.level = sfxVol / 100f }
    // Ton erst nach der ersten Berührung – dann begrüßt die Kurvenflug-Erkennungsmelodie
    var touched by remember { mutableStateOf(false) }
    LaunchedEffect(touched) {
        if (touched) { platform.onFirstTouch(); kotlinx.coroutines.delay(250); platform.sfx?.play(de.kurvenflug.app.ui.fx.Sound.JINGLE, volume = 0.7f) }
    }
    // Vorlesen: Stimme und Tempo übernehmen; solange Volt spricht, wird die Musik leiser
    val readAloud by vm.settings.readAloud.collectAsStateWithLifecycle()
    val reader = platform.reader
    LaunchedEffect(readAloud) { if (!readAloud) reader?.stop() }
    val readSpeed by vm.settings.readSpeed.collectAsStateWithLifecycle()
    LaunchedEffect(readSpeed) { reader?.setSpeed(readSpeed / 100f) }
    // Spielernamen gehen nie an den Stimmen-Server
    val profiles by vm.settings.profiles.collectAsStateWithLifecycle()
    LaunchedEffect(profiles) { reader?.setPrivate(profiles.map { it.name }.toSet()) }
    val talking = reader?.speaking == true
    LaunchedEffect(musicVol, talking) { platform.setMusicVolume(musicVol / 100f * (if (talking) 0.35f else 1f)) }
    val proState = rememberProState(vm, platform.proShop)
    KurvenflugTheme {
        val look by vm.settings.look.collectAsStateWithLifecycle()
        val voltVoice by vm.settings.voltVoice.collectAsStateWithLifecycle()
        CompositionLocalProvider(de.kurvenflug.app.LocalPlatform provides platform, LocalSfx provides platform.sfx, LocalVibration provides vibration,
            de.kurvenflug.app.ui.flight.LocalLook provides look, de.kurvenflug.app.ui.volt.LocalVoltHat provides look.third,
            de.kurvenflug.app.ui.volt.LocalVoltVoice provides voltVoice, LocalPro provides proState,
            de.kurvenflug.app.ui.fx.LocalReader provides reader, de.kurvenflug.app.ui.fx.LocalReadAloud provides (readAloud && reader != null)) {
            Surface(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (!touched) {
                        val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (e.changes.any { it.pressed }) touched = true
                    }
                }
            }, color = Q.Space) {
                if (!platform.isWeb) Nav(vm, platform, incomingDuel, widgetLevel)
                // Browser am PC: Kurvenflug als Handy-Spalte in der Mitte – das Spielfeld ist quadratisch, Regler bleiben sichtbar
                else androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    val w = minOf(maxWidth, androidx.compose.ui.unit.max(360.dp, minOf(520.dp, maxHeight * 0.62f)))
                    androidx.compose.foundation.layout.Box(Modifier.width(w).fillMaxHeight()) { Nav(vm, platform, incomingDuel, widgetLevel) }
                }
            }
        }
    }
}

@Composable
private fun Nav(vm: AppState, platform: Platform, incomingDuel: MutableState<String?>, widgetLevel: MutableState<String?>) {
    val nav = rememberNavController()
    // Musik passend zum Bildschirm: ruhig im Menü, treibend im Level
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val levelArg = entry?.arguments?.read { getStringOrNull("id") }
    LaunchedEffect(route, levelArg) {
        platform.setMusicMood(when (route) {
            Routes.LEVEL -> levelArg.orEmpty().let { id ->
                val ch = Levels.byId(id)?.chapter ?: de.kurvenflug.core.model.ChapterExtreme.levelFor(id)?.chapter ?: 0
                de.kurvenflug.core.model.SpaceMusic.forLevel(id, ch)
            }
            Routes.STORM -> MusicMood(MusicMood.Mode.STORM, 0)
            Routes.BOSS -> MusicMood(MusicMood.Mode.STORM, 7)
            Routes.DUEL, Routes.PLAY -> MusicMood(MusicMood.Mode.FLIGHT, 50)
            // Klassen-Challenge (Unterricht): standardmäßig still
            Routes.CHALLENGE, Routes.CHALLENGE_NEW -> null
            else -> MusicMood.MENU
        })
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
    val allStars = remember(allProgress) { allProgress.mapValues { it.value.stars } }
    val rescued = remember(allProgress) { allProgress.keys.filter { de.kurvenflug.core.model.Season2.isRescue(it) }.mapNotNull { it.removePrefix(de.kurvenflug.core.model.Season2.PREFIX).toIntOrNull() }.toSet() }
    val stats by vm.stats.collectAsStateWithLifecycle()
    var celebrate by remember { mutableStateOf<List<de.kurvenflug.core.model.Badge>>(emptyList()) }
    LaunchedEffect(stats) { val n = vm.newBadges(stats); if (n.isNotEmpty()) celebrate = n }
    // Kurze Übergänge: Während des Überblendens nimmt ein Bildschirm keine Tipps an – bei 700 ms (Standard)
    // wirkte die App nach „Zurück“ wie eingefroren, wenn Kinder schnell weitertippen.
    // Im Browser: „Zurück“ des Browsers bzw. Esc führt eine Ebene zurück
    de.kurvenflug.app.ui.fx.BackHandler(enabled = platform.isWeb && route != Routes.HOME) { nav.popSafe() }
    NavHost(nav, Routes.HOME,
        enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(150)) },
        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100)) },
        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(150)) },
        popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100)) },
    ) {
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
                chapters = Levels.chapters, levelStars = stars + vm.extremeStars.collectAsStateWithLifecycle().value,
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
                    val (f, text) = de.kurvenflug.core.model.Greeting.home(de.kurvenflug.core.model.CalDate.hourNow(), vm.daysAway, stars.values.sum())
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
                    saved.takeIf { it.isNotEmpty() }?.let { l -> val m = l[de.kurvenflug.core.model.floorModK(day, l.size.toLong()).toInt()]; m to m.lines[de.kurvenflug.core.model.floorModK(day / l.size, m.lines.size.toLong()).toInt()] }
                },
                rescueCall = remember(rescued, stars) {
                    val parts = de.kurvenflug.core.model.Story.found(Levels.chapters, stars).size
                    val gold = de.kurvenflug.core.model.Season2.gold(Levels.chapters, stars).size
                    de.kurvenflug.core.model.Season2.crew.firstOrNull { it.n !in rescued && de.kurvenflug.core.model.Season2.lock(it.n, parts, gold) == null }
                },
                crewCount = rescued.size,
                andromeda = remember(allStars, stars) {
                    if (!de.kurvenflug.core.model.Andromeda.unlocked(de.kurvenflug.core.model.Story.found(Levels.chapters, stars).size)) null
                    else (de.kurvenflug.core.model.Andromeda.completed(allStars) + 1) to de.kurvenflug.core.model.Andromeda.novaParts(allStars)
                },
                onAndromeda = { nav.goOnce(Routes.ANDROMEDA) },
                season = remember(allStars) {
                    val today = de.kurvenflug.core.model.CalDate.today()
                    de.kurvenflug.core.model.Seasons.active(today)?.let { e -> e to de.kurvenflug.core.model.Seasons.done(e, de.kurvenflug.core.model.Seasons.year(today, e), allStars) }
                },
                onSeason = { nav.goOnce(Routes.SEASON) },
                onStation = { nav.goOnce(Routes.STATION) },
                onCrew = { nav.goOnce(Routes.CREW) },
                learning = {
                    val now = remember(progress) { de.kurvenflug.core.model.nowMillis() }
                    val played = remember(progress) { vm.played(progress) }
                    de.kurvenflug.app.ui.learning.LearningCard(
                        remember(played) { de.kurvenflug.core.model.Learning.status(played, now) },
                        remember(played) { de.kurvenflug.core.model.Learning.review(played, now, vm.today()) },
                    ) { nav.goOnce(Routes.LEARN) }
                },
            )
        }
        composable(Routes.LEVEL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
            val id = e.arguments?.read { getStringOrNull("id") }.orEmpty()
            val level = remember(id) {
                if (Daily.isDaily(id)) id.removePrefix(Daily.PREFIX).toLongOrNull()?.let { Daily.forDay(it) }
                else if (de.kurvenflug.core.model.Learning.isReview(id)) de.kurvenflug.core.model.Learning.levelFor(id)
                else if (de.kurvenflug.core.model.Andromeda.isAndromeda(id)) de.kurvenflug.core.model.Andromeda.levelFor(id)
                else if (de.kurvenflug.core.model.Seasons.isSeason(id)) de.kurvenflug.core.model.Seasons.levelFor(id)
                else if (de.kurvenflug.core.model.ChapterExtreme.isExtreme(id)) de.kurvenflug.core.model.ChapterExtreme.levelFor(id)
                else Levels.byId(id)
            } ?: return@composable
            // Pro-Inhalt ohne Pro (nur wenn ein Kaufweg angeboten wird): stattdessen die Pro-Seite
            val ps = LocalPro.current
            if (!de.kurvenflug.core.model.Pro.open(id, ps.pro, ps.locking, allStars)) {
                de.kurvenflug.app.ui.pro.ProScreen(ps, platform.proShop, onCode = vm.settings::setProCode, onBack = { nav.popSafe() }, locked = true)
                return@composable
            }
            val chapter = (if (de.kurvenflug.core.model.Andromeda.isAndromeda(id)) de.kurvenflug.core.model.Andromeda.chapter(id.removePrefix(de.kurvenflug.core.model.Andromeda.PREFIX).substringBefore('-').toInt()) else null)
                ?: (if (de.kurvenflug.core.model.Seasons.isSeason(id)) id.removePrefix(de.kurvenflug.core.model.Seasons.PREFIX).split('-').let { p ->
                    de.kurvenflug.core.model.SeasonEvent.entries.firstOrNull { it.key == p[0] }?.let { e -> de.kurvenflug.core.model.Seasons.chapter(e, p[1].toInt()) } } else null)
                ?: Levels.chapters.firstOrNull { it.number == level.chapter }
                ?: Chapter(0, "Tagesmission", "Jeden Tag neu", "🛰️", emptyList(), listOf(level))
            val next = if (de.kurvenflug.core.model.Andromeda.isAndromeda(id)) de.kurvenflug.core.model.Andromeda.next(id)
                else if (de.kurvenflug.core.model.Seasons.isSeason(id)) de.kurvenflug.core.model.Seasons.next(id)
                else if (de.kurvenflug.core.model.ChapterExtreme.isExtreme(id)) null
                else Levels.next(level.id)?.takeIf { it.chapter == level.chapter }
            val opened = remember(id) { de.kurvenflug.core.model.nowMillis() }
            LevelScreen(
                level = level, chapter = chapter, probeColor = probeColor,
                showIntro = level.number == 1 && chapter.intro.isNotEmpty() && !vm.settings.introSeen(chapter.number),
                onIntroSeen = { vm.settings.markIntroSeen(chapter.number) },
                onSolved = { s, c, a ->
                    vm.record(level.id, s, c, a, ((de.kurvenflug.core.model.nowMillis() - opened) / 1000).toInt())
                    // Gast-Planet geschafft → Themen-Sticker ins Album
                    if (de.kurvenflug.core.model.Seasons.isSeason(level.id)) {
                        val p = level.id.removePrefix(de.kurvenflug.core.model.Seasons.PREFIX).split('-')
                        val e = de.kurvenflug.core.model.SeasonEvent.entries.firstOrNull { it.key == p[0] }
                        val y = p.getOrNull(1)?.toIntOrNull()
                        if (e != null && y != null && de.kurvenflug.core.model.Seasons.done(e, y, allStars + (level.id to s))) vm.settings.addSticker(e.sticker.id)
                    }
                },
                onMiss = { a -> if (a == 3) vm.stuck(level.id, a, ((de.kurvenflug.core.model.nowMillis() - opened) / 1000).toInt()) },
                onNext = next?.let { n -> {
                    nav.navigate(Routes.level(n.id)) { popUpTo(Routes.LEVEL) { inclusive = true } }
                } },
                onBack = { nav.popSafe() },
                header = if (de.kurvenflug.core.model.Learning.isReview(level.id)) "🔁 Auffrischen · ${chapter.emoji} ${chapter.title}" else null,
                voltSolves = if (vm.settings.walkthroughsSeen(level.kind.name) == 0) 1 else 0,
                onWalkthrough = { vm.settings.markWalkthrough(level.kind.name) },
                onReward = { s -> vm.rollSticker(s) },
                onCheckStar = { s -> vm.upgradeStars(level.id, s) },
            )
        }
        composable(Routes.BOSS, arguments = listOf(navArgument("chapter") { type = NavType.IntType })) { e ->
            val boss = de.kurvenflug.core.model.Bosses.forChapter(e.arguments?.read { if (contains("chapter")) getInt("chapter") else 0 } ?: 0) ?: return@composable
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
            val today = remember { de.kurvenflug.core.model.CalDate.today() }
            val todayStart = remember { de.kurvenflug.core.model.CalDate.startOfToday() }
            val plays by remember { vm.weekPlays(todayStart - 13 * de.kurvenflug.core.model.Weekly.DAY) }.collectAsStateWithLifecycle(emptyList())
            val ps by vm.profiles.collectAsStateWithLifecycle()
            val cur by vm.profile.collectAsStateWithLifecycle()
            val week = remember(plays) {
                de.kurvenflug.core.model.Weekly.summary(plays.map { de.kurvenflug.core.model.PlayRecord(it.levelId, it.stars, it.attempts, it.seconds, it.at) },
                    de.kurvenflug.core.model.nowMillis(), todayStart)
            }
            val labels = remember { (6 downTo 0).map { de.kurvenflug.core.model.CalDate.WEEKDAYS[today.minusDays(it.toLong()).dayOfWeek - 1] } }
            de.kurvenflug.app.ui.together.ParentScreen(ps.first { it.id == cur }, week, labels) { nav.popSafe() }
        }
        composable(Routes.RESULTS) { de.kurvenflug.app.ui.together.ResultsBoard({ nav.popSafe() }) }
        composable(Routes.STICKERS) {
            val owned by vm.settings.stickers.collectAsStateWithLifecycle()
            de.kurvenflug.app.ui.collection.StickerAlbum(owned) { nav.popSafe() }
        }
        composable(Routes.HANGAR) {
            de.kurvenflug.app.ui.story.HangarScreen(remember(stars) { de.kurvenflug.core.model.Story.found(Levels.chapters, stars) }, { nav.popSafe() },
                gold = remember(stars) { de.kurvenflug.core.model.Season2.gold(Levels.chapters, stars) }, onCrew = { nav.goOnce(Routes.CREW) },
                onAndromeda = { nav.goOnce(Routes.ANDROMEDA) })
        }
        composable(Routes.STATION) {
            val rooms by vm.settings.rooms.collectAsStateWithLifecycle()
            val decor by vm.settings.decor.collectAsStateWithLifecycle()
            val owned by vm.settings.stickers.collectAsStateWithLifecycle()
            de.kurvenflug.app.ui.story.StationScreen(allStars, rooms, decor, owned, rescued,
                onUnlock = { id -> de.kurvenflug.core.model.Station.rooms.first { it.id == id }.let { r -> if (de.kurvenflug.core.model.Station.canUnlock(r, allStars, rooms)) vm.settings.unlockRoom(id) } },
                onDecor = { r, sl, k -> vm.settings.setDecor(r, sl, k) }, onBack = { nav.popSafe() })
        }
        composable(Routes.SEASON) {
            val today = remember { de.kurvenflug.core.model.CalDate.today() }
            val e = de.kurvenflug.core.model.Seasons.active(today) ?: run { LaunchedEffect(Unit) { nav.popSafe() }; return@composable }
            de.kurvenflug.app.ui.story.SeasonScreen(e, de.kurvenflug.core.model.Seasons.year(today, e), allStars,
                onLevel = { nav.goOnce(Routes.level(it.id)) }, onBack = { nav.popSafe() })
        }
        composable(Routes.ANDROMEDA) {
            de.kurvenflug.app.ui.story.AndromedaScreen(allStars, onLevel = { nav.goOnce(Routes.level(it.id)) }, onBack = { nav.popSafe() })
        }
        composable(Routes.CREW) {
            de.kurvenflug.app.ui.story.CrewScreen(rescued, remember(stars) { de.kurvenflug.core.model.Story.found(Levels.chapters, stars).size },
                remember(stars) { de.kurvenflug.core.model.Season2.gold(Levels.chapters, stars).size },
                onRescue = { nav.goOnce(Routes.rescue(it)) }, onBack = { nav.popSafe() })
        }
        composable(Routes.RESCUE, arguments = listOf(navArgument("n") { type = NavType.IntType })) { e ->
            val n = e.arguments?.read { if (contains("n")) getInt("n") else null } ?: return@composable
            de.kurvenflug.app.ui.story.RescueScreen(n, probeColor, onWon = { s, m -> vm.record(de.kurvenflug.core.model.Season2.id(n), s, emptySet(), m + 5) },
                onBack = { nav.popSafe() }, reward = { s -> vm.rollSticker(s) })
        }
        composable(Routes.LEARN) {
            val progress by vm.progress.collectAsStateWithLifecycle()
            val now = remember(progress) { de.kurvenflug.core.model.nowMillis() }
            val played = remember(progress) { vm.played(progress) }
            de.kurvenflug.app.ui.learning.LearningScreen(
                status = remember(played) { de.kurvenflug.core.model.Learning.status(played, now) },
                review = remember(played) { de.kurvenflug.core.model.Learning.review(played, now, vm.today()) },
                doneToday = de.kurvenflug.core.model.Learning.doneToday(played, vm.today()),
                now = now,
                onReview = { nav.goOnce(Routes.level(it.id)) },
                onBack = { nav.popSafe() },
                stars = stars + vm.extremeStars.collectAsStateWithLifecycle().value,
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
        composable(Routes.PRO) {
            de.kurvenflug.app.ui.pro.ProScreen(LocalPro.current, platform.proShop, onCode = vm.settings::setProCode, onBack = { nav.popSafe() })
        }
        composable(Routes.SETTINGS) {
            val upd by vm.availableUpdate.collectAsStateWithLifecycle()
            val autoUpd by vm.settings.autoUpdate.collectAsStateWithLifecycle()
            val sound by vm.settings.sound.collectAsStateWithLifecycle()
            val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
            val music by vm.settings.music.collectAsStateWithLifecycle()
            SettingsScreen(
                music = music, onMusic = vm.settings::setMusic,
                musicVolume = vm.settings.musicVolume.collectAsStateWithLifecycle().value, onMusicVolume = vm.settings::setMusicVolume,
                sfxVolume = vm.settings.sfxVolume.collectAsStateWithLifecycle().value, onSfxVolume = { vm.settings.setSfxVolume(it); platform.sfx?.play(de.kurvenflug.app.ui.fx.Sound.STAR, volume = 0.6f) },
                sound = sound, onSound = vm.settings::setSound,
                vibration = vibration, onVibration = vm.settings::setVibration,
                autoUpdate = autoUpd, onAutoUpdate = vm.settings::setAutoUpdate,
                installed = platform.versionName,
                update = upd?.let { UpdateState.Available(it) } ?: UpdateState.Idle,
                onBack = { nav.popSafe() },
                onReset = vm::resetAll,
                onPro = if (platform.proShop != null) ({ nav.goOnce(Routes.PRO) }) else null,
                voltVoice = vm.settings.voltVoice.collectAsStateWithLifecycle().value, onVoltVoice = vm.settings::setVoltVoice,
                readAloud = vm.settings.readAloud.collectAsStateWithLifecycle().value, onReadAloud = vm.settings::setReadAloud,
                readSpeed = vm.settings.readSpeed.collectAsStateWithLifecycle().value, onReadSpeed = vm.settings::setReadSpeed,
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

