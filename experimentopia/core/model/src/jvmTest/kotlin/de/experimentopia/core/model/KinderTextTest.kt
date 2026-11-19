package de.experimentopia.core.model

import org.junit.Test
import org.junit.Assert.assertTrue

/** Standard ELTERN-TOR (AppZentrale, 19.11.2026; UWG Anhang Nr. 28): Was Blubb zu Kindern sagt, fordert nie zum Kauf auf und nennt keine Preise. */
class KinderTextTest {
    private val verboten = listOf("frag deine eltern", "kauf dir", "kaufen", "€", "nur noch heute", "\\babo\\b").map { Regex(it) }
    @Test fun blubbWirbtNie() {
        val schlecht = Say.all().filter { t -> verboten.any { it.containsMatchIn(t.lowercase()) } }
        assertTrue("Werbung/Kaufaufforderung in Kindertexten: ${schlecht.take(5)}", schlecht.isEmpty())
    }
    @Test fun schlossSachlich() {
        assertTrue(Say.proWhat("Knallgas").endsWith("Deine Eltern können im Elternbereich nachsehen."))
    }
}
