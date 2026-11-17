package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class NotationTest {
    @Test fun strengeSchreibweise() {
        fun t(a: String, b: String) = assertEquals(b, Notation.strict(a))
        t("v⃗ = B − A", "v⃗ = OB⃗ − OA⃗")
        t("M = ½ · (A + B)", "OM⃗ = ½ · (OA⃗ + OB⃗)")
        t("OS⃗ = ⅓ · (OA⃗ + OB⃗ + OC⃗)", "OS⃗ = ⅓ · (OA⃗ + OB⃗ + OC⃗)")
        t("Schwerpunkt Dreieck: S = ⅓ · (A + B + C)", "Schwerpunkt Dreieck: OS⃗ = ⅓ · (OA⃗ + OB⃗ + OC⃗)")
        t("Parameterform: g: x⃗ = A + t · d⃗", "Parameterform: g: x⃗ = OA⃗ + t · d⃗")
        t("P′ = 2 · F − P = (4|6|4) − (4|2|1) = (0|4|3)", "OP′⃗ = 2 · OF⃗ − OP⃗ = (4|6|4) − (4|2|1) = (0|4|3)")
        t("Lotfußpunkt auf Gerade: (P − A − t · d⃗) · d⃗ = 0", "Lotfußpunkt auf Gerade: (OP⃗ − OA⃗ − t · d⃗) · d⃗ = 0")
        t("D = A + BC⃗", "OD⃗ = OA⃗ + BC⃗")
        t("A + r · v⃗ = Z", "OA⃗ + r · v⃗ = OZ⃗")
        t("S = (1|1|1) + 2 · (2|1|1) = (5|3|3)", "OS⃗ = (1|1|1) + 2 · (2|1|1) = (5|3|3)")
        // bleibt, wie es ist: Koordinaten, Funktionen, Zahlen-Größen, Wahrscheinlichkeiten, Ebenen
        listOf("M(4|2|2)", "F(3) − F(0) = 9", "V = ⅓ · 16 · 3 = 16", "A = |a⃗ × b⃗| = 9", "P(X ≤ 1) = P(X = 0) + P(X = 1)", "E: x₁ + 2x₂ + 2x₃ = 9", "A(x) = 8x − 2x²", "d = |7 − 16| : 3 = 3",
            "Abstand P–E = |12 − 18| : 3 = 2", "n⃗ · P = 12, |n⃗| = 3", "F = ½ · 18 = 9", "x⃗(3) = (1|1|0) + 3 · (1|2|1)").forEach { t(it, it) }
    }

    /** Analysis und Stochastik kennen keine Ortsvektoren: Dort darf die Umstellung nichts verändern. Für die Geometrie entsteht eine Liste zum Nachlesen. */
    @Test fun nurGeometrieWirdUmgeschrieben() {
        val changed = LinkedHashSet<String>()
        fun texts(l: Level) = listOf(l.mission, l.tip) + l.way + l.formula(l.start).flatten().filterIsInstance<Part.Txt>().map { it.s } + l.evaluate(l.solution).message + l.evaluate(l.start).message
        Levels.chapters.forEach { c -> c.levels.forEach { l -> texts(l).forEach { t -> val s = Notation.strict(t)
            if (s != t) { assertEquals("${l.id}: „$t“ → „$s“", Area.GEOMETRIE, c.area); changed += "${l.id}\t$t\t$s" } } } }
        (Exams.all.flatMap { e -> e.parts.map { it.level } } + Gen.Kind.entries.flatMap { k -> (1L..12L).mapNotNull { runCatching { Gen.level(k, it) }.getOrNull() } } +
            MoreGen.Kind.entries.flatMap { k -> (1L..12L).map { MoreGen.level(k, it) } } + ProGen.Kind.entries.flatMap { k -> (1L..12L).map { ProGen.level(k, it) } }).forEach { l ->
            texts(l).forEach { t -> val s = Notation.strict(t); if (s != t) changed += "${l.id}\t$t\t$s" }
            // dieselbe Aufgabe in strenger Schreibweise wird genauso gelöst
            assertTrue(l.id, l.withText(Notation::strict).evaluate(l.solution).success) }
        Sheet.cards.forEach { c -> c.lines.forEach { t -> val s = Notation.strict(t); if (s != t) changed += "Formelsammlung\t$t\t$s" } }
        File("build").mkdirs(); File("build/schreibweise.tsv").writeText(changed.joinToString("\n"))
        assertTrue(changed.size > 40)
    }
}
