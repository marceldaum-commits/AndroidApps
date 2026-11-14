package de.wissensgarten.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import de.wissensgarten.app.ui.duel.DuelHubScreen
import de.wissensgarten.app.ui.duel.DuelPlayScreen
import de.wissensgarten.app.ui.common.PackBrowser
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.notify.ReminderPromptCard
import de.wissensgarten.app.notify.rememberNotificationPermission
import de.wissensgarten.app.ui.theme.WissensgartenTheme
import de.wissensgarten.app.ui.fx.LocalSfx
import de.wissensgarten.app.ui.fx.LocalWind
import de.wissensgarten.app.ui.fx.rememberWind
import kotlinx.coroutines.flow.MutableStateFlow

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

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private val waterRequest = MutableStateFlow(false)
    /** Duell-Code aus Link oder geteilter Nachricht */
    private val duelRequest = MutableStateFlow<String?>(null)

    private fun readDuelIntent(i: Intent?) {
        val text = i?.dataString ?: i?.getStringExtra(Intent.EXTRA_TEXT)
        if (text != null && text.contains("WG1-")) duelRequest.value = text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        waterRequest.value = intent?.getBooleanExtra(EXTRA_WATER, false) == true
        readDuelIntent(intent)
        val startRoute = if (vm.onboarded.value) Routes.GARDEN else Routes.ONBOARDING

        setContent {
            val theme by vm.theme.collectAsStateWithLifecycle()
            val soundOn by vm.settings.sound.collectAsStateWithLifecycle()
            val sfx = (application as WissensgartenApp).sfx
            LaunchedEffect(soundOn) { sfx.enabled = soundOn }
            WissensgartenTheme(theme) {
              CompositionLocalProvider(LocalWind provides rememberWind(), LocalSfx provides sfx) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                        if (vm.openDuelCode(t)) nav.navigate(Routes.DUEL)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_WATER, false)) waterRequest.value = true
        readDuelIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.tick()
    }

    override fun onStop() {
        super.onStop()
        vm.refreshWidget()
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

        fun goTab(r: String) = nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }

        val sfx = LocalSfx.current
        Scaffold(
            bottomBar = {
                if (showBar) NavigationBar {
                    TABS.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { sfx?.play(de.wissensgarten.app.ui.fx.Sound.TAP, volume = 0.5f); goTab(t.route) },
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
                    OnboardingScreen(packs, addingProfile = false, onCancel = {}) { name, avatar, level, ids, own ->
                        vm.finishOnboarding(name, avatar, level, ids, own)
                        nav.navigate(Routes.tutorial(true)) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    }
                }
                composable(Routes.ADD_PROFILE) {
                    OnboardingScreen(packs, addingProfile = true, onCancel = { nav.popBackStack() }) { name, avatar, level, ids, own ->
                        vm.addProfile(name, avatar, level, ids, own)
                        nav.popBackStack(); goTab(Routes.GARDEN)
                    }
                }
                composable(Routes.TUTORIAL) { entry ->
                    val first = entry.arguments?.getString("erstes") == "true"
                    TutorialScreen {
                        if (first) nav.navigate(Routes.GARDEN) { popUpTo(0) { inclusive = true } } else nav.popBackStack()
                    }
                }
                composable(Routes.GARDEN) {
                    val decos by vm.activeDecos.collectAsStateWithLifecycle()
                    val asked by vm.settings.reminderAsked.collectAsStateWithLifecycle()
                    val hour by vm.settings.reminderHour.collectAsStateWithLifecycle()
                    val askPermission = rememberNotificationPermission { ok -> vm.setReminder(ok, hour) }
                    WithProfile(profile) { p ->
                        GardenScreen(
                            profile = p, beds = beds, now = now, seedsLeft = vm.seedsLeft(p),
                            onWater = { if (vm.startSession()) nav.navigate(Routes.REVIEW) },
                            onOpenBed = { nav.navigate(Routes.bed(it)) },
                            onProfile = { goTab(Routes.FAMILY) },
                            onShop = { goTab(Routes.SHOP) },
                            decos = decos,
                            onDuel = { nav.navigate(Routes.DUEL) },
                            reminderPrompt = if (asked) null else {
                                { ReminderPromptCard(hour, onYes = askPermission, onNo = { vm.setReminder(false) }) }
                            },
                        )
                    }
                }
                composable(Routes.SHOP) {
                    WithProfile(profile) { p ->
                        ShopScreen(
                            profile = p, packs = packs, beds = beds, canUse = vm.entitlements::canUse,
                            onPlant = vm::plantPack,
                            onOpenBed = { nav.navigate(Routes.bed(it)) },
                            onOwnBed = { n, e -> vm.addOwnBed(n, e) { id -> nav.navigate(Routes.bed(id)) } },
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
                        )
                    }
                }
                composable(Routes.DUEL) {
                    val history by vm.duelHistory.collectAsStateWithLifecycle()
                    DuelHubScreen(
                        myName = profile?.name ?: "",
                        packs = packs, history = history, preview = vm.currentDuel,
                        onBack = { nav.popBackStack() },
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
                            TextButton(onClick = { nav.popBackStack() }) { Text("Zurück") }
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
                    FamilyScreen(
                        profiles, profile?.id, allBeds, logs, now,
                        onSwitch = { vm.switchProfile(it); goTab(Routes.GARDEN) },
                        onAdd = { nav.navigate(Routes.ADD_PROFILE) },
                        onDuel = { nav.navigate(Routes.DUEL) },
                    )
                }
                composable(Routes.SETTINGS) {
                    val theme by vm.theme.collectAsStateWithLifecycle()
                    val sound by vm.settings.sound.collectAsStateWithLifecycle()
                    val reminderOn by vm.settings.reminderOn.collectAsStateWithLifecycle()
                    val reminderHour by vm.settings.reminderHour.collectAsStateWithLifecycle()
                    WithProfile(profile) { p ->
                        SettingsScreen(
                            profile = p, canDeleteProfile = profiles.size > 1, theme = theme,
                            sound = sound, onSound = { vm.settings.setSound(it) },
                            reminderOn = reminderOn, reminderHour = reminderHour, onReminder = { on, h -> vm.setReminder(on, h) },
                            packCount = packs.size, cardCount = packs.sumOf { it.cards.size }, version = BuildConfig.VERSION_NAME,
                            onTheme = { vm.setTheme(it) }, onUpdateProfile = { vm.updateProfile(it) },
                            onDeleteProfile = { vm.deleteProfile(p.id); goTab(Routes.GARDEN) },
                            onTutorial = { nav.navigate(Routes.tutorial(false)) },
                        )
                    }
                }
                composable(Routes.BED) { entry ->
                    val inventory by vm.inventory.collectAsStateWithLifecycle()
                    val id = entry.arguments?.getString("id")
                    val data = beds.firstOrNull { it.bed.id == id }
                    if (data == null) {
                        Box(Modifier.fillMaxSize())
                    } else BedScreen(
                        data = data, now = now, seedsLeft = vm.seedsLeft(),
                        onBack = { nav.popBackStack() },
                        onWater = { if (vm.startSession(data.bed.id)) nav.navigate(Routes.REVIEW) },
                        onAddCard = { f, b, i -> vm.addCard(data.bed.id, f, b, i) },
                        onDeleteCard = { vm.deleteCard(it) },
                        onDeleteBed = { nav.popBackStack(); vm.deleteBed(data.bed.id) },
                        ownedSpecies = inventory.map { it.itemId },
                        onSpecies = { vm.setBedSpecies(data.bed.id, it) },
                    )
                }
                composable(Routes.REVIEW) {
                    val session = remember { vm.session }
                    ReviewScreen(
                        items = session,
                        beds = beds.associate { it.bed.id to it.bed },
                        simpleRating = profile?.simpleRating == true,
                        onRate = { c, r, dew -> vm.review(c, r, dew) },
                        peers = beds.associate { b -> b.bed.id to b.cards.map { it.back } },
                        onBonus = { vm.addDew(it) },
                        onClose = { vm.endSession(); nav.popBackStack() },
                    )
                }
            }
        }
    }

    @Composable
    private fun WithProfile(profile: de.wissensgarten.core.data.db.ProfileEntity?, content: @Composable (de.wissensgarten.core.data.db.ProfileEntity) -> Unit) {
        if (profile == null) Box(Modifier.fillMaxSize()) else content(profile)
    }

    companion object {
        const val EXTRA_WATER = "water"
    }
}
