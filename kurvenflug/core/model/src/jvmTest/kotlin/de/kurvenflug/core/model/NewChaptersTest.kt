package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class NewChaptersTest {
    private fun wins(l: Level): Int {
        val grids = l.params.map { p -> generateSequence(p.min) { it + p.step }.takeWhile { it <= p.max + 1e-9 }.toList() }
        var combos = listOf(emptyMap<String, Double>())
        l.params.forEachIndexed { i, p -> combos = combos.flatMap { m -> grids[i].map { m + (p.key to it) } } }
        return combos.count { Flight.fly(l, it).success }
    }
    private fun ch(n: Int) = Levels.chapters.first { it.number == n }

    @Test fun fractionsWinkelAndZerosNeedExactAnswers() {
        ch(15).levels.forEach { assertEquals(it.id, 1, wins(it)) }      // Zahlenstrahl: genau eine Stelle
        ch(16).levels.forEach { assertTrue(it.id, wins(it) in 1..3) }   // Winkel: ±1° Ablesegenauigkeit
        ch(18).levels.forEach { assertEquals(it.id, 1, wins(it)) }      // Nullstellen: eindeutig
    }

    @Test fun refractionLevelsAreSolvableButNotByAimingStraight() {
        for (l in ch(17).levels) {
            assertTrue(l.id, Flight.fly(l, l.solution).success)
            val t = l.targets[0]
            val straight = Math.toDegrees(atan2(t.y - l.launch.y, t.x - l.launch.x)).roundToInt().toDouble()
            assertFalse("${l.id}: direkt zielen darf nicht reichen", Flight.fly(l, mapOf("angle" to straight)).success)
        }
    }

    /** Snellius: Durch eine Glasplatte läuft der Strahl parallel versetzt weiter; im Glas gilt sin α₁ = n · sin α₂. */
    @Test fun lightThroughASlabFollowsSnell() {
        val l = ch(17).levels[0] // Platte x −3…3, n = 1,5, Start (−9 | −4)
        val p = Flight.path(l, mapOf("angle" to 30.0))
        fun dirAt(x: Double): Double { val i = p.indexOfFirst { it.x >= x }; return Math.toDegrees(atan2(p[i + 1].y - p[i].y, p[i + 1].x - p[i].x)) }
        val inGlass = dirAt(0.0); val after = dirAt(6.0)
        assertEquals(30.0, after, 0.5)                                            // parallel versetzt
        assertEquals(sin(Math.toRadians(30.0)), 1.5 * sin(Math.toRadians(inGlass)), 0.01) // zum Lot hin gebrochen
        // ohne Platte läge der Strahl bei x = 6 höher als mit Platte
        assertTrue(p.first { it.x >= 6.0 }.y < -4 + 15 * tan(Math.toRadians(30.0)))
    }

    @Test fun steepLightInsideGlassIsTotallyReflected() {
        val l = ch(17).levels[2] // im Glas, Oberkante y = −2, Grenzwinkel ≈ 41,8° zum Lot
        val flat = Flight.path(l, mapOf("angle" to 40.0))   // 50° zum Lot → Totalreflexion, bleibt unter der Kante
        assertTrue(flat.all { it.y <= -2.0 + 1e-6 })
        val steep = Flight.path(l, mapOf("angle" to 70.0))  // 20° zum Lot → tritt aus
        assertTrue(steep.any { it.y > -1.0 })
    }

    @Test fun newChaptersHaveWalkthroughsWhereCalculable() {
        assertEquals(6, ch(18).levels.count { Walkthrough.of(it) != null })
        assertNotNull(Walkthrough.of(Levels.byId("16-6")!!))
    }
}
