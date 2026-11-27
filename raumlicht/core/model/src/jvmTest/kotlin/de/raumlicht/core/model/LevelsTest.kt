package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

/** Jedes Level wird mit seiner Lösung durchgespielt; die Mathematik wird zusätzlich unabhängig nachgerechnet. */
class LevelsTest {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)

    @Test fun idsAreUniqueAndOrdered() {
        assertEquals(Levels.all.size, Levels.all.map { it.id }.toSet().size)
        Levels.chapters.forEach { c -> c.levels.forEachIndexed { i, l -> assertEquals("${c.number}-${i + 1}", l.id); assertEquals(c.number, l.chapter) } }
        assertEquals(107, Levels.all.size)
    }

    @Test fun everyLevelIsSolvableWithItsSolution() = Levels.all.forEach { l ->
        val o = l.evaluate(l.solution)
        assertTrue("${l.id}: Lösung wird nicht anerkannt (${o.message})", o.success)
    }

    @Test fun startValuesNeverSolve() = Levels.all.forEach { l ->
        assertFalse("${l.id}: Startwerte lösen das Level", l.evaluate(l.start).success)
    }

    @Test fun solutionAndStartAreWithinControlRanges() = Levels.all.forEach { l ->
        l.controls.forEach { c ->
            listOf(l.solution.getValue(c.key), c.start).forEach { v ->
                v.forEachIndexed { i, x -> assertTrue("${l.id}/${c.key}[$i] = $x außerhalb ${c.min[i]}..${c.max[i]}", x in c.min[i]..c.max[i]) }
            }
        }
        assertEquals(l.controls.map { it.key }.toSet(), l.solution.keys)
    }

    @Test fun setClampsToRange() {
        val l = Levels.byId("1-1")!!
        assertEquals(listOf(8, 0, 6), l.set(l.start, "P", listOf(99, -5, 6)).getValue("P"))
    }

    @Test fun everyLevelHasTipWayAndMission() = Levels.all.forEach { l ->
        assertTrue(l.id, l.tip.isNotBlank() && l.way.isNotEmpty() && l.mission.isNotBlank() && l.title.isNotBlank())
        assertTrue("${l.id}: Formel leer", l.formula(l.start).isNotEmpty())
    }

    @Test fun scenesSerializeInBothPhases() = Levels.all.forEach { l ->
        for (ph in Phase.entries) {
            val objs = l.objects(if (ph == Phase.DONE) l.solution else l.start, ph)
            assertEquals("${l.id}: doppelte Objekt-IDs", objs.size, objs.map { it.id }.toSet().size)
            assertTrue(l.scene(l.start, ph).startsWith("{\"bounds\""))
            assertEquals("${l.id}: Lux fehlt", 1, objs.count { it.type == "drone" })
        }
    }

    @Test fun everythingVisibleStaysInsideTheRoom() = Levels.all.forEach { l ->
        l.objects(l.solution, Phase.DONE).forEach { o ->
            o.p?.takeIf { o.type == "point" || o.type == "drone" }?.let { q ->
                q.forEachIndexed { i, x -> assertTrue("${l.id}/${o.id}: $q außerhalb", x >= l.bounds.min[i] - 1e-9 && x <= l.bounds.max[i] + 1e-9) }
            }
        }
    }

    @Test fun wrongAnswersGetSpecificFeedback() {
        val l = Levels.byId("1-2")!!
        assertEquals("x₁ und x₃ stimmen ✓ – x₂ noch nicht.", l.evaluate(mapOf("P" to listOf(2, 4, 3))).message)
        val aim = Levels.byId("3-1")!!
        assertTrue(aim.evaluate(mapOf("d" to listOf(-1, -2, -1))).message.contains("rückwärts"))
        assertTrue(aim.evaluate(mapOf("d" to listOf(0, 0, 0))).message.contains("Nullvektor"))
        assertTrue(aim.evaluate(mapOf("d" to listOf(1, 2, 1))).success)      // gekürzte Richtung trifft auch
        val cross = Levels.byId("4-1")!!
        assertEquals("Dein Punkt liegt auf g ✓, aber noch nicht auf h ✗.", cross.evaluate(mapOf("P" to listOf(3, 2, 2))).message)
    }

    @Test fun mathIsIndependentlyCorrect() {
        fun sol(id: String, key: String = "P") = V3.of(Levels.byId(id)!!.solution.getValue(key))
        // Mittelpunkt, Parallelogramm, Schwerpunkt
        assertTrue(sol("1-4").near((p(2, 0, 4) + p(6, 4, 0)) * 0.5))
        assertTrue(sol("2-5").near(p(1, 1, 1) + (p(6, 6, 3) - p(5, 2, 1))))
        assertTrue(sol("2-6").near((p(1, 2, 0) + p(7, 1, 3) + p(4, 6, 3)) * (1 / 3.0)))
        // Spurpunkt, Schnittpunkte, Durchstoßpunkte: liegen auf allen beteiligten Objekten
        assertTrue(Geo.onLine(sol("3-6"), p(2, 1, 6), p(1, 2, -2)) && sol("3-6").z == 0.0)
        assertTrue(Geo.onLine(sol("4-2"), p(0, 6, 1), p(2, -1, 1)) && Geo.onLine(sol("4-2"), p(7, 1, 0), p(-1, 1, 1)))
        assertTrue(Geo.onLine(sol("4-5"), p(0, 0, 2), p(3, 4, 1)) && Geo.onLine(sol("4-5"), p(7, 0, 1), p(-2, 2, 1)))
        assertTrue(Geo.onLine(sol("5-2"), p(1, 1, 1), p(1, 2, 0)) && Geo.onPlane(sol("5-2"), p(1, 1, 1), 9.0))
        assertTrue(Geo.onLine(sol("5-6"), p(2, 2, 5), p(1, 2, -1)) && sol("5-6").y == 6.0)
        // Ebenen durch drei Punkte
        for ((id, pts) in listOf("5-3" to listOf(p(6, 0, 0), p(0, 3, 0), p(0, 0, 2)), "5-5" to listOf(p(3, 2, 0), p(1, 2, 2), p(0, 4, 2)))) {
            val n = sol(id, "n"); val c = Levels.byId(id)!!.solution.getValue("c")[0].toDouble()
            pts.forEach { assertTrue(id, Geo.onPlane(it, n, c)) }
            assertTrue(Geo.parallel(n, (pts[1] - pts[0]) cross (pts[2] - pts[0])))
        }
        // Normalenvektor steht senkrecht auf beiden Spannvektoren
        assertEquals(0.0, sol("5-4", "d") dot p(2, 0, 1), 0.0); assertEquals(0.0, sol("5-4", "d") dot p(0, 2, 1), 0.0)
        // Lotfußpunkt, Spiegelpunkt, Abstand
        assertTrue(sol("6-2").near(Geo.footOnPlane(p(4, 4, 4), p(1, 1, 1), 6.0)))
        assertTrue(sol("6-3").near(Geo.mirrorAtPlane(p(2, 2, 3), p(1, 1, 0), 8.0)))
        assertEquals(Levels.byId("6-4")!!.solution.getValue("a")[0].toDouble(), Geo.distPointPlane(p(1, 2, 1), p(1, 2, 2), 16.0), 1e-9)
        // Über Bande: Einfallswinkel = Ausfallswinkel (gespiegelte Richtung zeigt zum Ziel)
        val s = sol("6-5"); val d = s - p(4, 1, 1)
        assertTrue(Geo.sameDirection(V3(-d.x, d.y, d.z), p(2, 7, 4) - s))
    }

    @Test fun skewLevelHasExactlyOneSolution() {
        val l = Levels.byId("4-4")!!
        assertEquals(listOf(1), (-3..3).filter { l.evaluate(mapOf("c" to listOf(it))).success })
        assertTrue(l.evaluate(l.start).message.contains("verfehlen sich um"))
    }

    @Test fun mirrorShotReflectsOnMiss() {
        val l = Levels.byId("6-5")!!
        assertEquals(2, l.evaluate(l.solution).beams.size)
        assertEquals(2, l.evaluate(mapOf("P" to listOf(0, 3, 2))).beams.size)   // auf dem Spiegel, falscher Punkt: wird reflektiert
        assertEquals(1, l.evaluate(mapOf("P" to listOf(2, 3, 2))).beams.size)   // gar nicht auf dem Spiegel
    }

    @Test fun stars() {
        assertEquals(3, Level.stars(1, false)); assertEquals(2, Level.stars(1, true)); assertEquals(2, Level.stars(2, false)); assertEquals(1, Level.stars(5, true))
    }
}
