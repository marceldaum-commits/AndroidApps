package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/** Motor, Dynamo & Trafo: Formeln gegen Buchwerte, Antworten aus der Simulation (Formeln), jedes Ziel mit den Reglern erreichbar */
class EmagTest {
    private fun near(a: Double, b: Double, tol: Double = 1e-9) = assertEquals(b, a, tol)

    @Test fun konstanten() { near(Emag.MU0, 1.2566370614e-6, 1e-15) }

    @Test fun spule() {
        near(Emag.coilCurrent(3.0), 1.5)                                      // I = U ÷ R = 3 V ÷ 2 Ω
        near(Emag.coilField(100, 1.5, false), 1.884955592e-3, 1e-11)          // 1,2566·10⁻⁶ · 100 · 1,5 ÷ 0,10 = 1,885 mT
        near(Emag.coilField(250, 1.6, false), 5.026548246e-3, 1e-11)          // Beispiel der Theorie: ≈ 5,03 mT
        near(Emag.coilField(250, 1.6, true), 1.005309649, 1e-8)               // mit Eisenkern (·200) ≈ 1,0 T
        near(Emag.coilField(300, 1.5, false), 3 * Emag.coilField(100, 1.5, false), 1e-12)        // proportional zu N
        near(Emag.coilField(100, 3.0, false), 2 * Emag.coilField(100, 1.5, false), 1e-12)        // proportional zu I
        near(Emag.coilField(200, 1.0, false, 0.05), 2 * Emag.coilField(200, 1.0, false, 0.10), 1e-12)   // halbe Länge, doppeltes Feld
        near(Emag.currentForField(5.0e-3, 250, false), 1.5915494, 1e-6)
        near(Emag.coilField(100, 0.5, true) / Emag.coilField(100, 0.5, false), 200.0, 1e-9)
    }
    @Test fun kraftUndMotor() {
        near(Emag.conductorForce(0.1, 5.0, 0.04), 0.02); near(Emag.conductorForce(0.1, 5.0, 0.06), 0.03)
        near(Emag.currentForForce(0.03, 0.1, 0.06), 5.0)
        near(Emag.conductorForce(0.1, 10.0, 0.03), Emag.conductorForce(0.1, 5.0, 0.06))          // doppelter Strom, halbe Länge
        near(Emag.motorRpm(2.0), 1200.0); near(Emag.motorRpm(4.0), 2400.0); near(Emag.motorRpm(5.0), 3000.0); near(Emag.voltsForRpm(3000.0), 5.0)
    }
    @Test fun induktion() {
        near(Emag.inducedByMagnet(500, 0.5), 0.0625, 1e-12); near(Emag.inducedByMagnet(500, 1.0), 0.125, 1e-12)
        near(Emag.inducedByMagnet(500, 0.8), 0.1, 1e-12); near(Emag.inducedByMagnet(1000, 0.4), 0.1, 1e-12)
        near(Emag.induced(500, 2.5e-5, 0.125), 0.1, 1e-12); near(Emag.speedForInduced(0.1, 500), 0.8, 1e-12)
        near(Emag.inducedByMagnet(1000, 1.0), 4 * Emag.inducedByMagnet(500, 0.5), 1e-12)         // doppelte Windungen und Geschwindigkeit: viermal so groß
    }
    @Test fun generator() {
        near(Emag.generatorPeak(100, 0.1, 0.002, 10.0), 1.2566370614, 1e-9)
        near(Emag.generatorPeak(100, 0.1, 0.002, 20.0), 2.5132741229, 1e-9)
        near(Emag.effective(2.5132741229), 1.7771531753, 1e-8); near(Emag.effective(325.27), 230.0, 0.05)       // Steckdose: 230 V ≙ Û ≈ 325 V
        near(Emag.frequencyForPeak(2.5, 100, 0.1, 0.002), 19.89, 0.01)
        near(Emag.instant(2.0, 50.0, 0.005), 2.0, 1e-9)      // Viertelperiode (5 ms bei 50 Hz): Maximum
        near(Emag.instant(2.0, 50.0, 0.010), 0.0, 1e-9)      // halbe Periode: Nulldurchgang
    }
    @Test fun trafoUndLeitung() {
        near(Emag.secondaryVolts(12.0, 600, 300), 6.0); near(Emag.secondaryVolts(12.0, 600, 200), 4.0); near(Emag.windingsForVolts(12.0, 600, 4.0), 200.0)
        near(Emag.secondaryVolts(230.0, 1000, 50), 11.5)
        near(Emag.secondaryVolts(12.0, 600, 900), 18.0); near(18.0 / 6.0, 3.0); near(Emag.primaryCurrent(3.0, 600, 900), 4.5)
        near(12.0 * Emag.primaryCurrent(3.0, 600, 900), 18.0 * 3.0)                                 // P₁ = P₂ = 54 W
        near(Emag.primaryCurrent(5.0, 1000, 100), 0.5); near(23.0 * 5.0 / 230.0, 0.5)                 // 230 V → 23 V
        near(Emag.lineCurrent(2e6, 10_000.0), 200.0); near(Emag.lineLoss(2e6, 10_000.0, 5.0), 200_000.0); near(Emag.lossShare(2e6, 10_000.0, 5.0), 0.10)
        near(Emag.lineLoss(2e6, 100_000.0, 5.0), 2_000.0); near(Emag.lossShare(2e6, 100_000.0, 5.0), 0.001)
        near(Emag.lineLoss(2e6, 50_000.0, 5.0), 8_000.0); assertTrue(Emag.lossShare(2e6, 30_000.0, 5.0) > 0.01)
        near(Emag.lineLoss(2e6, 20_000.0, 5.0), Emag.lineLoss(2e6, 10_000.0, 5.0) / 4, 1e-6)       // doppelte Spannung: Verlust ein Viertel
    }

