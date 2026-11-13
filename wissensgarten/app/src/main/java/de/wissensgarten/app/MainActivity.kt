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
import de.wissensgarten.app.ui.theme.WissensgartenTheme
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val ONBOARDING = "einrichtung"
    const val ADD_PROFILE = "profil-neu"
    const val TUTORIAL = "anleitung?erstes={erstes}"
    fun tutorial(first: Boolean) = "anleitung?erstes=$first"
    const val GARDEN = "garten"
    const val SHOP = "samen"
    const val FAMILY = "familie"
    const val SETTINGS = "mehr"
    const val BED = "beet/{id}"
    fun bed(id: String) = "beet/$id"
    const val REVIEW = "giessen"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.GARDEN, "Garten", Icons.Filled.LocalFlorist),
    Tab(Routes.SHOP, "Samen", Icons.Filled.Storefront),
    Tab(Routes.FAMILY, "Familie", Icons.Filled.Group),
    Tab(Routes.SETTINGS, "Mehr", Icons.Filled.Settings),
)

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private val waterRequest = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        waterRequest.value = intent?.getBooleanExtra(EXTRA_WATER, false) == true
        val startRoute = if (vm.onboarded.value) Routes.GARDEN else Routes.ONBOARDING

        setContent {
            val theme by vm.theme.collectAsStateWithLifecycle()
            WissensgartenTheme(theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    AppNav(nav, startRoute)
                    // Widget-Tipp: direkt in die Gieß-Runde
                    val water by waterRequest.collectAsStateWithLifecycle()
                    val beds by vm.beds.collectAsStateWithLifecycle()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_WATER, false)) waterRequest.value = true
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

        Scaffold(
            bottomBar = {
                if (showBar) NavigationBar {
                    TABS.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { goTab(t.route) },
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
                    WithProfile(profile) { p ->
                        GardenScreen(
                            profile = p, beds = beds, now = now, seedsLeft = vm.seedsLeft(p),
                            onWater = { if (vm.startSession()) nav.navigate(Routes.REVIEW) },
                            onOpenBed = { nav.navigate(Routes.bed(it)) },
                            onProfile = { goTab(Routes.FAMILY) },
                            onShop = { goTab(Routes.SHOP) },
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
                composable(Routes.FAMILY) {
                    val allBeds by vm.allBeds.collectAsStateWithLifecycle()
                    val logs by vm.weekLogs.collectAsStateWithLifecycle()
                    FamilyScreen(
                        profiles, profile?.id, allBeds, logs, now,
                        onSwitch = { vm.switchProfile(it); goTab(Routes.GARDEN) },
                        onAdd = { nav.navigate(Routes.ADD_PROFILE) },
                    )
                }
                composable(Routes.SETTINGS) {
                    val theme by vm.theme.collectAsStateWithLifecycle()
                    WithProfile(profile) { p ->
                        SettingsScreen(
                            profile = p, canDeleteProfile = profiles.size > 1, theme = theme,
                            packCount = packs.size, cardCount = packs.sumOf { it.cards.size }, version = BuildConfig.VERSION_NAME,
                            onTheme = { vm.setTheme(it) }, onUpdateProfile = { vm.updateProfile(it) },
                            onDeleteProfile = { vm.deleteProfile(p.id); goTab(Routes.GARDEN) },
                            onTutorial = { nav.navigate(Routes.tutorial(false)) },
                        )
                    }
                }
                composable(Routes.BED) { entry ->
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
                    )
                }
                composable(Routes.REVIEW) {
                    val session = remember { vm.session }
                    ReviewScreen(
                        items = session,
                        beds = beds.associate { it.bed.id to it.bed },
                        simpleRating = profile?.simpleRating == true,
                        onRate = vm::review,
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
