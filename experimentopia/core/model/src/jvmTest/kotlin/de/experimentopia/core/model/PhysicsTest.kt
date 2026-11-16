package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/** Die Modelle gegen bekannte Lehrbuchwerte prüfen. */
class PhysicsTest {
    private fun near(exp: Double, act: Double, tol: Double) = assertEquals(exp, act, tol)

    @Test fun schwimmen() {
        assertEquals(FloatState.SCHWIMMT, Buoyancy.state(Thing.HOLZ, Liquid.WASSER))
        assertEquals(FloatState.SINKT, Buoyancy.state(Thing.EI, Liquid.WASSER))
        assertEquals(FloatState.SCHWIMMT, Buoyancy.state(Thing.EI, Liquid.SALZWASSER))
        assertEquals(FloatState.SINKT, Buoyancy.state(Thing.EIS, Liquid.SPIRITUS))
        near(0.55, Buoyancy.submerged(0.55, 1.0), 1e-12)
        near(0.98, Buoyancy.buoyantForceN(100.0, 1.0, 9.81), 0.01)   // 100 cm³ Wasser ≈ 0,98 N
        near(2.5, Buoyancy.density(50.0, 20.0), 1e-12)
    }

    @Test fun dichteturm() {
        val l = DensityTower.layers(listOf(Liquid.OEL, Liquid.SPIRITUS, Liquid.HONIG, Liquid.WASSER, Liquid.SPUELI))
        assertEquals(listOf(Liquid.HONIG, Liquid.SPUELI, Liquid.WASSER, Liquid.OEL, Liquid.SPIRITUS), l.map { it.liquids.single() })
        assertTrue(DensityTower.layers(listOf(Liquid.SPIRITUS, Liquid.WASSER)).single().mixed)
    }

    @Test fun hebel() {
        assertTrue(Lever.balanced(listOf(Load(2.0, -3), Load(3.0, 2))))
        assertEquals(1, Lever.tilt(listOf(Load(80.0, -1), Load(30.0, 4))))
        assertNull(Lever.balancePos(listOf(Load(4.0, -2)), 1.0))
        near(117.72, Lever.effortN(60.0, 1.0, 5.0), 0.01)
    }

    @Test fun rollenUndRampe() {
        near(196.2, Pulley.forceN(40.0, 2), 1e-9)
        assertEquals(4, Pulley.minRopes(200.0, 500.0, listOf(1, 2, 4, 6)))
        near(Pulley.workJ(40.0, 1.0), Pulley.forceN(40.0, 2) * Pulley.ropeM(1.0, 2), 1e-9)   // Goldene Regel
        near(122.625, Ramp.forceN(50.0, 1.0, 4.0), 1e-9)
    }

    @Test fun stromkreise() {
        val one = Circuits.solve(Layout.single(1), listOf(Slot(Part.LAMPE)), 4.5)
        near(0.45, one.totalA, 1e-12)
        val series = listOf(Slot(Part.LAMPE), Slot(Part.LAMPE))
        val rs = Circuits.solve(Layout.single(2), series, 4.5)
        near(0.25f.toDouble(), Circuits.brightness(rs, series, 0, 4.5).toDouble(), 1e-6)      // halber Strom → Viertel Leistung
        val rp = Circuits.solve(Layout.ladder(0, listOf(1, 1)), series, 4.5)
        near(0.9, rp.totalA, 1e-12); near(1.0, Circuits.brightness(rp, series, 1, 4.5).toDouble(), 1e-6)
        // Offener Schalter unterbricht, Kurzschluss wird erkannt
        val open = listOf(Slot(Part.SCHALTER_AUF), Slot(Part.LAMPE))
        assertEquals(0.0, Circuits.solve(Layout.single(2), open, 4.5).totalA, 0.0)
        val short = listOf(Slot(Part.LAMPE), Slot(Part.KABEL))
        val r = Circuits.solve(Layout.ladder(0, listOf(1, 1)), short, 4.5)
        assertTrue(r.short); assertEquals(0.0, r.amps[0], 0.0)
        // Gemischt: 10 Ω in Reihe mit (10 Ω ∥ 10 Ω) = 15 Ω
        val mixed = List(3) { Slot(Part.WIDERSTAND, 10.0) }
        near(15.0, Circuits.solve(Layout.ladder(1, listOf(1, 1)), mixed, 6.0).totalOhm, 1e-12)
        near(0.2, Circuits.solve(Layout.ladder(1, listOf(1, 1)), mixed, 6.0).amps[1], 1e-12)
    }

    @Test fun saeurenUndLaugen() {
        near(1.0, Acid.acidPH(0.1), 1e-6)
        near(7.0, Beaker().addAcid(0.1, 10.0).addBase(0.1, 10.0).pH, 1e-9)
        near(11.68, Beaker().addAcid(0.1, 10.0).addBase(0.1, 11.0).pH, 0.01)
        near(2.0, Beaker().addAcid(0.1, 10.0).addWater(90.0).pH, 1e-6)
        assertEquals("rot", Indicator.name(Household.ZITRONE.pH)); assertEquals("grün", Indicator.name(Household.SEIFE.pH))
        assertEquals("basisch", Indicator.kind(Household.NATRON.pH))
        assertEquals(0xFF5D55C2L, Indicator.color(7.0))
    }

