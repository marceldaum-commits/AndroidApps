package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class SafariTest {
    @Test fun translatesAndFiltersLabels() {
        val c = Safari.candidates(listOf("Event" to 0.9f, "Dog" to 0.88f, "Pet" to 0.8f, "Cat" to 0.3f))
        assertEquals(listOf("der Hund", "das Haustier"), c.map { it.de })
        assertEquals("dog", c.first().en)
    }

    @Test fun cardsForYoungAndOld() {
        val w = Safari.translate("Teddy bear")!!
        assertEquals("teddy bear", Safari.card(w, english = true).a)
        assertEquals("Wie heißt „Teddybär“ auf Englisch?", Safari.card(w, english = true).q)
        assertEquals("der Teddybär", Safari.card(w, english = false).a)
    }
}
