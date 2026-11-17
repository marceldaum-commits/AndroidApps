package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Schreibweise in allen Aufgabentexten – auch in den erzeugten Übungsaufgaben mit vielen verschiedenen Zahlen (Korrektheitsprüfung 17.11.2026).
 * Jede Regel hier war einmal ein echter Fund: „+ −3 ·“, „n⃗ · M = -8“, „1 Einheiten“, „liegt auf g und liegt auf h noch nicht“, gerundete Werte mit „=“.
 */
class PruefTextTest {
    private fun flat(rows: List<List<Part>>) = rows.joinToString(" ⏎ ") { r -> r.joinToString("") { p -> when (p) { is Part.Txt -> p.s; is Part.Col -> "(" + p.cells.joinToString("|") + ")"; is Part.Num -> p.s } } }
    private fun wild(l: Level, r: kotlin.random.Random): Values = l.controls.associate { c -> c.key to c.start.indices.map { i -> r.nextInt(c.min[i], c.max[i] + 1) } }
    private val all: List<Level> by lazy {
        Levels.all + Exams.all.flatMap { e -> e.parts.map { it.level } } + Guides.all.flatMap { g -> g.parts.map { it.level } } +
            Gen.Kind.entries.flatMap { k -> (1L..40L).mapNotNull { runCatching { Gen.level(k, it) }.getOrNull() } } +
            MoreGen.Kind.entries.flatMap { k -> (1L..40L).map { MoreGen.level(k, it) } } + ProGen.Kind.entries.flatMap { k -> (1L..40L).map { ProGen.level(k, it) } }
    }
    private fun texts(l: Level): List<String> { val r = kotlin.random.Random(l.id.hashCode())
        return listOf(l.title, l.mission, l.tip) + l.way + flat(l.formula(l.start)) + flat(l.formula(l.solution)) + l.evaluate(l.solution).message + l.evaluate(l.start).message +
            (1..25).map { val v = wild(l, r); l.evaluate(v).message } + (1..5).map { flat(l.formula(wild(l, r))) } + (l.finale?.invoke(l.solution)?.acts?.map { it.say } ?: emptyList()) }

    private val rules = listOf(
        "Bindestrich statt Minuszeichen vor einer Zahl" to Regex("""(?<![A-Za-zÄÖÜäöüß₀-₉0-9])-\d"""),
        "zwei Rechenzeichen hintereinander (negative Zahl ohne Klammer)" to Regex("""[\d)a-z|] [+·:] −\d|− −"""),
        "Einzahl/Mehrzahl" to Regex("""(?<![\d,])1 (Einheiten|Sekunden|Stunden|Versuchen|Punkte|Aufgaben|Schritte)"""),
        "leere Formelzeile" to Regex("""⏎ *$|⏎ *⏎"""),
        "Dezimalpunkt" to Regex("""\d\.\d"""),
        "missverständliches „und liegt … noch nicht“" to Regex("""und liegt (?!nicht)|aber liegt"""),
        "Rechner-Schreibweise" to Regex("""\dE\d|NaN|Infinity|(?<=[ (|=])−0(?![,\d])"""),
    )

    @Test fun schreibweiseInAllenAufgaben() {
        assertTrue(all.size > 1300)
        val found = all.flatMap { l -> texts(l).flatMap { t -> rules.filter { (_, re) -> re.containsMatchIn(t) }.map { "${l.id}: ${it.first}: „$t“" } } }
        assertEquals("Schreibweise:\n" + found.take(25).joinToString("\n"), 0, found.size)
    }

    /** Wer eine Lösung einstellt, bekommt recht – und die Start-Einstellung ist nie schon die Lösung */
    @Test fun loesungWirdAngenommenStartNicht() = all.forEach { l ->
        assertTrue("${l.id}: Lösung wird nicht angenommen", l.evaluate(l.solution).success); assertFalse("${l.id}: Start ist schon richtig", l.evaluate(l.start).success) }
}
