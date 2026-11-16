package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Die mitgelieferte Lautschrift muss alles abdecken, was Lux in der App sagt – sonst spränge mitten im Spiel die Handy-Stimme ein. */
class LautschriftTest {
    private val dir = File("../../app/src/main/assets/stimme")
    private val l = Lautschrift(File(dir, "saetze.tsv").readLines().asSequence(), File(dir, "woerter.tsv").readLines().asSequence())

    @Test fun festeTexteStehenAlsGanzeSaetzeDrin() {
        Levels.chapters.forEach { c -> (c.intro + c.levels.flatMap { listOf(it.mission, it.tip, it.evaluate(it.solution).message) }).forEach { t ->
            val clean = Speech.clean(t); if (clean.isNotBlank()) assertNotNull("Lautschrift fehlt (tools/stimme/lautschrift.py neu laufen lassen): $clean", l.of(clean)) } }
    }
    @Test fun veraenderlicheTexteSindSprechbar() {
        val r = kotlin.random.Random(99); val gaps = LinkedHashSet<String>(); var n = 0
        fun check(t: String) { n++; gaps += l.missing(Speech.clean(t)) }
        Levels.all.forEach { lv -> repeat(25) { runCatching { check(lv.evaluate(lv.controls.associate { c -> c.key to c.start.indices.map { i -> r.nextInt(c.min[i], c.max[i] + 1) } }).message) } } }
        Gen.Kind.entries.forEach { k -> for (seed in 100L..130L) runCatching { val g = Gen.level(k, seed); check(g.mission); check(g.tip); check(g.evaluate(g.solution).message) } }
        for (s in 0..100) check(Ghost.verdict(s))
        SpotKind.entries.forEach { check("${it.title} bei x = ${Geo.num(-2.5)}"); check(it.title) }
        assertTrue(n > 2000); assertEquals("Wörter ohne Lautschrift", emptySet<String>(), gaps)
    }
    /** Schreibt die Laute einiger Beispieltexte nach build/hoerprobe.txt – daraus lässt sich am Entwicklungsrechner eine Hörprobe erzeugen. */
    @Test fun hoerprobe() {
        val texts = listOf(Levels.byId("1-1")!!.mission, Levels.byId("1-1")!!.let { it.evaluate(it.solution).message }, Levels.byId("5-3")!!.tip,
            "Hochpunkt bei x = 2,5", Ghost.verdict(82), "Fast. Gesucht ist der Punkt (4|−2|0).", "Das sind 1250 Punkte!")
        File("build").mkdirs(); File("build/hoerprobe.txt").writeText(texts.joinToString("\n") { t -> Speech.clean(t) + "\t" + (l.of(Speech.clean(t)) ?: error("ohne Lautschrift: ${l.missing(Speech.clean(t))} in „${Speech.clean(t)}“")).joinToString("|") })
    }
    @Test fun zahlenUndSatzgrenzen() {
        assertEquals(2, l.of("Genau 25 Prozent. Treffer!")!!.size)
        assertNotNull(l.of("Das sind 1250 Punkte bei 2,75 Sekunden."))
        assertNull(l.of("Quarkstrudelxyz ist kein Punkt."))
        assertEquals(listOf("Quarkstrudelxyz"), l.missing("Quarkstrudelxyz ist kein Punkt."))
        assertTrue(Lautschrift.codepoints("aɪ").contentEquals(intArrayOf(0x61, 0x26A)))
    }
}
