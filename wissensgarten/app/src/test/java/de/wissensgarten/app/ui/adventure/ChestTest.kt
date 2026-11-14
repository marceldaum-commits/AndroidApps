package de.wissensgarten.app.ui.adventure

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.wissensgarten.core.model.Friends
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Die Truhe muss sich per Antippen öffnen und genau einen Freund freigeben. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChestTest {
    @get:Rule val rule = createComposeRule()

    @Test fun tappingOpensChestOnce() {
        var opened = 0
        rule.setContent { ChestReveal(onOpen = { opened++; Friends.all[0] }, onClose = {}) }
        rule.onNodeWithText("Tippe, um sie zu öffnen 👆").performClick()
        rule.mainClock.advanceTimeBy(3000)
        rule.mainClock.advanceTimeBy(3000) // Spannungspause + Animation
        rule.onNodeWithText("Ein neuer Gartenfreund!").assertExists()
        rule.onNodeWithText("Marie").assertExists()
        assertEquals(1, opened)
        rule.mainClock.advanceTimeBy(5000)
        assertEquals("nur ein Freund pro Truhe", 1, opened)
    }
}
