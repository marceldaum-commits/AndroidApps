package de.wissensgarten.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.updateAll
import de.wissensgarten.app.data.GardenRepository
import de.wissensgarten.app.data.Session
import de.wissensgarten.app.data.SessionItem
import de.wissensgarten.app.ui.BedScreen
import de.wissensgarten.app.ui.GardenScreen
import de.wissensgarten.app.ui.ReviewScreen
import de.wissensgarten.app.ui.theme.WissensgartenTheme
import de.wissensgarten.app.widget.GardenWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Garden : Screen
    data class BedView(val bedId: String) : Screen
    data class Review(val items: List<SessionItem>, val returnTo: Screen) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = GardenRepository.get(this)
        val refreshWidget = { lifecycleScope.launch { GardenWidget().updateAll(this@MainActivity) } }
        val startWatering = intent?.getBooleanExtra(EXTRA_WATER, false) == true

        setContent {
            WissensgartenTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val state by repo.state.collectAsStateWithLifecycle()
                    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while (true) { delay(60_000); now = System.currentTimeMillis() }
                    }
                    var screen by remember {
                        mutableStateOf<Screen>(
                            Session.build(repo.state.value, now).takeIf { startWatering && it.isNotEmpty() }
                                ?.let { Screen.Review(it, Screen.Garden) } ?: Screen.Garden
                        )
                    }

                    BackHandler(enabled = screen != Screen.Garden) {
                        screen = when (val s = screen) {
                            is Screen.Review -> s.returnTo
                            else -> Screen.Garden
                        }
                        now = System.currentTimeMillis()
                    }

                    AnimatedContent(
                        targetState = screen,
                        contentKey = { it::class },
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen",
                    ) { s ->
                        when (s) {
                            Screen.Garden -> GardenScreen(
                                state = state,
                                now = now,
                                onWater = { screen = Screen.Review(Session.build(state, now), Screen.Garden) },
                                onOpenBed = { screen = Screen.BedView(it) },
                                onAddBed = { name, emoji -> repo.addBed(name, emoji) },
                            )
                            is Screen.BedView -> {
                                val bed = state.beds.firstOrNull { it.id == s.bedId }
                                if (bed == null) {
                                    LaunchedEffect(Unit) { screen = Screen.Garden }
                                } else BedScreen(
                                    bed = bed,
                                    now = now,
                                    onBack = { screen = Screen.Garden },
                                    onWater = { screen = Screen.Review(Session.build(state, now, bed.id), s) },
                                    onAddCard = { f, b -> repo.addCard(bed.id, f, b); refreshWidget() },
                                    onDeleteCard = { repo.deleteCard(bed.id, it); refreshWidget() },
                                    onDeleteBed = { repo.deleteBed(bed.id); refreshWidget(); screen = Screen.Garden },
                                )
                            }
                            is Screen.Review -> ReviewScreen(
                                items = s.items,
                                state = state,
                                onGrade = { item, grade -> repo.review(item.bedId, item.card.id, grade) },
                                onClose = {
                                    refreshWidget()
                                    now = System.currentTimeMillis()
                                    screen = s.returnTo
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch { GardenWidget().updateAll(this@MainActivity) }
    }

    companion object {
        const val EXTRA_WATER = "water"
    }
}
