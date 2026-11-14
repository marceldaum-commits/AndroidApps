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
        rule.onNodeWithText("Kapitel fertig! 🏆").assertExists()
        // Start ist gesperrt – kein zweites Speichern durch Doppeltipp
        rule.onNodeWithText("🚀 Start").performClick()
        rule.mainClock.advanceTimeBy(4000)
        assertEquals(1, solved.size)
    }
}
