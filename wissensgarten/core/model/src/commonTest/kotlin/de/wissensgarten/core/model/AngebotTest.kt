package de.wissensgarten.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AngebotTest {
    private fun p(id: String, subject: String, tier: String = Tier.FREE) = Pack(id, 1, id, subject, listOf(5), "📚", tier, cards = emptyList())

    @Test fun sprachenNichtMehrImAngebot() {
        assertFalse(Angebot.angeboten(p("s1-englisch-tenses", "englisch")))
        assertFalse(Angebot.angeboten(p("s1-latein-grundwortschatz", "latein")))
        assertFalse(Angebot.angeboten(p("gs-rechtschreibung", "deutsch")))
        assertFalse(Angebot.angeboten(p("aw-redewendungen", "deutsch")))
    }

    @Test fun literaturUndAndereFaecherBleiben() {
        assertTrue(Angebot.angeboten(p("s1-deutsch-stilmittel", "deutsch")))
        assertTrue(Angebot.angeboten(p("s1-mathe-algebra", "mathe")))
        assertTrue(Angebot.angeboten(p("gs-tiere", "sachunterricht")))
    }

    @Test fun plusBleibtUnberuehrt() {
        assertTrue(Angebot.angeboten(p("ob-englisch-abi-vokabeln", "englisch", Tier.PLUS)))
    }
}
