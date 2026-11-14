package de.wissensgarten.app.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.wissensgarten.app.ui.review.ReviewScreen
import de.wissensgarten.app.ui.theme.WissensgartenTheme
import de.wissensgarten.core.data.ThemeMode
import de.wissensgarten.core.data.db.BedEntity
import de.wissensgarten.core.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression: Nach einer richtigen Antwort darf die Runde nicht weiterspringen, während man ein Merkbild malt. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ReviewDrawTest {
    @get:Rule val rule = createComposeRule()

    private val known = Memory(stability = 3.0, difficulty = 5.0, lastReview = 1, reps = 2, lapses = 0)
    // „c1“ ergibt im ruhigen Modus die Auswahl-Spielart
    private val card = StudyCard("c1", "b", "Wie heißt Hund auf Englisch?", "dog", listOf("cat", "cow", "bird"), null, known, "🐶")
    private val second = StudyCard("c3", "b", "Wie heißt Katze auf Englisch?", "cat", listOf("dog", "cow", "bird"), null, known, "🐱")

    @Test fun drawingPausesAutoAdvance() {
        var completed = false
        rule.setContent {
            WissensgartenTheme(ThemeMode.LIGHT) { Surface {
                ReviewScreen(listOf(card, second), mapOf("b" to BedEntity("b", "p", "Englisch", "💬", 340f, null, 0, 0)),
                    true, { _, _, _ -> }, {}, calm = true, onComplete = { _, _ -> completed = true })
            } }
        }
        rule.mainClock.advanceTimeBy(1500)
        rule.onNodeWithText("dog").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("✏️ Selbst malen").performClick()
        rule.mainClock.advanceTimeBy(5000) // weit länger als das automatische Weiterblättern
        rule.onNodeWithText("✏️ Mal dein Merkbild!").assertExists()
        rule.onNodeWithText("Wie heißt Katze auf Englisch?").assertDoesNotExist()
        // Malfläche schließen: jetzt geht es per Knopf weiter, nicht von allein
        rule.onNode(isDialog()).performTouchInput { swipe(center, center.copy(x = center.x + 100f), 200) }
        rule.onNodeWithText("Fertig! 🎨").performClick()
        rule.mainClock.advanceTimeBy(3000)
        rule.onNodeWithText("Gemerkt – weiter").assertExists().performClick()
        rule.mainClock.advanceTimeBy(1500)
        assert(!completed)
        rule.onAllNodesWithText("Wie heißt Katze auf Englisch?", substring = true).onFirst().assertExists()
    }
}