    @Test fun formelnUndReaktionen() {
        assertEquals(mapOf(Element.C to 2, Element.H to 4, Element.O to 2, Element.Na to 0).filterValues { it > 0 }, Formula.atoms("CH3COOH"))
        assertEquals(mapOf(Element.Ca to 1, Element.O to 2, Element.H to 2), Formula.atoms("Ca(OH)2"))
        assertEquals("H₂O", Formula.pretty("H2O"))
        for (r in Reactions.all) {
            assertTrue("${r.id} ausgeglichen", r.balanced(r.coeffs))
            assertEquals("${r.id} kleinste Zahlen", 1, r.coeffs.reduce(Reaction::gcd))
            assertFalse("${r.id}: Start mit lauter Einsen darf nicht schon stimmen, sonst gibt es nichts zu tun", r.balanced(List(r.coeffs.size) { 1 }) && r.id !in setOf("kohle", "neutral", "natron"))
            assertTrue(r.notSmallest(r.coeffs.map { it * 2 }))
            assertTrue("${r.id}: Zahlen bis 15 reichen (so weit geht der Zähler)", r.coeffs.all { it in 1..15 })
        }
    }

    @Test fun ballon() {
        near(0.0595, Fizz.natronMol(1.0), 1e-4)
        near(2.0, Fizz.gasL(6.0, 100.0), 0.01)      // Essig begrenzt
        assertEquals("Essig", Fizz.limiting(6.0, 100.0)); assertEquals("Natron", Fizz.limiting(1.0, 100.0))
    }

    @Test fun waerme() {
        near(50.0, Heat.mixWater(100.0, 80.0, 100.0, 20.0), 1e-9)
        near(27.76, Heat.mix(100.0, Stuff.EISEN.c, 100.0, 100.0, Stuff.WASSER.c, 20.0), 0.01)
        assertEquals(-20.0, Heat.heat(100.0, -20.0, 0.0).tempC, 1e-9)
        assertEquals(0.0, Heat.heat(100.0, -20.0, 100 * 2.09 * 20 + 1000.0).tempC, 1e-9)
        val boil = Heat.heat(100.0, -20.0, Heat.energyToSteam(100.0, -20.0) - 1)
        assertEquals(100.0, boil.tempC, 1e-9); assertTrue(boil.steam > 0.99)
        // Temperatur steigt nie, wenn Energie dazukommt
        var last = -100.0
        for (e in 0..300) { val t = Heat.heat(50.0, -10.0, e * 500.0).tempC; assertTrue(t >= last - 1e-9); last = t }
    }

    @Test fun fallen() {
        near(sqrt(40 / 9.81), Fall.timeNoAir(20.0, 9.81), 1e-12)
        near(Fall.timeNoAir(20.0, 9.81), Fall.simulate(Faller.KUGEL, 20.0, Planet.VAKUUM).last().t, 0.002)
        near(0.6, Fall.terminal(Faller.FEDER), 0.05)
        assertTrue(Fall.time(Faller.KUGEL, 2.0, Planet.ERDE) < 0.7)
        assertTrue(Fall.time(Faller.FEDER, 2.0, Planet.ERDE) > 3.0)
    }

    @Test fun pendel() {
        near(2 * PI * sqrt(1 / 9.81), Pendulum.period(1.0, 9.81, 0.001), 1e-6)
        near(1.0 + 1.0 / 16 * (Math.toRadians(30.0).let { it * it }), Pendulum.period(1.0, 9.81, 30.0) / Pendulum.periodSmall(1.0, 9.81), 0.002)
        near(0.994, Pendulum.lengthFor(2.0, 9.81), 0.001)
    }

    @Test fun klang() {
        near(880.0, Klang.tubeHz(Klang.airFor(880.0)), 1e-9)
        assertTrue(Klang.glassHz(0.9) < Klang.glassHz(0.1))
        near(1200.0, Klang.cents(880.0, 440.0), 1e-9)
        assertEquals(Note.A5, Note.nearest(870.0))
        // Alle Töne der Melodie passen in eine 25-cm-Röhre
        Note.entries.forEach { assertTrue(it.name, Klang.waterFor(it.hz) in 0.0..Klang.TUBE_M) }
    }

    @Test fun licht() {
        assertEquals("Gelb", Colors.name(Colors.add(listOf(Rgb.ROT, Rgb.GRUEN))))
        assertEquals("Weiß", Colors.name(Colors.add(listOf(Rgb.GELB, Rgb.BLAU))))
        assertEquals("Grün", Colors.name(Colors.filter(1.0, 0.0, 1.0)))
        near(1.5167, Prism.n(589.0), 0.0005)
        near(38.6, Prism.minDeviation(589.0), 0.2)
        assertTrue(Prism.minDeviation(420.0) > Prism.minDeviation(700.0))
        // kleinste Ablenkung ist wirklich die kleinste
        val d = (30..80).mapNotNull { Prism.deviation(589.0, it.toDouble()) }
        assertTrue(abs(d.min() - Prism.minDeviation(589.0)) < 0.1)
    }

    @Test fun eingaben() {
        assertEquals(Check.Ok(2.5), checkInput("2,5", 0.0, 10.0, "kg"))
        assertTrue(checkInput("abc", 0.0, 10.0, "kg") is Check.Bad)
        assertTrue(checkInput("20", 0.0, 10.0, "kg") is Check.Bad)
        assertEquals("1,5", Fmt.num(1.5)); assertEquals("−2", Fmt.num(-2.0)); assertEquals("118", Fmt.num(117.7, 0))
    }
}

class StoreTest {
    @Test fun bestesBleibt() {
        val a = Progress.merge(null, Finished("w1", 3, "x", "y", true, 2, true, false), 1000)
        val b = Progress.merge(a, Finished("w1", 1, "x", "y", false, 0, false, false), 2000)
        org.junit.Assert.assertEquals(3, b.stars); org.junit.Assert.assertTrue(b.firstGuessRight)
        org.junit.Assert.assertEquals(2, b.plays); org.junit.Assert.assertEquals(1000, b.firstAt); org.junit.Assert.assertEquals(2000, b.lastAt)
    }
}
