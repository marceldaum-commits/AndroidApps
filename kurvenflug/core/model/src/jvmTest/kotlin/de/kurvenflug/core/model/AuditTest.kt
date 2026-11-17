package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.*

/**
 * Korrektheitsprüfung 17.11.: unabhängige Nachrechnung aller Level (eigene Formeln, nicht Flight.f),
 * Rechenwege, Live-Rechnung, Rechen-Stern und Text-Hygiene. Schreibt einen Bericht nach build/audit.txt.
 */
class AuditTest {
    /** Alle Level, die ein Kind spielen kann (Kapitel, Extrem-Prüfungen, Generator aller Stufen, Bosse, Andromeda, Rettungen, Gastplaneten). */
    private val all: List<Level> by lazy {
        Levels.all + Levels.chapters.mapNotNull { ChapterExtreme.forChapter(it.number) } +
            Gen.kinds.flatMap { k -> (0..Tier.MAX).flatMap { t -> (1L..6L).map { Gen.level(k, it * 977 + t, t) } } } +
            Bosses.all.flatMap { it.stages } + (1..20).flatMap { Andromeda.planet(it).levels } + (1..12).flatMap { Season2.stages(it) } +
            SeasonEvent.entries.flatMap { Seasons.levels(it, 2026) } + (0..30).map { Daily.forDay(20_400L + it) }
    }

    /** Eigene Formeln (aus dem Schulbuch, unabhängig von Flight.f) */
    private fun indep(l: Level, v: Map<String, Double>, x: Double): Double? = when (l.kind) {
        Kind.LINE -> v["m"]!! * x + v["b"]!!
        Kind.PARABOLA -> v["a"]!! * (x - v["d"]!!).pow(2) + v["e"]!!
        Kind.SINE -> v["a"]!! * sin(v["b"]!! * (x - (v["c"] ?: 0.0))) + v["d"]!!
        Kind.EXP -> v["a"]!! * v["b"]!!.pow(x) + v["d"]!!
        Kind.PROP -> v["k"]!! * x
        Kind.ANTI -> if (x == 0.0) null else v["k"]!! / x
        Kind.LOG -> if (x <= 0) null else ln(x) / ln(v["b"]!!) + v["d"]!!
        Kind.MOTION -> v["v"]!! * x + v["s0"]!!
        Kind.ACCEL -> 0.5 * v["a"]!! * x * x + v["v0"]!! * x + (v["s0"] ?: 0.0)
        Kind.POWER -> { val n = v["n"]!!; if (x < 0 && n != round(n)) null else v["a"]!! * x.pow(n) + (v["d"] ?: 0.0) }
        Kind.EXPE -> v["a"]!! * exp(v["k"]!! * x) + (v["d"] ?: 0.0)
        else -> null
    }

    private val issues = mutableListOf<String>()
    /** Lücken (keine Live-Rechnung / kein Rechen-Stern) – kein Rechenfehler, eigene Liste */
    private val gaps = mutableListOf<String>()
    private fun bad(s: String) { issues += s }

