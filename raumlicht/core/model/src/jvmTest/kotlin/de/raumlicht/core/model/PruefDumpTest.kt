package de.raumlicht.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

/**
 * Schreibt alle Aufgaben mit Text, Reglern, Lösung, Rechenweg und Bühne nach build/pruefung/ – als Grundlage für eine unabhängige Nachrechnung von außen
 * (Korrektheitsprüfung). Zählt außerdem durch Ausprobieren aller einstellbaren Werte, wie viele Antworten die Aufgabe als richtig annimmt.
 */
class PruefDumpTest {
    private fun q(s: String) = Json.encodeToString(s)
    private fun flat(rows: List<List<Part>>) = rows.joinToString(" ⏎ ") { r -> r.joinToString("") { p -> when (p) { is Part.Txt -> p.s; is Part.Col -> "(" + p.cells.joinToString("|") + ")"; is Part.Num -> p.s } } }
    private fun objs(o: List<Obj>) = Json.encodeToString(o.filter { it.type !in setOf("podium") })
    private fun accepted(l: Level): Pair<Long, List<Values>> {
        val dims = l.controls.flatMap { c -> c.start.indices.map { i -> Triple(c.key, i, c.min[i]..c.max[i]) } }
        val total = dims.fold(1L) { a, d -> a * (d.third.last - d.third.first + 1) }
        if (total > 400_000) return -1L to emptyList()
        var n = 0L; val others = ArrayList<Values>()
        val cur = IntArray(dims.size) { dims[it].third.first }
        while (true) {
            val v: Values = l.controls.associate { c -> c.key to c.start.indices.map { i -> cur[dims.indexOfFirst { it.first == c.key && it.second == i }] } }
            if (runCatching { l.evaluate(v).success }.getOrDefault(false)) { n++; if (v != l.solution && others.size < 6) others += v }
            var k = 0
            while (k < dims.size) { if (cur[k] < dims[k].third.last) { cur[k]++; break } else { cur[k] = dims[k].third.first; k++ } }
            if (k == dims.size) break
        }
        return n to others
    }
    private fun dump(l: Level, group: String, count: Boolean = true): String {
        val out = l.evaluate(l.solution); val (n, others) = if (count) accepted(l) else -2L to emptyList()
        val wrong = runCatching { l.evaluate(l.start) }.getOrNull()
        return buildString {
            append("{\"gruppe\":${q(group)},\"id\":${q(l.id)},\"kapitel\":${l.chapter},\"titel\":${q(l.title)},\"auftrag\":${q(l.mission)},\"tipp\":${q(l.tip)},\"rechenweg\":${Json.encodeToString(l.way)},")
            append("\"regler\":[" + l.controls.joinToString(",") { c -> "{\"key\":${q(c.key)},\"name\":${q(c.name)},\"art\":\"${c.kind}\",\"start\":${c.start},\"min\":${c.min},\"max\":${c.max},\"schritt\":${c.step},\"einheit\":${q(c.suffix)}}" } + "],")
            append("\"loesung\":${Json.encodeToString(l.solution)},\"loesung_angenommen\":${out.success},\"meldung_richtig\":${q(out.message)},\"meldung_start\":${q(wrong?.message ?: "")},\"start_angenommen\":${wrong?.success},")
            append("\"formel_start\":${q(flat(l.formula(l.start)))},\"formel_loesung\":${q(flat(l.formula(l.solution)))},")
            append("\"angenommene_antworten\":$n,\"weitere_angenommene\":${Json.encodeToString(others)},")
            append("\"finale\":${Json.encodeToString(l.finale?.invoke(l.solution)?.acts?.map { it.say } ?: emptyList())},")
            append("\"buehne_start\":${objs(l.objects(l.start, Phase.AIM))},\"buehne_fertig\":${objs(l.objects(l.solution, Phase.DONE))}}")
        }
    }
    @Test fun dump() {
        val dir = File("build/pruefung").apply { mkdirs() }
        fun write(name: String, rows: List<String>) = File(dir, name).writeText("[\n" + rows.joinToString(",\n") + "\n]\n")
        Levels.chapters.forEach { c -> write("kapitel-${c.number.toString().padStart(2, '0')}.json",
            listOf("{\"kapitel\":${c.number},\"titel\":${q(c.title)},\"thema\":${q(c.topic)},\"kulisse\":${q(c.venue)},\"gebiet\":\"${c.area}\",\"einfuehrung\":${Json.encodeToString(c.intro)}}") + c.levels.map { dump(it, "tour") }) }
        write("pruefungen.json", Exams.all.flatMap { e -> listOf("{\"pruefung\":${q(e.id)},\"titel\":${q(e.title)},\"einfuehrung\":${q(e.intro)},\"punkte\":${e.parts.map { it.points }}}") + e.parts.map { dump(it.level, "abi ${e.id} ${it.label} (${it.points} P)") } })
        write("schritte.json", Guides.all.flatMap { g -> g.parts.map { dump(it.level, "schritte") } })
        Gen.Kind.entries.forEach { k -> write("ueben-gen-${k.name.lowercase()}.json", (1L..40L).mapNotNull { s -> runCatching { dump(Gen.level(k, s), "ueben ${k.name}", count = s <= 6) }.getOrNull() }) }
        MoreGen.Kind.entries.forEach { k -> write("ueben-mehr-${k.name.lowercase()}.json", (1L..40L).map { s -> dump(MoreGen.level(k, s), "ueben ${k.tier} ${k.name}", count = s <= 6) }) }
        ProGen.Kind.entries.forEach { k -> write("ueben-profi-${k.name.lowercase()}.json", (1L..40L).map { s -> dump(ProGen.level(k, s), "ueben PROFI ${k.name}", count = s <= 6) }) }
        write("tagesaufgabe.json", (20_770L..20_800L).map { d -> dump(Daily.forDay(d), "tagesaufgabe $d", count = false) })
    }
}
