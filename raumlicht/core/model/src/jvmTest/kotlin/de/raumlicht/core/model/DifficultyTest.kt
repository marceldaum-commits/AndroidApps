package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/** Belegt mit Kennzahlen, dass jede Stufe im endlosen Üben schwerer ist als die vorige – und rechnet die Profi-Aufgaben unabhängig nach. */
class DifficultyTest {
    private fun f(v: Double) = "%.2f".format(v).replace('.', ',')

    @Test fun jedeStufeIstMessbarSchwerer() {
        val rows = JamLevel.entries.map { it to Practice.average(Practice.sample(it, 60)) }
        val table = buildString {
            append("| Stufe | Aufgabenarten | Rechenschritte | Länge des Rechenwegs (Zeichen) | beteiligte Objekte | Parameter | Anteil krummer Werte | Gesamtwert |\n|---|---|---|---|---|---|---|---|\n")
            rows.forEach { (l, m) -> val kinds = Practice.count(JamTopic.MIX, l)
                append("| ${l.title} | $kinds | ${f(m.steps)} | ${f(m.wayLength)} | ${f(m.objects)} | ${f(m.parameters)} | ${f(m.nonInteger * 100)} % | ${f(m.score)} |\n") } }
        File("build").mkdirs(); File("build/schwierigkeit.md").writeText(table); println(table)
        rows.zipWithNext().forEach { (lo, hi) ->
            val a = lo.second; val b = hi.second; val name = "${lo.first.title} → ${hi.first.title}"
            assertTrue("$name: Rechenschritte ${a.steps} → ${b.steps}", b.steps > a.steps)
            assertTrue("$name: Länge des Rechenwegs", b.wayLength > a.wayLength)
            assertTrue("$name: Parameter", b.parameters > a.parameters)
            assertTrue("$name: Anteil krummer Werte", b.nonInteger > a.nonInteger)
            assertTrue("$name: Gesamtwert", b.score > a.score * 1.2)              // deutlich, nicht nur knapp
        }
        assertTrue("Profi hat die meisten beteiligten Objekte", rows.last().second.objects > rows.dropLast(1).maxOf { it.second.objects })
        // die leichteste Profi-Aufgabe ist noch schwerer als der Durchschnitt der Abi-Stufe
        assertTrue(Practice.sample(JamLevel.PROFI, 60).minOf { Practice.metrics(it).score } > rows[1].second.score)
    }

    @Test fun profiAufgabenStimmen() {
        for (kind in ProGen.Kind.entries) for (seed in 0L until 300) { val l = ProGen.level(kind, seed); val id = "${kind.name}/$seed"
            assertTrue(id, l.evaluate(l.solution).success); assertFalse(id, l.evaluate(l.start).success)
            l.controls.forEach { c -> (l.solution.getValue(c.key) + c.start).let { } ; l.solution.getValue(c.key).forEachIndexed { i, x -> assertTrue("$id außerhalb", x in c.min[i]..c.max[i]) } }
            if (l.controls[0].kind == ControlKind.SCALAR) { val c = l.controls[0]; assertEquals(id, 1, (c.min[0]..c.max[0]).count { l.evaluate(mapOf(c.key to listOf(it))).success }) }
            for (ph in Phase.entries) { val o = l.objects(if (ph == Phase.DONE) l.solution else l.start, ph); assertEquals("$id doppelte Objekte", o.size, o.map { it.id }.toSet().size); assertTrue(l.scene(l.start, ph).isNotEmpty())
                o.filter { it.type == "point" }.forEach { p -> p.p!!.forEachIndexed { i, x -> assertTrue("$id/${p.id} außerhalb", x >= -1e-9 && x <= l.bounds.max[i] + 1e-9) } } }
            assertTrue(id, l.way.size >= 4 && l.tip.isNotBlank() && l.formula(l.start).isNotEmpty())
        }
        assertEquals(ProGen.level(ProGen.Kind.CUT_CIRCLE, 5).mission, ProGen.level(ProGen.Kind.CUT_CIRCLE, 5).mission)      // gleiche Zahl → gleiche Aufgabe
        assertTrue((0L until 40).map { ProGen.level(ProGen.Kind.SKEW_DIST, it).way[0] }.toSet().size > 10)                    // viele verschiedene
    }

