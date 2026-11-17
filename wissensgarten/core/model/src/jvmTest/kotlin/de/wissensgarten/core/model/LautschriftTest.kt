package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class LautschriftTest {
    private val lex = Lautschrift(sequenceOf(
        "das\tdas", "ist\tɪst", "ein\taɪn", "hund\thˈʊnt", "DNA\tdˌeːˌɛnˈɑː", "komma\tkˈɔma", "hundert\thˈʊndɜt", "tausend\ttˈaʊzənt",
        "eintausend\tˈaɪntˌaʊzənt", "zum\ttsʊm", "beispiel\tbˈaɪʃpiːl", "prozent\tpɾotsˈɛnt", "café\tkafˈeː", "minus\tmˈiːnʊs",
        "1\tˈaɪns", "2\ttsvˈaɪ", "5\tfˈʏnf", "14\tfˈiːɐtseːn", "19\tnˈɔøntseːn", "25\ttsvˈaɪʊntsvˈantsɪç", "250\tzweihundertfünfzig",
    ))

    @Test fun textWirdAufbereitet() {
        assertEquals("Das ist zum Beispiel ein Hund.", SprechText.clean("Das ist z. B. ein Hund. 🐶"))
        assertEquals("25 Prozent, das ist ein Hund!", SprechText.clean("25 % – das ist „ein“ Hund!!"))
        assertEquals("1250 Euro", SprechText.clean("1.250 €"))
        assertEquals("minus 5 plus 2 gleich minus 3", SprechText.clean("-5 + 2 = -3"))
        assertEquals("E Mail", SprechText.clean("E-Mail"))
        assertEquals("", SprechText.clean("🐝🌻"))
    }

    @Test fun saetzeWerdenGesetzt() {
        assertEquals(listOf("das ɪst aɪn hˈʊnt.", "aɪn hˈʊnt!"), lex.of(SprechText.clean("Das ist ein Hund. Ein Hund!")))
        assertEquals(listOf("das ɪst, aɪn hˈʊnt"), lex.of("Das ist, ein Hund"))
        assertEquals(listOf("dˌeːˌɛnˈɑː ɪst kafˈeː"), lex.of("DNA ist Café"))
        assertEquals(listOf("das ɪst aɪn hˈʊnt"), lex.of("DAS IST EIN HUND"))       // betonte Großschreibung → kleines Wort
        assertNull(lex.of("Das ist ein Quarkstrudel."))
        assertEquals(listOf("Quarkstrudel"), lex.missing("Das ist ein Quarkstrudel."))
        assertEquals(listOf("π"), lex.missing("Das ist π"))
        assertNull(lex.of(""))
    }

    @Test fun zahlen() {
        assertEquals(listOf("tsvˈaɪ kˈɔma fˈʏnf"), lex.of("2,5"))
        assertEquals(listOf("ˈaɪntˌaʊzənt zweihundertfünfzig"), lex.of("1250"))     // ohne „12“ im Wörterbuch: eintausend …
        assertEquals(listOf("tsvˈaɪ tˈaʊzənt zweihundertfünfzig"), lex.of("2250"))
        assertEquals(listOf("nˈɔøntseːn hˈʊndɜt fˈiːɐtseːn"), lex.of("1914"))     // Jahreszahl
        assertEquals(listOf("tsvˈaɪ tˈaʊzənt fˈʏnf"), lex.of("2005"))
        assertNull(lex.of("1000000"))
    }

    @Test fun piperNummern() {
        val map = PiperIds.tokens(sequenceOf("_ 0", "^ 1", "$ 2", "  3", "a 14", "ɪ 74", "ˈ 120"))
        assertEquals(3, map[' '.code])
        // ^ _ (Laut _)* $
        assertArrayEquals(intArrayOf(1, 0, 120, 0, 14, 0, 74, 0, 3, 0, 14, 0, 2), PiperIds.encode("ˈaɪ a", map))
        assertArrayEquals(intArrayOf(1, 0, 14, 0, 2), PiperIds.encode("a?", map))        // unbekannte Zeichen fallen weg (wie bei piper)
        assertArrayEquals(intArrayOf(0x61, 0x26A), PiperIds.codepoints("aɪ"))
    }
}
