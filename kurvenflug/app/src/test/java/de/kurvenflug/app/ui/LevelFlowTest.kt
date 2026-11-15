package de.kurvenflug.app.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.kurvenflug.app.ui.flight.LevelScreen
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.theme.KurvenflugTheme
import de.kurvenflug.core.model.Levels
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ein Level von vorn bis hinten spielen: einstellen, daneben, Tipp, Treffer, Sterne. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class LevelFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun missThenHitGivesTwoStarsOnce() {
        val l = Levels.byId("1-1")!!
        val solved = mutableListOf<Triple<Int, Set<Int>, Int>>()
        rule.setContent { KurvenflugTheme { Surface(color = Q.Space) {
            LevelScreen(l, Levels.chapters[0], Q.Cyan, false, {}, { s, c, a -> solved += Triple(s, c, a) }, null, {})
        } } }
        rule.mainClock.advanceTimeBy(1000)
        // x auf 4 stellen, y bleibt 0 → daneben
        repeat(4) { rule.onAllNodesWithText("+")[0].performClick() }
        rule.onNodeWithText("🚀 Start").performClick()
        rule.mainClock.advanceTimeBy(4000)
        rule.onNodeWithText("Die Höhe stimmt", substring = true).assertDoesNotExist()
        rule.onNodeWithText("x stimmt", substring = true).assertExists()
        assertTrue(solved.isEmpty())
        // y auf 3 → Treffer im zweiten Versuch
        repeat(3) { rule.onAllNodesWithText("+")[1].performClick() }
        rule.onNodeWithText("🚀 Start").performClick()
        rule.mainClock.advanceTimeBy(4000)
        assertEquals(listOf(Triple(2, emptySet<Int>(), 2)), solved)
        rule.onNodeWithText("Fertig! 🏆").assertExists()
        // Start ist gesperrt – kein zweites Speichern durch Doppeltipp
        rule.onNodeWithText("🚀 Start").performClick()
        rule.mainClock.advanceTimeBy(4000)
        assertEquals(1, solved.size)
    }

    /** Rechenweg erst nach dem 3. Fehlversuch; durchrechnen, Regler stellen, Treffer. Volt rechnet beim ersten Mal vor. */
    @Test fun walkthroughAppearsAfterThirdMissAndLeadsToTheSolution() {
        val l = Levels.byId("3-1")!! // y = x² + e durch (2 | 1), Start e = 0
        var opened = 0; var solvedStars = 0
        rule.setContent { KurvenflugTheme { Surface(color = Q.Space) {
            LevelScreen(l, Levels.chapters.first { it.number == 3 }, Q.Cyan, false, {}, { s, _, _ -> solvedStars = s }, null, {},
                voltSolves = 1, onWalkthrough = { opened++ })
        } } }
        rule.mainClock.advanceTimeBy(1000)
        repeat(3) { n ->
            rule.onNodeWithText("🧮 Rechenweg mit Volt").assertDoesNotExist()
            rule.onNodeWithText("🚀 Start").performClick()
            rule.mainClock.advanceTimeBy(4000)
        }
        rule.onNodeWithText("🧮 Rechenweg mit Volt").performClick()
        rule.mainClock.advanceTimeBy(800)
        assertEquals(1, opened)
        // Schritt 1 hat Volt vorgerechnet; falsch tippen → bleibt stehen, richtig → weiter
        rule.onNodeWithText("Schritt 2 von 3").assertExists()
        rule.onNodeWithText("5").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Schritt 2 von 3").assertExists()
        rule.onAllNodesWithText("4").filterToOne(hasClickAction()).performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Schritt 3 von 3").assertExists()
        rule.onNodeWithText("−3").performClick()
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("e = −3", substring = true).assertExists()
        rule.onNodeWithText("🚀 Los geht's – Regler einstellen").performClick()
        rule.mainClock.advanceTimeBy(500)
        // Regler selbst einstellen: e von 0 auf −3
        repeat(3) { rule.onAllNodesWithText("−")[0].performClick() }
        rule.onNodeWithText("🚀 Start").performClick()
        rule.mainClock.advanceTimeBy(4000)
        assertEquals(1, solvedStars)
    }

    /** Zurücksetzen nur nach 2 s Gedrückthalten – kurzes Tippen oder früh loslassen löscht nichts. */
    @Test fun resetNeedsALongPress() {
        var resets = 0
        rule.setContent { KurvenflugTheme { Surface(color = Q.Space) {
            de.kurvenflug.app.ui.settings.SettingsScreen(sound = true, onSound = {}, vibration = true, onVibration = {}, autoUpdate = true,
                onAutoUpdate = {}, installed = "0.9.0", update = de.kurvenflug.app.ui.settings.UpdateState.Idle, onBack = {}, onReset = { resets++ })
        } } }
        rule.mainClock.autoAdvance = false
        val button = rule.onNodeWithText("Zum Löschen 2 Sekunden gedrückt halten").performScrollTo()
        button.performClick()
        rule.mainClock.advanceTimeBy(3000)
        assertEquals(0, resets)
        button.performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithText("Festhalten", substring = true).performTouchInput { up() }
        rule.mainClock.advanceTimeBy(3000)
        assertEquals(0, resets)
        rule.onNodeWithText("Zum Löschen 2 Sekunden gedrückt halten").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(2300)
        assertEquals(1, resets)
        rule.onNodeWithText("Alles zurückgesetzt", substring = true).assertExists()
    }

    @Test fun stormEndsOnFirstMissAndReportsWaves() {
        var reported = -1
        rule.setContent { KurvenflugTheme { Surface(color = Q.Space) {
            de.kurvenflug.app.ui.modes.StormScreen(3, Q.Cyan, { reported = it; it > 3 }, {}, seed = 9)
        } } }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithText("Los! 🚀").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("🚀 Start").performClick()      // Startwerte treffen nie → Fehlschuss
        rule.mainClock.advanceTimeBy(5000)
        assertEquals(0, reported)
        rule.onNodeWithText("Sturm vorbei").assertExists()
        rule.onNodeWithText("Neuer Sturm ↻").assertExists()
    }

    @Test fun duelCodeOpensChallenge() {
        val code = de.kurvenflug.core.model.DuelCode.encode(123456, 2)
        rule.setContent { KurvenflugTheme { Surface(color = Q.Space) {
            de.kurvenflug.app.ui.modes.DuelScreen(Q.Cyan, "Schau mal: $code", {}, {})
        } } }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithText("Herausforderung!").assertExists()
        rule.onNodeWithText("2 Versuchen", substring = true).assertExists()
    }
}
