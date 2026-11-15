package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt

// ───────────────────────── Schritt für Schritt ─────────────────────────

/** Geführte Aufgaben: Ein schweres Verfahren wird in kleine Schritte zerlegt, nach jedem Schritt gibt es Rückmeldung. */
object Guides {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))
    private fun part(label: String, level: Level) = ExamPart(label, 1, level)

    private val pierce = run {
        val a = p(1, 0, 1); val d = p(1, 2, 1); val n = p(1, 1, 2); val s = p(3, 4, 3)
        val scene = { listOf(Stage.plane("E", n, 13.0, "violet", "E"), Stage.fullLine("g", a, d, Bounds(), "cyan", "g")) }
        Exam("weg-durchstoss", "Durchstoßpunkt", "🔷", "g: x⃗ = (1|0|1) + t · (1|2|1) trifft E: x₁ + x₂ + 2x₃ = 13. Wo?", listOf(
            part("1", Families.scalar("weg-durchstoss-1", 0, "Einsetzen", "Setz die Gerade in E ein: (1 + t) + (2t) + 2 · (1 + t) = 13. Welches t kommt heraus? Die Marke wandert mit.", "t", -2..5, 2, 0,
                tip = "Zusammenfassen: 3 + 5t = 13.", way = listOf("(1 + t) + 2t + 2 + 2t = 13", "3 + 5t = 13", "t = 2"), color = "yellow",
                formula = { t -> listOf(lineText("g", a, d), listOf(Txt("E: x₁ + x₂ + 2x₃ = 13    t = "), Num(Geo.num(t), "a"))) },
                objects = { t, _ -> scene() + Stage.point("marke", a + d * t, "yellow", style = "ghost") },
                check = { t -> if (t == 2) Outcome(true, "t = 2 – die Marke liegt auf der Scheibe.") else Outcome(false, "Mit t = $t ergibt die linke Seite ${Geo.num(n dot (a + d * t))} statt 13.") })),
            part("2", Families.point("weg-durchstoss-2", 0, "Punkt ausrechnen", "Jetzt t = 2 in die Gerade einsetzen. Wo liegt der Durchstoßpunkt?", s, hidden = true, name = "S",
                given = listOf(lineText("g", a, d), listOf(Txt("t = 2"))), tip = "S = (1|0|1) + 2 · (1|2|1), Zeile für Zeile.", way = listOf("S = (1 + 2 | 0 + 4 | 1 + 2)", "S(3|4|3)"),
                mistakes = listOf(p(2, 4, 2) to "Du hast nur 2 · d⃗ gerechnet. Der Stützvektor (1|0|1) gehört dazu."), extras = { scene() })),
        ))
    }

    private val mirror = run {
        val q = p(1, 1, 0); val n = p(1, 2, 2); val f = p(2, 3, 2); val m = p(3, 5, 4)
        val scene = { listOf(Stage.plane("E", n, 12.0, "violet", "E"), Stage.point("P", q, "cyan", "P(1|1|0)")) }
        Exam("weg-spiegel", "Spiegelpunkt", "🪞", "P(1|1|0) wird an E: x₁ + 2x₂ + 2x₃ = 12 gespiegelt.", listOf(
            part("1", Families.scalar("weg-spiegel-1", 0, "Lotgerade einsetzen", "Lotgerade: x⃗ = (1|1|0) + t · (1|2|2). In E eingesetzt: 3 + 9t = 12. Welches t?", "t", -2..4, 1, 0,
                tip = "9t = 9.", way = listOf("(1 + t) + 2(1 + 2t) + 2 · 2t = 12", "3 + 9t = 12", "t = 1"), color = "yellow",
                formula = { t -> listOf(lineText("l", q, n), listOf(Txt("E: x₁ + 2x₂ + 2x₃ = 12    t = "), Num(Geo.num(t), "a"))) },
                objects = { t, _ -> scene() + Stage.line("lot", q, n, 0.0, t.toDouble().coerceAtLeast(0.05), "yellow") },
                check = { t -> if (t == 1) Outcome(true, "t = 1 – das Lot endet genau auf der Scheibe.") else Outcome(false, "Mit t = $t ergibt die linke Seite ${Geo.num(n dot (q + n * t))} statt 12.") })),
            part("2", Families.point("weg-spiegel-2", 0, "Lotfußpunkt", "Mit t = 1: Wo trifft das Lot die Scheibe?", f, hidden = true, name = "F",
                given = listOf(lineText("l", q, n), listOf(Txt("t = 1"))), tip = "F = P + 1 · n⃗.", way = listOf("F = (1 + 1 | 1 + 2 | 0 + 2)", "F(2|3|2)"), extras = { scene() })),
            part("3", Families.point("weg-spiegel-3", 0, "Spiegelpunkt", "Vom Lotfußpunkt denselben Weg noch einmal weiter. Wo liegt P′?", m, hidden = true, name = "P′",
                given = listOf(listOf(Txt("P(1|1|0)   F(2|3|2)"))), tip = "P′ = F + PF⃗ = P + 2 · PF⃗.", way = listOf("PF⃗ = (1|2|2)", "P′ = F + PF⃗", "P′(3|5|4)"),
                mistakes = listOf(f to "Das ist noch der Lotfußpunkt. Geh den Weg PF⃗ ein zweites Mal."), extras = { scene() + Stage.point("F", f, "yellow", "F") })),
        ))
    }

    private val cross = run {
        val a = p(1, 1, 1); val d1 = p(2, 1, 1); val b = p(7, 1, 1); val d2 = p(-1, 1, 1); val s = p(5, 3, 3)
        val scene = { listOf(Stage.fullLine("g", a, d1, Bounds(), "cyan", "g"), Stage.fullLine("h", b, d2, Bounds(), "yellow", "h")) }
        Exam("weg-schnitt", "Schnittpunkt", "✨", "g: x⃗ = (1|1|1) + s · (2|1|1) und h: x⃗ = (7|1|1) + t · (−1|1|1).", listOf(
            part("1", Families.scalar("weg-schnitt-1", 0, "Gleichsetzen", "Zeile 2 sagt: 1 + s = 1 + t, also s = t. In Zeile 1 eingesetzt: 1 + 2s = 7 − s. Welches s?", "s", -2..5, 2, 0,
                tip = "3s = 6.", way = listOf("1 + 2s = 7 − s", "3s = 6", "s = 2 (und t = 2)"), color = "cyan",
                formula = { v -> listOf(lineText("g", a, d1, "s"), lineText("h", b, d2), listOf(Txt("s = "), Num(Geo.num(v), "a"))) },
                objects = { v, _ -> scene() + Stage.point("mg", a + d1 * v, "cyan", style = "ghost") + Stage.point("mh", b + d2 * v, "yellow", style = "ghost") },
                check = { v -> if (v == 2) Outcome(true, "s = t = 2 – beide Marken treffen sich.") else Outcome(false, "Mit s = t = $v liegen die Marken noch ${Geo.num(((a + d1 * v) - (b + d2 * v)).length)} auseinander.") })),
            part("2", Families.point("weg-schnitt-2", 0, "Punkt ausrechnen", "s = 2 in g einsetzen: Wo ist der Schnittpunkt?", s, hidden = true, name = "S",
                given = listOf(lineText("g", a, d1, "s"), listOf(Txt("s = 2"))), tip = "S = (1|1|1) + 2 · (2|1|1).", way = listOf("S = (1 + 4 | 1 + 2 | 1 + 2)", "S(5|3|3)"), extras = { scene() })),
        ))
    }

    private val skew = run {
        val ga = p(1, 3, 1); val gd = p(1, 2, 2); val hb = p(4, 1, 0); val hd = p(1, 0, -2)
        val scene = { listOf(Stage.fullLine("g", ga, gd, Bounds(), "cyan", "g"), Stage.fullLine("h", hb, hd, Bounds(), "yellow", "h")) }
        Exam("weg-windschief", "Abstand windschiefer Geraden", "📏", "g: x⃗ = (1|3|1) + s · (1|2|2) und h: x⃗ = (4|1|0) + t · (1|0|−2). Es gilt n⃗ = (2|−2|1).", listOf(
            part("1", Families.scalar("weg-windschief-1", 0, "Skalarprodukt", "Der Verbindungsvektor der Stützpunkte ist (3|−2|−1). Wie groß ist sein Skalarprodukt mit n⃗ = (2|−2|1)?", "k", 0..12, 9, 0,
                tip = "3 · 2 + (−2) · (−2) + (−1) · 1.", way = listOf("(3|−2|−1) · (2|−2|1)", "= 6 + 4 − 1", "= 9"), color = "yellow",
                formula = { v -> listOf(listOf(Txt("(B − A) · n⃗ = "), col(p(3, -2, -1)), Txt(" · "), col(p(2, -2, 1)), Txt(" = "), Num(Geo.num(v), "a"))) },
                objects = { _, _ -> scene() + Stage.vector("ab", ga, hb, "pink", "B − A") },
                check = { v -> Families.compare(v, 9, "Das Skalarprodukt", "9 – stimmt.", mapOf(11 to "Achte auf das letzte Vorzeichen: (−1) · 1 = −1.", 1 to "(−2) · (−2) ist +4, nicht −4.")) })),
            part("2", Families.scalar("weg-windschief-2", 0, "Durch die Länge teilen", "Jetzt noch durch die Länge von n⃗ teilen: |n⃗| = √(4 + 4 + 1). Wie groß ist der Abstand?", "d", 0..9, 3, 1,
                tip = "|n⃗| = 3, also d = 9 : 3.", way = listOf("|n⃗| = √9 = 3", "d = 9 : 3", "d = 3"),
                formula = { v -> listOf(listOf(Txt("d = |(B − A) · n⃗| : |n⃗| = 9 : |n⃗| = "), Num(Geo.num(v), "a"))) },
                objects = { _, _ -> scene() },
                check = { v -> Families.compare(v, 3, "Der Abstand", "3 Einheiten – fertig.", mapOf(9 to "9 ist erst der Zähler. Teile durch |n⃗| = 3.")) })),
        ))
    }

    val all = listOf(pierce, mirror, cross, skew)
    fun byId(id: String) = all.firstOrNull { it.id == id }
}

