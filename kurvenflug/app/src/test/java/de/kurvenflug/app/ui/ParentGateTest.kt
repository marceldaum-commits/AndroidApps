package de.kurvenflug.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.kurvenflug.app.ui.pro.ParentGate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Eltern-Tor vor dem Elternbereich: nur Erwachsene kommen durch, Kinder-Jahrgänge nicht. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParentGateTest {
    @get:Rule val rule = createComposeRule()

    @Test fun adultsPassChildrenDoNot() {
        var passed = 0
        rule.setContent { ParentGate(onDismiss = {}, why = "Der Elternbereich ist für Erwachsene.") { passed++ } }
        rule.onNodeWithText("Der Elternbereich ist für Erwachsene.", substring = true).assertExists()
        val year = rule.onNode(hasSetTextAction())
        year.performTextInput("2015"); rule.onNodeWithText("Weiter").performClick()
        rule.onNodeWithText("Das hat nicht gepasst.").assertExists(); assertEquals(0, passed)
        year.performTextClearance(); year.performTextInput("1980"); rule.onNodeWithText("Weiter").performClick()
        assertEquals(1, passed)
    }
}
