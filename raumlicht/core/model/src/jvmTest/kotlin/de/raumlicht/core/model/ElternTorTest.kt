package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/** Eltern-Tor: Malaufgaben 13–19 mal 6–9 in Worten, nach einem Fehler eine andere Aufgabe, zehn Minuten offen. */
class ElternTorTest {
    @Test fun aufgabenInWortenImVereinbartenBereich() {
        val r = Random(7); val seen = HashSet<ElternTor.Task>()
        repeat(2000) { val t = ElternTor.fresh(r); seen += t
            assertTrue(t.a in 13..19 && t.b in 6..9); assertEquals(t.a * t.b, t.answer); assertFalse(t.words, t.words.any { it.isDigit() }); assertTrue(t.words, Regex("[a-zäöüß]+ mal [a-zäöüß]+").matches(t.words)) }
        assertEquals("alle 28 Aufgaben kommen vor", 28, seen.size)
        assertEquals("siebzehn mal acht", ElternTor.Task(17, 8).words); assertEquals("dreizehn mal sechs", ElternTor.Task(13, 6).words); assertEquals("neunzehn mal neun", ElternTor.Task(19, 9).words)
    }

    @Test fun pruefenUndNeueAufgabe() {
        val t = ElternTor.Task(17, 8)
        assertTrue(t.solves("136")); assertTrue(t.solves(" 136 ")); for (bad in listOf("", "135", "1360", "abc", "13 6")) assertFalse(bad, t.solves(bad))
        val r = Random(3); repeat(500) { val a = ElternTor.fresh(r); assertNotEquals(a, ElternTor.fresh(r, a)) }
    }

    @Test fun zehnMinutenOffen() {
        assertFalse(ElternTor.open(0, 1_000_000)); assertTrue(ElternTor.open(1_000_000, 1_000_000)); assertTrue(ElternTor.open(1_000_000, 1_000_000 + ElternTor.OPEN_MS - 1))
        assertFalse(ElternTor.open(1_000_000, 1_000_000 + ElternTor.OPEN_MS)); assertFalse("Uhr zurückgedreht", ElternTor.open(1_000_000, 999_999))
        assertEquals(600_000L, ElternTor.OPEN_MS); assertNotNull(Zaehler.body("elternbereich", null, null, "1"))
        for (bad in listOf("Frag", "Kauf dir", "€")) assertFalse(bad, (ElternTor.NOTE + ElternTor.TITLE + ElternTor.WRONG).contains(bad))
    }
}
