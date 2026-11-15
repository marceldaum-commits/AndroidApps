package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FunTest {
    @Test fun voltGreetsByTimeAndAbsenceWithoutBlame() {
        assertEquals(Feeling.IDLE, Greeting.home(12, 0, 0).first)
        assertEquals(Feeling.MISSED_YOU, Greeting.home(12, 5, 10).first)
        assertEquals(Feeling.SLEEPY, Greeting.home(22, 0, 10).first)
        assertEquals(Feeling.SLEEPY, Greeting.home(3, 1, 10).first)
        assertEquals(Feeling.HAPPY, Greeting.home(15, 1, 10).first)
        // keine Schuld, kein Druck
        for (h in 0..23) for (d in listOf(0, 1, 5, 30)) {
            val text = Greeting.home(h, d, 7).second.lowercase()
            assertFalse(text, listOf("verloren", "schade", "endlich", "zu lange").any { it in text })
        }
    }

    @Test fun stickerDropsAreRarerWithoutThreeStarsAndEpicsAreRare() {
        val r = Random(42)
        val three = (1..10_000).count { Stickers.roll(r, 3, emptySet()) != null }
        val one = (1..10_000).count { Stickers.roll(r, 1, emptySet()) != null }
        assertTrue("3 ⭐: $three", three in 5_700..6_300)
        assertTrue("1 ⭐: $one", one in 2_700..3_300)
        assertNull(Stickers.roll(r, 0, emptySet()))
        val drops = (1..20_000).mapNotNull { Stickers.roll(r, 3, emptySet()) }
        val epic = drops.count { it.sticker.rarity == Rarity.EPIC }.toDouble() / drops.size
        val glitter = drops.count { it.glitter }.toDouble() / drops.size
        assertEquals(0.05, epic, 0.015)
        assertEquals(0.10, glitter, 0.02)
        assertEquals(24, Stickers.all.size)
        assertEquals(Stickers.all.size, Stickers.all.map { it.id }.toSet().size)
    }

    @Test fun ownedStickerIsNotNew() {
        val r = Random(1)
        val d = generateSequence { Stickers.roll(r, 3, emptySet()) }.first()
        val again = Stickers.roll(Random(1), 3, setOf(Stickers.key(d.sticker, d.glitter)))!!
        assertEquals(d.sticker, again.sticker)
        assertFalse(again.new)
    }
}
