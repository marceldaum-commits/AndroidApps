package de.kurvenflug.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.kurvenflug.app.ui.pro.ParentGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Eltern-Tor: Malaufgabe in Worten, Ziffern-Tastatur, nach Fehler neue Aufgabe, 10 Minuten offen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParentGateTest {
    @get:Rule val rule = createComposeRule()

    private fun taskAnswer(): Int {
        val node = rule.onNode(hasContentDescription("Rechenaufgabe", substring = true)).fetchSemanticsNode()
        val words = node.config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].first().removePrefix("Rechenaufgabe: ")
        val (a, b) = words.split(" mal ")
        val all = (13..19).associateBy { de.kurvenflug.core.model.ParentGateTask.word(it) }
        val small = (6..9).associateBy { de.kurvenflug.core.model.ParentGateTask.word(it) }
        return all.getValue(a) * small.getValue(b)
    }
    private fun tap(number: Int) { number.toString().forEach { rule.onNodeWithContentDescription(it.toString()).performClick() } }

    @Test fun adultsSolveTaskChildrenDoNot() {
        var passed = 0
        rule.setContent { ParentGate(onDismiss = {}, why = "Der Elternbereich ist für Erwachsene.") { passed++ } }
        rule.onNodeWithText("Der Elternbereich ist für Erwachsene.", substring = true).assertExists()
        // Aufgabe steht in Worten, nicht als Ziffern
        rule.onNodeWithText(" mal ", substring = true).assertExists()
        // falsche Antwort → Fehlertext und neue Aufgabe, nichts durchgelassen
        tap(taskAnswer() + 1); rule.onNodeWithContentDescription("Antwort abschicken").performClick()
        rule.onNodeWithText("Das hat nicht gepasst", substring = true).assertExists(); assertEquals(0, passed)
        // richtige Antwort auf die NEUE Aufgabe
        tap(taskAnswer()); rule.onNodeWithContentDescription("Antwort abschicken").performClick()
        assertEquals(1, passed)
    }

    @Test fun cancelAlwaysPossibleAndOpenFor10Minutes() {
        var dismissed = 0; var passed = 0
        val memory = de.kurvenflug.core.model.GateMemory()
        rule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(de.kurvenflug.app.ui.pro.LocalGateMemory provides memory) {
                ParentGate(onDismiss = { dismissed++ }) { passed++ }
            }
        }
        rule.onNodeWithText("Abbrechen").performClick(); assertEquals(1, dismissed)
        tap(taskAnswer()); rule.onNodeWithContentDescription("Antwort abschicken").performClick()
        assertEquals(1, passed); assertTrue(memory.isOpen(de.kurvenflug.core.model.nowMillis()))
    }
}
