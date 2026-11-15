package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

/** Erzeugte Aufgaben: immer lösbar, ganzzahlig, im Raum – für viele Seeds geprüft. */
class GenTest {
    private fun check(l: Level) {
        assertTrue("${l.id}: Lösung wird nicht anerkannt", l.evaluate(l.solution).success)
        assertFalse("${l.id}: Startwerte lösen", l.evaluate(l.start).success)
        l.controls.forEach { c -> listOf(l.solution.getValue(c.key), c.start).forEach { v -> v.forEachIndexed { i, x -> assertTrue("${l.id}/${c.key}", x in c.min[i]..c.max[i]) } } }
        for (ph in Phase.entries) {
            val objs = l.objects(if (ph == Phase.DONE) l.solution else l.start, ph)
            assertEquals(l.id, objs.size, objs.map { it.id }.toSet().size)
            objs.filter { it.type == "point" }.forEach { o -> o.p!!.forEachIndexed { i, x -> assertTrue("${l.id}/${o.id} außerhalb", x >= -1e-9 && x <= l.bounds.max[i] + 1e-9) } }
        }
        assertTrue(l.way.isNotEmpty() && l.tip.isNotBlank())
    }

    @Test fun everyKindWorksForManySeeds() = Gen.Kind.entries.forEach { k -> for (seed in 0L until 300) check(Gen.level(k, seed)) }

    @Test fun sameSeedSameLevel() {
        val a = Gen.level(Gen.Kind.CROSS, 42); val b = Gen.level(Gen.Kind.CROSS, 42)
        assertEquals(a.solution, b.solution); assertEquals(a.scene(a.start, Phase.AIM), b.scene(b.start, Phase.AIM))
        assertNotEquals(a.solution to a.scene(a.start, Phase.AIM), Gen.level(Gen.Kind.CROSS, 43).let { it.solution to it.scene(it.start, Phase.AIM) })
    }

    @Test fun dailiesForThreeYears() {
        for (day in 20_700L until 20_700 + 1100) { val l = Daily.forDay(day); check(l); assertEquals("tag-$day", l.id) }
        assertTrue(Daily.isDaily("tag-20700")); assertFalse(Daily.isDaily("1-1"))
    }

    @Test fun jamSessionsDifferAndStayOnTopic() {
        assertNotEquals(Gen.jam(JamTopic.MIX, 1, 0).id, Gen.jam(JamTopic.MIX, 2, 0).id)
        repeat(40) { n -> assertTrue(Gen.jam(JamTopic.SCHNITT, 7, n).id.startsWith("jam-cross")); check(Gen.jam(JamTopic.MIX, 9, n)) }
    }

    @Test fun badges() {
        assertTrue(Badges.all(emptyMap(), 0).none { it.earned })
        val all = Levels.all.associate { it.id to 3 } + mapOf("tag-1" to 3, "tag-2" to 1, "tag-3" to 2)
        val b = Badges.all(all, 15)
        assertTrue(b.filter { it.id != "tag10" }.all { it.earned }); assertFalse(b.first { it.id == "tag10" }.earned)
        assertEquals(b.size, b.map { it.id }.toSet().size)
    }

    @Test fun newChaptersMathIsIndependentlyCorrect() {
        fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
        assertEquals(45.0, Geo.angle(p(5, 0, 0), p(5, 5, 0)), 1e-9); assertEquals(60.0, Geo.angle(p(4, 4, 0), p(0, 4, 4)), 1e-9)
        assertEquals(45.0, 90 - Geo.angle(p(3, 4, 5), p(0, 0, 1)), 1e-9); assertEquals(30.0, 90 - Geo.angle(p(0, 1, 1), p(1, 1, 0)), 1e-9)
        assertEquals(3.0, Geo.distPointLine(p(5, 3, 4), p(0, 1, 0), p(2, 2, 1)), 1e-9)
        assertEquals(3.0, Geo.distPointLine(p(5, 2, 6), p(1, 0, 1), p(1, 2, 2)), 1e-9)
        assertTrue(Geo.onLine(p(3, 4, 5), p(1, 0, 1), p(1, 2, 2)))
        assertEquals(3.0, Geo.lineLine(p(1, 3, 1), p(1, 2, 2), p(4, 1, 0), p(1, 0, -2)).distance, 1e-9)
        assertEquals(3.0, (p(6, 2, 2) - p(4, 4, 3)).length, 1e-9); assertEquals(3.0, (p(5, 6, 5) - p(4, 4, 3)).length, 1e-9)
        assertEquals(9.0, (p(2, 2, 1) cross p(4, 1, -1)).length, 1e-9); assertEquals(18.0, (p(4, 4, 2) cross p(4, 1, -1)).length, 1e-9)
        assertEquals(18.0, (p(3, 0, 0) cross p(0, 2, 0)) dot p(1, 1, 3), 1e-9)
        assertEquals(3.0, Geo.distPointPlane(p(3, 3, 3), p(1, 2, 2), 24.0), 1e-9)
        assertTrue(Geo.parallel(p(1, 1, 1) cross p(0, 0, 1), p(-1, 1, 0)))
        assertEquals("x₁ + 2x₂ − x₃ = 6", Geo.planeText(p(1, 2, -1), 6)); assertEquals("−x₂ + 3x₃ = −2", Geo.planeText(p(0, -1, 3), -2))
        // Tangentialebene 9-4: nur der richtige Normalenvektor zählt
        val l = Levels.byId("9-4")!!
        assertFalse(l.evaluate(mapOf("n" to listOf(1, 0, 0), "c" to listOf(5))).success)
        assertTrue(l.evaluate(mapOf("n" to listOf(1, 2, 2), "c" to listOf(27))).success)
        // Berührung 9-6: genau eine Lösung im Bereich
        assertEquals(listOf(24), (12..26).filter { Levels.byId("9-6")!!.evaluate(mapOf("a" to listOf(it))).success })
    }
}