    /** Die Lösungen unabhängig aus den angezeigten Angaben nachgerechnet (Zahlen aus dem Formelbereich gelesen). */
    @Test fun profiLoesungenUnabhaengigNachgerechnet() {
        fun nums(s: String) = Regex("""−?\d+(,\d+)?""").findAll(s).map { it.value.replace('−', '-').replace(',', '.').toDouble() }.toList()
        fun text(l: Level) = l.formula(l.start).map { line -> line.joinToString("") { p -> when (p) { is Part.Txt -> p.s; is Part.Num -> p.s; is Part.Col -> p.cells.joinToString("|", "(", ")") } } }
        fun plane(s: String): Pair<V3, Double> { val side = s.substringAfter("E: "); val lhs = side.substringBefore(" = "); var n = DoubleArray(3)
            Regex("""([+−]?)\s*(\d*)x([₁₂₃])""").findAll(lhs).forEach { m -> val k = (m.groupValues[2].ifEmpty { "1" }).toDouble() * if (m.groupValues[1] == "−") -1 else 1; n["₁₂₃".indexOf(m.groupValues[3])] = k }
            return V3(n[0], n[1], n[2]) to nums(side.substringAfter(" = "))[0] }
        for (seed in 0L until 120) {
            ProGen.level(ProGen.Kind.MIRROR_DIST, seed).let { l -> val t = text(l); val p = nums(t[0]).let { V3(it[0], it[1], it[2]) }; val (n, c) = plane(t[1])
                assertEquals(2 * Geo.distPointPlane(p, n, c), l.solution.getValue("a")[0].toDouble(), 1e-9) }
            ProGen.level(ProGen.Kind.CUT_CIRCLE, seed).let { l -> val t = text(l); val k = nums(t[0]); val m = V3(k[0], k[1], k[2]); val r2 = k.last(); val (n, c) = plane(t[1])
                assertEquals(sqrt(r2 - Geo.distPointPlane(m, n, c).let { it * it }), l.solution.getValue("a")[0].toDouble(), 1e-9) }
            ProGen.level(ProGen.Kind.SKEW_DIST, seed).let { l -> val t = text(l); val g = nums(t[0]); val h = nums(t[1])
                val rel = Geo.lineLine(V3(g[0], g[1], g[2]), V3(g[3], g[4], g[5]), V3(h[0], h[1], h[2]), V3(h[3], h[4], h[5])); assertNull(rel.point); assertEquals(rel.distance, l.solution.getValue("a")[0].toDouble(), 1e-9) }
            ProGen.level(ProGen.Kind.MIRROR_LINE, seed).let { l -> val t = text(l); val p = nums(t[0]).let { V3(it[0], it[1], it[2]) }; val g = nums(t[1]); val a = V3(g[0], g[1], g[2]); val d = V3(g[3], g[4], g[5])
                val q = V3.of(l.solution.getValue("P")); assertTrue(Geo.onLine((p + q) * 0.5, a, d)); assertEquals(0.0, (q - p) dot d, 1e-9); assertFalse(p.near(q)) }
            ProGen.level(ProGen.Kind.PARALLEL_PARAM, seed).let { l -> val sol = l.solution.getValue("a")[0]; val t = l.formula(mapOf("a" to listOf(sol))).map { line -> line.joinToString("") { p -> when (p) { is Part.Txt -> p.s; is Part.Num -> p.s; is Part.Col -> p.cells.joinToString("|", "(", ")") } } }
                val g = nums(t[0]); val (n, c) = plane(t[1]); assertEquals(0.0, V3(g[3], g[4], g[5]) dot n, 1e-9); assertTrue(abs((V3(g[0], g[1], g[2]) dot n) - c) > 1e-9) }
        }
    }

