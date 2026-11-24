package de.kurvenflug.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ParentGateTaskTest {
    @Test fun wordsAndAnswer() {
        val t = ParentGateTask(17, 8)
        assertEquals("siebzehn mal acht", t.words)
        assertEquals(136, t.answer)
        assertTrue(t.isRight("136")); assertTrue(t.isRight(" 136 ")); assertFalse(t.isRight("135")); assertFalse(t.isRight("")); assertFalse(t.isRight("abc"))
    }

    @Test fun everyTaskInRangeHasWords() {
        for (a in 13..19) for (b in 6..9) { val t = ParentGateTask(a, b); assertTrue(t.words.contains(" mal ")); assertEquals(a * b, t.answer) }
        assertEquals(28, ParentGateTask.A_RANGE.count() * ParentGateTask.B_RANGE.count())
    }

    @Test fun nextStaysInRangeAndNeverRepeats() {
        var prev: ParentGateTask? = null
        for (r in 0 until 200) {
            val t = ParentGateTask.next(r * 7919, prev)
            assertTrue(t.a in 13..19 && t.b in 6..9)
            assertNotEquals(prev, t)
            prev = t
        }
        // alle 28 Aufgaben kommen vor
        assertEquals(28, (0 until 28).map { ParentGateTask.next(it) }.toSet().size)
        // negative Zufallszahlen sind ok
        ParentGateTask.next(-5)
    }

    @Test fun memoryOpenTenMinutes() {
        val m = GateMemory(); assertFalse(m.isOpen(1_000))
        m.open(1_000)
        assertTrue(m.isOpen(1_000 + 9 * 60_000L + 59_000)); assertFalse(m.isOpen(1_000 + 10 * 60_000L))
        m.close(); assertFalse(m.isOpen(2_000))
    }
}
