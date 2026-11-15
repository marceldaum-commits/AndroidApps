package de.raumlicht.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.stage.LocalStageRenderer
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.Levels
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ein Level von vorn bis hinten spielen: einstellen, daneben, Rückmeldung, Treffer, Sterne. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class LevelFlowTest {
    @get:Rule val rule = createComposeRule()
    private val stage = StageController()
    private val solved = mutableListOf<Pair<Int, Int>>()

    private fun play(id: String) {
        val l = Levels.byId(id)!!
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }) {
            LevelScreen(l, Levels.chapters.first { it.number == l.chapter }, false, {}, { s, a -> solved += s to a }, {}, {}, controller = stage)
        } } } }
        rule.mainClock.advanceTimeBy(1000)
    }
    private fun tap(tag: String, times: Int = 1) = repeat(times) { rule.onNodeWithTag(tag).performClick() }
    private fun go() { rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(5000) }

    @Test fun missThenHitGivesTwoStarsOnce() {
        play("1-1")
        tap("plus-P-0", 4); go()                                  // (4|0|0) statt (4|2|0)
        rule.onNodeWithText("x₁ und x₃ stimmen ✓ – x₂ noch nicht.").assertExists()
        assertTrue(solved.isEmpty())
        assertTrue("Lux fliegt zum eingestellten Punkt", stage.lastState!!.contains("\"mood\":\"sad\""))
        tap("plus-P-1", 2); go()
        assertEquals(listOf(2 to 2), solved)
        rule.onNodeWithText("So rechnet man's").assertExists()
        rule.onNodeWithText("Weiter ▸").assertExists()
        assertTrue(stage.log.any { it.startsWith("RL.party") })
        rule.onNodeWithText("▶ Los").assertDoesNotExist()        // kein zweites Speichern möglich
    }

    @Test fun tipCostsOneStarAndLaserFires() {
        play("3-1")
        rule.onNodeWithText("💡 Tipp").performClick()
        rule.onNodeWithText("Richtungsvektor = Ziel − Stützpunkt", substring = true).assertExists()
        tap("plus-d-1", 2); tap("plus-d-2"); go()                 // (1|2|1): gekürzte Richtung trifft
        assertEquals(listOf(2 to 1), solved)
        assertTrue(stage.log.any { it.startsWith("RL.fire") && it.contains("hit:true") })
    }

    @Test fun draggingTheHandleSetsTheValues() {
        play("1-2")
        rule.runOnIdle { stage.onDrag!!.invoke("griff", listOf(2, 5, 3)) }
        rule.mainClock.advanceTimeBy(500)
        assertTrue(stage.lastState!!.contains("\"p\":[2.0,5.0,3.0]"))
        go()
        assertEquals(listOf(3 to 1), solved)
    }

    @Test fun vectorHandleIsRelativeToStartPoint() {
        play("2-1")                                               // A(1|1|0) → B(4|5|0)
        rule.runOnIdle { stage.onDrag!!.invoke("griff", listOf(4, 5, 0)) }
        rule.mainClock.advanceTimeBy(500); go()
        assertEquals(listOf(3 to 1), solved)
    }

    @Test fun valuesStayInsideTheRoom() {
        play("1-1")
        rule.onNodeWithTag("minus-P-0").performClick()            // schon am Rand: bleibt 0
        rule.mainClock.advanceTimeBy(300)
        assertTrue(stage.lastState!!.contains("\"id\":\"marke\",\"type\":\"point\",\"p\":[0.0,0.0,0.0]"))
    }
}
