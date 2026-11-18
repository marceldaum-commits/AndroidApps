package de.experimentopia.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import de.experimentopia.app.ui.home.HomeScreen
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.ExperimentopiaTheme
import de.experimentopia.core.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** PC-Bildschirm (1400 × 900 dp): eigene Anordnung in Spalten statt einer gestreckten Handy-Spalte */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1400dp-h900dp-land-mdpi")
class WideScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { ExperimentopiaTheme { Surface(color = E.Night) { content() } } }
        rule.mainClock.advanceTimeBy(3000)
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }
    private val someStars = Catalog.all.take(14).mapIndexed { i, e -> e.id to (3 - i % 3) }.toMap()
    private val stats = Stats(done = 14, stars = 28, hits = 9, ahas = 5, explained = 10)

    @Test fun home() = shot("90_pc_start") {
        androidx.compose.runtime.CompositionLocalProvider(de.experimentopia.app.ui.fx.LocalAllApps provides {}) {
        HomeScreen(someStars, stats, Daily.forDay(20_600), false, 0, false, onIsland = {}, onDaily = {}, onFreeLab = {}, onJournal = {}, onBadges = {}, onSettings = {}, onGrade = {}, onWelcomed = {})
        }
    }
    @Test fun subject() = shot("91_pc_fach") { de.experimentopia.app.ui.home.SubjectScreen(Subject.PHYSIK, someStars, 0, { false }, {}, {}, {}) }
    @Test fun freeLab() = shot("92_pc_freies_labor") { de.experimentopia.app.ui.free.FreeLabScreen({ false }, {}, {}) }
}
