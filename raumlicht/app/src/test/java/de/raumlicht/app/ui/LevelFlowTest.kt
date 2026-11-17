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
import com.github.takahirom.roborazzi.captureRoboImage
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
        rule.onNodeWithText("So rechnet man’s").assertExists()
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

    @Test fun angleStepperCountsInFifteenDegreeSteps() {
        play("7-1")
        tap("plus-a-0", 3)
        rule.mainClock.advanceTimeBy(600)
        rule.onAllNodesWithText("45°")[0].assertExists()
        go()
        assertEquals(listOf(3 to 1), solved)
    }

    @Test fun wrongNumberSaysBiggerOrSmaller() {
        play("9-1")                                               // Radius startet bei 1, richtig ist 3
        go()
        rule.onNodeWithText("Der Radius ist größer. Rechne noch einmal nach.").assertExists()
        assertTrue(stage.lastState!!.contains("\"type\":\"sphere\""))
        tap("plus-a-0", 2); go()
        assertEquals(listOf(2 to 2), solved)
    }

    @Test fun missionCanBeShownAgainAfterFeedbackOrTip() {
        play("1-1")
        val mission = Levels.byId("1-1")!!.mission
        rule.onNodeWithText(mission).assertExists(); rule.onNodeWithTag("aufgabe").assertDoesNotExist()
        go()                                                       // daneben → Rückmeldung verdrängt die Aufgabe
        rule.onNodeWithText(mission).assertDoesNotExist()
        rule.onNodeWithTag("aufgabe").performClick(); rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithText(mission).assertExists(); rule.onNodeWithTag("aufgabe").assertDoesNotExist()
        rule.onNodeWithText("💡 Tipp").performClick(); rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithText(mission).assertDoesNotExist()
        rule.onNodeWithTag("aufgabe").performClick(); rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithText(mission).assertExists()
    }

    @Test fun stepperNeverShowsTheRawCounter() {
        play("13-1")                                               // Schritte von 10 %
        rule.mainClock.autoAdvance = false
        tap("plus-a-0"); rule.mainClock.advanceTimeByFrame(); rule.mainClock.advanceTimeByFrame()
        // Mitten im Umblättern: alt und neu sind beide fertig beschriftet – nie die nackte „1“
        rule.onAllNodesWithText("1").assertCountEquals(0)
        rule.onAllNodesWithText("10 %")[0].assertExists()
        rule.mainClock.autoAdvance = true; rule.mainClock.advanceTimeBy(600)
    }

    @Test fun luxActsOutWhatTheQuestionAsks() {
        play("13-2")                                               // zweimal ziehen MIT Zurücklegen
        tap("plus-a-0", 5); rule.mainClock.autoAdvance = false
        rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(700)
        rule.onNodeWithText("1. Zug: pink – 5 von 10.").assertExists()
        rule.mainClock.advanceTimeBy(1500); rule.onNodeWithText("Zurücklegen – wieder alle 10 da.").assertExists()
        assertEquals(10, Regex("\"type\":\"sphere\"").findAll(stage.lastState!!).count())
        rule.mainClock.advanceTimeBy(1300); rule.onNodeWithText("2. Zug: pink – 5 von 10.").assertExists()
        rule.mainClock.autoAdvance = true; rule.mainClock.advanceTimeBy(4000)
        assertEquals(listOf(3 to 1), solved)
        assertEquals(10, Regex("\"type\":\"sphere\"").findAll(stage.lastState!!).count())          // keine Kugel verschwindet
        assertTrue(stage.lastState!!.contains("\"plain\":true") && stage.lastState!!.contains("2. Zug"))
        assertEquals(2, stage.log.count { it.startsWith("RL.fire") })
    }

    /** Theorie zu jeder Aufgabe: Der Knopf öffnet die passende Karte, sie kostet keinen Stern, und man kommt zurück zur Aufgabe */
    @Test fun theorieKostetKeinenStern() {
        play("5-2")
        rule.onNodeWithTag("theorie-karte").assertDoesNotExist()
        tap("theorie"); rule.mainClock.advanceTimeBy(1500)
        val card = de.raumlicht.core.model.Theory.forLevel(Levels.byId("5-2")!!)
        rule.onNodeWithTag("theorie-titel").assertTextEquals(card.title)
        rule.onNodeWithText(card.steps.last(), substring = true).assertExists(); rule.onNodeWithText("Beispiel").assertExists(); rule.onNodeWithText("Nachlesen kostet keinen Stern.").assertExists()
        rule.onRoot().captureRoboImage("../screenshots/78_theorie.png")
        tap("theorie-ok"); rule.mainClock.advanceTimeBy(800); rule.onNodeWithTag("theorie-karte").assertDoesNotExist()
        // danach beim ersten Versuch gelöst: volle drei Sterne
        val sol = Levels.byId("5-2")!!.solution.getValue("P"); sol.forEachIndexed { i, v -> tap("plus-P-$i", v) }; go()
        assertEquals(listOf(3 to 1), solved)
    }

    /** Strenge Vektorschreibweise aus den Einstellungen: dieselbe Aufgabe, Tipp und Rechenweg mit Ortsvektoren */
    @Test fun strengeSchreibweiseImLevel() {
        val l = Levels.byId("2-1")!!
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) },
            de.raumlicht.app.ui.settings.LocalNotation provides de.raumlicht.app.ui.settings.NotationChoice(true) {}) {
            LevelScreen(l, Levels.chapters.first { it.number == l.chapter }, false, {}, { s, a -> solved += s to a }, {}, {}, controller = stage)
        } } } }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithText("💡 Tipp").performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("OB⃗ − OA⃗", substring = true).assertExists(); rule.onNodeWithText("Spitze minus Fuß: B − A", substring = true).assertDoesNotExist()
        l.solution.getValue("v").forEachIndexed { i, v -> tap(if (v >= 0) "plus-v-$i" else "minus-v-$i", kotlin.math.abs(v)) }; go()
        rule.onNodeWithText("v⃗ = OB⃗ − OA⃗", substring = true).assertExists()
    }
}