    @Test fun audit() {
        val out = StringBuilder()
        out.appendLine("Geprüfte Level: ${all.size}")
        for (l in all) {
            val v = l.values(l.solution)
            // A) Formel ↔ Bahn: unabhängige Formel gegen die Flugberechnung, 41 Stellen
            if (l.kind in Flight.functionKinds && l.kind != Kind.BINOM) {
                for (i in 0..40) {
                    val x = -9.5 + i * 0.475
                    val a = indep(l, v, x) ?: continue; val b = Flight.f(l, l.solution, x)
                    if (a.isFinite() && b.isFinite() && abs(a - b) > 1e-9 * max(1.0, abs(a))) { bad("${l.id}: Formel ≠ Bahn bei x=$x ($a / $b)"); break }
                }
                // Stationen liegen auf der Lösungsbahn (eigene Formel)
                l.targets.forEach { t -> indep(l, v, t.x)?.let { y -> if (abs(y - t.y) > t.r + 1e-9) bad("${l.id}: Station (${t.x}|${t.y}) nicht auf Bahn (y=$y)") } }
            }
            // B) Wurf: Bahnpunkte erfüllen die Wurfparabel y = y0 + tan α·Δx − g·Δx² / (2 v² cos² α)
            if (l.kind == Kind.THROW) {
                val a = Math.toRadians(v["angle"]!!); val sp = v["v"]!!; val g = l.planet.g
                Flight.path(l, l.solution).forEach { p ->
                    val dx = p.x - l.launch.x
                    val y = l.launch.y + tan(a) * dx - g * dx * dx / (2 * sp * sp * cos(a).pow(2))
                    if (abs(y - p.y) > 0.02) { bad("${l.id}: Wurfbahn weicht ab bei x=${p.x}: ${p.y} statt $y"); return@forEach }
                }
            }
            // C) Lösbar, Startwerte lösen nicht
            if (!Flight.fly(l, l.solution).success) bad("${l.id}: nicht lösbar")
            // D) Rechenweg: Schritte, die einen Regler setzen, müssen die Lösung ergeben
            if (l.chapter in 1..99) Walkthrough.of(l)?.forEach { s ->
                if (!s.answer.isFinite()) bad("${l.id}: Rechenschritt ohne Zahl: ${s.text}")
                s.sets?.let { k -> val want = l.solution[k] ?: return@let
                    if (abs(s.answer - want) > max(0.01, abs(want) * 0.01)) bad("${l.id}: Rechenschritt „${s.text}“ = ${s.answer}, Lösung $k = $want") }
                if (s.choices.distinct().size != s.choices.size) bad("${l.id}: doppelte Auswahl im Rechenschritt „${s.text}“")
            }
            // E) Live-Rechnung und Rechen-Stern
            if (l.chapter in 1..99) {
                val live = Understand.live(l, l.solution)
                if (live == null) gaps += "${l.id} Live" else if (Regex("NaN|Infinity").containsMatchIn(live)) bad("${l.id}: Live-Rechnung „$live“")
                val c = Understand.check(l)
                if (c == null) gaps += "${l.id} Stern"
                else if (c.options.map { it.trim() }.distinct().size != c.options.size || c.options.any { it.isBlank() }) bad("${l.id}: Rechen-Stern-Antworten doppelt/leer: ${c.options}")
            }
            // F) Text-Hygiene
            val texts = listOfNotNull(l.title, l.task, l.tip, l.given) + (l.quiz ?: emptyList()) + (l.walk?.map { it.text } ?: emptyList())
            texts.forEach { t ->
                if (Regex("""(🔴 Profi|🔥 Extrem|🟠 Schwer).*\1""").containsMatchIn(t)) bad("${l.id}: Stufe doppelt in „$t“")
                if (Regex("""TODO|XXX|NaN|Infinity|null|\{|\}|\$""").containsMatchIn(t)) bad("${l.id}: Platzhalter in „$t“")
                if (Regex("""\d\.\d""").containsMatchIn(t)) bad("${l.id}: englisches Dezimaltrennzeichen in „$t“")
                if (Regex("""\S  \S""").containsMatchIn(t.replace(",  y", ", y"))) bad("${l.id}: doppeltes Leerzeichen in „$t“")
                if (Regex(""" [,.;!](\s|$)""").containsMatchIn(t)) bad("${l.id}: Leerzeichen vor Satzzeichen in „$t“")
                if (t.count { it == '(' } != t.count { it == ')' }) bad("${l.id}: Klammern unausgeglichen in „$t“")
            }
        }
        Levels.chapters.forEach { c -> (c.intro + c.title).forEach { t ->
            if ("  " in t || Regex("""\d\.\d""").containsMatchIn(t)) bad("Kapitel ${c.number}: Text „$t“")
            if (t.count { it == '(' } != t.count { it == ')' }) bad("Kapitel ${c.number}: Klammern „$t“")
        } }
        // G) „Raten reicht nicht“: ein Reglerschritt neben der Lösung trifft nicht (nur Info – bei manchen Leveln gewollt grob)
        val guess = Levels.all.filter { l -> l.params.any { p ->
            listOf(-1, 1).any { d -> val nv = p.snap(l.solution.getValue(p.key) + d * p.step); nv != l.solution[p.key] && Flight.fly(l, l.solution + (p.key to nv)).success } } }
        out.appendLine("Info – Level, bei denen ein Reglerschritt daneben auch trifft (${guess.size}): " + guess.joinToString { it.id })
        out.appendLine("Lücken (${gaps.size}): " + gaps.joinToString())
        out.appendLine("Abweichungen (${issues.size}):"); issues.forEach { out.appendLine(" - $it") }
        File("build").mkdirs(); File("build/audit.txt").writeText(out.toString())
        assertTrue("Abweichungen:\n" + issues.joinToString("\n"), issues.isEmpty())
    }
}