    @Test fun themenUndStufen() {
        assertTrue(JamTopic.entries.all { Practice.available(it, null) }); assertTrue(JamLevel.entries.all { Practice.available(JamTopic.MIX, it) })
        assertFalse(Practice.available(JamTopic.SCHNITT, JamLevel.EINSTIEG)); assertFalse(Practice.available(JamTopic.BEWEGUNG, JamLevel.PROFI)); assertTrue(Practice.available(JamTopic.WINKEL, JamLevel.PROFI)); assertTrue(JamLevel.entries.all { Practice.available(JamTopic.PUNKTE, it) })
        // ohne Stufe bleibt alles wie bisher; mit Stufe kommen nur Aufgaben dieser Stufe
        assertTrue((0 until 200).map { Practice.level(JamTopic.MIX, null, 5, it).id.substringBefore('-') }.toSet().containsAll(listOf("jam", "mehr", "profi")))   // „Alle“ mischt jetzt wirklich alle Arten
        repeat(60) { n -> assertTrue(Practice.level(JamTopic.MIX, JamLevel.PROFI, 9, n).id.let { it.startsWith("profi-") || it.startsWith("mehr-mirror_twice") || it.startsWith("mehr-rhombus") })
            val easy = Practice.level(JamTopic.MIX, JamLevel.EINSTIEG, 9, n); assertTrue(easy.way.size <= 2)
            assertTrue(Practice.level(JamTopic.EBENEN, JamLevel.PROFI, 4, n).id.let { it.startsWith("profi-mirror_dist") || it.startsWith("profi-parallel_param") }) }
    }

