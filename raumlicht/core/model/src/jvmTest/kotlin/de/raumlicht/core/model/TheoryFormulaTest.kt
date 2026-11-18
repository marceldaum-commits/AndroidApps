package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

/** Jede Theorie-Karte zeigt die Formel, die man für ihre Aufgaben braucht – mit Erklärung der Größen und einem durchgerechneten Beispiel. */
class TheoryFormulaTest {
    @Test fun jedeKarteHatFormelBedeutungUndBeispiel() {
        for (c in Theory.cards) {
            val f = c.formula; assertNotNull("Karte ${c.key} hat keine Formel", f)
            assertTrue(c.key, f!!.lines.isNotEmpty() && f.lines.all { it.length in 5..110 }); assertTrue("${c.key}: Bedeutung der Größen fehlt", f.meaning.length >= 40)
            assertTrue("${c.key}: Beispiel fehlt", c.example.size >= 2); assertTrue("${c.key}: Schritte fehlen", c.steps.size >= 2)
        }
        assertEquals("keine Formel ohne Karte", emptySet<String>(), TheoryFormulas.keys - Theory.cards.map { it.key }.toSet())
    }

    /** Jede Aufgabe der App – feste, Prüfungsteile, Übungs- und Tagesaufgaben – führt zu einer Karte mit Formel (nie zur allgemeinen Karte). */
    @Test fun jedeAufgabeFuehrtZuEinerKarteMitFormel() {
        val levels = Levels.all + Exams.all.flatMap { e -> e.parts.map { it.level } } +
            JamTopic.entries.flatMap { t -> (listOf<JamLevel?>(null) + JamLevel.entries).flatMap { s -> (0 until 40).mapNotNull { n -> runCatching { Practice.level(t, s, 7L, n) }.getOrNull() } } } +
            (20_000L until 20_120L).map { Daily.forDay(it) }
        assertTrue(levels.size > 400)
        for (l in levels) { assertTrue("${l.id} „${l.title}“ fällt auf die allgemeine Karte", Theory.stage(l) <= 3); assertNotNull("${l.id}: Karte ${Theory.forLevel(l).key} ohne Formel", Theory.forLevel(l).formula) }
    }

    /** Die Formeln, um die es Marcel ging: Sie stehen wörtlich da. */
    @Test fun winkelUndAbstandsFormelnStehenDa() {
        fun lines(key: String) = TheoryFormulas.of(key)!!.lines.joinToString(" ")
        assertTrue(lines("winkel").contains("cos α = (a⃗ · b⃗) : (|a⃗| · |b⃗|)")); assertTrue(lines("winkel-geraden").contains("cos α = |u⃗ · v⃗| : (|u⃗| · |v⃗|)"))
        assertTrue(lines("winkel-ebenen").contains("cos α = |n⃗₁ · n⃗₂|")); assertTrue(lines("winkel-gerade-ebene").contains("sin α = |n⃗ · d⃗|"))
        assertTrue(lines("abstand-punkt-ebene").contains(": |n⃗|")); assertTrue(lines("hesse").contains(": |n⃗|")); assertTrue(lines("abstand-windschief").contains("|(B − A) · n⃗| : |n⃗|"))
        assertTrue(lines("abstand-parallele-geraden").contains("|(B − A) × u⃗| : |u⃗|")); assertTrue(lines("abstand-parallel").contains("|c₂ − c₁| : |n⃗|"))
        // die Aufgaben zum Winkel zwischen Vektoren zeigen genau diese Karte – Ebenen und Geraden haben eigene
        for (id in listOf("7-1", "7-2")) assertEquals("winkel", Theory.forLevel(Levels.byId(id)!!).key)
        for (id in listOf("7-4", "19-1")) assertEquals("winkel-ebenen", Theory.forLevel(Levels.byId(id)!!).key)
        for (id in listOf("8-3", "19-5")) assertEquals("abstand-parallel", Theory.forLevel(Levels.byId(id)!!).key)
    }

    /** Die durchgerechneten Beispiele der neuen Karten stimmen (nachgerechnet). */
    @Test fun beispieleDerNeuenKartenStimmen() {
        fun cross(a: List<Int>, b: List<Int>) = listOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
        fun dot(a: List<Int>, b: List<Int>) = a.zip(b).sumOf { it.first * it.second }
        assertEquals(3, (dot(listOf(2, 1, -2), listOf(5, 3, -1)) - 6) / 3)                                   // HNF: P
        assertEquals(0, dot(listOf(2, 0, 1), listOf(1, 2, -2))); assertEquals(3, kotlin.math.abs(dot(listOf(1, 2, -2), listOf(1, 4, 2)) - 14) / 3)   // Gerade ∥ Ebene
        assertEquals(listOf(12, -6, -9), cross(listOf(3, 6, 0), listOf(2, 1, 2))); assertEquals(261, 144 + 36 + 81); assertEquals(29 * 9, 261)      // parallele Geraden
        assertEquals(-1, dot(listOf(1, 0, 1), listOf(0, -1, -1))); assertEquals(1, dot(listOf(1, 1, 0), listOf(0, 1, 1)))                          // Winkel Geraden, Ebenen
    }
}