    @Test fun antworten() {
        fun ans(id: String) = Catalog.byId(id)!!.predict.let { it.choices[it.answer].text.replace(Typo.NBSP, ' ') }
        fun tr(id: String) = Catalog.byId(id)!!.transfer.let { it.choices[it.answer].text.replace(Typo.NBSP, ' ') }
        assertEquals("Sie werden angezogen", ans("y1")); assertEquals("5,7 mT", ans("y2")); assertEquals("etwa 200-mal so stark", ans("y3")); assertEquals("5 mT", ans("y4"))
        assertEquals("in die Gegenrichtung aus", ans("y5")); assertEquals("0,02 N", ans("y6")); assertEquals("2400 min⁻¹", ans("y7")); assertEquals("nichts – nur wenn sich der Magnet bewegt", ans("y8"))
        assertEquals("125 mV", ans("y9")); assertEquals("1,3 V", ans("y10")); assertEquals("6 V", ans("y11")); assertEquals("2 A", ans("y12")); assertEquals("2 kW", ans("y13"))
        assertEquals("Es wird viermal so stark", tr("y2")); assertEquals("doppelt so groß", tr("y4"))
        assertEquals("gleich groß", tr("y6")); assertEquals("viermal so groß", tr("y9")); assertEquals("11,5 V", tr("y11")); assertEquals("0,5 A", tr("y12")); assertEquals("viermal kleiner", tr("y13"))
    }

    /** Mit den Reglern der App ist jedes Ziel erreichbar – und mehr als eine falsche Einstellung führt nicht zum Ziel */
    @Test fun zieleErreichbar() {
        Topic.ELEKTROMAGNETISMUS.experiments().forEach { e ->
            when (val s = e.setup) {
                is CoilSetup -> {
                    val ns = if (s.adjustWindings) EmagUi.WINDINGS else listOf(s.windings); val us = if (s.adjustVolts) EmagUi.COIL_VOLTS else listOf(s.volts)
                    val cores = if (s.adjustCore) listOf(false, true) else listOf(s.core)
                    val hits = ns.flatMap { n -> us.flatMap { u -> cores.map { k -> Emag.coilField(n, Emag.coilCurrent(u), k) * 1000 } } }
                    s.minMilliT?.let { m -> assertTrue("${e.id} min", hits.any { it >= m }); assertTrue("${e.id}: nicht schon am Start erreicht", Emag.coilField(s.windings, Emag.coilCurrent(s.volts), s.core) * 1000 < m) }
                    s.targetMilliT?.let { t -> assertTrue("${e.id} Ziel", hits.any { EmagUi.hit(it, t) }); assertFalse("${e.id}: nicht schon am Start", EmagUi.hit(Emag.coilField(s.windings, Emag.coilCurrent(s.volts), s.core) * 1000, t)) }
                }
                is MotorSetup -> when (s.mode) {
                    MotorMode.DIRECTION -> {}
                    MotorMode.FORCE -> assertTrue(e.id, EmagUi.FORCE_AMPS.any { EmagUi.hit(Emag.conductorForce(s.teslas, it, s.lengthM), s.targetN!!) })
                    MotorMode.SPEED -> assertTrue(e.id, EmagUi.MOTOR_VOLTS.any { EmagUi.hit(Emag.motorRpm(it), s.targetRpm!!) })
                }
                is DynamoSetup -> {
                    val ns = if (s.adjustWindings) EmagUi.DYNAMO_WINDINGS else listOf(s.windings)
                    val us = if (s.mode == DynamoMode.MAGNET) ns.flatMap { n -> EmagUi.MAGNET_SPEEDS.map { v -> Emag.inducedByMagnet(n, v) } } else EmagUi.GEN_HZ.map { Emag.generatorPeak(s.windings, s.teslas, s.areaM2, it) }
                    s.minV?.let { m -> assertTrue("${e.id} min", us.any { it >= m }) }
                    s.targetV?.let { t -> assertTrue("${e.id} Ziel", us.any { EmagUi.hit(it, t) }) }
                }
                is TrafoSetup -> when (s.mode) {
                    TrafoMode.RATIO -> {
                        val u2 = EmagUi.N2_STEPS.map { Emag.secondaryVolts(s.u1, s.n1, it) }
                        s.targetU2?.let { t -> assertTrue("${e.id} U2", u2.any { EmagUi.hit(it, t) }) }
                        s.targetI2?.let { t -> assertTrue("${e.id} I2", u2.any { EmagUi.hit(it / s.loadOhm, t) }) }
                    }
                    TrafoMode.LINE -> assertTrue(e.id, EmagUi.LINE_KV.any { Emag.lossShare(s.powerW, it * 1000, s.lineOhm) <= s.maxLossShare!! } && EmagUi.LINE_KV.any { Emag.lossShare(s.powerW, it * 1000, s.lineOhm) > s.maxLossShare!! })
                }
                else -> fail("${e.id}: unerwartetes Gerät ${e.setup}")
            }
        }
    }