// ───────────────────────── Lernstand ─────────────────────────

enum class Grip(val label: String) { OFFEN("offen"), WACKELIG("wackelig"), SICHER("sicher") }
data class TopicState(val chapter: Chapter, val played: Int, val stars: Int, val grip: Grip) {
    val total get() = chapter.levels.size
    /** 0..1 – Anteil der möglichen Sterne */
    val share get() = stars / (total * 3f)
}

object Learn {
    /** Sicher: alles gespielt und mindestens 80 % der Sterne. Offen: noch nichts gespielt. */
    fun topics(stars: Map<String, Int>): List<TopicState> = Levels.chapters.map { c ->
        val played = c.levels.count { (stars[it.id] ?: 0) > 0 }; val got = c.levels.sumOf { stars[it.id] ?: 0 }
        TopicState(c, played, got, when { played == 0 -> Grip.OFFEN; played == c.levels.size && got * 10 >= c.levels.size * 3 * 8 -> Grip.SICHER; else -> Grip.WACKELIG })
    }
    fun summary(t: List<TopicState>) = "${t.count { it.grip == Grip.SICHER }} sicher · ${t.count { it.grip == Grip.WACKELIG }} wackelig · ${t.count { it.grip == Grip.OFFEN }} offen"
}

// ───────────────────────── Klausur-Planer ─────────────────────────

