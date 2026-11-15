package de.raumlicht.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.raumlicht.app.ui.home.*
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.level.LocalMistakeSink
import de.raumlicht.app.ui.stage.LocalLuxLook
import de.raumlicht.app.ui.stage.LocalStageRenderer
import de.raumlicht.app.ui.stage.Stage
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.app.widget.LearnWidget
import de.raumlicht.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.random.Random

/** Die Erlebnis- und Übungsbereiche aus 0.5.0 von vorn bis hinten. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class PlayFlowTest {
    @get:Rule val rule = createComposeRule()
    private val stage = StageController()
    private fun show(content: @Composable () -> Unit) {
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }) { content() } } } }
        rule.mainClock.advanceTimeBy(800)
    }
    private fun slide(tag: String, u: Float) { rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(u) }; rule.mainClock.advanceTimeBy(300) }

    @Test fun flightShowsWhereTheLinePiercesThePlane() {
        show { FlightScreen({}, controller = stage) }
        assertTrue(stage.lastState!!.contains("\"style\":\"line\"")); assertTrue(stage.lastRide!!.contains("view:'fp'"))
        rule.onNodeWithText("Abstand 4").assertExists()                                  // Start: 4 von E₁ entfernt
        slide("flug-regler", 0.5f)                                                       // t = 2: genau in E₁
        rule.onNodeWithText("Durchstoßpunkt mit E₁: x₁ = 4").assertExists(); rule.onNodeWithText("t = 2 → S(4|4|3)").assertExists()
        rule.onNodeWithText("du bist genau in der Ebene").assertExists()
        rule.onNodeWithTag("flug-kugel").performClick(); rule.mainClock.advanceTimeBy(400)
        assertTrue(stage.lastState!!.contains("\"type\":\"sphere\""))
    }

    @Test fun galtonBallsFillTheBins() {
        show { GaltonScreen({}, controller = stage, random = Random(3)) }
        rule.onNodeWithText("0 Kugeln").assertExists(); rule.onNodeWithText("4").assertExists()          // μ = 8 · 0,5
        rule.onNodeWithTag("galton-10").performClick(); rule.mainClock.advanceTimeBy(6000)
        rule.onNodeWithText("10 Kugeln").assertExists()
        assertTrue(stage.lastState!!.contains("\"id\":\"erwartung\""))
        rule.onNodeWithTag("plus-p").performClick(); rule.mainClock.advanceTimeBy(400)                    // neues p → neues Experiment
        rule.onNodeWithText("0 Kugeln").assertExists(); rule.onNodeWithText("4,8").assertExists()
    }

    @Test fun quickModeHasNoTipButton() {
        show { QuickScreen({ _, _, _ -> }, {}, seed = 3) }
        rule.onNodeWithText("▶ Los geht's").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("▶ Los").assertExists(); rule.onNodeWithText("💡 Tipp").assertDoesNotExist()
        rule.onNodeWithText("✍️ Ohne Hilfsmittel · 1/6 · Geometrie").assertExists()
    }

    @Test fun namedMistakesGoToTheDiary() {
        val seen = ArrayList<Pair<String, String>>()
        val l = Levels.byId("2-1")!!
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }, LocalMistakeSink provides { id, msg -> seen += id to msg }) {
            LevelScreen(l, Levels.chapters[1], false, {}, { _, _ -> }, {}, {}, mapOf("v" to listOf(-3, -4, 0)))
        } } } }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(5000)
        assertEquals(1, seen.size); assertEquals("2-1", seen[0].first); assertTrue(seen[0].second.startsWith("Du hast A − B gerechnet"))
        // Zählen und Sortieren im Tagebuch
        var text = ""; repeat(3) { text = DiaryEntry.add(text, "2-1", seen[0].second) }; text = DiaryEntry.add(text, "7-5", "Kosinus statt Sinus")
        val entries = DiaryEntry.parse(text); assertEquals(listOf(3, 1), entries.map { it.count }); assertEquals("2-1", entries[0].levelId)
    }

    @Test fun rideCanShowDerivativeTrackAndFillTheArea() {
        show { RideScreen({}, "huegel", controller = stage) }
        assertFalse(stage.lastState!!.contains("ableitung")); rule.onNodeWithTag("flaeche").assertDoesNotExist()
        rule.onNodeWithTag("zeige-ableitung").performClick(); rule.onNodeWithTag("zeige-flaeche").performClick(); rule.mainClock.advanceTimeBy(400)
        slide("fahrt-regler", 0.5f); slide("fahrt-regler", 1f)
        assertTrue(stage.lastState!!.contains("\"id\":\"ableitung\"") && stage.lastState!!.contains("\"id\":\"plus"))
        rule.onNodeWithText("10,67").assertExists()                                       // ∫₀⁴ (−x² + 4x) dx = 32/3
    }

    @Test fun introShowsTheFirstTaskBeingSolved() {
        val l = Levels.byId("1-1")!!; var seenIntro = false
        show { LevelScreen(l, Levels.chapters[0], true, { seenIntro = true }, { _, _ -> }, {}, {}, controller = stage) }
        assertEquals(l.scene(l.start, Phase.AIM), stage.lastState)
        rule.onNodeWithText("Weiter ›").performClick(); rule.mainClock.advanceTimeBy(500)
        assertEquals(l.scene(l.solution, Phase.AIM), stage.lastState)
        rule.onNodeWithText("Weiter ›").performClick(); rule.mainClock.advanceTimeBy(500)
        assertEquals(l.scene(l.solution, Phase.DONE), stage.lastState)
        rule.onNodeWithText("Licht an! ▶").performClick(); rule.mainClock.advanceTimeBy(500)
        assertTrue(seenIntro); assertEquals(l.scene(l.start, Phase.AIM), stage.lastState)
    }

    @Test fun workshopExtrasAndStageLook() {
        var look = "none" to "none"
        show { CompositionLocalProvider(LocalLuxLook provides ("rainbow" to "crown")) { Box { Stage(stage); WorkshopScreen(0, "pink", {}, badges = 2, look = look, onLook = { t, h -> look = t to h }) {} } } }
        assertTrue(stage.log.any { it == "RL.look({trail:'rainbow',hat:'crown'})" })
        rule.onNodeWithTag("zubehoer-sparks-0").performClick(); assertEquals("sparks" to "none", look)
        rule.onNodeWithTag("zubehoer-crown-1").performClick(); assertEquals("sparks" to "none", look)     // Krone braucht 8 Erfolge
    }

    @Test fun learnWidgetAndShareCard() {
        assertEquals(listOf(0, 0, 0), LearnWidget.percents(emptyMap()))
        val all = Levels.all.associate { it.id to 3 }; assertEquals(listOf(100, 100, 100), LearnWidget.percents(all))
        val geo = Levels.chapters.filter { it.area == Area.GEOMETRIE }.flatMap { it.levels }
        assertEquals(listOf(50, 0, 0), LearnWidget.percents(geo.take(geo.size / 2).associate { it.id to 3 }).let { listOf(it[0], it[1], it[2]) })
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val v = LearnWidget.views(ctx, listOf(42, 10, 0)).apply(ctx, android.widget.FrameLayout(ctx))
        assertEquals("42 %", v.findViewById<android.widget.TextView>(de.raumlicht.app.R.id.learn_value_0).text.toString())
        assertEquals(42, v.findViewById<android.widget.ProgressBar>(de.raumlicht.app.R.id.learn_bar_0).progress)
        val card = ShareCard.draw("🎪 Festivalzelt", "11 von 12 BE", Exam.verdict(11, 12)); assertEquals(1080, card.width); assertEquals(1080, card.height)
    }

    @Test fun resetAsksFirst() {
        var resets = 0
        show { de.raumlicht.app.ui.settings.SettingsScreen(true, {}, true, {}, true, {}, true, {}, "0.5.3", de.raumlicht.app.ui.settings.UpdateState.Idle, {}, onReset = { resets++ }) }
        rule.onNodeWithTag("zuruecksetzen").performScrollTo().performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Wirklich alles zurücksetzen?").assertExists()
        rule.onNodeWithTag("zuruecksetzen-nein").performClick(); rule.mainClock.advanceTimeBy(500); assertEquals(0, resets)      // Abbrechen löscht nichts
        rule.onNodeWithTag("zuruecksetzen").performScrollTo().performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("zuruecksetzen-ja").performClick(); rule.mainClock.advanceTimeBy(500); assertEquals(1, resets)
        rule.onNodeWithText("✓ Alles zurückgesetzt – du startest wieder bei null.").assertExists()
    }
}