    @Test fun inhalt() {
        val l = Topic.ELEKTROMAGNETISMUS.experiments()
        assertEquals(13, l.size); assertEquals((1..13).map { "y$it" }.toSet(), l.map { it.id }.toSet())
        assertTrue(l.all { it.island == Island.STROM && it.setup.lab.island == Island.STROM }); assertEquals(setOf(Lab.SPULE, Lab.MOTOR, Lab.DYNAMO, Lab.TRAFO), l.map { it.setup.lab }.toSet())
        assertEquals(5, l.minOf { it.grades.first }); assertEquals(10, l.maxOf { it.grades.last })
        assertTrue("Thema steht nicht mehr unter „Kommt bald“", Coming.all.none { it.title.startsWith("Motor, Dynamo") })
        assertEquals("elektromagnetismus", Links.name(Topic.ELEKTROMAGNETISMUS)); assertFalse("elektromagnetismus" in Links.PLANNED)
        assertEquals(Links.Target(Subject.PHYSIK, Topic.ELEKTROMAGNETISMUS), Links.target("https://lernen.mathenachhilfe-online.com/experimentopia/#thema=elektromagnetismus"))
        val card = Theory.of(Topic.ELEKTROMAGNETISMUS)
        listOf("B = μ₀ · N · I ÷ l", "F = B · I · l", "U = N · ΔΦ ÷ Δt", "Û = N · B · A · 2π · f", "U₁ ÷ U₂ = N₁ ÷ N₂", "P_V = I² · R").forEach { f -> assertTrue(f, card.pro.any { it.text.contains(f) }) }
        assertTrue("Beispiel zu jeder Formel", card.pro.all { it.text.contains("Beispiel") })
    }

    /** Alle Zahlen der Theorie-Beispiele stimmen mit der Simulation überein */
    @Test fun theorieBeispiele() {
        val t = Theory.of(Topic.ELEKTROMAGNETISMUS).pro.joinToString(" ") { it.text }
        assertTrue(t.contains("≈ 5,0 mT")); near(Emag.coilField(250, 1.6, false) * 1000, 5.03, 0.005)
        assertTrue(t.contains("F = 0,1 · 5 · 0,06 = 0,03 N")); near(Emag.conductorForce(0.1, 5.0, 0.06), 0.03)
        assertTrue(t.contains("= 0,10 V")); near(Emag.induced(500, 2.5e-5, 0.125), 0.10)
        assertTrue(t.contains("≈ 2,51 V") && t.contains("≈ 1,78 V")); near(Emag.generatorPeak(100, 0.1, 0.002, 20.0), 2.51, 0.005); near(Emag.effective(Emag.generatorPeak(100, 0.1, 0.002, 20.0)), 1.78, 0.005)
        assertTrue(t.contains("U₂ = 12 · 900 ÷ 600 = 18 V")); near(Emag.secondaryVolts(12.0, 600, 900), 18.0)
        assertTrue(t.contains("200 kW (10 %)") && t.contains("2 kW (0,1 %)"))
    }
}
