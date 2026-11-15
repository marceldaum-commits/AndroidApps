package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ModesTest {
    private fun solvable(l: Level) { assertTrue(l.id, l.evaluate(l.solution).success); assertFalse(l.id, l.evaluate(l.start).success)
        l.controls.forEach { c -> listOf(l.solution.getValue(c.key), c.start).forEach { v -> v.forEachIndexed { i, x -> assertTrue("${l.id}/${c.key}", x in c.min[i]..c.max[i]) } } }
        Phase.entries.forEach { ph -> val o = l.objects(if (ph == Phase.DONE) l.solution else l.start, ph); assertEquals(l.id, o.size, o.map { it.id }.toSet().size) } }

    @Test fun examsAreSolvableAndScored() {
        assertEquals(3, Exams.all.size)
        Exams.all.forEach { e -> e.parts.forEach { solvable(it.level) }; assertEquals(12, e.maxPoints); assertEquals(listOf("a", "b", "c", "d"), e.parts.map { it.label }) }
        val part = Exams.all[0].parts[1]                       // 4 BE
        assertEquals(4, Exam.points(part, 3)); assertEquals(2, Exam.points(part, 2)); assertEquals(1, Exam.points(part, 1))
        assertTrue(Exam.verdict(11, 12).startsWith("Stark")); assertTrue(Exam.verdict(4, 12).startsWith("Dranbleiben"))
    }

    @Test fun examMathIsIndependentlyCorrect() {
        fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
        listOf(p(1, 1, 0), p(5, 1, 0), p(3, 3, 3)).forEach { assertTrue(Geo.onPlane(it, p(0, 3, -2), 3.0)) }
        assertTrue(Geo.mirrorAtPlane(p(1, 2, 2), p(1, 2, 2), 18.0).near(p(3, 6, 6)))
        assertEquals(3.0, Geo.distPointPlane(p(1, 2, 2), p(1, 2, 2), 18.0), 1e-9)
        // engste Stelle der Drohnen: bei t = 2 kleiner als bei allen anderen ganzen t
        fun d(t: Int) = ((p(0, 2, 4) + p(2, 0, -1) * t) - (p(6, 2, 1) + p(0, 1, 1) * t)).length
        assertEquals(3.0, d(2), 1e-9); assertTrue((0..4).all { it == 2 || d(it) > d(2) })
        // Treffpunkt 11-4 und Rotationsvolumen 12-x (numerisch nachintegriert)
        assertTrue((p(0, 0, 2) + p(2, 1, 0) * 3).near(p(6, 6, 5) + p(0, -1, -1) * 3))
        fun vol(a: Double, b: Double, f: (Double) -> Double) = (0 until 20000).sumOf { val x = a + (b - a) * (it + 0.5) / 20000; f(x) * f(x) } * (b - a) / 20000
        assertEquals(16.0, vol(0.0, 4.0) { 2.0 }, 1e-3); assertEquals(9.0, vol(0.0, 3.0) { it }, 1e-3); assertEquals(8.0, vol(0.0, 4.0) { kotlin.math.sqrt(it) }, 1e-3)
        assertEquals(36.0, vol(-3.0, 3.0) { kotlin.math.sqrt(9 - it * it) }, 1e-3); assertEquals(12.0, vol(0.0, 4.0) { kotlin.math.sqrt(it + 1) }, 1e-3)
    }

    @Test fun lageRoundsHaveTheClaimedRelation() {
        val seen = HashSet<Relation>()
        for (seed in 0L until 400) { val r = LageCheck.round(seed); assertEquals(r.relation, Geo.lineLine(r.a, r.d1, r.b, r.d2).relation); seen += r.relation
            assertTrue(r.scene(true).contains("\"id\":\"g\"")); assertFalse(r.scene(false).contains("\"id\":\"g\"")); assertTrue(r.explanation.isNotBlank()) }
        assertEquals(4, seen.size); assertEquals(4, LageCheck.names.size)
    }

    @Test fun duelCodeRoundTripAndTypoDetection() {
        repeat(200) { i -> val d = DuelCode.create(JamTopic.entries[i % JamTopic.entries.size], Random(i)); assertEquals(d, DuelCode.find("Schau mal: ${d.code} 😀")); assertEquals(9, d.code.length) }
        val d = Duel(JamTopic.MIX, 12345)
        assertEquals(d.level(0).scene(d.level(0).start, Phase.AIM), DuelCode.find(d.code.lowercase())!!.level(0).let { it.scene(it.start, Phase.AIM) })
        val typo = d.code.dropLast(1) + (if (d.code.last() == 'A') 'B' else 'A')
        assertNull(DuelCode.find(typo)); assertNull(DuelCode.find("kein Code"))
        assertTrue(DuelCode.shareText(d, 4).contains(d.code))
    }

    @Test fun showCodeRoundTrip() {
        assertEquals(Show.demo, Show.decode("Meine Show: " + Show.encode(Show.demo)))
        assertNull(Show.decode("RLS-")); assertNull(Show.decode("RLS-Lzzzzzz")); assertNull(Show.decode("RLS-L00"))
        assertTrue(Show.scene(Show.demo).contains("\"type\":\"sphere\""))
        val l = ShowItem(ShowType.LASER, ShowType.LASER.start)
        assertEquals(4, l.set(3, 99).v[3]); assertNull(l.set(3, 0).set(4, 0).set(5, 0).obj(0)); assertNotNull(l.beam(0))
        assertEquals("x₁ + 2x₂ = 6", ShowItem(ShowType.PLANE, listOf(1, 2, 0, 6)).text)
    }

    @Test fun skinsAndTraining() {
        assertEquals(1, Skins.unlocked(0).size); assertEquals(Skins.all.size, Skins.unlocked(999).size)
        assertEquals(listOf("1-2", "1-3"), Training.levels(mapOf("1-1" to 3, "1-2" to 2, "1-3" to 1, "tag-5" to 1)).map { it.id })
    }
}
