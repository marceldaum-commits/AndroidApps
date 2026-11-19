package de.wissensgarten.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AngebotTest {
    private fun p(id: String, subject: String, tier: String = Tier.FREE) = Pack(id, 1, id, subject, listOf(5), "📚", tier, cards = emptyList())

    @Test fun sprachenAusgeblendet() {
        assertFalse(Angebot.angeboten(p("s1-englisch-tenses", "englisch")))
        assertFalse(Angebot.angeboten(p("s1-latein-grundwortschatz", "latein")))
        assertFalse(Angebot.angeboten(p("gs-rechtschreibung", "deutsch")))
        assertFalse(Angebot.angeboten(p("aw-redewendungen", "deutsch")))
        assertFalse(Angebot.angeboten(p("ob-englisch-abi-vokabeln", "englisch", Tier.PLUS)))
        assertFalse(Angebot.angeboten(p("aw-experte-sprache", "deutsch", Tier.PLUS)))
        assertEquals(21, Angebot.AUSGEBLENDET.size)
    }

    @Test fun literaturUndAndereFaecherBleiben() {
        assertTrue(Angebot.angeboten(p("s1-deutsch-stilmittel", "deutsch")))
        assertTrue(Angebot.angeboten(p("ob-deutsch-epochen", "deutsch", Tier.PLUS)))
        assertTrue(Angebot.angeboten(p("s1-mathe-algebra", "mathe")))
        assertFalse(Angebot.ausgeblendet(null))
    }
}