/** Klausurtermin (Tag seit 1970) und die Kapitel, die drankommen. */
data class Plan(val examDay: Long, val chapters: Set<Int>) {
    fun encode() = "$examDay;${chapters.sorted().joinToString(",")}"
    companion object {
        fun decode(s: String): Plan? = runCatching {
            val (d, c) = s.split(";"); Plan(d.toLong(), c.split(",").filter { it.isNotBlank() }.map { it.toInt() }.toSet()).takeIf { it.chapters.isNotEmpty() }
        }.getOrNull()
    }
}

object Planner {
    const val MAX_PER_DAY = 8
    fun daysLeft(plan: Plan, today: Long) = (plan.examDay - today).toInt()
    /** Alles, was in den gewählten Kapiteln noch nicht mit drei Sternen sitzt – zuerst das noch nie Gespielte. */
    fun open(plan: Plan, stars: Map<String, Int>): List<Level> =
        Levels.chapters.filter { it.number in plan.chapters }.flatMap { it.levels }.filter { (stars[it.id] ?: 0) < 3 }.sortedBy { if ((stars[it.id] ?: 0) == 0) 0 else 1 }
    /** Das Paket für heute: der Rest gleichmäßig auf die verbleibenden Tage verteilt (höchstens [MAX_PER_DAY]). */
    fun today(plan: Plan, today: Long, stars: Map<String, Int>): List<Level> {
        val left = daysLeft(plan, today); if (left < 0) return emptyList()
        val rest = open(plan, stars); if (rest.isEmpty()) return emptyList()
        val perDay = ((rest.size + maxOf(left, 1) - 1) / maxOf(left, 1)).coerceIn(1, MAX_PER_DAY)
        return rest.take(perDay)
    }
    fun headline(plan: Plan, today: Long, stars: Map<String, Int>): String? {
        val left = daysLeft(plan, today); val n = today(plan, today, stars).size
        return when {
            left < 0 -> null
            left == 0 -> "Klausur heute – viel Erfolg!"
            n == 0 -> "Klausur in $left ${if (left == 1) "Tag" else "Tagen"} · alles sitzt ✓"
            else -> "Klausur in $left ${if (left == 1) "Tag" else "Tagen"} · heute $n ${if (n == 1) "Gig" else "Gigs"}"
        }
    }
}

