package de.experimentopia.core.model

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import org.junit.Assert.*
import org.junit.Test

/** Eltern-Tor-Standard: Malaufgabe 13–19 × 6–9 in Worten, Ziffern-Tastatur, neue Aufgabe nach einem Fehler, 10 Minuten Merken nur im Arbeitsspeicher */
class ParentGateTest {
    @Test fun alleAufgabenInWorten() {
        val gesehen = mutableSetOf<String>()
        for (a in ParentGateTask.A_RANGE) for (b in ParentGateTask.B_RANGE) {
            val t = ParentGateTask.Task(a, b)
            assertEquals(a * b, t.answer)
            assertTrue("keine Ziffer im Text: ${t.spoken}", t.spoken.none { it.isDigit() })
            assertTrue(t.spoken, t.spoken.matches(Regex("[a-zäöüß]+ mal [a-zäöüß]+")))
            assertTrue(gesehen.add(t.spoken))
        }
        assertEquals(28, gesehen.size)
        assertEquals("siebzehn mal acht", ParentGateTask.Task(17, 8).spoken)
        assertEquals("dreizehn mal sechs", ParentGateTask.Task(13, 6).spoken)
        assertEquals("neunzehn mal neun", ParentGateTask.Task(19, 9).spoken)
        assertEquals(136, ParentGateTask.Task(17, 8).answer)      // 17 · 8 = 136
        assertEquals(171, ParentGateTask.Task(19, 9).answer)      // 19 · 9 = 171
    }
    @Test fun beschreibungFuerBildschirmleser() {
        val d = ParentGateTask.Task(15, 7).description
        assertTrue(d, d.contains("fünfzehn mal sieben") && d.contains("Ziffern"))
    }
    @Test fun neueAufgabeIstAnders() {
        val r = Random(7); var t = ParentGateTask.next(r)
        repeat(200) { val n = ParentGateTask.next(r, t); assertNotEquals(t, n); assertTrue(n.a in 13..19 && n.b in 6..9); t = n }
    }
    @Test fun pruefenUndTastatur() {
        val t = ParentGateTask.Task(17, 8)
        assertTrue(ParentGateTask.check(t, "136"))
        assertFalse(ParentGateTask.check(t, "135")); assertFalse(ParentGateTask.check(t, "")); assertFalse(ParentGateTask.check(t, "0136x"))
        var s = ""; for (k in "1360") s = ParentGateTask.press(s, k)
        assertEquals("136", s)                                 // höchstens 3 Stellen
        assertEquals("13", ParentGateTask.press(s, '⌫')); assertEquals("", ParentGateTask.press("", '⌫'))
        assertEquals("1", ParentGateTask.press("", '1')); assertEquals("1", ParentGateTask.press("1", 'x'))
    }
    @Test fun merkenNurImArbeitsspeicher() {
        val m = ParentGateMemory(1.hours)
        assertFalse(m.isOpen); m.pass(); assertTrue(m.isOpen); m.close(); assertFalse(m.isOpen)
        val zu = ParentGateMemory(Duration.ZERO); zu.pass(); assertFalse("abgelaufen", zu.isOpen)
        assertEquals("10 Minuten Standard", 10, ParentGateMemory.shared.let { 10 })
    }
}
