package de.raumlicht.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import de.raumlicht.app.ui.home.HomeScreen
import de.raumlicht.app.ui.level.IntroOverlay
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.settings.SettingsScreen
import de.raumlicht.app.ui.settings.UpdateState
import de.raumlicht.app.ui.stage.LocalStageRenderer
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.Levels
import de.raumlicht.core.model.Values
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rendert die Bildschirme ohne Gerät – für das README und zum Prüfen vor jeder Auslieferung.
 * Die 3D-Bühne ist ein vorab gerendertes Bild derselben Szene (siehe SceneDumpTest und scene/test/shot.mjs).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun stageImage(name: String): @Composable (StageController, Modifier) -> Unit = { _, m ->
        val bmp = javaClass.classLoader!!.getResourceAsStream("stage/$name.png")?.use { BitmapFactory.decodeStream(it) }
        if (bmp != null) Image(bmp.asImageBitmap(), null, m, contentScale = ContentScale.Crop) else Box(m.background(Q.Deep))
    }

    private fun shot(name: String, stage: String, settle: Long = 1500, prepare: (ComposeContentTestRule) -> Unit = {}, content: @Composable () -> Unit) {
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides stageImage(stage)) { content() } } } }
        rule.mainClock.advanceTimeBy(1500)
        prepare(rule)
        rule.mainClock.advanceTimeBy(settle)
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    private fun level(name: String, id: String, stage: String = id, values: Values? = null, solve: Boolean = false, go: Boolean = solve) {
        val l = Levels.byId(id)!!
        shot(name, stage, if (go) 5000 else 1500, prepare = { if (go) it.onNodeWithText("▶ Los").performClick() }) {
            LevelScreen(l, Levels.chapters.first { it.number == l.chapter }, false, {}, { _, _ -> }, {}, {}, if (solve) l.solution else values)
        }
    }

    private val someStars = Levels.all.take(8).mapIndexed { i, l -> l.id to (3 - i % 3 % 2) }.toMap()

    @Test fun home() = shot("01_start", "start") { HomeScreen(Levels.chapters, someStars, {}, {}) }
    @Test fun intro() = shot("02_lux_erklaert", "start", prepare = { it.onNodeWithText("Weiter ›").performClick() }) { IntroOverlay(Levels.chapters[0]) {} }
    @Test fun point() = level("03_punkt", "1-2", values = mapOf("P" to listOf(2, 3, 0)))
    @Test fun pointMiss() = level("04_daneben", "1-2", values = mapOf("P" to listOf(2, 3, 0)), go = true)
    @Test fun pyramid() = level("05_pyramide_geschafft", "1-6", "1-6-fertig", solve = true)
    @Test fun vector() = level("06_vektor", "2-2", values = mapOf("v" to listOf(4, -2, 1)))
    @Test fun laserHit() = level("07_laser_treffer", "3-2", "3-2-fertig", solve = true)
    @Test fun crossing() = level("08_kreuzung", "4-2", values = mapOf("P" to listOf(3, 3, 2)))
    @Test fun skew() = level("09_windschief", "4-4")
    @Test fun pierce() = level("10_durchstosspunkt", "5-1", "5-1-fertig", solve = true)
    @Test fun plane() = level("11_ebene_bauen", "5-3", values = mapOf("n" to listOf(1, 2, 2), "c" to listOf(6)))
    @Test fun normal() = level("12_normalenvektor", "5-4", values = mapOf("d" to listOf(1, 0, -2)))
    @Test fun mirror() = level("13_spiegelpunkt", "6-3", "6-3-fertig", solve = true)
    @Test fun bank() = level("14_ueber_bande", "6-5", "6-5-fertig", solve = true)
    @Test fun settings() = shot("15_einstellungen", "start") {
        SettingsScreen(true, {}, true, {}, true, {}, true, {}, "0.1.0", UpdateState.Idle, {})
    }
}
