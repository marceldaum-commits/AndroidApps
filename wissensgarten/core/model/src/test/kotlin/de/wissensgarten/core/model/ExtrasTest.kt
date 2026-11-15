package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class ExtrasTest {
    private fun card(id: String, s: Double, reps: Int, lapses: Int, back: String = "A") =
        StudyCard(id, "b", "Q$id", back, null, null, Memory(s, 5.0, 0, reps, lapses))

    @Test fun weedsAreForgottenCardsUntilPulled() {
        val cards = listOf(card("a", 1.0, 5, 3), card("b", 10.0, 5, 3), card("c", 1.0, 3, 1), card("d", 0.0, 0, 0))
        assertEquals(listOf("a"), Weeds.active(cards, emptyMap()).map { it.id })
        assertTrue(Weeds.active(cards, mapOf("a" to 3)).isEmpty())
        // erneut vergessen → wächst wieder
        assertEquals(listOf("a"), Weeds.active(listOf(card("a", 1.0, 6, 4)), mapOf("a" to 3)).map { it.id })
    }

    @Test fun rhythmJudgesTiming() {
        val b = Rhythm.BEAT_MS
        assertEquals(1, Rhythm.lit(b + 10, 4))
        assertEquals(Rhythm.Hit.PERFECT, Rhythm.judge(b + 10, 1, 1, 4))
        assertEquals(Rhythm.Hit.GOOD, Rhythm.judge(b + b * 9 / 10, 1, 1, 4))
        assertEquals(Rhythm.Hit.MISS, Rhythm.judge(b + 10, 2, 1, 4))
        assertEquals(Rhythm.Hit.MISS, Rhythm.judge(2 * b + 10, 1, 1, 4))
    }

    @Test fun hiveRoomsOpenWithHoney() {
        assertEquals(1, Hive.open(0).size)
        assertEquals("schlafen", Hive.next(0)!!.id)
        assertEquals(2, Hive.open(50).size)
        assertEquals(0.5f, Hive.combProgress(55), 0.001f)
        assertNull(Hive.next(5000))
    }

    @Test fun streaksAndCritters() {
        assertEquals(3, Critters.streak(setOf(8, 9, 10), 10))
        assertEquals(3, Critters.streak(setOf(7, 8, 9), 10))   // heute noch nicht gelernt: Serie zählt weiter
        assertEquals(0, Critters.streak(setOf(5, 6), 10))
        assertEquals(listOf("igel", "vogel"), Critters.earned(9).map { it.id })
        assertEquals("frosch", Critters.next(9)!!.id)
    }

    @Test fun lociStoreAndStory() {
        val m = mapOf(0 to "x", 3 to "y=z")
        assertEquals(m, Loci.decode(Loci.encode(m)))
        assertTrue(Loci.decode("99=a;kaputt").isEmpty())
        assertTrue(Loci.story(1, card("a", 1.0, 1, 0, "Paris")).contains("Paris"))
    }

    @Test fun questsAreStablePerDayAndAlwaysWater() {
        val a = Quests.forDay(100, "p", hasWeeds = false)
        assertEquals(a, Quests.forDay(100, "p", false))
        assertEquals(Quests.PER_DAY, a.size)
        assertEquals(Quests.Kind.WATER, a.first().kind)
        assertTrue(a.none { it.kind == Quests.Kind.WEED })
        assertEquals(a.map { it.kind }.toSet().size, a.size)
        val q = Quests.Quest(Quests.Kind.WATER, 5)
        assertTrue(Quests.done(q, mapOf("WATER" to 6))); assertFalse(Quests.done(q, emptyMap()))
        assertEquals(mapOf("WATER" to 3), Quests.decode(Quests.encode(mapOf("WATER" to 3))))
    }

    @Test fun filmGrowsOverTime() {
        val d = Scheduler.DAY_MS
        val f = GardenFilm.frames(listOf(GardenFilm.Plant(0, 40 * d, 30.0, 6), GardenFilm.Plant(20 * d, 0, 0.0, 0)), 40 * d, 4)
        assertEquals(4, f.size)
        assertEquals(1, f[0].stages.size)
        assertEquals(2, f[3].stages.size)
        assertTrue(f[0].stages[0].ordinal <= f[3].stages[0].ordinal)
        assertEquals(Stage.BLOOM, f[3].stages[0])
    }

    @Test fun blitzRewards() {
        assertEquals(0f, Blitz.brightness(0)); assertEquals(1f, Blitz.brightness(20))
        assertEquals(20, Blitz.dew(100))
    }
}
