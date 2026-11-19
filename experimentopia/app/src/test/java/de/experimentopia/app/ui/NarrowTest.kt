package de.experimentopia.app.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import de.experimentopia.app.ui.experiment.ExperimentScreen
import de.experimentopia.app.ui.experiment.Phase
import de.experimentopia.app.ui.free.FreeLabRun
import de.experimentopia.app.ui.home.HomeScreen
import de.experimentopia.app.ui.home.IslandScreen
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.ExperimentopiaTheme
import de.experimentopia.core.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Prüfung für schmale Handys (320 dp): jedes Gerät einmal im Versuch und einmal im Freien Labor, dazu Start und Inseln.
 * Bilder landen in build/schmal/ (nicht im README) und werden vor jeder Auslieferung angesehen.
 * Läuft nur auf Wunsch: -Pschmal (sonst würde jeder Testlauf sehr lang).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w320dp-h2200dp-mdpi")
class NarrowTest(private val name: String) {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun names(): List<Array<Any>> = (listOf("start", "theorie", "fach") + Island.entries.map { "insel_${it.name}" } + Lab.entries.flatMap { listOf("versuch_${it.name}", "frei_${it.name}") }).map { arrayOf<Any>(it) }
    }
    @get:Rule val rule = createComposeRule()

    @Test fun narrow() {
        if (System.getProperty("schmal") == null) return
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ExperimentopiaTheme { Surface(color = E.Night) {
                when {
                    name == "theorie" -> androidx.compose.foundation.layout.Box { de.experimentopia.app.ui.common.TheorySheet(true, Topic.STROM, pro = true) {} }
                    name == "fach" -> de.experimentopia.app.ui.home.SubjectScreen(Subject.CHEMIE, emptyMap(), 6, { false }, {}, {}, onBack = {})
                    name == "start" -> HomeScreen(emptyMap(), Stats(), Daily.forDay(20_600), false, 0, true, onIsland = {}, onDaily = {}, onFreeLab = {}, onJournal = {}, onBadges = {}, onSettings = {}, onGrade = {}, onWelcomed = {})
                    name.startsWith("insel_") -> IslandScreen(Island.valueOf(name.removePrefix("insel_")), emptyMap(), 0, { false }, {}, {})
                    name.startsWith("versuch_") -> { val lab = Lab.valueOf(name.removePrefix("versuch_")); val e = Catalog.all.first { it.setup.lab == lab }
                        ExperimentScreen(e, {}, null, {}, startPhase = Phase.LAB, startGuess = 0) }
                    else -> FreeLabRun(Lab.valueOf(name.removePrefix("frei_"))) {}
                }
            } }
        }
        rule.mainClock.advanceTimeBy(2500)
        rule.onRoot().captureRoboImage("build/schmal/$name.png")
    }
}
