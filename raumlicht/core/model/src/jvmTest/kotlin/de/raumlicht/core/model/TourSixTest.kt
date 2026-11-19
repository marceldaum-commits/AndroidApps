package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/** Rechnet die Lösungen der Kapitel 18–21 unabhängig von den Aufgaben-Texten nach. */
class TourSixTest {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun l(id: String) = Levels.byId(id)!!
    private fun vec(id: String, key: String) = V3.of(l(id).solution.getValue(key))
    private fun int(id: String, key: String = "a") = l(id).solution.getValue(key)[0]
    private fun close(a: Double, b: Double) = assertEquals(b, a, 1e-9)

    @Test fun ebenenformen() {
        // 18-1: Normalenvektor = Kreuzprodukt der Spannvektoren, alle drei Punkte erfüllen die Gleichung
        val n1 = vec("18-1", "n"); assertTrue(Geo.parallel(n1, (p(0, 3, 0) - p(2, 0, 0)) cross (p(0, 0, 1) - p(2, 0, 0))))
        listOf(p(2, 0, 0), p(0, 3, 0), p(0, 0, 1)).forEach { close(n1 dot it, int("18-1", "c").toDouble()) }
        // 18-2: c = n · P
        assertEquals((p(2, 1, 2) dot p(1, 2, 3)).toInt(), int("18-2", "c")); assertTrue(Geo.parallel(vec("18-2", "n"), p(2, 1, 2)))
        // 18-3: Spurpunkt liegt auf der x₁-Achse und in E
        vec("18-3", "P").let { close(p(3, 4, 6) dot it, 12.0); close(it.y, 0.0); close(it.z, 0.0) }
        // 18-4: alle drei Achsenabschnitte
        listOf(p(6, 0, 0), p(0, 3, 0), p(0, 0, 2)).forEach { close(vec("18-4", "n") dot it, int("18-4", "c").toDouble()) }
        // 18-5: P(2|a|1) in E
        close(p(2, -1, 2) dot p(2, int("18-5"), 1), 3.0)
        // 18-6: Ebene enthält g und P
        val n6 = vec("18-6", "n"); val c6 = int("18-6", "c").toDouble()
        close(n6 dot p(1, 1, 0), 0.0); close(n6 dot p(1, 0, 2), c6); close(n6 dot p(3, 0, 1), c6)
        // jede Ebenen-Aufgabe nimmt auch Vielfache an, aber keine falsche Ebene
        assertTrue(l("18-1").evaluate(mapOf("n" to listOf(-3, -2, -6), "c" to listOf(-6))).success)
        assertFalse(l("18-1").evaluate(mapOf("n" to listOf(3, 2, 6), "c" to listOf(7))).success)
        assertFalse(l("18-2").evaluate(mapOf("n" to listOf(0, 0, 1), "c" to listOf(3))).success)     // P liegt drin, aber falscher Normalenvektor
    }

    @Test fun schnitteUndWinkel() {
        close(Geo.angle(p(1, 1, 0), p(1, 0, 0)), 45.0); assertEquals(3, int("19-1"))
        close(90 - Geo.angle(p(1, 0, 1), p(1, 1, 0)), 30.0); assertEquals(2, int("19-2"))
        close(p(1, 1, 0) dot p(5, 1, 5), 6.0)                                               // Treffpunkt des Lasers liegt in E
        // 19-3: Richtung senkrecht zu beiden Normalen, Stützpunkt in beiden Ebenen
        val d = vec("19-3", "d"); close(d dot p(1, 1, 1), 0.0); close(d dot p(1, -1, 1), 0.0)
        close(p(1, 1, 1) dot p(4, 2, 0), 6.0); close(p(1, -1, 1) dot p(4, 2, 0), 2.0)
        assertTrue(l("19-3").evaluate(mapOf("d" to listOf(-2, 0, 2))).success)
        // 19-4: genau ein a macht g parallel, und A liegt nicht in E
        assertEquals(listOf(int("19-4")), (-3..5).filter { abs(p(2, it, -3) dot p(2, 1, 2)) < 1e-9 }); assertNotEquals(12.0, p(2, 1, 2) dot p(1, 2, 5), 1e-9)
        // 19-5
        close(p(1, 0, -1) dot p(2, 1, 2), 0.0); close(Geo.distPointPlane(p(1, 2, 1), p(2, 1, 2), 18.0), int("19-5").toDouble())
        // 19-6
        vec("19-6", "P").let { close(it.z, 0.0); close(p(1, 1, 1) dot it, 6.0); close(p(1, -1, 1) dot it, 2.0) }
    }

    @Test fun spiegelnUndLote() {
        assertTrue(vec("20-1", "P").near(p(3, 3, 2) * 2 - p(1, 2, 1)))
        // 20-2: Mitte von P und P′ liegt auf g, Verbindung steht senkrecht auf g
        val q = p(4, 2, 1); val r = vec("20-2", "P"); assertTrue(Geo.onLine((q + r) * 0.5, p(0, 1, 0), p(1, 1, 1))); close((r - q) dot p(1, 1, 1), 0.0)
        // 20-3: Z hat zu beiden Ebenen denselben Abstand
        close(Geo.distPointPlane(p(2, 2, 2), p(1, 2, 2), 6.0), Geo.distPointPlane(p(2, 2, 2), p(1, 2, 2), int("20-3").toDouble())); assertNotEquals(6, int("20-3"))
        // 20-4: im erlaubten Bereich hat nur t = 5 den Abstand 3
        assertEquals(listOf(5), (0..6).filter { abs(Geo.distPointPlane(p(1, 1, 0) + p(1, 0, 1) * it, p(1, 2, 2), 9.0) - 3) < 1e-9 }); assertEquals(5, l("20-4").solution.values.first()[0])
        // 20-5: Abstand windschiefer Geraden
        val rel = Geo.lineLine(p(2, 4, 1), p(2, -1, 0), p(4, 7, 6), p(0, 1, -1)); assertNull(rel.point); close(rel.distance, int("20-5").toDouble())
        // 20-6
        close(2 * Geo.distPointPlane(p(2, 2, 0), p(2, 1, 2), 18.0), int("20-6").toDouble())
    }

