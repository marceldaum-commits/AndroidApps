package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class ModesTest {
    @Test fun everyChapterHasASolvableBoss() {
        assertTrue(Bosses.all.size >= 7)
        for (b in Bosses.all) {
            assertEquals(3, b.stages.size)
            b.stages.forEach { s ->
                assertNotEquals("${s.id} ist Ersatz", Gen.FALLBACK, s.title)
                assertTrue("${s.id} nicht lösbar", Flight.fly(s, s.solution).success)
                assertFalse("${s.id} mit Startwerten gelöst", Flight.fly(s, s.start).success)
            }
            assertEquals(3, b.stages.map { it.id }.toSet().size)
        }
        assertEquals(listOf(3, 2, 2, 1), listOf(0, 1, 2, 3).map { Bosses.stars(it) })
    }

    @Test fun duelCodesRoundTripAndAreFoundInMessages() {
        val seed = DuelCode.newSeed(kotlin.random.Random(1))
        val text = DuelCode.shareText(seed, 2)
        assertEquals(DuelCode.Challenge(seed, 2), DuelCode.decode(text))
        assertEquals(DuelCode.Challenge(seed, 2), DuelCode.decode(DuelCode.encode(seed, 2).lowercase().replace("kf-", "KF-")))
        assertNull(DuelCode.decode("Hallo!"))
        assertEquals(DuelCode.level(seed), DuelCode.level(seed))
        assertTrue(DuelCode.verdict(1, 3).startsWith("Gewonnen"))
        assertTrue(DuelCode.verdict(2, 2).startsWith("Unentschieden"))
    }

    @Test fun stormGetsHarderAndStaysSolvable() {
        val levels = (0 until 30).map { Storm.level(42, it) }
        levels.forEach { assertTrue(it.id, Flight.fly(it, it.solution).success) }
        assertEquals(30, levels.map { it.id }.toSet().size)
    }

    @Test fun badgesFollowStats() {
        assertTrue(Badges.earned(Stats()).isEmpty())
        val ids = Badges.earned(Stats(stars = 1, perfect = 10, stormBest = 12)).map { it.id }
        assertEquals(listOf("first", "sharp", "storm10"), ids)
    }
}
