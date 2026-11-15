package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class AndromedaTest {
    @Test fun first50PlanetsAreSolvableHardAndStable() {
        for (n in 1..50) {
            val p = Andromeda.planet(n)
            assertEquals(3, p.levels.size)
            p.levels.forEach { l ->
                assertTrue(l.id, Flight.fly(l, l.solution).success)
                assertFalse(l.id, Flight.fly(l, l.start).success)
                assertEquals(l, Andromeda.levelFor(l.id))
            }
            assertEquals(p, Andromeda.planet(n))
        }
        assertEquals(50, (1..50).map { Andromeda.planet(it).levels.first().id }.toSet().size)
    }

    @Test fun progressAndNovaParts() {
        assertFalse(Andromeda.unlocked(17)); assertTrue(Andromeda.unlocked(18))
        val stars = (1..3).flatMap { n -> (1..3).map { "andro-$n-$it" to 2 } }.toMap() + ("andro-5-1" to 3)
        assertEquals(3, Andromeda.completed(stars))
        assertEquals(3, Andromeda.novaParts(stars))
        assertEquals("andro-1-2", Andromeda.next("andro-1-1")!!.id)
        assertNull(Andromeda.next("andro-1-3"))
        assertNull(Andromeda.levelFor("andro-0-1"))
        val many = (1..25).flatMap { n -> (1..3).map { "andro-$n-$it" to 1 } }.toMap()
        assertEquals(18, Andromeda.novaParts(many))
    }
}