    @Test fun kugelUndEbene() {
        close(Geo.distPointPlane(p(3, 3, 2), p(1, 2, 2), 22.0), int("21-1").toDouble())
        vec("21-2", "P").let { close((it - p(3, 3, 2)).length, 3.0); close(p(1, 2, 2) dot it, 22.0) }
        assertTrue(vec("21-3", "P").near(Geo.footOnPlane(p(5, 5, 4), p(1, 2, 2), 14.0)))
        close(sqrt(13.0 - (p(5, 5, 4) - p(4, 3, 2)).let { it dot it }), int("21-4").toDouble())
        close((p(6, 4, 6) - p(2, 2, 2)).length, 3.0 + int("21-5"))
        // 21-6: Zeiten, zu denen die Drohne genau auf der Kugel ist
        val on = (0..8).filter { abs((p(1, 1, 4) + p(1, 1, 0) * it - p(4, 4, 3)).length - 3) < 1e-9 }; assertEquals(listOf(1, 5), on); assertEquals(on[1] - on[0], int("21-6"))
        // zu große Kugeln werden mit eigenem Hinweis abgelehnt
        assertTrue(l("21-1").evaluate(mapOf("a" to listOf(4))).message.startsWith("Zu groß"))
    }

    private fun integrate(a: Double, b: Double, f: (Double) -> Double): Double { val n = 20000; val h = (b - a) / n; return (0 until n).sumOf { f(a + (it + 0.5) * h) } * h }
    private fun binom(n: Int, k: Int): Double { var c = 1.0; for (i in 1..k) c = c * (n - k + i) / i; return c }
    private fun atLeast(n: Int, k: Int, p: Double) = (k..n).sumOf { binom(n, it) * Math.pow(p, it.toDouble()) * Math.pow(1 - p, (n - it).toDouble()) }



    @Test fun neuePruefungsaufgaben() {
        val dome = Exams.all.first { it.id == "planetarium" }.parts.map { it.level }
        val m = p(3, 3, 2); val n = p(1, 2, 2)
        close(Geo.distPointPlane(m, n, 22.0), dome[0].solution.getValue("a")[0].toDouble())
        assertTrue(V3.of(dome[1].solution.getValue("P")).near(Geo.footOnPlane(m, n, 22.0)))
        assertTrue(V3.of(dome[2].solution.getValue("P")).near(Geo.mirrorAtPlane(m, n, 22.0)))
        close(sqrt(25.0 - 9.0), dome[3].solution.getValue("a")[0].toDouble())
        dome.forEach { lv -> assertTrue(lv.id, lv.evaluate(lv.solution).success); assertFalse(lv.id, lv.evaluate(lv.start).success) }
    }



    /** Jede Zahlen-Aufgabe hat genau eine richtige Antwort im einstellbaren Bereich. */
    @Test fun genauEineLoesung() = (TourSix.chapters + TourSeven.chapters + TourEight.chapters).filter { it in Levels.chapters }.flatMap { it.levels }.filter { it.controls.size == 1 && it.controls[0].kind == ControlKind.SCALAR }.forEach { lv ->
        val c = lv.controls[0]; val ok = (c.min[0]..c.max[0]).filter { lv.evaluate(mapOf(c.key to listOf(it))).success }
        assertEquals("${lv.id}: richtige Antworten $ok", 1, ok.size)
    }
}

/** Schreibt die Bühnen aller neuen Aufgaben (vorher und gelöst) nach build/neu.json – zum Ansehen mit scene/test/shot.mjs. */
class NewScenesDump {
    @Test fun dump() {
        val shots = (TourSix.chapters + TourSeven.chapters + TourEight.chapters).filter { it in Levels.chapters }.flatMap { it.levels }.flatMap { l ->
            listOf("""{"name":"${l.id}-a","settle":1.2,"state":${l.scene(l.start, Phase.AIM)}}""", """{"name":"${l.id}-z","settle":1.2,"state":${l.scene(l.solution, Phase.DONE)}}""") }
        val profi = (ProGen.Kind.entries.flatMap { k -> listOf(3L, 8L).map { ProGen.level(k, it) } } + MoreGen.Kind.entries.flatMap { k -> listOf(2L, 7L).map { MoreGen.level(k, it) } }).flatMap { l ->
            listOf("""{"name":"${l.id}-a","settle":1.2,"state":${l.scene(l.start, Phase.AIM)}}""", """{"name":"${l.id}-z","settle":1.2,"state":${l.scene(l.solution, Phase.DONE)}}""") }
        java.io.File("build").mkdirs(); java.io.File("build/neu.json").writeText("[" + (shots + profi).joinToString(",") + "]")
    }
}
