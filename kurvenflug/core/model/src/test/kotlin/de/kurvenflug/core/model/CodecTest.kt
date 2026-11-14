package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class CodecTest {
    @Test fun builtInLevelsSurviveTheRoundTrip() {
        for (l in Levels.all.filter { it.kind in Templates.buildable && it.fixed.keys.all { k -> k in Templates.fixed(it.kind) } }) {
            val code = Codec.encodeLevel(LevelSpec.of(l, "Lena"))
            assertTrue("${l.id}: Code zu lang (${code.length})", code.length < 600)
            val back = Codec.decodeLevel("Probier mal: $code 🚀")!!.toLevel("x")!!
            assertTrue("${l.id} nach dem Teilen nicht mehr lösbar", Flight.fly(back, back.solution).success)
            assertEquals(l.targets.size, back.targets.size)
        }
    }

    @Test fun challengeKeepsOrderAndMixesOwnLevels() {
        val own = LevelSpec.of(Gen.level(Kind.LINE, 5, 1), "Herr Meier")
        val c = ChallengeSpec("Hausaufgabe 8b", refs = listOf("2-6", "7-1"), custom = listOf(own), order = listOf("r1", "c0", "r0"))
        val back = Codec.decodeChallenge(Codec.encodeChallenge(c))!!
        assertEquals("Hausaufgabe 8b", back.title)
        val lv = back.levels()
        assertEquals(listOf("7-1", "challenge-c0", "2-6"), lv.map { it.id })
        assertEquals(Codec.Type.CHALLENGE, Codec.detect("Code: " + Codec.encodeChallenge(c)))
    }

    @Test fun brokenCodesAreRejected() {
        assertNull(Codec.decodeLevel("KFL1:kaputt!!"))
        assertNull(Codec.decodeLevel("nichts"))
        assertNull(LevelSpec("ORBITX", "t", "t", emptyList(), solution = emptyMap()).toLevel("x"))
        assertEquals(Codec.Type.DUEL, Codec.detect("KF-ABCDE-2"))
    }

    @Test fun checkExplainsProblems() {
        val l = Levels.byId("2-6")!!
        assertNull(Codec.check(l))
        assertEquals("Setz mindestens eine Station.", Codec.check(l.copy(targets = emptyList())))
        assertTrue(Codec.check(l.copy(solution = mapOf("m" to 0.0, "b" to 0.0)))!!.contains("trifft noch nicht"))
    }

    @Test fun detectiveParsesGermanNumbersAndComparesCurves() {
        assertEquals(-0.5, Detective.parse("−0,5")!!, 0.0)
        assertEquals(-0.5, Detective.parse("-1/2")!!, 0.0)
        assertEquals(0.25, Detective.parse(",25")!!, 0.0)
        assertNull(Detective.parse("abc"))
        for (r in 0 until 8) {
            val l = Detective.case(7, r)
            assertTrue(l.id, Detective.matches(l, l.solution))
            assertFalse(l.id, Detective.matches(l, l.start))
        }
        // Sinus: a·sin(b·x) = (−a)·sin(−b·x) – gleiche Kurve zählt
        val s = (0 until 8).map { Detective.case(7, it) }.first { it.kind == Kind.SINE }
        val flipped = s.solution.toMutableMap().apply { put("a", -getValue("a")); put("b", -getValue("b")) }
        assertTrue(Detective.matches(s, flipped))
    }
}
