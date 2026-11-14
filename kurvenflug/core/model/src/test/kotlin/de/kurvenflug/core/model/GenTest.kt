package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class GenTest {
    private fun isFallback(l: Level) = l.title == Gen.FALLBACK

    @Test fun everyKindAndDifficultyIsSolvable() {
        for (kind in Gen.kinds) for (d in 0..2) for (seed in 1L..150L) {
            val l = Gen.level(kind, seed * 31 + d, d)
            assertFalse("$kind/$d/$seed: Notfall-Level", isFallback(l))
            assertEquals(kind, l.kind)
            assertTrue("$kind/$d/$seed nicht lösbar", Flight.fly(l, l.solution).success)
            assertFalse("$kind/$d/$seed schon mit Startwerten gelöst", Flight.fly(l, l.start).success)
            l.params.forEach { p -> assertEquals("$kind/$seed/${p.key}", l.solution.getValue(p.key), p.snap(l.solution.getValue(p.key)), 1e-9) }
        }
    }

    @Test fun hardHasMoreAsteroids() {
        val easy = (1L..50L).sumOf { Gen.level(Kind.LINE, it, 1).asteroids.size }
        val hard = (1L..50L).sumOf { Gen.level(Kind.LINE, it, 2).asteroids.size }
        assertTrue("$hard > $easy", hard > easy)
    }

    @Test fun anyCyclesThroughKindsAndIsDeterministic() {
        assertEquals(Gen.kinds.toSet(), (0L until Gen.kinds.size.toLong()).map { Gen.any(it).kind }.toSet())
        assertEquals(Gen.any(4711), Gen.any(4711))
    }
}