// ───────────────────────── Klassen-Modus ─────────────────────────

/** Ein Aufgabenpaket für die ganze Klasse als Code – ohne Konto, ohne Server. */
object ClassCode {
    const val MAX = 10
    private const val ALPHA = "0123456789abcdefghijklmnopqrstuvwxyz"
    private fun check(body: String) = ALPHA[body.fold(11) { h, c -> (h * 29 + ALPHA.indexOf(c) + 1) % 36 }].uppercaseChar()
    fun encode(levels: List<Level>): String {
        val body = levels.take(MAX).joinToString("") { l -> val i = Levels.all.indexOf(l); "${ALPHA[i / 36]}${ALPHA[i % 36]}" }
        return "RLK-${body.uppercase()}${check(body)}"
    }
    fun find(text: String): List<Level>? {
        val m = Regex("RLK-((?:[0-9A-Za-z]{2}){1,$MAX})([0-9A-Za-z])", RegexOption.IGNORE_CASE).find(text) ?: return null
        val body = m.groupValues[1].lowercase()
        if (check(body) != m.groupValues[2].uppercase()[0]) return null
        return body.chunked(2).map { Levels.all.getOrNull(ALPHA.indexOf(it[0]) * 36 + ALPHA.indexOf(it[1])) ?: return null }
    }
    fun shareText(levels: List<Level>) = "Raumlicht-Klassenpaket (${levels.size} Aufgaben: ${levels.joinToString(", ") { it.id }}). In der App unter Backstage → Klasse eingeben: ${encode(levels)}"
    fun resultText(levels: List<Level>, firstTry: Int) = "Raumlicht-Klassenpaket ${encode(levels)}: $firstTry von ${levels.size} beim ersten Versuch gelöst."
}

// ───────────────────────── Zwei an einem Gerät ─────────────────────────

/** Abwechselnd spielen: Punkt gibt es für einen Treffer beim ersten Versuch. Gewonnen hat, wer nach einer vollen Runde vorn liegt und mindestens [TARGET] hat. */
data class Match(val scores: List<Int> = listOf(0, 0), val turn: Int = 0, val played: Int = 0) {
    val winner: Int? get() = if (played % 2 == 0 && scores.max() >= TARGET && scores[0] != scores[1]) scores.indexOf(scores.max()) else null
    fun after(hit: Boolean) = Match(scores.mapIndexed { i, s -> if (i == turn && hit) s + 1 else s }, 1 - turn, played + 1)
    companion object { const val TARGET = 3; val names = listOf("Blau", "Pink"); val colors = listOf("cyan", "pink") }
}
