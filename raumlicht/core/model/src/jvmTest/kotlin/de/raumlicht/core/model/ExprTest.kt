package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class ExprTest {
    private fun v(src: String, x: Double) = Expr.parse(src).getOrThrow().at(x)

    @Test fun readsTermsTheWayPeopleTypeThem() {
        assertEquals(2.0, v("x^3-3x", 2.0), 1e-9); assertEquals(-4.0, v("-x^2", 2.0), 1e-9)                 // Minus bindet schwächer als hoch
        assertEquals(9.0, v("3(x+1)", 2.0), 1e-9); assertEquals(2 * sin(1.0), v("2sin(x)", 1.0), 1e-9); assertEquals(sin(2.0), v("sin 2x", 1.0), 1e-9)
        assertEquals(1.5, v("f(x) = 0,5x + 1", 1.0), 1e-9); assertEquals(5.0, v("x² + 1", 2.0), 1e-9); assertEquals(3.0, v("√(x+5)", 4.0), 1e-9)
        assertEquals(exp(-1.0) * cos(3.0), v("e^(-x)·cos(3x)", 1.0), 1e-9); assertEquals(0.25, v("2^-x", 2.0), 1e-9); assertEquals(PI, v("π", 0.0), 1e-9)
        assertEquals(512.0, v("2^3^2", 0.0), 1e-9); assertEquals(1.0, v("x:2", 2.0), 1e-9); assertEquals(sin(3.0) * 3, v("xsin(x)", 3.0), 1e-9)
        assertEquals(2.0, v("ln(e^2)", 0.0), 1e-9); assertEquals(3.0, v("log(1000)", 0.0), 1e-9); assertEquals(4.0, v("abs(-4)", 0.0), 1e-9)
        assertTrue(v("1/x", 0.0).isInfinite()); assertTrue(v("sqrt(x)", -1.0).isNaN())
        assertEquals("x³−3x", Expr.parse(" x^3-3x ").getOrThrow().text)
    }

    /** C1: Eine Funktion ohne Klammer nimmt das ganze folgende Produkt aus Zahl und Variable – oder bittet um Klammern, wenn es nicht eindeutig ist */
    @Test fun functionsWithoutBracketsTakeTheWholeProduct() {
        assertEquals(sin(1.4), v("sin 2x", 0.7), 1e-12); assertEquals(sin(1.4), v("sin2x", 0.7), 1e-12); assertEquals(ln(3.0), v("ln x", 3.0), 1e-12); assertEquals(ln(3.0), v("lnx", 3.0), 1e-12)
        assertEquals(sin(2.25), v("sin x^2", 1.5), 1e-12); assertEquals(sin(2.25), v("sin x²", 1.5), 1e-12); assertEquals(exp(2.0), v("exp x", 2.0), 1e-12)
        assertEquals(sin(2 * PI * 0.3), v("sin 2πx", 0.3), 1e-12); assertEquals(sin(0.7) * cos(0.7), v("sin x cos x", 0.7), 1e-12); assertEquals(3 * sin(1.4) + 1, v("3sin 2x + 1", 0.7), 1e-12)
        assertEquals(sqrt(18.0), v("√2x", 9.0), 1e-12); assertEquals(sin(0.7).pow(2), v("sin(x)^2", 0.7), 1e-12); assertEquals(sin(0.7).pow(2), v("sin(x)²", 0.7), 1e-12)
        fun err(src: String) = Expr.parse(src).exceptionOrNull()?.message ?: "kein Fehler: ${Expr.parse(src).getOrThrow().read}"
        // nicht eindeutig: lieber nachfragen als still falsch rechnen
        listOf("sin x/2", "sin 2*x", "sin 2(x+1)", "sin -x", "ln sin x", "sin").forEach { assertTrue("$it: ${err(it)}", err(it).contains("Klammer") || err(it).contains("mittendrin")) }
        assertEquals("Bitte mit Klammer schreiben: sin(…), zum Beispiel sin(2x).", err("sin x/2")); assertEquals("Bitte mit Klammer schreiben: √(…), zum Beispiel √(2x).", err("√x/2"))
        assertTrue(err("sin^2(x)").startsWith("Bitte so schreiben: sin(x)^2"))
    }

    /** C2: Bekannte Namen werden als Anfangsstück erkannt; Meldungen nennen nur, was getippt wurde. C3: „2 2“ ist keine 22. */
    @Test fun knownNamesAreReadAsPrefixesAndSpacesMatter() {
        assertEquals(2 * PI, v("πx", 2.0), 1e-12); assertEquals(4 * PI, v("2πx", 2.0), 1e-12); assertEquals(2 * PI, v("pi x", 2.0), 1e-12); assertEquals(1.0, v("sin(pi x)", 0.5), 1e-12)
        assertEquals(3.0, v("√x", 9.0), 1e-12); assertEquals(9.0, v("3√x", 9.0), 1e-12); assertEquals(8.0, v("x√x", 4.0), 1e-12); assertEquals(2 * E, v("ex", 2.0), 1e-12)
        assertEquals(2 * E, v("xe", 2.0), 1e-12); assertEquals(sin(3.0) * 3, v("xsin(x)", 3.0), 1e-12); assertEquals(3.0, v("WURZEL(X)", 9.0), 1e-12); assertEquals(exp(2.0), v("exp(x)", 2.0), 1e-12)
        fun err(src: String) = Expr.parse(src).exceptionOrNull()?.message ?: "kein Fehler"
        assertEquals("„q“ kenne ich nicht. Es gibt: sin, cos, tan, exp, ln, log, sqrt, abs.", err("√q")); assertTrue(err("pi q").startsWith("„q“")); assertTrue(err("2πy").startsWith("„πy“"))
        assertTrue(err("Foo(x)").startsWith("„Foo“")); assertTrue(err("cosh(x)").startsWith("„cosh“"))
        listOf("√q", "πy", "pi q", "3√y").forEach { assertFalse(it, err(it).substringBefore("Es gibt").let { m -> m.contains("sqrt") || m.contains("pix") || m.contains("piq") }) }
        assertEquals("Zwischen 2 und 2 fehlt ein Rechenzeichen.", err("2 2")); assertEquals("Zwischen 3 und 4 fehlt ein Rechenzeichen.", err("3 4x"))
        assertEquals("Zwischen 2,5 und 3 fehlt ein Rechenzeichen.", err("x + 2,5 3")); assertEquals(22.0, v("22", 0.0), 0.0); assertEquals(6.0, v("2 x", 3.0), 0.0); assertEquals(6.0, v("x 2", 3.0), 0.0)
    }

    /** C4: ² und ³ nur bei genau 2 bzw. 3. C5: [Expr.read] zeigt, wie der Term verstanden wurde. */
    @Test fun shownTermAndUnderstoodTerm() {
        fun text(src: String) = Expr.parse(src).getOrThrow().text
        fun read(src: String) = Expr.parse(src).getOrThrow().read
        assertEquals("x^23", text("x^23")); assertEquals("x^2,5", text("x^2,5")); assertEquals("2^20", text("2^20")); assertEquals("x^3.5", text("x^3.5"))
        assertEquals("x²+x³", text("x^2+x^3")); assertEquals("(x²)−2x³ · 4", text("(x^2)-2x^3*4"))
        assertEquals(2.0.pow(23), v("x^23", 2.0), 1e-6); assertEquals(2.0.pow(2.5), v("x^2,5", 2.0), 1e-12)
        // das weggelassene Malzeichen bindet wie „·“ – in der verstandenen Schreibweise sieht man es
        assertEquals("e^(−0,3) · x", read("e^-0,3x")); assertEquals(exp(-0.3) * 2, v("e^-0,3x", 2.0), 1e-12)
        assertEquals("(1 / 2) · x", read("1/2x")); assertEquals(1.0, v("1/2x", 2.0), 1e-12)
        assertEquals("e^(−0,3x)", read("e^(-0,3x)")); assertEquals("1 / (2x)", read("1/(2x)")); assertEquals("sin(2x)", read("sin 2x")); assertEquals("x³ − 3x", read("x^3-3x"))
        assertEquals("sin(2) · x", read("sin(2)x")); assertEquals("√(x + 1)", read("√(x+1)")); assertEquals("2^(3²)", read("2^3^2")); assertEquals("−x²", read("-x^2")); assertEquals("(−x)²", read("(-x)^2"))
        assertEquals("π · x", read("πx")); assertEquals("2π · x", read("2πx")); assertEquals("x − (x + 1)", read("x-(x+1)")); assertEquals("(x + 1) / (x − 1)", read("(x+1)/(x-1)"))
    }

    /** Der geschätzte Rundungsfehler trennt Rauschen von echten Stellen */
    @Test fun noiseEstimateCoversTheRealRoundingError() {
        fun e(s: String) = Expr.parse(s).getOrThrow()
        val one = e("sin(x)^2+cos(x)^2"); for (i in 1..50) { val x = i * 0.137; assertTrue(abs(one.at(x) - 1) <= 4 * one.noise(x)); assertTrue(one.noise(x) < 1e-14) }
        val line = e("(x+100)-100"); for (i in 1..50) { val x = i * 0.0137; assertTrue(abs(line.at(x) - x) <= 4 * line.noise(x)) }
        assertEquals(0.0, e("x^3").noise(0.0), 0.0); assertTrue(e("1/x").noise(1e-3) < 1e-9); assertFalse(e("ln(x)").noise(-1.0).isFinite())
    }

    /** A4: Die Art folgt dem Vorzeichenwechsel von f′; an Knicken und flachen Stellen behauptet der Regeltext nichts Falsches */
    @Test fun kinksAndFlatPointsGetAnHonestRule() {
        fun spots(t: String, a: Double, b: Double) = Coaster.custom(Expr.parse(t).getOrThrow(), a, b)!!.spots
        val kink = spots("abs(sin(x))", 0.0, 7.0).single { it.kind != SpotKind.ZERO && it.x == 3.14 }
        assertEquals(SpotKind.LOW, kink.kind); assertTrue(kink.rule.startsWith("Knick: f′ wechselt das Vorzeichen von − nach +")); assertFalse(kink.rule.contains("f′ = 0"))
        assertEquals(SpotKind.HIGH.rule, spots("abs(sin(x))", 0.0, 7.0).first { it.kind == SpotKind.HIGH }.rule)
        assertTrue(spots("sqrt(abs(x))", -2.0, 2.0).first().rule.startsWith("Knick")); assertTrue(spots("abs(x)", -2.0, 2.0).first().rule.startsWith("Knick"))
        val flat = spots("x^4", -2.0, 2.0).first(); assertEquals(SpotKind.LOW, flat.kind); assertTrue(flat.rule, flat.rule.contains("f″ = 0 hilft hier nicht") && flat.rule.contains("von − nach +"))
        assertTrue(spots("-x^4", -2.0, 2.0).first().rule.contains("von + nach −")); assertEquals(SpotKind.LOW.rule, spots("x^2", -1.0, 2.0).first().rule)
        assertTrue(spots("x^3", -2.0, 2.0).first().rule.startsWith("Sattelpunkt")); assertEquals(SpotKind.TURN.rule, spots("x^3-3x", -2.0, 2.0).first { it.kind == SpotKind.TURN }.rule)
    }

    /** A6: Wo die Funktion nicht definiert ist, gibt es keine Zahlen; neben Polstellen steht der echte Wert, nicht die abgeschnittene Höhe */
    @Test fun cockpitShowsNoInventedValues() {
        fun ride(t: String, a: Double, b: Double) = Coaster.custom(Expr.parse(t).getOrThrow(), a, b)!!
        listOf("ln(x)", "sqrt(x)").forEach { t -> val d = ride(t, -2.0, 2.0).let { it.data(it.u(-1.0)) }
            assertFalse(d.defined); assertFalse(d.smooth); assertTrue(d.height.isNaN()); assertEquals("nicht definiert", d.slopeWord); assertEquals("nicht definiert", d.bendWord)
            assertEquals("nicht definiert", Geo.num(d.height)); assertEquals(0.0, d.slope, 0.0) }                         // die Fahrt rechnet mit 0 weiter, angezeigt wird nichts
        val ok = ride("ln(x)", -2.0, 2.0).let { it.data(it.u(1.0)) }; assertTrue(ok.defined && ok.smooth && !ok.offscreen); assertEquals(0.0, ok.height, 1e-12); assertEquals(1.0, ok.slope, 1e-6)
        val pole = ride("1/x", -2.0, 2.0); val near = pole.data(pole.u(0.01))
        assertEquals(100.0, near.height, 1e-6); assertTrue(near.offscreen); assertTrue(pole.f(0.01) < 50)                    // gezeichnet wird abgeschnitten, angezeigt der echte Wert
        assertFalse(pole.data(0.5).defined); assertFalse(pole.data(pole.u(1.0)).offscreen)
        val root = ride("sqrt(x)", 0.0, 4.0).data(0.0); assertTrue(root.defined); assertFalse(root.smooth); assertEquals(0.0, root.height, 0.0)   // am Rand: Wert ja, Steigung nein
    }

    /** A9: Sehr viele Stellen werden gleichmäßig ausgedünnt, nicht nach den ersten 60 abgeschnitten */
    @Test fun manySpotsAreThinnedEvenly() {
        val r = Coaster.custom(Expr.parse("sin(x)").getOrThrow(), 0.0, 1000.0)!!
        assertEquals(Coaster.MAX_SPOTS, r.spots.size); assertEquals(955, r.own!!.found)                      // 318 Extrema, 318 Wendestellen, 319 Nullstellen
        assertEquals(0.0, r.spots.first().x, 0.0); assertTrue(r.spots.last().x > 990); assertTrue(r.spots.zipWithNext().all { (a, b) -> b.x - a.x < 25 })
        val short = Coaster.custom(Expr.parse("sin(x)").getOrThrow(), 0.0, 7.0)!!; assertEquals(short.spots.size, short.own!!.found)
    }

    /** A10: Die Fläche gibt es nur, solange die Funktion vom Start bis x durchgehend definiert ist */
    @Test fun areaStopsAtGapsAndPoles() {
        fun ride(t: String, a: Double, b: Double) = Coaster.custom(Expr.parse(t).getOrThrow(), a, b)!!
        val h = ride("1/x", -2.0, 2.0)
        assertEquals(-ln(2.0), h.integral(-1.0), 1e-6); assertEquals(ln(0.05) - ln(2.0), h.integral(-0.05), 1e-4)        // die echte Fläche, nicht die der abgeschnittenen Kurve
        listOf(0.0, 0.01, 1.0, 2.0).forEach { assertTrue("$it", h.integral(it).isNaN()) }; assertEquals("nicht definiert", Geo.num(h.integral(0.0)))
        val p2 = ride("1/x^2", -2.0, 2.0); assertEquals(1.5, p2.integral(-0.5), 1e-6); assertTrue(p2.integral(0.5).isNaN())
        val off = ride("1/(x-0,3337)^2", -2.0, 2.0); assertTrue(off.integral(0.3).isFinite()); assertTrue(off.integral(0.4).isNaN())   // Polstelle zwischen den Rasterpunkten
        assertTrue(ride("tan(x)", 0.0, 6.0).integral(1.5).isFinite()); assertTrue(ride("tan(x)", 0.0, 6.0).integral(1.6).isNaN())
        assertTrue(ride("ln(x)", 0.0, 2.0).integral(1.0).isNaN()); assertTrue(ride("sqrt(x)", -1.0, 4.0).integral(2.0).isNaN())  // am Start nicht definiert
        assertEquals(16.0 / 3, ride("sqrt(x)", 0.0, 4.0).integral(4.0), 1e-2); assertEquals(8.0 / 3, ride("x^2", 0.0, 4.0).integral(2.0), 1e-9)
        assertEquals(2.0, ride("abs(x)", -2.0, 2.0).integral(0.0), 1e-9); assertEquals(0.0, ride("x^3-3x", -2.0, 2.0).integral(2.0), 1e-9)
        // gefüllt wird nur bis vor die Lücke
        assertFalse(h.scene(1.0, derivative = false, fill = true).contains("\"id\":\"plus")); assertTrue(h.scene(1.0, derivative = false, fill = true).contains("\"id\":\"minus"))
    }

    /** A11: Abgelehnt wird nur, was (fast) nirgends definiert ist – sonst wird gefahren, mit Hinweis, wo die Funktion definiert ist */
    @Test fun partlyDefinedFunctionsStillRide() {
        fun ride(t: String, a: Double, b: Double) = Coaster.custom(Expr.parse(t).getOrThrow(), a, b)
        assertEquals("Definiert nur zwischen 0 und 2", ride("sqrt(x)", -10.0, 2.0)!!.own!!.domain); assertEquals("Definiert nur zwischen 0 und 2", ride("ln(x)", -7.0, 2.0)!!.own!!.domain)
        assertEquals("Definiert nur zwischen −2 und 2", ride("sqrt(4-x^2)", -3.0, 3.0)!!.own!!.domain)
        assertEquals("Definiert nur zwischen −3 und −1 sowie zwischen 1 und 3", ride("sqrt(x^2-1)", -3.0, 3.0)!!.own!!.domain)
        assertEquals("Definiert nur zwischen 0,5 und 4", ride("ln(2x-1)", 0.0, 4.0)!!.own!!.domain)
        listOf("1/x" to -2.0, "x^2" to -2.0, "ln(x)" to 0.0, "sin(x)/x" to -2.0, "tan(x)" to 0.0).forEach { (t, a) -> assertNull(t, ride(t, a, 6.0)!!.own!!.domain) }
        assertNull(ride("sqrt(x)", -9.0, -1.0)); assertNull(ride("ln(x)", -1000.0, 0.0)); assertNull(ride("sqrt(x)", -1000.0, 1.0)); assertNull(ride("x", 2.0, 2.0))
        assertNull(Coaster.byId("huegel")!!.own)
    }

    @Test fun explainsWhatItDoesNotUnderstand() {
        fun err(src: String) = Expr.parse(src).exceptionOrNull()!!.message!!
        assertTrue(err("").contains("Tipp einen Term ein")); assertTrue(err("2*(x+1").contains("schließende Klammer")); assertTrue(err("x+1)").contains("Klammer zu viel"))
        assertTrue(err("foo(x)").contains("kenne ich nicht")); assertTrue(err("x+").contains("hört mittendrin auf")); assertTrue(err("x $ 2").contains("„$“"))
    }

    @Test fun ownFunctionBecomesARideWithItsSpecialPoints() {
        val r = Coaster.custom(Expr.parse("x^3-3x").getOrThrow(), -2.0, 2.0)!!
        assertEquals(listOf(SpotKind.ZERO to -1.73, SpotKind.HIGH to -1.0, SpotKind.TURN to 0.0, SpotKind.ZERO to 0.0, SpotKind.LOW to 1.0, SpotKind.ZERO to 1.73), r.spots.map { it.kind to it.x })
        assertEquals(-9.0, r.data(0.0).let { it.slope } - 18, 1e-3)                                   // f′(−2) = 9, numerisch
        assertEquals(2.0, r.data(0.25).height, 1e-9); assertFalse(r.clipped)
        // jede Kurve passt in die Bühne, egal wie groß die Werte sind
        listOf("1000x^2" to (-3.0 to 3.0), "0,001sin(x)" to (0.0 to 60.0), "x" to (100.0 to 400.0), "5" to (0.0 to 4.0), "e^x" to (0.0 to 12.0)).forEach { (t, iv) ->
            val ride = Coaster.custom(Expr.parse(t).getOrThrow(), iv.first, iv.second)!!
            ride.points.forEach { p -> assertTrue("$t $p", p.y in 0.6..7.4 && p.z in 0.4..6.0) }
            assertTrue(t, ride.scene(ride.x(0.5), derivative = true, fill = true).contains("axes2d"))
        }
        // Konstante und Gerade: keine erfundenen Extrema oder Wendestellen
        assertEquals(emptyList<Spot>(), Coaster.custom(Expr.parse("5").getOrThrow(), 0.0, 4.0)!!.spots)
        assertEquals(listOf(SpotKind.ZERO to 2.0), Coaster.custom(Expr.parse("2x-4").getOrThrow(), 0.0, 4.0)!!.spots.map { it.kind to it.x })
        // lange Fahrt: sin über 10 Perioden hat 20 Extrema
        val wave = Coaster.custom(Expr.parse("sin(x)").getOrThrow(), 0.0, 20 * PI)!!
        assertEquals(20, wave.spots.count { it.kind == SpotKind.HIGH || it.kind == SpotKind.LOW })
    }

    @Test fun polesAreClippedNotInvented() {
        val r = Coaster.custom(Expr.parse("1/x").getOrThrow(), -2.0, 2.0)!!
        assertTrue(r.clipped); assertEquals(emptyList<Spot>(), r.spots)                                   // der Vorzeichensprung bei 0 ist keine Nullstelle
        r.points.forEach { p -> assertTrue(p.z.isFinite() && p.z in 0.4..6.0) }
        val t = Coaster.custom(Expr.parse("tan(x)").getOrThrow(), 0.0, 6.0)!!
        assertEquals(listOf(0.0, 3.14), t.spots.filter { it.kind == SpotKind.ZERO }.map { it.x })      // tan(0) = 0: die Nullstelle am Rand zählt mit
        assertNull(Coaster.custom(Expr.parse("sqrt(x)").getOrThrow(), -9.0, -1.0))                        // nirgends definiert
        assertNotNull(Coaster.custom(Expr.parse("sqrt(x)").getOrThrow(), -1.0, 9.0))
    }
    /**
     * Vergleich mit einer unabhängigen Referenz (sympy: Ableitungen symbolisch, Stellen mit 40 Stellen Genauigkeit gesucht):
     * Nullstellen (auch am Rand), Hoch-/Tiefpunkte und Wendepunkte im Abschnitt. H/T/W/N + Stelle.
     */
    private val reference = listOf(
        Triple("x^3", -2.0 to 2.0, "W0.000 N0.000"),
        Triple("x^4", -2.0 to 2.0, "T0.000 N0.000"),
        Triple("-x^4", -2.0 to 2.0, "H0.000 N0.000"),
        Triple("x^6", -2.0 to 2.0, "T0.000 N0.000"),
        Triple("(x-1)^4", -2.0 to 2.0, "T1.000 N1.000"),
        Triple("x^5", -2.0 to 2.0, "W0.000 N0.000"),
        Triple("(x-1)^2", 0.0 to 3.0, "T1.000 N1.000"),
        Triple("x^2", -1.0 to 2.0, "T0.000 N0.000"),
        Triple("x^3-6x^2+9x", 0.0 to 7.0, "N0.000 H1.000 W2.000 T3.000 N3.000"),
        Triple("sin(x)^2", 0.0 to 7.0, "N0.000 W0.785 H1.571 W2.356 T3.142 N3.142 W3.927 H4.712 W5.498 T6.283 N6.283"),
        Triple("x^4-4x^3", -2.0 to 5.0, "W0.000 N0.000 W2.000 T3.000 N4.000"),
        Triple("x^3(x-2)", -1.0 to 3.0, "W0.000 N0.000 W1.000 T1.500 N2.000"),
        Triple("1/x^2", -2.0 to 2.0, ""),
        Triple("1/x^2", -1.0 to 3.0, ""),
        Triple("1/abs(x)", -2.0 to 2.0, ""),
        Triple("1/x^4", -2.0 to 2.0, ""),
        Triple("cos(x)/x^2", -2.0 to 2.0, "N-1.571 N1.571"),
        Triple("1/(x-1)^2", -2.0 to 2.0, ""),
        Triple("ln(x^2)", -2.0 to 2.0, "N-1.000 N1.000"),
        Triple("ln(abs(x))", -2.0 to 2.0, "N-1.000 N1.000"),
        Triple("sin(x)/x", -10.0 to 10.0, "N-9.425 W-9.206 H-7.725 N-6.283 W-5.940 T-4.493 N-3.142 W-2.082 W2.082 N3.142 T4.493 W5.940 N6.283 H7.725 W9.206 N9.425"),
        Triple("e^(ln(x))", 1.0 to 5.0, ""),
        Triple("sqrt(x)^2", 0.0 to 4.0, "N0.000"),
        Triple("(x^2-4)/(x-2)", -3.0 to 3.0, "N-2.000"),
        Triple("x^2/x", 1.0 to 3.0, ""),
        Triple("x*x/x", 1.0 to 3.0, ""),
        Triple("abs(sin(x))", 0.0 to 7.0, "N0.000 H1.571 T3.142 N3.142 H4.712 T6.283 N6.283"),
        Triple("sqrt(abs(x))", -2.0 to 2.0, "T0.000 N0.000"),
        Triple("x^2", 0.0 to 4.0, "N0.000"),
        Triple("-x^2+4x", 0.0 to 4.0, "N0.000 H2.000 N4.000"),
        Triple("x^2-2x-3", -1.0 to 4.0, "N-1.000 T1.000 N3.000"),
        Triple("2sin(x)", 0.0 to 7.0, "N0.000 H1.571 W3.142 N3.142 T4.712 W6.283 N6.283"),
        Triple("4x*e^(-x)", 0.0 to 6.0, "N0.000 H1.000 W2.000"),
        Triple("x^3-3x", -2.0 to 2.0, "N-1.732 H-1.000 W0.000 N0.000 T1.000 N1.732"),
        Triple("sin(x)", 0.0 to 60.0, "N0.000 H1.571 W3.142 N3.142 T4.712 W6.283 N6.283 H7.854 W9.425 N9.425 T10.996 W12.566 N12.566 H14.137 W15.708 N15.708 T17.279 W18.850 N18.850 H20.420 W21.991 N21.991 T23.562 W25.133 N25.133 H26.704 W28.274 N28.274 T29.845 W31.416 N31.416 H32.987 W34.558 N34.558 T36.128 W37.699 N37.699 H39.270 W40.841 N40.841 T42.412 W43.982 N43.982 H45.553 W47.124 N47.124 T48.695 W50.265 N50.265 H51.836 W53.407 N53.407 T54.978 W56.549 N56.549 H58.119 W59.690 N59.690"),
        Triple("1/x", -2.0 to 2.0, ""),
        Triple("sqrt(x)", -10.0 to 2.0, "N0.000"),                      // √0 = 0 am Rand des Definitionsbereichs (von Hand ergänzt)
        Triple("ln(x)", -7.0 to 2.0, "N1.000"),
        Triple("ln(x)", 0.0 to 6.0, "N1.000"),
        Triple("e^(-0,3x)*sin(3x)", 0.0 to 8.0, "N0.000 H0.490 W0.981 N1.047 T1.538 W2.028 N2.094 H2.585 W3.075 N3.142 T3.632 W4.122 N4.189 H4.679 W5.170 N5.236 T5.726 W6.217 N6.283 H6.774 W7.264 N7.330 T7.821"),
        Triple("x*ln(x)", 0.0 to 3.0, "T0.368 N1.000"),
        Triple("e^(-x^2)", -3.0 to 3.0, "W-0.707 H0.000 W0.707"),
        Triple("1/(x^2+1)", -3.0 to 3.0, "W-0.577 H0.000 W0.577"),
        Triple("tan(x)", 0.0 to 6.0, "N0.000 W3.142 N3.142"),
        Triple("5", 0.0 to 4.0, ""),
        Triple("0", 0.0 to 4.0, ""),
        Triple("2x-4", 0.0 to 4.0, "N2.000"),
        Triple("x^4-4x^2", -3.0 to 3.0, "N-2.000 T-1.414 W-0.816 H0.000 N0.000 W0.816 T1.414 N2.000"),
        Triple("x^5-5x^3+4x", -3.0 to 3.0, "N-2.000 H-1.644 W-1.225 N-1.000 T-0.544 W0.000 N0.000 H0.544 N1.000 W1.225 T1.644 N2.000"),
        Triple("x^4-2x^2+1", -2.0 to 2.0, "T-1.000 N-1.000 W-0.577 H0.000 W0.577 T1.000 N1.000"),
        Triple("x^4-4x^3+6x^2-4x+1", -1.0 to 3.0, "T1.000 N1.000"),
        Triple("x^3-3x^2+3x-1", -1.0 to 3.0, "W1.000 N1.000"),
        Triple("(x+100)-100", -2.0 to 2.0, "N0.000"),
        Triple("sin(x)^2+cos(x)^2", 0.0 to 7.0, ""),
        Triple("cos(x)", -4.0 to 4.0, "T-3.142 W-1.571 N-1.571 H0.000 W1.571 N1.571 T3.142"),
        Triple("cos(x)+1", 0.0 to 7.0, "W1.571 T3.142 N3.142 W4.712 H6.283"),
        Triple("x*sin(x)", 0.0 to 10.0, "N0.000 W1.077 H2.029 N3.142 W3.644 T4.913 N6.283 W6.578 H7.979 N9.425 W9.630"),
        Triple("e^x", -2.0 to 2.0, ""),
        Triple("e^x-2", -2.0 to 2.0, "N0.693"),
        Triple("x*e^x", -4.0 to 1.0, "W-2.000 T-1.000 N0.000"),
        Triple("x^2*e^(-x)", -1.0 to 6.0, "T0.000 N0.000 W0.586 H2.000 W3.414"),
        Triple("abs(x)", -2.0 to 2.0, "T0.000 N0.000"),
        Triple("abs(x^2-1)", -2.0 to 2.0, "T-1.000 N-1.000 H0.000 T1.000 N1.000"),
        Triple("x*abs(x)", -2.0 to 2.0, "W0.000 N0.000"),
        Triple("abs(x-1)+abs(x+1)", -3.0 to 3.0, ""),
        Triple("1/(x-1)", -2.0 to 3.0, ""),
        Triple("(x^2-1)/x", -3.0 to 3.0, "N-1.000 N1.000"),
        Triple("x+1/x", -3.0 to 3.0, "H-1.000 T1.000"),
        Triple("x^2/(x-1)", -2.0 to 4.0, "H0.000 N0.000 T2.000"),
        Triple("1/(x^2-1)", -3.0 to 3.0, "H0.000"),
        Triple("sqrt(x)", 0.0 to 4.0, "N0.000"),
        Triple("sqrt(4-x^2)", -3.0 to 3.0, "N-2.000 H0.000 N2.000"),   // Nullstellen am Rand des Definitionsbereichs (von Hand ergänzt)
        Triple("ln(x)^2", 0.0 to 4.0, "T1.000 N1.000 W2.718"),
        Triple("ln(x)/x", 0.0 to 8.0, "N1.000 H2.718 W4.482"),
        Triple("x^3+x", -2.0 to 2.0, "W0.000 N0.000"),
        Triple("0,1x^3-x", -4.0 to 4.0, "N-3.162 H-1.826 W0.000 N0.000 T1.826 N3.162"),
        Triple("1000x^2-1", -1.0 to 1.0, "N-0.032 T0.000 N0.032"),
        Triple("0,001sin(x)+1000", 0.0 to 7.0, "H1.571 W3.142 T4.712 W6.283"),
        Triple("sin(3x)", 0.0 to 7.0, "N0.000 H0.524 W1.047 N1.047 T1.571 W2.094 N2.094 H2.618 W3.142 N3.142 T3.665 W4.189 N4.189 H4.712 W5.236 N5.236 T5.760 W6.283 N6.283 H6.807"),
        Triple("sin(x)+sin(2x)", 0.0 to 7.0, "N0.000 H0.936 W1.696 N2.094 T2.574 W3.142 N3.142 H3.709 N4.189 W4.587 T5.347 W6.283 N6.283"),
        Triple("e^(-x)*(1,5+sin(3x))", 0.0 to 30.0, "H0.252 W0.783 T1.628 W1.930 H2.346 W2.877 T3.723 W4.024 H4.440 W4.971 T5.817 W6.119 H6.535 W7.066 T7.911 W8.213 H8.629 W9.160 T10.006 W10.308 H10.724 W11.254 T12.100 W12.402 H12.818 W13.349 T14.195 W14.496 H14.912 W15.443 T16.289 W16.591 H17.007 W17.538 T18.383 W18.685 H19.101 W19.632 T20.478 W20.780 H21.196 W21.726 T22.572 W22.874 H23.290 W23.821 T24.667 W24.968 H25.384 W25.915 T26.761 W27.063 H27.479 W28.010 T28.855 W29.157 H29.573"),
        Triple("x^3", 0.0 to 2.0, "N0.000"),
        Triple("(x-2)^3+1", 0.0 to 4.0, "N1.000 W2.000"),
        Triple("sin(x)^3", 0.0 to 7.0, "N0.000 W0.955 H1.571 W2.186 W3.142 N3.142 W4.097 T4.712 W5.328 W6.283 N6.283"),
        Triple("x^2*(x-1)^2*(x+1)", -2.0 to 2.0, "N-1.000 H-0.740 W-0.475 T0.000 N0.000 W0.258 H0.540 W0.817 T1.000 N1.000"),
        Triple("1/sqrt(x)", 0.0 to 4.0, ""),
        Triple("x/abs(x)", -2.0 to 2.0, ""),
        Triple("2^x-x^2", -2.0 to 5.0, "N-0.767 H0.485 N2.000 W2.058 T3.212 N4.000"),
        Triple("log(x)", 0.0 to 20.0, "N1.000"),
        Triple("abs(x)+x", -2.0 to 2.0, ""),
    )

    @Test fun curveScanMatchesTheSympyReference() {
        val letter = mapOf(SpotKind.HIGH to "H", SpotKind.LOW to "T", SpotKind.TURN to "W", SpotKind.ZERO to "N")
        val wrong = ArrayList<String>()
        reference.forEach { (term, iv, expected) ->
            val ride = Coaster.custom(Expr.parse(term).getOrThrow(), iv.first, iv.second)
            val got = ride?.spots.orEmpty().map { letter.getValue(it.kind) to it.x }
            val want = expected.split(' ').filter { it.isNotEmpty() }.map { it.take(1) to it.drop(1).toDouble() }
            // gleiche Arten in gleicher Reihenfolge, Stellen auf 0,01 gerundet (Rundungsgrenze: 0,006 Spielraum)
            val same = got.size == want.size && got.indices.all { got[it].first == want[it].first && abs(got[it].second - want[it].second) < 0.006 }
            if (!same) wrong += "$term auf [${iv.first}; ${iv.second}]: erwartet <$expected>, bekommen <${got.joinToString(" ") { it.first + it.second }}>"
            assertFalse("$term: −0 darf nie vorkommen", ride?.spots.orEmpty().any { Geo.num(it.x) == "−0" || 1 / it.x == Double.NEGATIVE_INFINITY })
        }
        assertTrue(reference.size >= 60); assertEquals(wrong.joinToString("\n"), 0, wrong.size)
    }
}
