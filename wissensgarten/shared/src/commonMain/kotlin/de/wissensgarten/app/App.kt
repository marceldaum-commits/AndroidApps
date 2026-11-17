package de.wissensgarten.app

import de.wissensgarten.app.ui.garden.QuestCard
import de.wissensgarten.app.ui.garden.CrittersCard
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.wissensgarten.app.ui.family.FamilyScreen
import de.wissensgarten.app.ui.garden.BedScreen
import de.wissensgarten.app.ui.garden.GardenScreen
import de.wissensgarten.app.ui.onboarding.OnboardingScreen
import de.wissensgarten.app.ui.onboarding.TutorialScreen
import de.wissensgarten.app.ui.review.ReviewScreen
import de.wissensgarten.app.ui.settings.SettingsScreen
import de.wissensgarten.app.ui.shop.ShopScreen
import de.wissensgarten.app.ui.store.StoreScreen
import de.wissensgarten.app.ui.adventure.*
import de.wissensgarten.app.ui.duel.DuelHubScreen
import de.wissensgarten.app.ui.duel.DuelPlayScreen
import de.wissensgarten.app.ui.common.PackBrowser
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.ui.garden.ReminderPromptCard
import de.wissensgarten.app.ui.theme.WissensgartenTheme
import de.wissensgarten.app.ui.fx.LocalSfx
import de.wissensgarten.app.ui.fx.LocalWind
import de.wissensgarten.app.ui.fx.rememberWind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

object Routes {
    const val ONBOARDING = "einrichtung"
    const val ADD_PROFILE = "profil-neu"
    const val TUTORIAL = "anleitung?erstes={erstes}"
    fun tutorial(first: Boolean) = "anleitung?erstes=$first"
    const val GARDEN = "garten"
    const val SHOP = "samen"
    const val STORE = "laden"
    const val FAMILY = "familie"
    const val SETTINGS = "mehr"
    const val BED = "beet/{id}"
    fun bed(id: String) = "beet/$id"
    const val REVIEW = "giessen"
    const val DUEL = "duell"
    const val INTRO = "erklaerung/{pack}"
    fun intro(pack: String) = "erklaerung/$pack"
    const val CHEST = "truhe"
    const val STORY = "abenteuer"
    const val HYBRID = "kreuzung"
    const val KLASSE = "klasse"
    const val RADIO = "radio"
    const val SAFARI = "safari"
    const val PRIVACY = "datenschutz"
    const val WEEK_REPORT = "wochenbericht"
    const val PLUS = "plus"
    const val WEEDS = "unkraut"
    const val BLITZ = "blitz"
    const val RHYTHM = "rhythmus"
    const val HIVE = "bienenstock"
    const val LOCI = "spaziergang"
    const val FILM = "film"
    const val LOCAL_DUEL = "zu-zweit"
    const val CHAPTER = "kapitel/{n}"
    fun chapter(n: Int) = "kapitel/$n"
    const val DUEL_PACK = "duell-paket"
    const val DUEL_PLAY = "duell-spiel"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.GARDEN, "Garten", Icons.Filled.LocalFlorist),
    Tab(Routes.SHOP, "Samen", Icons.Filled.Spa),
    Tab(Routes.STORE, "Laden", Icons.Filled.Storefront),
    Tab(Routes.FAMILY, "Familie", Icons.Filled.Group),
    Tab(Routes.SETTINGS, "Mehr", Icons.Filled.Settings),
)

/** Nur zurück, wenn der Bildschirm gerade aktiv ist – ein Doppeltipp auf „Zurück“ darf nicht den Garten wegnehmen. */
fun androidx.navigation.NavController.popSafe(): Boolean {
    val cur = currentBackStackEntry ?: return false
    if (cur.lifecycle.currentState != androidx.lifecycle.Lifecycle.State.RESUMED || previousBackStackEntry == null) return false
    return popBackStack()
}

/** Für Start-Knöpfe: ein Doppeltipp startet nicht zwei Runden übereinander. */
fun androidx.navigation.NavController.goOnce(route: String) {
    val cur = currentBackStackEntry
    if (cur != null && cur.lifecycle.currentState != androidx.lifecycle.Lifecycle.State.RESUMED) return
    if (cur?.destination?.route == route) return
    navigate(route) { launchSingleTop = true }
}


/**
 * Die ganze Wissensgarten-Oberfläche – gemeinsam für Android-App und WebApp.
 * [host] liefert, was je Plattform anders ist (Ton, Teilen, Sync, Updates …).
 */
@Composable
fun WissensgartenApp(vm: AppViewModel, host: AppHost, startRoute: String,
                     waterRequest: MutableStateFlow<Boolean> = MutableStateFlow(false),
                     duelRequest: MutableStateFlow<String?> = MutableStateFlow(null)) {
    AppUi(vm, host).Root(startRoute, waterRequest, duelRequest)
}

