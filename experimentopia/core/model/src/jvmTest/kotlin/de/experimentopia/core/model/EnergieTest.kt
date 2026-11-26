package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/** Energie-Achterbahn: Formeln gegen Buchwerte, Antworten aus dem Modell, jedes Ziel mit den Reglern erreichbar */
class EnergieTest {
    private fun close(e: Double, a: Double, tol: Double = 1e-6) = assertEquals(e, a, tol)

    @Test fun lageUndBewegungsenergie() {
        close(78_480.0, Energie.potential(400.0, 20.0))                    // 400 · 9,81 · 20
        close(80_000.0, Energie.kinetic(400.0, 20.0))                      // ½ · 400 · 20²
    }

    @Test fun tempoUntenUnabhaengigVonMasse() {
        val v = Energie.speed(20.0)
        close(sqrt(2 * 9.81 * 20), v)
        assertEquals(19.8, v, 0.05); assertEquals(71.3, Energie.toKmh(v), 0.1)
        // Energiebilanz mit zwei Massen: Lageenergie oben = Bewegungsenergie unten
        for (m in listOf(200.0, 800.0)) close(Energie.potential(m, 20.0), Energie.kinetic(m, v), 1e-6)
    }

    @Test fun hoeheFuerTempo() {
        assertEquals(20.39, Energie.heightForSpeed(20.0), 0.01)             // 72 km/h
        assertEquals(11.47, Energie.heightForSpeed(15.0), 0.01)             // 54 km/h
        assertEquals(5.10, Energie.heightForSpeed(10.0), 0.01)
        assertEquals(43.7, Energie.heightForSpeed(Energie.toMs(100.0), 0.1), 0.05)
    }

    @Test fun looping() {
        close(12.5, Energie.loopMinHeight())
        close(15.625, Energie.loopMinHeight(friction = 0.2))
        assertTrue(Energie.passesLoop(13.0)); assertFalse(Energie.passesLoop(12.0))
        assertTrue(Energie.passesLoop(16.0, friction = 0.2)); assertFalse(Energie.passesLoop(15.0, friction = 0.2))
        // Herleitung: oben v² = 2 g (h − 2R) ≥ g R genau bei h = 2,5 R
        val vTop2 = 2 * Energie.G * (12.5 - 2 * 5.0); assertEquals(Energie.G * 5.0, vTop2, 1e-9)
    }

    @Test fun huegel() {
        close(11.111, Energie.hillMinHeight(friction = 0.1), 1e-3)
        assertTrue(Energie.passesHill(12.0, friction = 0.1)); assertFalse(Energie.passesHill(11.0, friction = 0.1))
    }

    @Test fun anteil() {
        close(0.5, Energie.kineticShare(30.0, 15.0)); close(0.75, Energie.kineticShare(40.0, 10.0)); close(0.0, Energie.kineticShare(30.0, 30.0))
    }

    @Test fun jedesZielErreichbar() {
        val exps = Topic.ENERGIE.experiments()
        assertEquals(8, exps.size)
        for (e in exps) {
            val s = e.setup as CoasterSetup
            val ok = when (s.mode) {
                CoasterMode.ENERGY -> CoasterUi.HEIGHTS.any { Energie.potential(s.massKg, it) / 1000 >= s.minEnergyKJ!! }
                CoasterMode.SPEED -> CoasterUi.HEIGHTS.any { CoasterUi.hit(Energie.toKmh(Energie.speed(it, s.frictionPct / 100.0)), s.targetKmh!!) }
                CoasterMode.LOOP -> CoasterUi.HEIGHTS.any { it <= s.maxStartM!! && Energie.passesLoop(it, friction = s.frictionPct / 100.0) }
                CoasterMode.HILL -> CoasterUi.HEIGHTS.any { it <= s.maxStartM!! && Energie.passesHill(it, friction = s.frictionPct / 100.0) }
                CoasterMode.SPLIT -> (0..s.startM.toInt()).any { abs(Energie.kineticShare(s.startM, it.toDouble()) - 0.5) <= CoasterUi.SPLIT_TOLERANCE }
            }
            assertTrue("${e.id}: Ziel nicht erreichbar", ok)
            // Das Ziel ist am Anfang noch nicht erreicht (sonst gäbe es nichts zu tun)
            val atStart = when (s.mode) {
                CoasterMode.ENERGY -> Energie.potential(s.massKg, s.startM) / 1000 >= s.minEnergyKJ!!
                CoasterMode.SPEED -> CoasterUi.hit(Energie.toKmh(Energie.speed(s.startM, s.frictionPct / 100.0)), s.targetKmh!!)
                CoasterMode.LOOP -> Energie.passesLoop(s.startM, friction = s.frictionPct / 100.0)
                CoasterMode.HILL -> Energie.passesHill(s.startM, friction = s.frictionPct / 100.0)
                CoasterMode.SPLIT -> false
            }
            assertFalse("${e.id}: Ziel schon zu Beginn erreicht", atStart)
        }
    }

    @Test fun antwortenStimmen() {
        fun answer(id: String) = Catalog.byId(id)!!.let { it.predict.choices[it.predict.answer].text }.replace(Regex("[\\s\u00A0\u202F]+"), " ")
        assertEquals("78 kJ", answer("z1"))
        assertEquals("71 km/h", answer("z2"))
        assertEquals("12,5 m", answer("z4"))
        assertEquals("15,6 m", answer("z5"))
        assertEquals("11,1 m", answer("z6"))
        assertEquals("15 m", answer("z7"))
        assertEquals("100 km/h", answer("z8"))
        // Das Tempo in z8: v = √(2 · 9,81 · 45 · 0,9) ≈ 28,2 m/s ≈ 101,5 km/h → nächste Auswahl 100 km/h
        assertEquals(101.5, Energie.toKmh(Energie.speed(45.0, 0.1)), 0.1)
    }

    @Test fun theorieBeispieleStimmen() {
        val t = Theory.of(Topic.ENERGIE).pro.joinToString(" ") { it.text }
        assertTrue(t.contains("78 480")); assertTrue(t.contains("12,5 m")); assertTrue(t.contains("15,6 m")); assertTrue(t.contains("11,1 m"))
        close(78_480.0, Energie.potential(400.0, 20.0)); close(12.5, 2.5 * 5.0); close(15.625, 12.5 / 0.8)
    }
}
