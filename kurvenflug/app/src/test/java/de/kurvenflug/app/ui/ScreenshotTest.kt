package de.kurvenflug.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import de.kurvenflug.app.ui.flight.IntroOverlay
import de.kurvenflug.app.ui.flight.LevelScreen
import de.kurvenflug.app.ui.home.HomeScreen
import de.kurvenflug.app.ui.settings.SettingsScreen
import de.kurvenflug.app.ui.settings.UpdateState
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.theme.KurvenflugTheme
import de.kurvenflug.core.model.Levels
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Rendert die Bildschirme ohne Gerät – für das README und zum Prüfen vor jeder Auslieferung. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, settle: Long = 1500, prepare: (ComposeContentTestRule) -> Unit = {}, content: @Composable () -> Unit) {
        rule.setContent { KurvenflugTheme { Surface(color = Q.Space) { content() } } }
        rule.mainClock.advanceTimeBy(1500)
        prepare(rule)
        rule.mainClock.advanceTimeBy(settle)
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    private val someStars = Levels.all.take(9).mapIndexed { i, l -> l.id to (3 - i % 3) }.toMap() +
        Levels.chapters[1].levels.take(3).associate { it.id to 3 }

    private fun level(id: String, values: Map<String, Double>? = null, prepare: (ComposeContentTestRule) -> Unit = {}, name: String, settle: Long = 1500) {
        val l = Levels.byId(id)!!
        shot(name, settle, prepare) {
            LevelScreen(l, Levels.chapters.first { it.number == l.chapter }, Q.Cyan, false, {}, { _, _, _ -> }, {}, {}, values)
        }
    }

    @Test fun home() = shot("01_start") {
        HomeScreen(someStars.values.sum(), Levels.all.size * 3, 7, 0, {}, Levels.chapters, someStars, {}, {},
            daily = de.kurvenflug.core.model.Daily.forDay(20_500), dailiesDone = 4)
    }

    @Test fun intro() = shot("03_volt_erklaert", prepare = { it.onNodeWithText("Weiter ›").performClick() }) {
        IntroOverlay(Levels.chapters[1]) {}
    }

    @Test fun coordinates() = level("1-2", name = "04_koordinaten", values = mapOf("x" to -5.0, "y" to 0.0))

    @Test fun lineAim() = level("2-7", name = "05_gerade_zielen", values = mapOf("m" to -1.0, "b" to 2.0))

    @Test fun lineMiss() = level("2-6", name = "06_gerade_daneben", values = mapOf("m" to 1.0, "b" to 0.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun lineFlying() = level("2-6", name = "07_gerade_flug", values = mapOf("m" to 0.5, "b" to 1.0), settle = 900,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun parabolaWin() = level("3-6", name = "08_parabel_geschafft", values = mapOf("a" to -0.25, "d" to 0.0, "e" to 5.0), settle = 4500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun throwHelp() = level("4-5", name = "09_wurf_berg", values = mapOf("angle" to 60.0, "v" to 12.0),
        prepare = { it.onNodeWithText("👁 Ganze Bahn").performClick() })

    @Test fun throwMoon() = level("4-6", name = "10_wurf_mond", values = mapOf("angle" to 35.0, "v" to 5.5), settle = 1500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun settings() = shot("11_einstellungen") {
        SettingsScreen(sound = true, onSound = {}, vibration = true, onVibration = {}, autoUpdate = true, onAutoUpdate = {}, installed = "0.2.0", update = UpdateState.Idle, onBack = {})
    }

    @Test fun sine() = level("5-4", name = "12_sinus", values = mapOf("a" to 2.0, "b" to 1.0, "d" to -1.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun exponential() = level("5-7", name = "13_zerfall", values = mapOf("b" to 0.75))

    @Test fun slingshot() = level("6-4", name = "14_schwerkraft_schleuder", values = mapOf("angle" to 0.0, "v" to 3.5), settle = 1600,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun slingshotHelp() = level("6-3", name = "15_schleuder_ganze_bahn", values = mapOf("angle" to 70.0, "v" to 7.0),
        prepare = { it.onNodeWithText("👁 Ganze Bahn").performClick() })

    @Test fun daily() {
        val l = de.kurvenflug.core.model.Daily.forDay(20_501)
        shot("16_tagesmission") {
            LevelScreen(l, de.kurvenflug.core.model.Chapter(0, "Tagesmission", "Jeden Tag neu", "🛰️", emptyList(), listOf(l)),
                Q.Pink, false, {}, { _, _, _ -> }, null, {})
        }
    }
}