private class AppUi(val vm: AppViewModel, val host: AppHost) {
    @Composable
    fun Root(startRoute: String, waterRequest: MutableStateFlow<Boolean>, duelRequest: MutableStateFlow<String?>) {
            val theme by vm.theme.collectAsStateWithLifecycle()
        val soundOn by vm.settings.sound.collectAsStateWithLifecycle()
        val sfx = host.sfx
        LaunchedEffect(soundOn) { sfx.enabled = soundOn }
        val musicOn by vm.settings.music.collectAsStateWithLifecycle()
        LaunchedEffect(musicOn) { host.music.setEnabled(musicOn) }
        WissensgartenTheme(theme) {
          val readMode by vm.settings.readAloud.collectAsStateWithLifecycle()
          val prof by vm.profile.collectAsStateWithLifecycle()
          val sylMode by vm.settings.syllables.collectAsStateWithLifecycle()
          val syllablesOn = when (sylMode) {
              de.wissensgarten.core.data.ReadAloud.ON -> true
              de.wissensgarten.core.data.ReadAloud.OFF -> false
              de.wissensgarten.core.data.ReadAloud.AUTO -> (prof?.level ?: 0) in 1..2
          }
          val readAloud = when (readMode) {
              de.wissensgarten.core.data.ReadAloud.ON -> true
              de.wissensgarten.core.data.ReadAloud.OFF -> false
              de.wissensgarten.core.data.ReadAloud.AUTO -> (prof?.level ?: 0) in 1..4
          }
          CompositionLocalProvider(
              LocalWind provides rememberWind(), LocalSfx provides sfx,
              de.wissensgarten.app.ui.fx.LocalVoice provides host.voice,
                  de.wissensgarten.app.ui.fx.LocalHost provides host,
              de.wissensgarten.app.ui.fx.LocalReadAloud provides readAloud,
              de.wissensgarten.app.ui.fx.LocalSyllables provides syllablesOn,
              de.wissensgarten.app.ui.fx.LocalVibration provides vm.settings.vibration.collectAsStateWithLifecycle().value,
          ) {
            // imePadding: Eingabefelder rutschen über die Tastatur (bei Randlos-Darstellung nötig)
            Surface(Modifier.fillMaxSize().imePadding(), color = MaterialTheme.colorScheme.background) {
              // Tablets: Inhalt mittig in angenehmer Breite statt über den ganzen Bildschirm gezogen
              Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
              Box(Modifier.fillMaxHeight().widthIn(max = 720.dp)) {
              de.wissensgarten.app.ui.common.ParentGateHost {
                val nav = rememberNavController()
                AppNav(nav, startRoute)
                // Widget-Tipp: direkt in die Gieß-Runde
                val water by waterRequest.collectAsStateWithLifecycle()
                val beds by vm.beds.collectAsStateWithLifecycle()
                val duelText by duelRequest.collectAsStateWithLifecycle()
                val onboardedNow by vm.onboarded.collectAsStateWithLifecycle()
                LaunchedEffect(duelText, onboardedNow) {
                    val t = duelText ?: return@LaunchedEffect
                    if (!onboardedNow) return@LaunchedEffect
                    duelRequest.value = null
                    if (t.contains("WGK1-")) { if (vm.openClassCode(t)) nav.navigate(Routes.KLASSE) }
                    else if (vm.openDuelCode(t)) nav.navigate(Routes.DUEL)
                }
                LaunchedEffect(water, beds) {
                    if (water && beds.isNotEmpty()) {
                        waterRequest.value = false
                        if (vm.startSession()) nav.navigate(Routes.REVIEW)
                    }
                }
              }
              }
              }
            }
          }
        }
    }

    @Composable
    private fun AppNav(nav: NavHostController, startRoute: String) {
        val profile by vm.profile.collectAsStateWithLifecycle()
        val profiles by vm.profiles.collectAsStateWithLifecycle()
        val beds by vm.beds.collectAsStateWithLifecycle()
        val packs by vm.packs.collectAsStateWithLifecycle()
        val now by vm.now.collectAsStateWithLifecycle()
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val showBar = TABS.any { it.route == route }

        val gate = de.wissensgarten.app.ui.common.LocalParentGate.current
        fun goTab(r: String) = nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }

        val sfx = LocalSfx.current
        // Musik passend zum Ort: Gieß-Runde/Duell schwungvoll, Gute-Nacht/abends ein Schlaflied, sonst Garten
        val music = host.music
        val hour = remember(now) { ((now + de.wissensgarten.core.model.Platform.tzOffsetMs(now)) / 3_600_000L % 24).toInt() }
        LaunchedEffect(Unit) { music.prepare(de.wissensgarten.core.model.MusicMood.GARDEN, de.wissensgarten.core.model.MusicMood.PLAY, de.wissensgarten.core.model.MusicMood.NIGHT) }
        LaunchedEffect(route) { music.duck(route == Routes.RHYTHM) }
        LaunchedEffect(route, hour) {
            val evening = hour >= 19 || hour < 6
            music.setMood(when {
                route == Routes.REVIEW && vm.sessionCalm -> de.wissensgarten.core.model.MusicMood.NIGHT
                route == Routes.REVIEW || route == Routes.DUEL_PLAY || route == Routes.BLITZ || route == Routes.WEEDS -> de.wissensgarten.core.model.MusicMood.PLAY
                evening || route == Routes.STORY || route == Routes.CHAPTER -> de.wissensgarten.core.model.MusicMood.NIGHT
                // Festzeit: eigene Musik im Garten
                else -> when (de.wissensgarten.core.model.Seasons.festival(de.wissensgarten.core.model.Seasons.today())?.id) {
                    "advent" -> de.wissensgarten.core.model.MusicMood.ADVENT
                    "halloween" -> de.wissensgarten.core.model.MusicMood.HALLOWEEN
                    "ostern" -> de.wissensgarten.core.model.MusicMood.OSTERN
                    else -> de.wissensgarten.core.model.MusicMood.GARDEN
                }
            })
        }
        // Gesunde Lernzeit: nach Ablauf ruht der Garten – laufende Runden dürfen zu Ende gespielt werden
        val limit by vm.settings.dailyLimit.collectAsStateWithLifecycle()
        var restRev by remember { mutableIntStateOf(0) }
        var resting by remember { mutableStateOf(false) }
        LaunchedEffect(now, limit, restRev) {
            host.flushUsage()
            val (used, extra) = vm.settings.usage(de.wissensgarten.core.model.Scheduler.localDay(de.wissensgarten.core.model.Platform.nowMs()))
            resting = de.wissensgarten.core.model.StudyTime.isOver(used, limit, extra)
        }
        val playing = route in setOf(Routes.WEEDS, Routes.BLITZ, Routes.RHYTHM, Routes.LOCI, Routes.REVIEW, Routes.DUEL_PLAY, Routes.LOCAL_DUEL, Routes.ONBOARDING, Routes.TUTORIAL, Routes.ADD_PROFILE, Routes.PRIVACY)
        Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBar) NavigationBar {
                    TABS.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                sfx?.play(de.wissensgarten.app.ui.fx.Sound.TAP, volume = 0.5f)
                                // „Mehr“ enthält Konto, Updates, Löschen → nur für Eltern
                                if (t.route == Routes.SETTINGS) gate("Die Einstellungen sind für Eltern: Sync mit Google, Updates, Daten löschen.") { goTab(t.route) }
                                else goTab(t.route)
                            },
                            icon = { Icon(t.icon, null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets(0),
        ) { pad ->
            NavHost(nav, startDestination = startRoute, modifier = Modifier.padding(pad)) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(packs, addingProfile = false, onCancel = {}, onFinish = { name, avatar, level, ids, own ->
                        vm.finishOnboarding(name, avatar, level, ids, own)
                        nav.navigate(Routes.tutorial(true)) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    }, welcomeExtra = {
                        // Neues Gerät: Garten aus dem Google Drive holen (nur Android)
                        host.WelcomeRestore { found ->
                            if (found && vm.settings.onboarded.value) nav.navigate(Routes.GARDEN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                        }
                    })
                }
                composable(Routes.ADD_PROFILE) {
                    OnboardingScreen(packs, addingProfile = true, onCancel = { nav.popSafe() }) { name, avatar, level, ids, own ->
                        vm.addProfile(name, avatar, level, ids, own)
                        nav.popSafe(); goTab(Routes.GARDEN)
                    }
                }
                composable(Routes.TUTORIAL) { entry ->
                    val first = entry.savedStateHandle.get<String>("erstes") == "true"
                    TutorialScreen {
                        if (first) nav.navigate(Routes.GARDEN) { popUpTo(0) { inclusive = true } } else nav.popSafe()
                    }
                }
                composable(Routes.GARDEN) {
                    val decos by vm.activeDecos.collectAsStateWithLifecycle()
                    val rev by vm.adventureRev.collectAsStateWithLifecycle()
                    val friends = remember(rev, profile) { vm.gardenFriends() }
                    val waiting = remember(rev, beds) { (vm.chestsEarned() - vm.chestsOpened()).coerceAtLeast(0) }
                    val asked by vm.settings.reminderAsked.collectAsStateWithLifecycle()
                    val hour by vm.settings.reminderHour.collectAsStateWithLifecycle()
                    val askPermission = host.rememberNotificationPermission { ok -> vm.setReminder(ok, hour) }
                    WithProfile(profile) { p ->
                        GardenScreen(
                            profile = p, beds = beds, now = now, seedsLeft = vm.seedsLeft(p),
                            onWater = { if (vm.startSession()) nav.goOnce(Routes.REVIEW) },
                            onOpenBed = { nav.navigate(Routes.bed(it)) },
                            onProfile = { goTab(Routes.FAMILY) },
                            onShop = { goTab(Routes.SHOP) },
                            decos = decos,
                            onDuel = { nav.navigate(Routes.DUEL) },
                            friends = friends, chestsWaiting = waiting, onChest = { nav.goOnce(Routes.CHEST) },
                            surprises = remember(rev, profile) { vm.surprises() },
                            festivalGreeting = vm.festival()?.greeting,
                            onSurprise = { vm.collectSurprise(it) },
                            extraCards = {
                                val newCh = remember(rev, beds) { vm.newChapter() }
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    val upd by vm.availableUpdate.collectAsStateWithLifecycle()
                                    upd?.let { de.wissensgarten.app.ui.settings.UpdateBanner(it.version) { gate("Ein Update installieren dürfen nur Eltern.") { goTab(Routes.SETTINGS) } } }
                                    // Klassenarbeiten: Countdown-Karten oder Einladung zum Eintragen
                                    val plans = remember(rev, beds, now) { vm.examPlans() }
                                    var addExam by remember { mutableStateOf(false) }
                                    plans.forEach { plan ->
                                        de.wissensgarten.app.ui.exam.ExamCard(
                                            plan, beds.firstOrNull { it.bed.id == plan.exam.bedId }?.bed?.name ?: "",
                                            onStart = { if (vm.startExamSession(plan.exam.id)) nav.goOnce(Routes.REVIEW) },
                                            onFeedback = { vm.removeExam(plan.exam.id) },
                                            onDelete = { vm.removeExam(plan.exam.id) },
                                        )
                                    }
                                    if (plans.isNotEmpty()) TextButton(onClick = { addExam = true }, modifier = Modifier.padding(horizontal = 16.dp)) { Text("📝 Weiteren Termin eintragen") }
                                    if (addExam) de.wissensgarten.app.ui.exam.AddExamDialog(
                                        beds.map { it.bed.id to "${it.bed.emoji} ${it.bed.name}" }, null,
                                        onDismiss = { addExam = false },
                                        onAdd = { b, t, d -> vm.addExam(b, t, d); addExam = false },
                                    )
                                    // Tagesaufgaben und Garten-Bewohner
                                    if (beds.isNotEmpty()) {
                                        val quests = remember(rev, beds) { vm.questsToday() }
                                        QuestCard(quests, remember(rev, beds) { vm.questProgress() }, remember(rev, beds) { vm.questWeekDone() },
                                            remember(rev) { vm.questChestOpened() }) { vm.openQuestChest() }
                                        var streak by remember { mutableStateOf(0 to 0) }
                                        LaunchedEffect(beds, now) { streak = vm.streaks() }
                                        CrittersCard(streak.first, streak.second)
                                    }
                                    // Alles Weitere als kompakte Kachelreihe – „Gießen“ bleibt oben sichtbar
                                    val mix = remember(beds, packs) { vm.mixCandidates() }
                                    val weeds = remember(rev, beds) { vm.weeds() }
                                    val known = remember(beds) { beds.sumOf { b -> b.cards.count { !it.memory.isSeed } } }
                                    de.wissensgarten.app.ui.garden.DiscoverRow(buildList {
                                        if (weeds.isNotEmpty()) add(de.wissensgarten.app.ui.garden.DiscoverItem("🌿", "Unkraut-Jäten",
                                            "${weeds.size} freche Unkräuter wachsen – zupf sie raus!", Color(0xFFDCEDC8), badge = "${weeds.size}") { nav.navigate(Routes.WEEDS) })
                                        if (known >= 4) add(de.wissensgarten.app.ui.garden.DiscoverItem("⚡", "Blitzrunde",
                                            "60 Sekunden Gewitter – vertreib es mit Wissen!", Color(0xFFCFD8DC)) { nav.navigate(Routes.BLITZ) })
                                        if (known >= 4) add(de.wissensgarten.app.ui.garden.DiscoverItem("🎵", "Rhythmus",
                                            "Tippe die richtige Antwort im Takt!", Color(0xFFF3E5F5)) { nav.navigate(Routes.RHYTHM) })
                                        add(de.wissensgarten.app.ui.garden.DiscoverItem("🍯", "Bienenstock",
                                            "Brummis Zuhause – mit Honig Räume freischalten.", Color(0xFFFFF8E1)) { nav.navigate(Routes.HIVE) })
                                        if (known >= 3) add(de.wissensgarten.app.ui.garden.DiscoverItem("🚶", "Spaziergang",
                                            "Schwierige Karten an Orte im Garten legen.", Color(0xFFE0F2F1)) { nav.navigate(Routes.LOCI) })
                                        if (known >= 1) add(de.wissensgarten.app.ui.garden.DiscoverItem("🎬", "Garten-Film",
                                            "Zeitraffer: So ist dein Garten gewachsen.", Color(0xFFFCE4EC)) { nav.navigate(Routes.FILM) })
                                        add(de.wissensgarten.app.ui.garden.DiscoverItem("📖", "Abenteuer",
                                            newCh?.let { "Neues Kapitel: ${it.title}" } ?: "Die Geschichte von der Wissensblume", Color(0xFFEDE7F6),
                                            badge = if (newCh != null) "NEU" else null) { nav.navigate(Routes.STORY) })
                                        if (mix != null) add(de.wissensgarten.app.ui.garden.DiscoverItem("🔀", "Mischrunde",
                                            "${mix.first.bed.name} × ${mix.second.bed.name} – züchte eine Kreuzung!", Color(0xFFE0F7FA)) { if (vm.startMixed()) nav.goOnce(Routes.REVIEW) })
                                        if (plans.isEmpty() && (profile?.level ?: 0) in 1..13 && beds.isNotEmpty()) add(de.wissensgarten.app.ui.garden.DiscoverItem("📝", "Klassenarbeit?",
                                            "Eintragen – Brummi plant, damit am Tag X alles sitzt.", Color(0xFFFFF8E1)) { addExam = true })
                                        if (beds.isNotEmpty()) add(de.wissensgarten.app.ui.garden.DiscoverItem("🎧", "Brummi-Radio",
                                            "Lernen ohne Bildschirm – zuhören und laut antworten.", Color(0xFFE8EAF6)) { nav.navigate(Routes.RADIO) })
                                        if (host.features.safari) add(de.wissensgarten.app.ui.garden.DiscoverItem("🔭", "Wissens-Safari",
                                            "Dinge fotografieren und benennen.", Color(0xFFE8F5E9)) { nav.navigate(Routes.SAFARI) })
                                    })
                                }
                            },
                            onGoodNight = if (remember(rev) { vm.goodNightDoneToday() }) null else { { if (vm.startGoodNight()) nav.goOnce(Routes.REVIEW) } },
                            reminderPrompt = if (asked) null else {
                                { ReminderPromptCard(hour, onYes = askPermission, onNo = { vm.setReminder(false) }) }
                            },
                        )
                    }
                }
                composable(Routes.SHOP) {
                    WithProfile(profile) { p ->
                        ShopScreen(
                            profile = p, packs = packs + vm.plusPreviews.collectAsStateWithLifecycle().value, beds = beds, canUse = vm.entitlements::canUse,
                            onLocked = {
                                if (vm.entitlements.code != null) { host.toast("🐝 Brummi holt die Plus-Pakete … gleich noch einmal antippen."); vm.refreshPlusPacksIfDue(force = true) }
                                else gate("Dieses Paket gehört zu Wissensgarten Plus.") { nav.navigate(Routes.PLUS) }
                            },
                            onPlant = vm::plantPack,
                            onOpenBed = { nav.navigate(Routes.bed(it)) },
                            onOwnBed = { n, e -> vm.addOwnBed(n, e) { id -> nav.navigate(Routes.bed(id)) } },
                            onClassCode = { vm.pendingClassSet = null; nav.navigate(Routes.KLASSE) },
                        )
                    }
                }
                composable(Routes.STORE) {
                    val inventory by vm.inventory.collectAsStateWithLifecycle()
                    WithProfile(profile) { p ->
                        StoreScreen(
                            profile = p, inventory = inventory, beds = beds,
                            plusPacks = packs.filter { it.isPlus },
                            onBuy = vm::buy,
                            onToggleDeco = vm::setDecoActive,
                            onUseSpecies = { bed, sp -> vm.setBedSpecies(bed, sp) },
                            onOpenPacks = { goTab(Routes.SHOP) },
                            album = {
                                val rev by vm.adventureRev.collectAsStateWithLifecycle()
                                val opened = remember(rev, p) { vm.chestsOpened() }
                                val earned = remember(rev, beds) { vm.chestsEarned() }
                                val inGarden = remember(rev, p) { vm.gardenFriends().map { it.id }.toSet() }
                                FriendsAlbum(opened, earned, inGarden, onToggle = { vm.toggleGardenFriend(it) }, onOpenChest = { nav.goOnce(Routes.CHEST) },
                                    hybrids = remember(rev) { vm.hybrids() },
                                    crystals = remember(beds) { beds.flatMap { b -> b.cards.filter { de.wissensgarten.core.model.Crystals.isCrystal(it.memory) }.map { it.front to it.back } } },
                                    crystalsOnTheWay = remember(beds) { beds.sumOf { b -> b.cards.count { c -> de.wissensgarten.core.model.Crystals.progress(c.memory)?.let { it < 1.0 } == true } } })
                            },
                        )
                    }
                }
                composable(Routes.INTRO) { entry ->
                    val pid = entry.savedStateHandle.get<String>("pack")
                    val pack = packs.firstOrNull { it.id == pid }
                    if (pack == null) LaunchedEffect(Unit) { nav.popBackStack() } else IntroScreen(pack) {
                        vm.markIntroSeen(pack.id); nav.popSafe()
                    }
                }
                composable(Routes.LOCAL_DUEL) {
                    // Thema: ein Paket aus dem Garten des aktiven Profils (sonst ein passendes für die Stufe)
                    val pack = remember {
                        val mine = beds.mapNotNull { b -> b.bed.packId?.let { pid -> packs.firstOrNull { it.id == pid } } }
                        (mine.ifEmpty { packs.filter { (profile?.level ?: 0) in it.grades }.ifEmpty { packs } }).randomOrNull()
                    }
                    // Pakete noch nicht geladen (z. B. nach Neustart der App) → zurück statt Absturz
                    if (pack == null) { LaunchedEffect(Unit) { nav.popBackStack() }; return@composable }
                    val qs = remember(pack) { de.wissensgarten.core.model.Duels.questions(pack, kotlin.random.Random.nextInt()) }
                    de.wissensgarten.app.ui.duel.LocalDuelScreen(qs, "${pack.emoji} ${pack.title}", onClose = { nav.popSafe() }, onDew = { vm.addDew(it) })
                }
                if (host.features.safari) composable(Routes.SAFARI) {
                    host.SafariScreen(
                        found = remember { vm.safariCount() },
                        englishDefault = (profile?.level ?: 0) !in 1..2,
                        onPlant = { w, en, img -> vm.addSafariFind(w, en, img) },
                        onBack = { nav.popSafe() },
                    )
                }
                composable(Routes.RADIO) {
                    val cards = remember { vm.radioCards() }
                    // Keine Musik im Radio – Brummi und das Kind sollen sich hören
                    DisposableEffect(Unit) { host.music.setEnabled(false); onDispose { host.music.setEnabled(vm.settings.music.value) } }
                    de.wissensgarten.app.ui.radio.RadioScreen(
                        cards = cards,
                        peers = beds.associate { b -> b.bed.id to b.cards.map { it.back }.distinct() },
                        subjects = beds.associate { b -> b.bed.id to b.bed.packId?.let { pid -> packs.firstOrNull { it.id == pid }?.subject } },
                        onRate = { c, r, d -> vm.review(c, r, d) },
                        onClose = { vm.endSession(); nav.popSafe(); host.syncSoon(5_000) },
                    )
                }
                composable(Routes.KLASSE) {
                    val initial = remember { vm.pendingClassSet.also { vm.pendingClassSet = null } }
                    de.wissensgarten.app.ui.klasse.ClassImportScreen(
                        initial = initial,
                        onDecode = { de.wissensgarten.core.model.ClassCodes.decode(it) },
                        onPlant = { s -> vm.importClassSet(s) { id -> nav.navigate(Routes.bed(id)) { popUpTo(Routes.KLASSE) { inclusive = true } } } },
                        onBack = { nav.popSafe() },
                    )
                }
                composable(Routes.HYBRID) {
                    val h = remember { vm.consumeNewHybrid() }
                    if (h == null) LaunchedEffect(Unit) { nav.popBackStack() } else HybridReveal(h) { nav.popSafe() }
                }
                composable(Routes.STORY) {
                    val rev by vm.adventureRev.collectAsStateWithLifecycle()
                    StoryScreen(remember(beds) { vm.grownPlants() }, remember(rev) { vm.chaptersRead() },
                        onBack = { nav.popSafe() }, onRead = { nav.navigate(Routes.chapter(it.number)) })
                }
                composable(Routes.CHAPTER) { entry ->
                    val n = entry.savedStateHandle.get<String>("n")?.toIntOrNull()
                    val ch = de.wissensgarten.core.model.Story.chapters.firstOrNull { it.number == n }
                    var finished by remember(n) { mutableStateOf(false) }
                    if (ch == null) LaunchedEffect(Unit) { nav.popBackStack() }
                    else if (!finished) StoryPlayer("Kapitel ${ch.number}: ${ch.title}", ch.scenes) { vm.markChapterRead(ch.number); finished = true }
                    else ChapterEnd(
                        done = ch, grown = remember(beds) { vm.grownPlants() },
                        onNext = { nx -> nav.navigate(Routes.chapter(nx.number)) { popUpTo(Routes.STORY) } },
                        onWater = {
                            nav.popBackStack(Routes.STORY, inclusive = true)
                            if (vm.startSession()) nav.navigate(Routes.REVIEW) else nav.navigate(Routes.SHOP)
                        },
                        onList = { nav.popSafe() },
                    )
                }
                composable(Routes.CHEST) {
                    ChestReveal(onOpen = { vm.openChest() }, onClose = { nav.popSafe() })
                }
                composable(Routes.DUEL) {
                    val history by vm.duelHistory.collectAsStateWithLifecycle()
                    DuelHubScreen(
                        myName = profile?.name ?: "",
                        packs = packs, history = history, preview = vm.currentDuel,
                        onBack = { nav.popSafe() },
                        onNew = { nav.navigate(Routes.DUEL_PACK) },
                        onCode = vm::openDuelCode,
                        onPlay = { d -> vm.showDuel(d); nav.navigate(Routes.DUEL_PLAY) },
                        onShowDuel = { vm.showDuel(it) },
                    )
                }
                composable(Routes.DUEL_PACK) {
                    Column(Modifier.fillMaxSize()) {
                        PackBrowser(
                            packs = packs, level = profile?.level ?: 0,
                            isChosen = { false },
                            onToggle = { p -> vm.newDuel(p); nav.navigate(Routes.DUEL_PLAY) { popUpTo(Routes.DUEL) } },
                            chosenLabel = "", chooseLabel = "Duell",
                            modifier = Modifier.fillMaxSize().statusBarsPadding(),
                            header = {
                                item {
                                    Text("Worum geht's im Duell?", style = MaterialTheme.typography.headlineSmall)
                                    Text("Wähle ein Wissenspaket – dein Gegner bekommt genau dieselben 10 Fragen.", style = MaterialTheme.typography.bodyMedium)
                                }
                            },
                        )
                    }
                }
                composable(Routes.DUEL_PLAY) {
                    val d = vm.currentDuel
                    val pack = d?.let { dd -> packs.firstOrNull { it.id == dd.packId } }
                    if (d == null || pack == null) {
                        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                            Text("Dieses Wissenspaket gibt es in deiner App-Version nicht. Bitte aktualisiere den Wissensgarten.")
                            TextButton(onClick = { nav.popSafe() }) { Text("Zurück") }
                        }
                    } else DuelPlayScreen(
                        duel = d, pack = pack, myName = profile?.name ?: "Ich",
                        onFinish = { c, ms, sc -> vm.finishDuel(c, ms, sc) },
                        onClose = { nav.popBackStack(Routes.DUEL, inclusive = false).let { ok -> if (!ok) nav.navigate(Routes.DUEL) } },
                    )
                }
                composable(Routes.FAMILY) {
                    val allBeds by vm.allBeds.collectAsStateWithLifecycle()
                    val logs by vm.weekLogs.collectAsStateWithLifecycle()
                    val fRev by vm.adventureRev.collectAsStateWithLifecycle()
                    val scope = rememberCoroutineScope()
                    FamilyScreen(
                        profiles, profile?.id, allBeds, logs, now,
                        onSwitch = { vm.switchProfile(it); goTab(Routes.GARDEN) },
                        onAdd = {
                            if (profiles.size < vm.entitlements.maxProfiles()) nav.navigate(Routes.ADD_PROFILE)
                            else gate("Mehr als ${de.wissensgarten.core.data.PlusEntitlements.FREE_PROFILES} Profile gibt es mit Wissensgarten Plus.") { nav.navigate(Routes.PLUS) }
                        },
                        onDuel = { nav.navigate(Routes.DUEL) },
                        familyClaimed = remember(fRev) { vm.familyClaimed() },
                        onClaimFamily = { vm.claimFamilyTree() },
                        familyReward = remember(fRev) { vm.settings.familyReward() },
                        onFamilyReward = { vm.settings.setFamilyReward(it) },
                        onLocalDuel = { nav.navigate(Routes.LOCAL_DUEL) },
                        onWeekReport = { gate("Der Wochenbericht ist für Eltern.") { nav.navigate(if (vm.entitlements.canUseWeekReport()) Routes.WEEK_REPORT else Routes.PLUS) } },
                        onPostcard = { id -> gate("Die Postkarte wird an eine andere App (z. B. WhatsApp) geschickt.") {
                            scope.launch { runCatching { host.sharePostcard(vm.keepsakeData(id)) }.onFailure { host.toast("Die Postkarte konnte nicht erstellt werden.") } }
                        } },
                        onCertificate = { id -> gate("Die Urkunde wird an eine andere App geschickt.") {
                            scope.launch { runCatching { host.shareCertificate(vm.keepsakeData(id)) }.onFailure { host.toast("Die Urkunde konnte nicht erstellt werden.") } }
                        } },
                    )
                }
                composable(Routes.PRIVACY) { de.wissensgarten.app.ui.settings.PrivacyScreen { nav.popSafe() } }
                composable(Routes.SETTINGS) {
                    val scope = rememberCoroutineScope()
                    val theme by vm.theme.collectAsStateWithLifecycle()
                    val sound by vm.settings.sound.collectAsStateWithLifecycle()
                    val reminderOn by vm.settings.reminderOn.collectAsStateWithLifecycle()
                    val readMode2 by vm.settings.readAloud.collectAsStateWithLifecycle()
                    val reminderHour by vm.settings.reminderHour.collectAsStateWithLifecycle()
                    val upd by vm.availableUpdate.collectAsStateWithLifecycle()
                    val autoUpd by vm.settings.autoUpdate.collectAsStateWithLifecycle()
                    WithProfile(profile) { p ->
                        SettingsScreen(
                            profile = p, canDeleteProfile = profiles.size > 1, theme = theme,
                            sound = sound, onSound = { vm.settings.setSound(it) },
                            reminderOn = reminderOn, reminderHour = reminderHour, onReminder = { on, h -> vm.setReminder(on, h) },
                            readAloud = readMode2, onReadAloud = { vm.settings.setReadAloud(it) },
                            packCount = packs.size, cardCount = packs.sumOf { it.cardCount }, version = AppInfo.VERSION,
                            onTheme = { vm.setTheme(it) }, onUpdateProfile = { vm.updateProfile(it) },
                            onDeleteProfile = { vm.deleteProfile(p.id); goTab(Routes.GARDEN) },
                            onTutorial = { nav.navigate(Routes.tutorial(false)) },
                            update = upd, autoUpdate = autoUpd, onAutoUpdate = { vm.settings.setAutoUpdate(it) },
                            packUpdates = vm.settings.packUpdates.collectAsStateWithLifecycle().value, onPackUpdates = { vm.settings.setPackUpdates(it) },
                            onCheckPacks = { vm.refreshPacksIfDue(force = true) },
                            music = vm.settings.music.collectAsStateWithLifecycle().value, onMusic = { vm.settings.setMusic(it) },
                            syllables = vm.settings.syllables.collectAsStateWithLifecycle().value, onSyllables = { vm.settings.setSyllables(it) },
                            vibration = vm.settings.vibration.collectAsStateWithLifecycle().value, onVibration = { vm.settings.setVibration(it) },
                            syncCard = { host.SyncCard() },
                            onPrivacy = { nav.navigate(Routes.PRIVACY) },
                            typing = vm.settings.typing.collectAsStateWithLifecycle().value, onTyping = { vm.settings.setTyping(it) },
                            dailyLimit = vm.settings.dailyLimit.collectAsStateWithLifecycle().value, onDailyLimit = { vm.settings.setDailyLimit(it) },
                            holiday = de.wissensgarten.core.model.Holidays.isActive(vm.settings.holidays.collectAsStateWithLifecycle().value),
                            onHoliday = { vm.settings.setHoliday(it); vm.tick(); vm.refreshWidget() },
                            onWeekReport = { nav.navigate(if (vm.entitlements.canUseWeekReport()) Routes.WEEK_REPORT else Routes.PLUS) },
                            onPlus = if (host.features.plusPurchase) ({ nav.navigate(Routes.PLUS) }) else null,
                            plusActive = vm.plusStatus.collectAsStateWithLifecycle().value.active,
                            onWipeAll = {
                                scope.launch {
                                    val msg = host.wipeAll()
                                    host.toast(msg ?: "Alles gelöscht. Der Wissensgarten startet neu.")
                                    kotlinx.coroutines.delay(if (msg != null) 3500 else 1200)
                                    host.restart()
                                }
                            },
                        )
                    }
                }
                composable(Routes.BED) { entry ->
                    val inventory by vm.inventory.collectAsStateWithLifecycle()
                    val id = entry.savedStateHandle.get<String>("id")
                    val data = beds.firstOrNull { it.bed.id == id }
                    val rev by vm.adventureRev.collectAsStateWithLifecycle()
                    val pack = data?.bed?.packId?.let { pid -> packs.firstOrNull { it.id == pid } }
                    // Erste Begegnung mit einem Paket: Brummi erklärt das Thema
                    LaunchedEffect(pack?.id) {
                        if (pack != null && pack.intro.isNotEmpty() && !vm.introSeen(pack.id)) nav.navigate(Routes.intro(pack.id))
                    }
                    if (data == null) {
                        Box(Modifier.fillMaxSize())
                    } else BedScreen(
                        data = data, now = now, seedsLeft = vm.seedsLeft(),
                        onBack = { nav.popSafe() },
                        onWater = { if (vm.startSession(data.bed.id)) nav.goOnce(Routes.REVIEW) },
                        onAddCard = { f, b, i -> vm.addCard(data.bed.id, f, b, i) },
                        onDeleteCard = { vm.deleteCard(it) },
                        onDeleteBed = { nav.popSafe(); vm.deleteBed(data.bed.id) },
                        ownedSpecies = inventory.map { it.itemId },
                        onSpecies = { vm.setBedSpecies(data.bed.id, it) },
                        classSet = { vm.classSetOf(data.bed.id) },
                        onCraft = { gate("Der Bastelbogen wird zum Ausdrucken an eine andere App (Drucken, Mail …) gegeben.") {
                            runCatching {
                                host.printCraftSheet(data.bed.name, data.bed.emoji,
                                    data.cards.sortedBy { it.sortIndex }.map { de.wissensgarten.app.ui.share.CraftItem(it.front, it.back, it.img) })
                            }.onFailure { host.toast("Der Bastelbogen konnte nicht erstellt werden.") }
                        } },
                        path = {
                            val stations = remember(beds, rev) { vm.stations(data.bed.id) }
                            AdventurePathView(
                                stations,
                                onStation = { st -> if (vm.startStation(data.bed.id, st.index)) nav.goOnce(Routes.REVIEW) },
                                onIntro = pack?.takeIf { it.intro.isNotEmpty() }?.let { pk -> { nav.navigate(Routes.intro(pk.id)) } },
                            )
                        },
                    )
                }
                composable(Routes.REVIEW) {
                    // Nach einem Neustart durch Android: Runde aus der Datenbank zurückholen, sobald die Beete geladen sind
                    var session by remember { mutableStateOf(vm.session.takeIf { it.isNotEmpty() || !vm.hasSavedSession() }) }
                    if (session == null) LaunchedEffect(Unit) {
                        val loaded = kotlinx.coroutines.withTimeoutOrNull(5_000) { vm.beds.first { it.isNotEmpty() } }.orEmpty()
                        session = vm.restoreSession(loaded)
                    }
                    val items = session
                    if (items == null) Box(Modifier.fillMaxSize()) else ReviewScreen(
                        items = items,
                        beds = beds.associate { it.bed.id to it.bed },
                        simpleRating = profile?.simpleRating == true,
                        canType = (profile?.level ?: 0) >= 5 && vm.settings.typing.collectAsStateWithLifecycle().value,
                        onRate = { c, r, dew -> vm.review(c, r, dew) },
                        peers = beds.associate { b -> b.bed.id to b.cards.map { it.back } },
                        calm = vm.sessionCalm,
                        onComplete = { c, t -> vm.sessionComplete(c, t) },
                        subjects = beds.associate { b -> b.bed.id to b.bed.packId?.let { pid -> packs.firstOrNull { it.id == pid }?.subject } },
                        onBonus = { vm.addDew(it) },
                        onClose = {
                            // Doppeltipp auf „Schließen“: nur einmal zurück und nur einmal Truhe/Kreuzung öffnen
                            if (nav.popSafe()) {
                                vm.endSession()
                                host.syncSoon(5_000)
                                // Truhen-Station geschafft? Dann gleich öffnen.
                                if (vm.newHybrid != null) nav.navigate(Routes.HYBRID)
                                else if (vm.chestsEarned() > vm.chestsOpened()) nav.navigate(Routes.CHEST)
                            }
                        },
                    )
                }
                composable(Routes.WEEDS) {
                    val list = remember { vm.weeds() }
                    de.wissensgarten.app.ui.play.WeedScreen(list, remember { vm.peers() }, remember { vm.weedCount() },
                        onRight = { vm.pullWeed(it) }, onWrong = { vm.review(it, de.wissensgarten.core.model.Rating.AGAIN, 0) }) { nav.popSafe(); vm.tick() }
                }
                composable(Routes.BLITZ) {
                    de.wissensgarten.app.ui.play.BlitzScreen(remember { de.wissensgarten.core.model.Blitz.pool(vm.studyCards()) }, remember { vm.peers() },
                        onDone = { vm.blitzDone(it) }) { nav.popSafe() }
                }
                composable(Routes.RHYTHM) {
                    de.wissensgarten.app.ui.play.RhythmScreen(remember { de.wissensgarten.core.model.Blitz.pool(vm.studyCards()) }, remember { vm.peers() },
                        onDone = { p, r -> vm.rhythmDone(p, r) }) { nav.popSafe() }
                }
                composable(Routes.HIVE) {
                    val hRev by vm.adventureRev.collectAsStateWithLifecycle()
                    LaunchedEffect(Unit) { vm.quest(de.wissensgarten.core.model.Quests.Kind.HIVE) }
                    val pid = profile?.id ?: ""
                    de.wissensgarten.app.ui.garden.HiveScreen(remember(beds) { vm.totalReps() }, furniture = { room -> hRev; vm.settings.hiveFurniture(pid, room) },
                        onFurnish = { room, i -> vm.settings.setHiveFurniture(pid, room, i) }) { nav.popSafe() }
                }
                composable(Routes.LOCI) {
                    val pid = profile?.id ?: ""
                    val all = remember { vm.studyCards().associateBy { it.id } }
                    de.wissensgarten.app.ui.garden.LociScreen(remember { de.wissensgarten.core.model.Loci.suggest(all.values.toList()) }, all,
                        remember { vm.settings.loci(pid) }, remember { vm.peers() },
                        onPlace = { vm.settings.setLoci(pid, it) },
                        onAnswer = { c, ok -> vm.review(c, if (ok) de.wissensgarten.core.model.Rating.GOOD else de.wissensgarten.core.model.Rating.AGAIN, if (ok) 2 else 0) },
                        onWalked = { vm.quest(de.wissensgarten.core.model.Quests.Kind.LOCI) }) { nav.popSafe() }
                }
                composable(Routes.FILM) {
                    val frames = remember { vm.filmFrames() }
                    val name = profile?.name ?: ""
                    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
                    de.wissensgarten.app.ui.share.GardenFilmScreen(frames, name, onShare = {
                        gate("Der Garten-Film wird als Bild an eine andere App (z. B. WhatsApp) geschickt.") {
                            runCatching {
                                host.shareImage(de.wissensgarten.app.ui.share.FilmPainter.strip(frames, name, measurer),
                                    "Garten-Film-${de.wissensgarten.app.ui.share.safeFileName(name)}.png", "🎬 So ist ${name}s Wissensgarten gewachsen! 🌻")
                            }.onFailure { host.toast("Der Film konnte nicht erstellt werden.") }
                        }
                    }) { nav.popSafe() }
                }
                composable(Routes.PLUS) {
                    val st by vm.plusStatus.collectAsStateWithLifecycle()
                    de.wissensgarten.app.ui.plus.PlusScreen(
                        status = st, today = de.wissensgarten.core.model.Scheduler.localDay(now),
                        purchaseAllowed = host.features.plusPurchase,
                        checkoutYear = vm.entitlements.config.checkoutYear, checkoutForever = vm.entitlements.config.checkoutForever,
                        onTrial = { vm.startPlusTrial() }, onRedeem = { vm.redeemPlus(it) }, onBack = { nav.popSafe() },
                    )
                }
                composable(Routes.WEEK_REPORT) {
                    val allBeds by vm.allBeds.collectAsStateWithLifecycle()
                    val logs by vm.weekLogs.collectAsStateWithLifecycle()
                    de.wissensgarten.app.ui.parents.WeekReportScreen(profiles, profile?.id, report = { pid ->
                        val subj = packs.associate { it.id to it.subject }
                        val cards = allBeds.filter { it.bed.profileId == pid }.flatMap { b -> b.cards.map { c ->
                            c.id to de.wissensgarten.core.model.WeekReport.CardInfo(c.front, b.bed.packId?.let(subj::get) ?: "eigene", c.stability, c.lapses) } }.toMap()
                        de.wissensgarten.core.model.WeekReport.build(
                            logs.filter { it.profileId == pid }.map { de.wissensgarten.core.model.WeekReport.Entry(it.cardId, it.time, it.rating) }, cards, now)
                    }) { nav.popSafe() }
                }
            }
        }
        if (resting && !playing && vm.onboarded.collectAsStateWithLifecycle().value) {
            de.wissensgarten.app.ui.parents.RestOverlay(profile?.name ?: "") {
                gate("Eltern können heute noch 15 Minuten Lernzeit dazugeben.") {
                    vm.settings.addExtraMinutes(de.wissensgarten.core.model.Scheduler.localDay(de.wissensgarten.core.model.Platform.nowMs()), 15); restRev++
                }
            }
        }
        }
    }

    @Composable
    private fun WithProfile(profile: de.wissensgarten.core.data.db.ProfileEntity?, content: @Composable (de.wissensgarten.core.data.db.ProfileEntity) -> Unit) {
        if (profile == null) Box(Modifier.fillMaxSize()) else content(profile)
    }
}
