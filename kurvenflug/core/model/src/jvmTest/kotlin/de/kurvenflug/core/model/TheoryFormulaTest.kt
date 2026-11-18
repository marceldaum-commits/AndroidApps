package de.kurvenflug.core.model

import org.junit.Assert.assertTrue
import org.junit.Test

/** Theorie-Prüfung (Marcel 18.11.): Jedes Level zeigt die Formel, die man braucht, mit Beispiel – und keine fremde. */
class TheoryFormulaTest {
    private val levels = Levels.all + Levels.chapters.mapNotNull { ChapterExtreme.forChapter(it.number) }

    @Test fun everyLevelHasFormulaAndExample() {
        levels.forEach { l ->
            val t = Theories.of(l, Levels.chapters.firstOrNull { it.number == l.chapter })
            assertTrue("${l.id}: keine Theorie", t != null)
            assertTrue("${l.id}: keine Merkformel", !t!!.formula.isNullOrBlank())
            assertTrue("${l.id}: kein Beispiel", !t.example.isNullOrBlank())
        }
        Levels.chapters.forEach { c -> assertTrue("Kapitel ${c.number} ohne eigene Merkformel", c.number in TheoryFormulas.chapters) }
    }

    @Test fun missionsOutsideChaptersToo() {
        val missions = ReadTexts.fixedLevels() + (0..60).map { Daily.forDay(20_400L + it) } +
            Gen.kinds.flatMap { k -> (0..Tier.MAX).map { t -> Gen.level(k, 977L + t, t) } }
        val missing = missions.filter { l -> Theories.of(l, Levels.chapters.firstOrNull { it.number == l.chapter })?.example.isNullOrBlank() }
        assertTrue("ohne Theorie/Beispiel: " + missing.map { it.id + " " + it.kind }.distinct().take(20), missing.isEmpty())
    }

    @Test fun everyAdjustableValueIsInTheFormula() {
        // Reglernamen → wie sie in der Formel heißen
        val names = mapOf("angle" to "α", "s0" to "s₀", "v0" to "v₀", "v1" to "v", "v2" to "v", "A" to "F(b)")
        levels.filter { it.chapter in TheoryFormulas.chapters }.forEach { l ->
            val f = Theories.of(l, Levels.chapters.first { it.number == l.chapter })!!.formula!!
            l.params.forEach { p ->
                val n = names[p.key] ?: p.key
                assertTrue("${l.id}: Regler ${p.key} fehlt in „$f“", n in f || l.kind in setOf(Kind.POINT, Kind.RENDEZVOUS, Kind.ORBIT, Kind.LASER, Kind.PEAK))
            }
        }
    }

    @Test fun noForeignFormula() {
        // Winkel, Lichtbrechung, Trigonometrie dürfen nicht das Reflexionsgesetz zeigen; waagerechter Wurf nicht die Schrägwurf-Weite
        levels.filter { it.chapter in setOf(14, 16, 17) }.forEach { l ->
            val f = Theories.of(l, Levels.chapters.first { it.number == l.chapter })!!.formula!!
            assertTrue("${l.id}: Reflexionsgesetz statt passender Formel", "Einfallswinkel = Ausfallswinkel" !in f)
        }
        levels.filter { it.chapter == 27 }.forEach { l -> assertTrue("sin(2α)" !in Theories.of(l, Levels.chapters.first { it.number == 27 })!!.formula!!) }
        levels.filter { it.chapter == 28 }.forEach { l -> assertTrue("e^(k·x)" in Theories.of(l, Levels.chapters.first { it.number == 28 })!!.formula!!) }
        levels.filter { it.kind == Kind.EXP }.forEach { l -> assertTrue("bˣ" in Theories.of(l, Levels.chapters.firstOrNull { it.number == l.chapter })!!.formula!!) }
    }
}