    /** Die neuen Arten (Spiegeln, Parallelogramm, Raute, dieselbe Gerade, drei Punkte, offene Punkte) – unabhängig nachgerechnet. */
    @Test fun weitereArtenStimmen() {
        fun flip(p: V3, vararg a: Int) = V3.of(p.ints.mapIndexed { i, v -> if (i in a) -v else v })
        for (kind in MoreGen.Kind.entries) for (seed in 0L until 250) { val l = MoreGen.level(kind, seed); val id = "${kind.name}/$seed"
            assertTrue(id, l.evaluate(l.solution).success); assertFalse(id, l.evaluate(l.start).success); assertTrue(id, l.tip.isNotBlank() && l.way.size >= 2)
            l.controls.forEach { c -> (l.solution.getValue(c.key)).forEachIndexed { i, x -> assertTrue("$id außerhalb", x in c.min[i]..c.max[i]) }; c.start.forEachIndexed { i, x -> assertTrue("$id Start außerhalb", x in c.min[i]..c.max[i]) } }
            if (l.controls[0].kind == ControlKind.SCALAR) { val c = l.controls[0]; assertEquals(id, 1, (c.min[0]..c.max[0]).count { l.evaluate(mapOf(c.key to listOf(it))).success }) }
            for (ph in Phase.entries) { val o = l.objects(if (ph == Phase.DONE) l.solution else l.start, ph); assertEquals("$id doppelte Objekte", o.size, o.map { it.id }.toSet().size); assertTrue(l.scene(l.start, ph).isNotEmpty())
                o.filter { it.type == "point" }.forEach { p -> p.p!!.forEachIndexed { i, x -> assertTrue("$id/${p.id} außerhalb", x >= l.bounds.min[i] - 1e-9 && x <= l.bounds.max[i] + 1e-9) } } }
        }
        fun nums(s: String) = Regex("""−?\d+(,\d+)?""").findAll(s).map { it.value.replace('−', '-').replace(',', '.').toDouble() }.toList()
        fun text(l: Level, v: Values = l.solution) = l.formula(v).map { line -> line.joinToString("") { p -> when (p) { is Part.Txt -> p.s; is Part.Num -> p.s; is Part.Col -> p.cells.joinToString("|", "(", ")") } } }
        for (seed in 0L until 120) {
            // offene Punkte: viele Lösungen, der Ursprung nie, und die Bedingungen stimmen geometrisch (Abstand zur Achse nachgerechnet)
            MoreGen.level(MoreGen.Kind.OPEN_POINT, seed).let { l -> val ok = ArrayList<V3>()
                for (x in 0..8) for (y in 0..8) for (z in 0..6) { val p = V3(x, y, z); if (l.evaluate(mapOf("P" to p.ints)).success) ok += p }
                assertTrue("${l.title}: zu wenige Lösungen", ok.size >= 4); assertFalse(ok.any { it.isZero })
                fun axisDist(p: V3, a: Int) = Math.sqrt((0..2).filter { it != a }.sumOf { p.list[it] * p.list[it] })
                if (l.title != "Zwei Ebenen zugleich") assertTrue(l.title, ok.all { p -> (0..2).flatMap { a -> (0..2).filter { it > a }.map { b -> a to b } }.any { (a, b) -> abs(axisDist(p, a) - axisDist(p, b)) < 1e-9 } })
                else assertTrue(ok.all { p -> p.ints.count { it == 0 } == 2 }) }
            MoreGen.level(MoreGen.Kind.MIRROR_PLANE, seed).let { l -> val p = nums(text(l)[0]).let { V3(it[0], it[1], it[2]) }; val q = V3.of(l.solution.getValue("P")); assertEquals(1, (0..2).count { p.list[it] != q.list[it] }); assertEquals(p.length, q.length, 1e-9) }
            MoreGen.level(MoreGen.Kind.MIRROR_AXIS, seed).let { l -> val p = nums(text(l)[0]).let { V3(it[0], it[1], it[2]) }; val q = V3.of(l.solution.getValue("P")); assertEquals(2, (0..2).count { p.list[it] == -q.list[it] && p.list[it] != q.list[it] }) }
            MoreGen.level(MoreGen.Kind.MIRROR_TWICE, seed).let { l -> val p = nums(text(l)[0]).let { V3(it[0], it[1], it[2]) }; val q = V3.of(l.solution.getValue("P")); assertEquals(2, (0..2).count { p.list[it] != q.list[it] }); assertEquals(p.length, q.length, 1e-9) }
            MoreGen.level(MoreGen.Kind.PARALLELOGRAM, seed).let { l -> val t = text(l); val ab = nums(t[0]); val cd = nums(t[1]); val a = V3(ab[0], ab[1], ab[2]); val b = V3(ab[3], ab[4], ab[5]); val c = V3(cd[0], cd[1], cd[2]); val d = V3(cd[3], cd[4], cd[5])
                assertTrue((b - a).near(c - d)); val m = V3.of(l.solution.getValue("P")); assertTrue(m.near((a + c) * 0.5)); assertTrue(m.near((b + d) * 0.5)) }
            MoreGen.level(MoreGen.Kind.COLLINEAR, seed).let { l -> val t = text(l); val ab = nums(t[0]); val c = nums(t[1]).let { V3(it[0], it[1], it[2]) }; val a = V3(ab[0], ab[1], ab[2]); val b = V3(ab[3], ab[4], ab[5]); assertTrue(Geo.onLine(c, a, b - a)) }
            MoreGen.level(MoreGen.Kind.SAME_LINE, seed).let { l -> val t = text(l); val g = nums(t[0]); val h = nums(t[1]); val a = V3(g[0], g[1], g[2]); val d = V3(g[3], g[4], g[5])
                assertTrue(Geo.parallel(d, V3(h[3], h[4], h[5]))); assertTrue(Geo.onLine(V3(h[0], h[1], h[2]), a, d)) }
            MoreGen.level(MoreGen.Kind.RHOMBUS, seed).let { l -> val t = text(l); val ab = nums(t[0]); val dd = nums(t[1]).let { V3(it[0], it[1], it[2]) }; val a = V3(ab[0], ab[1], ab[2]); val b = V3(ab[3], ab[4], ab[5])
                assertEquals((b - a).length, (dd - a).length, 1e-9); assertTrue(dd.y > a.y) }
        }
    }
}
