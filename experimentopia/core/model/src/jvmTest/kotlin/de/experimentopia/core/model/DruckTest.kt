package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test

/** Druck & Luftdruck: Formeln gegen Buchwerte, Antworten aus dem Modell, jedes Ziel mit den Reglern erreichbar */
class DruckTest {
    @Test fun nagelbrett() {
        assertEquals(686.7, Druck.weight(70.0), 1e-9)
        assertEquals(190_750.0, Druck.nailPressure(900), 1.0)                  // 686,7 ÷ 0,0036
        assertEquals(343_350.0, Druck.nailPressure(500), 1.0)
        assertEquals(430, Druck.nailsFor(400_000.0)); assertEquals(859, Druck.nailsFor(200_000.0))
        assertEquals(Druck.nailPressure(100) / 10, Druck.nailPressure(1000), 1e-6)   // zehnmal so viele Nägel = ein Zehntel des Drucks
    }

    @Test fun wasserdruck() {
        assertEquals(98_100.0, Druck.waterColumn(10.0), 1e-9)
        assertEquals(199_425.0, Druck.total(10.0), 1e-9)
        assertEquals(20.25, Druck.depthFor(300_000.0), 0.01)
        assertEquals(50.8, Druck.depthFor(600_000.0), 0.05)
        assertEquals(30.4, Druck.depthFor(400_000.0), 0.05)
    }

    @Test fun hydraulik() {
        assertEquals(10_000.0, Druck.hydraulicForce(200.0, 0.002, 0.10), 1e-6)
        assertEquals(5_000.0, Druck.hydraulicForce(100.0, 0.001, 0.05), 1e-6)
        assertEquals(0.1177, Druck.pistonAreaFor(Druck.weight(Druck.CAR_KG), 200.0, 0.002), 1e-4)
        assertEquals(510.0, Druck.hydraulicForce(100.0, 0.001, 0.05) / Druck.G, 0.5)
    }

    @Test fun halbkugeln() {
        assertEquals(3_183.0, Druck.hemisphereForce(0.10), 1.0)
        assertEquals(0.0561, Druck.hemisphereRadiusFor(1000.0), 1e-4)
        assertEquals(325.0, Druck.hemisphereForce(0.10) / Druck.G, 1.0)
    }

    @Test fun jedesZielErreichbarUndAmAnfangNicht() {
        val exps = Topic.DRUCK.experiments(); assertEquals(8, exps.size)
        for (e in exps) {
            val s = e.setup as PressureSetup
            val ok = when (s.mode) {
                PressureMode.NAILS -> PressureUi.NAILS.any { Druck.nailPressure(it) <= s.maxKPa!! * 1000 }
                PressureMode.DIVE -> PressureUi.DEPTHS.any { PressureUi.hit(Druck.total(it) / Druck.BAR, s.targetBar!!) }
                PressureMode.HYDRAULIC -> PressureUi.AREAS.any { Druck.hydraulicForce(s.f1, s.a1, it) >= s.minForceN!! - 1e-6 }
                PressureMode.HEMISPHERE -> PressureUi.RADII_CM.any { Druck.hemisphereForce(it / 100) >= s.minForceN!! }
            }
            assertTrue("${e.id}: Ziel nicht erreichbar", ok)
            // Startwerte des Geräts (siehe PressureLab): Nägel 100, Tiefe 0, A₂ 0,02, Radius 2 cm – dort ist das Ziel nicht erreicht
            val atStart = when (s.mode) {
                PressureMode.NAILS -> Druck.nailPressure(100) <= s.maxKPa!! * 1000
                PressureMode.DIVE -> PressureUi.hit(Druck.total(0.0) / Druck.BAR, s.targetBar!!)
                PressureMode.HYDRAULIC -> Druck.hydraulicForce(s.f1, s.a1, 0.02) >= s.minForceN!!
                PressureMode.HEMISPHERE -> Druck.hemisphereForce(0.02) >= s.minForceN!!
            }
            assertFalse("${e.id}: Ziel schon zu Beginn erreicht", atStart)
        }
    }

    @Test fun antwortenStimmen() {
        fun answer(id: String) = Catalog.byId(id)!!.let { it.predict.choices[it.predict.answer].text }.replace(Regex("[\\s\\u00A0\\u202F]+"), " ")
        assertEquals("191 kPa", answer("d2"))
        assertEquals("2 bar", answer("d3"))
        assertEquals("20 m", answer("d4"))
        assertEquals("10000N", answer("d5").replace(" ", ""))
        assertEquals("510 kg", answer("d6"))
        assertEquals("3200N", answer("d7").replace(" ", ""))
        assertEquals("50 m", answer("d8"))
    }

    @Test fun theorieBeispiele() {
        val t = Theory.of(Topic.DRUCK).pro.joinToString(" ") { it.text }
        assertTrue(t.contains("190 750")); assertTrue(t.contains("98 100")); assertTrue(t.contains("20,3 m")); assertTrue(t.contains("10 000 N")); assertTrue(t.contains("3 183 N"))
    }
}
