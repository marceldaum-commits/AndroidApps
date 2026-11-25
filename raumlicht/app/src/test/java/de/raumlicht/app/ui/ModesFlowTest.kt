package de.raumlicht.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.raumlicht.app.ui.home.*
import de.raumlicht.app.ui.stage.LocalStageRenderer
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Die neuen Spielarten von vorn bis hinten: Abi-Simulation, Lage-Check, Duell, Baukasten, Spickzettel-Sprung. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ModesFlowTest {
    @get:Rule val rule = createComposeRule()
    private val stage = StageController()
    private fun show(content: @Composable () -> Unit) {
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }) { content() } } } }
        rule.mainClock.advanceTimeBy(800)
    }
    private fun go() { rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(5000) }

    @Test fun examGivesFullPointsOnlyOnFirstTry() {
        val exam = Exams.byId("leinwand")!!
        var finished = -1
        show { ExamPlay(exam, { finished = it }, {}) }
        // a) Durchstoßpunkt (2|4|4): erst daneben, dann richtig → 2 von 3 BE
        go()
        repeat(2) { rule.onNodeWithTag("plus-P-0").performClick() }; repeat(4) { rule.onNodeWithTag("plus-P-1").performClick() }; repeat(4) { rule.onNodeWithTag("plus-P-2").performClick() }
        go()
        rule.onNodeWithText("2 BE").assertExists()
        rule.onNodeWithText("Weiter ▸").performClick(); rule.mainClock.advanceTimeBy(800)
        // b) 90° beim ersten Versuch → 2 BE
        repeat(6) { rule.onNodeWithTag("plus-a-0").performClick() }; go()
        rule.onNodeWithText("4 BE").assertExists()
        rule.onNodeWithText("Weiter ▸").performClick(); rule.mainClock.advanceTimeBy(800)
        // c) Abstand 3
        repeat(2) { rule.onNodeWithTag("plus-a-0").performClick() }; go()
        rule.onNodeWithText("Weiter ▸").performClick(); rule.mainClock.advanceTimeBy(800)
        // d) Spiegelpunkt (3|6|6)
        repeat(3) { rule.onNodeWithTag("plus-P-0").performClick() }; repeat(6) { rule.onNodeWithTag("plus-P-1").performClick() }; repeat(6) { rule.onNodeWithTag("plus-P-2").performClick() }
        go()
        assertEquals(11, finished)
        rule.onNodeWithText("Weiter ▸").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("11 von 12 BE").assertExists()
    }

    @Test fun lageCheckCountsRightAnswersAndRevealsLines() {
        var saved = -1
        show { LageScreen(0, { saved = it; true }, {}, session = 5, controller = stage) }
        assertFalse(stage.lastState!!.contains("\"id\":\"g\""))
        repeat(LageCheck.ROUNDS) { n ->
            val r = LageCheck.round(5 * 100L + n)
            rule.onNodeWithTag("lage-${r.relation.name}").performClick(); rule.mainClock.advanceTimeBy(400)
            assertTrue(stage.lastState!!.contains("\"id\":\"g\""))
            rule.onNodeWithText(if (n == LageCheck.ROUNDS - 1) "Auswertung ▸" else "Weiter ▸").performClick(); rule.mainClock.advanceTimeBy(400)
        }
        assertEquals(LageCheck.ROUNDS, saved)
        rule.onNodeWithText("Neuer Rekord! 🎉").assertExists()
    }

    @Test fun duelCodeFromFriendStartsTheSameTasks() {
        val d = Duel(JamTopic.SCHNITT, 99)
        show { DuelScreen({}) }
        rule.onNodeWithTag("duell-code").performTextInput("falsch"); rule.onNodeWithText("Los").performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithText("Diesen Code kenne ich nicht – bitte prüfen.").assertExists()
        rule.onNodeWithTag("duell-code").performTextReplacement(d.code); rule.onNodeWithText("Los").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("⚔️ Duell ${d.code} · 1/5").assertExists()
    }

    @Test fun builderAddsAndEditsItems() {
        var saved = emptyList<ShowItem>()
        show { BuilderScreen(emptyList(), { saved = it }, {}, controller = stage) }
        rule.onNodeWithTag("neu-BALL").performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("plus-show-3").performClick(); rule.mainClock.advanceTimeBy(300)
        assertEquals(listOf(ShowItem(ShowType.BALL, listOf(4, 4, 3, 3))), saved)
        assertTrue(stage.lastState!!.contains("\"type\":\"sphere\""))
        assertEquals(saved, Show.decode(Show.encode(saved)))
    }

    @Test fun sheetJumpsToChapterAndJam() {
        val chapters = ArrayList<Int>(); val jams = ArrayList<JamTopic>()
        show { SheetScreen(Sheet.cards.take(1), { chapters += it }, { jams += it }, {}) }
        rule.onNodeWithText("▶ Kapitel 2").performClick(); rule.onNodeWithText("♾️ Üben").performClick()
        assertEquals(listOf(2), chapters); assertEquals(listOf(JamTopic.PUNKTE), jams)
    }

    @Test fun timedExamEndsWhenTheClockRunsOut() {
        val exam = Exams.byId("zelt")!!
        var finished = -1
        show { ExamPlay(exam, { finished = it }, {}, timed = true) }
        rule.onNodeWithText("⏱ 9:00", substring = true).assertExists()     // 12 BE · 45 s
        rule.mainClock.advanceTimeBy(3000)
        rule.onNodeWithText("⏱ 8:5", substring = true).assertExists()      // die Uhr läuft
        rule.mainClock.advanceTimeBy(Exam.seconds(exam) * 1000L + 2000)
        assertEquals(0, finished)
        rule.onNodeWithText("⏱ Die Zeit ist abgelaufen").assertExists()
        rule.onNodeWithText("0 von 12 BE").assertExists()
    }

    @Test fun examPickerPassesTheTimeSwitch() {
        val picked = ArrayList<Pair<String, Boolean>>()
        show { ExamPicker(Exams.all, emptyMap(), { e, t -> picked += e.id to t }) {} }
        rule.onNodeWithText("Festivalzelt").performClick()
        rule.onNodeWithTag("zeit").performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithText("Festivalzelt").performClick()
        assertEquals(listOf("zelt" to false, "zelt" to true), picked)
    }

    @Test fun plannerSavesAndPlaysTodaysPackage() {
        val saved = ArrayList<Plan?>()
        show { PlannerScreen(null, 100, emptyMap(), { saved += it }, { _, _, _ -> }, {}) }
        rule.onNodeWithTag("kapitel-3").performClick(); rule.mainClock.advanceTimeBy(300)
        assertEquals(Plan(107, setOf(3)), saved.last())
        rule.onNodeWithText("Klausur in 7 Tagen · heute 1 Aufgabe").assertExists()
        rule.onNodeWithTag("minus-tage").performClick(); rule.mainClock.advanceTimeBy(300)
        assertEquals(Plan(106, setOf(3)), saved.last())
        rule.onNodeWithText("▶ Heutiges Paket spielen").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("🗓️ Klausur-Paket · 1/1 · Aufgabe 3-1").assertExists()
    }

    @Test fun classPackageCodeIsBuiltAndPlayable() {
        show { ClassScreen({ _, _, _ -> }) {} }
        rule.onNodeWithTag("wahl-1-1").performClick(); rule.onNodeWithTag("wahl-1-2").performClick(); rule.mainClock.advanceTimeBy(300)
        val code = ClassCode.encode(listOf(Levels.byId("1-1")!!, Levels.byId("1-2")!!))
        rule.onNodeWithTag("paket-code").assertTextEquals(code)
        rule.onNodeWithTag("klassen-code").performTextInput(code); rule.onNodeWithText("Los").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("🏫 Klassen-Paket · 1/2 · Aufgabe 1-1").assertExists()
    }

    @Test fun twoPlayersTakeTurns() {
        show { MatchScreen({}, session = 4) }
        rule.onNodeWithText("▶ Blau fängt an").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("👥 Blau ist dran", substring = true).assertExists()
        rule.onNodeWithText("0 : 0").assertExists()
    }

    @Test fun calculatorShowsFactsForTheBuiltScene() {
        show { BuilderScreen(listOf(ShowItem(ShowType.LASER, listOf(1, 1, 0, 1, 2, 2)), ShowItem(ShowType.PLANE, listOf(0, 0, 1, 4))), {}, {}) }
        rule.onNodeWithTag("rechner").performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Durchstoßpunkt (3|5|4)", substring = true).assertExists()
    }

    @Test fun guidedStepsGiveFeedbackAndFinish() {
        val g = Guides.byId("weg-schnitt")!!
        show { ExamPlay(g, {}, {}, guided = true) }
        rule.onNodeWithText("🧩 Schnittpunkt · Schritt 1 von 2").assertExists()
        rule.onNodeWithTag("plus-a-0").performClick(); go()
        rule.onNodeWithText("Mit s = t = 1 liegen die Marken noch 3 Einheiten auseinander.").assertExists()
        rule.onNodeWithTag("plus-a-0").performClick(); go()
        rule.onNodeWithText("Weiter ▸").performClick(); rule.mainClock.advanceTimeBy(800)
        repeat(5) { rule.onNodeWithTag("plus-P-0").performClick() }; repeat(3) { rule.onNodeWithTag("plus-P-1").performClick() }; repeat(3) { rule.onNodeWithTag("plus-P-2").performClick() }
        go(); rule.onNodeWithText("Weiter ▸").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("Geschafft!").assertExists()
    }

    @Test fun rideCockpitShowsSlopeAndCurvatureLive() {
        show { RideScreen({}, "huegel", controller = stage) }
        fun slide(u: Float) { rule.onNodeWithTag("fahrt-regler").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(u) }; rule.mainClock.advanceTimeBy(300) }
        assertTrue(stage.lastState!!.contains("\"type\":\"track\""))
        rule.onNodeWithText("↗ 4").assertExists(); rule.onNodeWithText("bergauf · 75,96°").assertExists()        // Start: f′(0) = 4
        rule.onNodeWithText("Rechtskurve (f″ < 0)").assertExists()
        assertTrue(stage.lastRide!!.contains("view:'fp'"))
        slide(0.5f)                                                                                                 // Gipfel bei x = 2
        rule.onNodeWithText("→ 0").assertExists(); rule.onNodeWithText("waagerecht · 0°").assertExists()
        rule.onNodeWithText("Hochpunkt bei x = 2").assertExists(); rule.onNodeWithText("f′ = 0 und f″ < 0").assertExists()
        slide(0.75f)
        rule.onNodeWithText("↘ −2").assertExists()
        rule.onNodeWithTag("sicht").performClick(); rule.mainClock.advanceTimeBy(300)
        assertTrue(stage.lastRide!!.contains("view:'out'"))
    }

    @Test fun rideDrivesByItselfAndStopsAtTheEnd() {
        show { RideScreen({}, "huegel", controller = stage) }
        rule.mainClock.autoAdvance = false        // sonst spult die Test-Uhr die laufende Fahrt von selbst bis zum Ende vor
        rule.onNodeWithTag("fahren").performClick(); rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithText("⏸ Anhalten").assertExists()
        rule.mainClock.advanceTimeBy(60_000)
        rule.onNodeWithText("↻ Noch einmal").assertExists()
        rule.mainClock.autoAdvance = true
        rule.onNodeWithTag("bahn-welle").performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("f(x) = 2 · sin(x)").assertExists(); rule.onNodeWithText("▶ Losfahren").assertExists()
    }


    /** Startseite seit 2.18.32: alle 17 Kapitel der Geometrie im Raum auf einen Blick, lückenlos nummeriert – und daneben der Weg zu „Analysis & Zufall erleben“. */
    @Test fun homeShowsGeometryAndTheWayToExperiences() {
        val opened = ArrayList<String>(); var experience = 0
        show { HomeScreen(Levels.chapters, mapOf("1-1" to 3), { opened += it }, {}, onExperience = { experience++ }) }
        rule.onNodeWithTag("gebiet-GEOMETRIE").assertExists(); rule.onNodeWithTag("gebiet-ANALYSIS").assertDoesNotExist(); rule.onNodeWithTag("gebiet-STOCHASTIK").assertDoesNotExist()
        rule.onNodeWithTag("kapitel-anzahl").assertTextContains("17 Kapitel")
        rule.onNodeWithTag("kapitel-3").assertExists(); for (gone in listOf(12, 13, 14, 15, 16, 17, 22, 23, 24, 25)) rule.onNodeWithTag("kapitel-$gone").assertDoesNotExist()
        // Kapitel 18 (intern) heißt auf dem Bildschirm Kapitel 12 – keine Lücke
        rule.onNodeWithTag("kapitel-18").performScrollTo().performClick(); rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithText("KAPITEL 12 · 6 AUFGABEN").assertExists(); rule.onNodeWithText("KAPITEL 18", substring = true).assertDoesNotExist()
        rule.onNodeWithTag("erleben").performClick(); rule.mainClock.advanceTimeBy(300); assertEquals(1, experience)
        assertEquals(setOf(Area.GEOMETRIE), Levels.chapters.map { it.area }.toSet())
    }
}
