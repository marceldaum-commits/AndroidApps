package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class FlightTest {
    private val line = Levels.byId("2-6")!!
    private val throwEarth = Levels.byId("4-1")!!

    @Test fun lineFollowsFormula() {
        val v = mapOf("m" to 0.5, "b" to 1.0)
        assertEquals(3.0, Flight.f(line, v, 4.0), 1e-9)
        Flight.path(line, v).forEach { assertEquals(0.5 * it.x + 1, it.y, 1e-9) }
    }

    @Test fun steepLineEntersAndLeavesTheScreen() {
        val p = Flight.path(line, mapOf("m" to 4.0, "b" to 0.0))
        assertTrue(p.first().x > -2.6 && p.last().x < 2.6)
        assertTrue(p.all { kotlin.math.abs(it.y) <= Flight.R })
    }

    @Test fun throwRangeMatchesPhysics() {
        // w = v² · sin(2α) / g: 10 m/s bei 45° auf der Erde → 10,19 m
        val p = Flight.path(throwEarth, mapOf("v" to 10.0))
        val landing = p.first { it.x > throwEarth.launch.x + 1 && it.y <= throwEarth.launch.y }
        assertEquals(throwEarth.launch.x + 100 / 9.81, landing.x, 0.05)
    }

    @Test fun moonThrowsAreSixTimesFarther() {
        val l = Levels.byId("4-2")!!
        val earth = l.copy(planet = Planet.EARTH)
        fun range(lv: Level) = Flight.path(lv, mapOf("v" to 3.0)).first { it.x > lv.launch.x + 0.1 && it.y <= lv.launch.y }.x - lv.launch.x
        assertEquals(9.81 / 1.62, range(l) / range(earth), 0.1)
    }

    @Test fun asteroidStopsTheProbe() {
        val l = Levels.byId("2-1")!!
        val r = Flight.fly(l, mapOf("b" to 0.0))
        assertEquals(0, r.crash)
        assertFalse(r.success)
        assertTrue(r.path.last().x < -2.5)
    }

    @Test fun probeDocksWhenAllStationsReached() {
        val r = Flight.fly(line, line.solution)
        assertEquals(4.0, r.path.last().x, 0.8)
    }

    @Test fun pointFlightGoesAlongXFirst() {
        val l = Levels.byId("1-1")!!
        val p = Flight.path(l, mapOf("x" to 4.0, "y" to 3.0))
        assertEquals(Vec(4.0, 0.0).x, p.first { it.y > 0.01 }.x, 1e-9)
        assertEquals(4.0, p.last().x, 1e-9); assertEquals(3.0, p.last().y, 1e-9)
    }

    @Test fun starsRewardFirstTryAndCapHelp() {
        assertEquals(3, Flight.stars(1, false))
        assertEquals(2, Flight.stars(2, false))
        assertEquals(1, Flight.stars(9, false))
        assertEquals(2, Flight.stars(1, true))
    }

    @Test fun snapAvoidsFloatNoise() {
        val p = Param("m", "m", -5.0, 5.0, 0.5, 0.0)
        assertEquals(0.5, p.snap(0.1 + 0.2 + 0.2), 0.0)
        assertEquals(5.0, p.snap(9.0), 0.0)
        assertEquals(0.3, Param("a", "a", -3.0, 3.0, 0.1, 0.0).snap(0.1 + 0.2), 0.0)
    }

    @Test fun numbersAreGerman() {
        assertEquals("0,5", num(0.5))
        assertEquals("−3", num(-3.0))
        assertEquals("10,2", num(10.19, 1))
    }

    @Test fun feedbackGivesConcreteNumbers() {
        val v = mapOf("m" to 0.5, "b" to 0.0)
        val fb = Feedback.after(line, v, Flight.fly(line, v))
        assertTrue(fb, fb.contains("x = −4") && fb.contains("höher"))
        val short = Feedback.after(throwEarth, mapOf("v" to 8.0), Flight.fly(throwEarth, mapOf("v" to 8.0)))
        assertTrue(short, short.contains("zu kurz"))
        val p = Levels.byId("1-3")!!
        assertTrue(Feedback.after(p, mapOf("x" to 3.0, "y" to 6.0), Flight.fly(p, mapOf("x" to 3.0, "y" to 6.0))).contains("x stimmt"))
    }

    @Test fun onlyKurvenflugReleasesCount() {
        val json = """[{"tag_name":"quanta-v5.0.0","assets":[{"name":"Q.apk","browser_download_url":"x"}]},{"tag_name":"wissensgarten-v9.0.0","assets":[{"name":"W.apk","browser_download_url":"u"}]},
            {"tag_name":"kurvenflug-v0.2.0","body":"neu","assets":[{"name":"Kurvenflug-0.2.0.apk","browser_download_url":"q","size":5}]}]"""
        assertEquals("0.2.0", Updates.latest(json)!!.version)
    }

    @Test fun sineAndExpFollowTheirFormulas() {
        val sine = Levels.byId("5-4")!!
        assertEquals(3 * kotlin.math.sin(2 * 1.0) - 1, Flight.f(sine, mapOf("a" to 3.0, "b" to 2.0, "d" to -1.0), 1.0), 1e-9)
        val exp = Levels.byId("5-6")!!
        assertEquals(27.0, Flight.f(exp, mapOf("b" to 3.0), 3.0), 1e-9)
    }

    @Test fun gravityBendsTowardsThePlanet() {
        val l = Levels.byId("6-1")!!
        // Ohne Planet flöge die Sonde bei 0° geradeaus auf y = 0; mit Planet stürzt sie hinein
        assertNotNull(Flight.fly(l, mapOf("angle" to 0.0)).crash)
        // Leicht nach oben gezielt: die Bahn wird zum Planeten hin (nach unten) gebogen
        val p = Flight.path(l, mapOf("angle" to 20.0))
        val straightY = l.launch.y + kotlin.math.tan(Math.toRadians(20.0)) * (0.0 - l.launch.x)
        assertTrue(p.first { it.x >= 0 }.y < straightY - 0.5)
    }

    @Test fun planetCrashHasOwnMessage() {
        val l = Levels.byId("6-1")!!
        val r = Flight.fly(l, mapOf("angle" to 0.0))
        assertTrue(Feedback.after(l, mapOf("angle" to 0.0), r).contains("Planet"))
    }

    @Test fun rampDistanceFollowsEnergyAndIgnoresGravity() {
        val earth = Levels.byId("12-1")!!
        for (pl in Planet.entries) {
            val l = earth.copy(planet = pl)
            val land = Flight.path(l, mapOf("h" to 8.0)).last { it.y >= l.ground - 0.01 }
            assertEquals("${pl.label}", Flight.RAMP_EDGE_X + 2 * kotlin.math.sqrt((8.0 - 4.0) * 4.0), land.x, 0.1)
        }
        // Unter der Kante gestartet: kommt nicht drüber
        assertFalse(Flight.fly(earth, mapOf("h" to 3.0)).success)
    }

    @Test fun laserReflectsWithEqualAngles() {
        val l = Levels.byId("11-1")!!   // Spiegel waagerecht bei y = −6
        val p = Flight.path(l, mapOf("angle" to -45.0))
        val bounce = p.minByOrNull { it.y }!!
        assertEquals(-6.0, bounce.y, 0.05)
        val after = p[p.indexOf(bounce) + 50]
        // nach dem Abprallen wieder 45° nach oben
        assertEquals(1.0, (after.y - bounce.y) / (after.x - bounce.x), 0.02)
    }

    @Test fun radiusWaveTouchingAJammerFails() {
        val l = Levels.byId("9-3")!!
        assertTrue(Flight.fly(l, mapOf("r" to 7.1)).success)
        assertNotNull(Flight.fly(l, mapOf("r" to 7.6)).crash)
    }

    /** Raten soll nicht reichen: Eine steile Kurve, die den Stationskreis nur seitlich streift, zählt nicht. */
    @Test fun steepCurveGrazingTheStationDoesNotCount() {
        val l = Levels.byId("3-1")!! // y = x² + e durch (2 | 1), Lösung e = −3
        assertTrue(Flight.fly(l, mapOf("e" to -3.0)).success)
        for (e in listOf(-6.0, -5.0, -4.0, -2.0, -1.0)) assertFalse("e = $e", Flight.fly(l, mapOf("e" to e)).success)
    }

    /** Für jedes Funktions-Level treffen nur wenige Reglerstellungen – wer rechnet, trifft; wer rät, kaum. */
    @Test fun oneParameterFunctionLevelsHaveNarrowWindows() {
        Levels.chapters.flatMap { it.levels }.filter { it.kind in Flight.functionKinds && it.params.size == 1 }.forEach { l ->
            val p = l.params.single()
            val wins = generateSequence(p.min) { it + p.step }.takeWhile { it <= p.max + 1e-9 }
                .count { Flight.fly(l, mapOf(p.key to it)).success }
            assertTrue("${l.id}: $wins Stellungen treffen", wins in 1..4)
        }
    }

    /** Laser-Zielhilfe: Die Vorschau endet genau am ersten Spiegel. */
    @Test fun laserFirstLegEndsAtFirstMirror() {
        val l = Levels.byId("11-1")!! // Boden-Spiegel von (−6 | −6) bis (3 | −6), Abschuss bei (−9 | 0)
        val path = Flight.path(l, mapOf("angle" to -45.0))
        val leg = Flight.firstLeg(path)
        assertTrue(leg.size in 2 until path.size)
        assertEquals(-6.0, leg.last().y, 0.1)
        assertEquals(-3.0, leg.last().x, 0.1)
        // ohne Spiegel im Weg: die ganze Bahn ist gerade
        val straight = Flight.path(l, mapOf("angle" to 30.0))
        assertEquals(straight.size, Flight.firstLeg(straight).size)
    }
}
