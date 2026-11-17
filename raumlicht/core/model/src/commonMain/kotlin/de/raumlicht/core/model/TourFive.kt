package de.raumlicht.core.model

import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt
import kotlin.math.abs
import kotlin.math.pow

/** Typische Fehler, die Lux beim Namen nennt – daraus entsteht das Fehler-Tagebuch. */
object MistakeBook {
    // wird nur beim Aufbau der Aufgaben gefüllt (ein Faden), danach nur gelesen
    private val known = HashSet<String>()
    private val builtIn = listOf("Du hast A − B gerechnet", "Das ist der Ortsvektor von B", "Die Richtung stimmt – aber der Strahl läuft rückwärts", "Das ist die Richtung vom Ursprung")
    fun add(messages: Collection<String>) { known += messages }
    fun isNamed(message: String) = message in known || builtIn.any { message.startsWith(it) }
}

/** Zeichnet Funktionsgraphen in die Ebene x₁ = 4: x nach rechts, y nach oben. */
internal class Graph(val from: Double, val to: Double, private val sx: Double, private val ox: Double, private val sy: Double, private val oz: Double) {
    fun at(x: Double, y: Double) = V3(4.0, ox + x * sx, (oz + y * sy).coerceIn(0.05, 6.6))
    /** Der Graph endet am Rand des Achsenkreuzes – er wird nicht am Boden plattgedrückt (das wäre eine falsche Kurve) */
    fun curve(id: String, color: String, f: (Double) -> Double): Obj {
        val pts = (0..96).map { val x = from + (to - from) * it / 96; x to f(x) }
        fun inside(y: Double) = oz + y * sy in 0.05..6.6
        return Stage.edges(id, pts.zipWithNext().filter { (a, b) -> inside(a.second) && inside(b.second) }.map { (a, b) -> at(a.first, a.second) to at(b.first, b.second) }, color)
    }
    fun axis() = Stage.axes2d(ox, oz, sx, sy, from, to)
    fun line(id: String, x0: Double, y0: Double, x1: Double, y1: Double, color: String, dashed: Boolean = false) = Stage.line(id, at(x0, y0), at(x1, y1) - at(x0, y0), 0.0, 1.0, color, dashed = dashed, thin = !dashed)
    fun lap(f: (Double) -> Double, stop: Double): List<V3> { val up = V3(0.0, 0.0, 0.45)
        return (0..16).map { val x = from + (to - from) * it / 16; at(x, f(x)) + up } + (0..5).map { val x = to + (stop - to) * it / 5; at(x, f(x)) + up } }
}

/** Kapitel 15–17: Funktionen bauen, mehr Stochastik, Ableiten und Anwenden. */
internal object TourFive {
    private fun fmtX(v: Double) = Geo.num(v)

    // ───────────────────────── 15 · Funktionen bauen (Steckbrief) ─────────────────────────
    private class Knob(val key: String, val name: String, val range: IntRange, val start: Int, val color: String)
    /** Die Bahn mit Reglern so bauen, dass sie durch alle Tore führt. */
    private fun build(id: String, title: String, mission: String, g: Graph, knobs: List<Knob>, gates: List<Pair<Int, Int>>, solution: List<Int>, text: (List<Int>) -> String,
                      f: (List<Int>) -> (Double) -> Double, tip: String, way: List<String>, abi: Boolean = false) = Level(
        id, 15, title, mission, Bounds(),
        knobs.map { Control(it.key, it.name, ControlKind.SCALAR, listOf(it.start), listOf(it.range.first), listOf(it.range.last), it.color) },
        knobs.mapIndexed { i, k -> k.key to listOf(solution[i]) }.toMap(), tip, way, abi,
        finale = { v -> val fn = f(knobs.map { v.int(it.key) }); Finale(path = g.lap(fn, g.from)) },
        formula = { v -> listOf(listOf(Txt("f(x) = "), Txt(text(knobs.map { v.int(it.key) }))), listOf(Txt("Tore: " + gates.joinToString("  ") { "(${it.first}|${it.second})" }))) },
        objects = { v, ph ->
            val fn = f(knobs.map { v.int(it.key) })
            listOf(g.curve("bahn", "cyan", fn), g.axis(), Stage.drone(V3(8, 0, 1), if (ph == Phase.DONE) "happy" else "idle")) +
                gates.mapIndexed { i, (x, y) -> Stage.point("tor$i", g.at(x.toDouble(), y.toDouble()), if (abs(fn(x.toDouble()) - y) < 1e-9) "green" else "yellow", "(${x}|${y})", "target", drop = false) }
        },
        evaluate = { v ->
            val fn = f(knobs.map { v.int(it.key) }); val miss = gates.firstOrNull { abs(fn(it.first.toDouble()) - it.second) > 1e-9 }
            if (miss == null) Outcome(true, "Die Bahn führt durch alle Tore!")
            else Outcome(false, "Bei x = ${miss.first} ist die Bahn auf Höhe ${fmtX(fn(miss.first.toDouble()))} – das Tor hängt bei ${miss.second}. ${gates.count { abs(fn(it.first.toDouble()) - it.second) < 1e-9 }} von ${gates.size} Toren passen schon.")
        },
    )

    private fun sgn(v: Int, first: Boolean = false) = if (first) Geo.num(v) else if (v < 0) "− ${abs(v)}" else "+ $v"
    private val ch15 = run {
        val g = Graph(0.0, 4.0, 1.6, 0.8, 0.9, 1.0)
        listOf(
            build("15-1", "Gerade durch zwei Tore", "Bau die Bahn selbst: Stell Steigung m und Achsenabschnitt b so ein, dass die Gerade durch beide Tore führt.", g,
                listOf(Knob("m", "m", -3..3, 0, "cyan"), Knob("b", "b", -2..4, 0, "pink")), listOf(0 to 1, 2 to 5), listOf(2, 1),
                { (m, b) -> "${Geo.num(m)}x ${sgn(b)}" }, { (m, b) -> { x -> m * x + b } },
                tip = "Das Tor bei x = 0 verrät b. Die Steigung ist Höhenunterschied durch Abstand: (5 − 1) : (2 − 0).", way = listOf("b = f(0) = 1", "m = (5 − 1) : (2 − 0) = 2", "f(x) = 2x + 1")),
            build("15-2", "Scheitel verschieben", "Eine Parabel in Scheitelform: d schiebt sie nach rechts, e nach oben. Leg den Scheitel ins erste Tor.", g,
                listOf(Knob("d", "d", 0..4, 0, "cyan"), Knob("e", "e", 0..3, 0, "pink")), listOf(2 to 1, 3 to 2), listOf(2, 1),
                { (d, e) -> "(x − $d)² ${sgn(e)}" }, { (d, e) -> { x -> (x - d) * (x - d) + e } },
                tip = "Der Scheitel liegt bei (d|e). Das zweite Tor ist die Probe.", way = listOf("Scheitel S(2|1) → d = 2, e = 1", "Probe: f(3) = 1² + 1 = 2 ✓", "f(x) = (x − 2)² + 1")),
            build("15-3", "Drei Tore", "Jetzt kommt die Öffnung a dazu: negativ heißt nach unten geöffnet. Triff alle drei Tore.", g,
                listOf(Knob("a", "a", -2..2, 1, "yellow"), Knob("d", "d", 0..4, 0, "cyan"), Knob("e", "e", 0..5, 0, "pink")), listOf(2 to 4, 0 to 0, 3 to 3), listOf(-1, 2, 4),
                { (a, d, e) -> "${Geo.num(a)} · (x − $d)² ${sgn(e)}" }, { (a, d, e) -> { x -> a * (x - d) * (x - d) + e } },
                tip = "Versuch es mit dem höchsten Tor als Scheitel: d = 2, e = 4. Dann ein anderes Tor einsetzen, um a zu bekommen – und am dritten Tor die Probe machen.", way = listOf("Scheitel S(2|4) → d = 2, e = 4", "f(0) = a · 4 + 4 = 0 → a = −1", "f(x) = −(x − 2)² + 4, Probe am dritten Tor: f(3) = −1 + 4 = 3 ✓")),
            build("15-4", "Abi-Check: Steckbrief", "Gesucht ist die Parabel f(x) = ax² + bx + c durch drei Punkte. Stell das Gleichungssystem auf – und bau sie.", g,
                listOf(Knob("a", "a", -2..2, 0, "yellow"), Knob("b", "b", -3..3, 1, "cyan"), Knob("c", "c", -2..3, 0, "pink")), listOf(0 to 1, 1 to 2, 2 to 5), listOf(1, 0, 1),
                { (a, b, c) -> "${Geo.num(a)}x² ${sgn(b)}x ${sgn(c)}" }, { (a, b, c) -> { x -> a * x * x + b * x + c } }, abi = true,
                tip = "f(0) = 1 liefert c. Dann: a + b + 1 = 2 und 4a + 2b + 1 = 5.", way = listOf("f(0) = c = 1", "a + b = 1 und 4a + 2b = 4", "a = 1, b = 0 → f(x) = x² + 1")),
        )
    }

    // ───────────────────────── 16 · Stochastik: Bäume, Tafeln, Binomial ─────────────────────────
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun percent(id: String, title: String, mission: String, pct: Int, step: Int, formula: String, tip: String, way: List<String>, objects: List<Obj>,
                        abi: Boolean = false, mistakes: Map<Int, String> = emptyMap(), done: List<Obj> = objects, finale: Finale) =
        Families.scalar(id, 16, title, mission, "P", 0..(100 / step), pct / step, 0, tip, way, "pink", step = step, suffix = " %", abi = abi,
            formula = { a -> listOf(listOf(Txt(formula)), listOf(Txt("P = "), Num("${a * step} %", "a"))) }, objects = { _, ph -> if (ph == Phase.DONE) done else objects }, finale = { finale },
            check = { a -> Families.compare(a * step, pct, "Die Wahrscheinlichkeit", "Genau $pct %.", mistakes) })

    private val root = V3(4.0, 4.0, 5.2); private val nodeA = V3(4.0, 2.2, 3.4); private val nodeB = V3(4.0, 5.8, 3.4)
    private val leaves = listOf(V3(4.0, 1.2, 1.4), V3(4.0, 3.2, 1.4), V3(4.0, 4.8, 1.4), V3(4.0, 6.8, 1.4))
    /** Lux leuchtet einen Pfad im Baum nach: Start → [mid] → Blatt [leaf] */
    private fun pathBeams(mid: V3, leaf: Int, color: String) = listOf(Beam(root, mid, color, false), Beam(mid, leaves[leaf], color, true))
    private val fields = listOf(Triple("VIP + Band 12", 12, "pink"), Triple("nur VIP 28", 28, "yellow"), Triple("nur Band 18", 18, "cyan"))
    private fun fieldAt(i: Int) = V3(4.0, 1.2 + (i % 2) * 3.0 + 1.3, 0.6 + (i / 2) * 2.8 + 0.4)
    private val luxAt = V3(8, 0, 1)
    /** Zweistufiges Baumdiagramm mit Wahrscheinlichkeit [q] für „pink“ */
    private fun tree(q: String, r: String): List<Obj> {
        val a = nodeA; val b = nodeB
        return listOf(Stage.edges("baum", listOf(root to a, root to b, a to leaves[0], a to leaves[1], b to leaves[2], b to leaves[3]), "white"),
            Stage.point("w", root, "white", "Start", drop = false), Stage.point("a", a, "pink", "pink $q", drop = false), Stage.point("b", b, "cyan", "blau $r", drop = false),
            Stage.point("l0", leaves[0], "pink", "pink", drop = false), Stage.point("l1", leaves[1], "cyan", "blau", drop = false),
            Stage.point("l2", leaves[2], "pink", "pink", drop = false), Stage.point("l3", leaves[3], "cyan", "blau", drop = false))
    }
    /** Vier Felder als Leuchtflächen, Höhe nach Anzahl */
    private fun table(cells: List<Triple<String, Int, String>>): List<Obj> = cells.mapIndexed { i, (label, n, color) ->
        val x0 = 1.2 + (i % 2) * 3.0; val z0 = 0.6 + (i / 2) * 2.8; val h = 0.4 + n / 25.0
        val q = listOf(V3(4.0, x0, z0), V3(4.0, x0 + 2.6, z0), V3(4.0, x0 + 2.6, z0 + h), V3(4.0, x0, z0 + h))
        listOf(Stage.edges("f$i", q.indices.map { q[it] to q[(it + 1) % 4] }, color, faces = listOf(q)), Stage.point("t$i", V3(4.0, x0 + 1.3, z0 + h + 0.35), color, label, style = "ghost", drop = false))
    }.flatten()

    /** Vier Lampen in einer Reihe; das Paar [on] leuchtet gelb */
    private fun lampRow(on: Pair<Int, Int>) = (0 until 4).map { Stage.sphere("k$it", V3(4.0, 2.0 + it * 1.4, 2.5), 0.5, if (it == on.first || it == on.second) "yellow" else "cyan") }
    private val pairs = listOf(0 to 1, 0 to 2, 0 to 3, 1 to 2, 1 to 3, 2 to 3)

    private val ch16 = listOf(
        percent("16-1", "Baumdiagramm", "Zwei von zehn Kugeln sind pink. Ich ziehe zweimal mit Zurücklegen. Wie wahrscheinlich ist genau eine davon pink?", 32, 4,
            "P(pink) = 0,2 · zweimal ziehen · genau einmal pink", tip = "Es gibt zwei Pfade: erst pink, dann blau – und umgekehrt. Pfade multiplizieren, dann addieren.",
            way = listOf("pink, blau: 0,2 · 0,8 = 0,16", "blau, pink: 0,8 · 0,2 = 0,16", "zusammen 0,32 = 32 %"), objects = tree("0,2", "0,8"),
            finale = Finale(acts = listOf(Act("Pfad 1: erst pink, dann blau – 0,2 · 0,8 = 0,16.", beams = pathBeams(nodeA, 1, "pink")),
                Act("Pfad 2: erst blau, dann pink – 0,8 · 0,2 = 0,16.", beams = pathBeams(nodeB, 2, "cyan")), Act("Beide Pfade zusammen: 0,16 + 0,16 = 32 %.", ms = 900))),
            mistakes = mapOf(16 to "16 % ist nur ein Pfad. Es gibt zwei Reihenfolgen: pink–blau und blau–pink.", 4 to "4 % ist „beide pink“. Gesucht ist: genau eine.")),
        Families.scalar("16-2", 16, "Pfade zählen", "Vier Lampen, jede geht an oder bleibt aus. Auf wie viele Arten können genau zwei leuchten?", "k", 0..12, 6, 0,
            tip = "Binomialkoeffizient „4 über 2“: 4 · 3 : (2 · 1).", way = listOf("4 über 2 = 4! : (2! · 2!)", "= (4 · 3) : (2 · 1)", "= 6"), color = "pink",
            formula = { a -> listOf(listOf(Txt("n = 4 Lampen, genau k = 2 leuchten")), listOf(Txt("Anzahl der Möglichkeiten = "), Num(Geo.num(a), "a"))) },
            finale = { Finale(acts = pairs.mapIndexed { n, on -> Act("Möglichkeit ${n + 1} von 6: Lampe ${on.first + 1} und ${on.second + 1} leuchten.", lampRow(on), ms = 650) }) },
            objects = { _, _ -> lampRow(0 to 1) },
            check = { a -> Families.compare(a, 6, "Die Anzahl", "6 Möglichkeiten – genau der Binomialkoeffizient.", mapOf(8 to "4 · 2 zählt etwas anderes. Hier gilt: 4 · 3 : 2.", 12 to "4 · 3 = 12 zählt jede Auswahl doppelt (Reihenfolge egal). Teile noch durch 2.")) }),
        Families.scalar("16-3", 16, "Vierfeldertafel", "100 Gäste: 40 haben ein VIP-Ticket, 30 ein Leuchtband, 12 beides. Wie viele haben weder noch?", "n", 0..10, 7, 0,
            tip = "Erst: Wie viele haben mindestens eins? 40 + 30 − 12. Der Rest hat keins.", way = listOf("VIP oder Band: 40 + 30 − 12 = 58", "weder noch: 100 − 58", "= 42"), color = "pink", step = 6,
            formula = { a -> listOf(listOf(Txt("100 Gäste · VIP 40 · Band 30 · beides 12")), listOf(Txt("weder VIP noch Band = "), Num(Geo.num(a * 6), "a"))) },
            finale = { Finale(acts = listOf(Act("VIP oder Band: 12 + 28 + 18 = 58 Gäste.", beams = (0..2).map { Beam(luxAt, fieldAt(it), fields[it].third, false) }),
                Act("Der Rest hat keins von beiden: 100 − 58 = 42.", table(fields + Triple("weder noch 42", 42, "violet")), listOf(Beam(luxAt, fieldAt(3), "violet", true))))) },
            objects = { _, ph -> table(fields + Triple(if (ph == Phase.DONE) "weder noch 42" else "weder noch ?", 42, "violet")) },
            check = { a -> Families.compare(a * 6, 42, "Die Anzahl", "42 Gäste haben weder VIP noch Band.", mapOf(30 to "Bei 100 − 40 − 30 ziehst du die 12 doppelt ab – sie stecken in beiden Gruppen.")) }),
        percent("16-4", "Bedingte Wahrscheinlichkeit", "Ich weiß schon: Dieser Gast hat ein VIP-Ticket. Wie wahrscheinlich trägt er dann auch ein Leuchtband?", 30, 10,
            "40 VIP, davon 12 mit Band · P(Band | VIP)", tip = "Nur noch die 40 VIP-Gäste zählen. Wie viele davon haben ein Band?", way = listOf("P(Band | VIP) = 12 : 40", "= 0,3", "= 30 %"),
            objects = table(fields + Triple("weder noch 42", 42, "violet")), done = table(fields.take(2)),
            finale = Finale(acts = listOf(Act("Bedingung „VIP“: Wer kein VIP-Ticket hat, zählt nicht mehr mit.", table(fields.take(2)), listOf(Beam(luxAt, fieldAt(2), "cyan", true), Beam(luxAt, fieldAt(3), "violet", true))),
                Act("Von den 40 VIP-Gästen tragen 12 ein Band: 12 von 40.", beams = listOf(Beam(luxAt, fieldAt(0), "pink", false))))),
            mistakes = mapOf(40 to "40 % ist P(VIP) – der Anteil der VIP-Gäste an allen. Gesucht ist der Anteil mit Band unter den VIP-Gästen.", 10 to "12 : 100 wäre P(VIP und Band). Bei der Bedingung „VIP“ teilst du durch 40.")),
        Families.scalar("16-5", 16, "Abi-Check: Streuung", "100 Blitzlichter, jedes zündet mit 50 %. Wie groß ist die Standardabweichung σ der Anzahl der Zündungen?", "σ", 0..25, 5, 0,
            tip = "σ = √(n · p · (1 − p)).", way = listOf("n · p · (1 − p) = 100 · 0,5 · 0,5 = 25", "σ = √25", "= 5 (also meist zwischen 45 und 55 Zündungen: μ ± σ)"), color = "pink", abi = true,
            formula = { a -> listOf(listOf(Txt("n = 100, p = 0,5 (binomialverteilt), μ = 50")), listOf(Txt("σ = √(n · p · (1 − p)) = "), Num(Geo.num(a), "a"))) },
            finale = { Finale(acts = listOf(Act("Im Mittel zündet die Hälfte: μ = 100 · 0,5 = 50.", beams = (0 until 10 step 2).map { Beam(luxAt, V3(4.0, 1.2 + (it % 5) * 1.4, 1.5 + (it / 5) * 1.6), "yellow", false) }),
                Act("Meist liegt die Anzahl nah dran: zwischen 45 und 55, also μ ± σ."))) },
            objects = { _, _ -> (0 until 10).map { Stage.sphere("k$it", V3(4.0, 1.2 + (it % 5) * 1.4, 1.5 + (it / 5) * 1.6), 0.5, if (it % 2 == 0) "yellow" else "cyan") } },
            check = { a -> Families.compare(a, 5, "Die Standardabweichung", "σ = 5.", mapOf(25 to "25 ist die Varianz σ². Die Standardabweichung ist die Wurzel daraus.")) }),
    )

    // ───────────────────────── 17 · Ableiten und Anwenden ─────────────────────────
    private val ch17 = run {
        val g1 = Graph(0.0, 4.0, 1.6, 0.8, 0.33, 0.6); val sq = { x: Double -> x * x }
        val g2 = Graph(0.0, 2.0, 3.2, 0.8, 0.2, 0.6); val chain = { x: Double -> (2 * x + 1) * (2 * x + 1) }
        val g3 = Graph(-1.0, 3.0, 1.6, 2.4, 0.5, 1.6)
        val g4 = Graph(0.0, 4.0, 1.6, 0.8, 0.6, 0.6)
        val g5 = Graph(0.0, 4.0, 1.6, 0.8, 0.33, 0.6); val grow = { x: Double -> 2.0.pow(x) }
        /** Tangente im Punkt (x0 | y0) mit einstellbarer Steigung m */
        fun slope(id: String, title: String, mission: String, g: Graph, text: String, f: (Double) -> Double, x0: Double, solution: Int, range: IntRange, step: Int, tip: String, way: List<String>, mistakes: Map<Int, String>) =
            Families.scalar(id, 17, title, mission, "m", range, solution / step, 0, tip, way, "yellow", step = step,
                formula = { a -> listOf(listOf(Txt("f(x) = $text")), listOf(Txt("f′(${fmtX(x0)}) = "), Num(Geo.num(a * step), "a"))) },
                objects = { a, _ -> val m = a * step; val w = 0.8
                    listOf(g.curve("bahn", "cyan", f), g.axis(), Stage.point("stelle", g.at(x0, f(x0)), "yellow", drop = false), g.line("tangente", x0 - w, f(x0) - m * w, x0 + w, f(x0) + m * w, "yellow")) },
                check = { a -> Families.compare(a * step, solution, "Die Steigung", "f′(${fmtX(x0)}) = $solution – die Tangente schmiegt sich an.", mistakes) })
        listOf(
            slope("17-1", "Steigung im Punkt", "Dreh die gelbe Tangente, bis sie die Bahn bei x = 3 nur noch berührt. Welche Steigung hat sie?", g1, "x²", sq, 3.0, 6, 0..10, 1,
                tip = "Potenzregel: f′(x) = 2x. Dann x = 3 einsetzen.", way = listOf("f′(x) = 2x", "f′(3) = 2 · 3", "= 6"), mistakes = mapOf(9 to "9 ist f(3), die Höhe. Gesucht ist die Steigung f′(3).")),
            slope("17-2", "Kettenregel", "Eine verschachtelte Funktion. Wie steil ist die Bahn bei x = 1?", g2, "(2x + 1)²", chain, 1.0, 12, 0..9, 2,
                tip = "Äußere mal innere Ableitung: 2 · (2x + 1) · 2.", way = listOf("f′(x) = 2 · (2x + 1) · 2 = 4 · (2x + 1)", "f′(1) = 4 · 3", "= 12"), mistakes = mapOf(6 to "2 · (2x + 1) ist nur die äußere Ableitung. Es fehlt die innere Ableitung: mal 2.")),
            Families.scalar("17-3", 17, "Tangentengleichung", "Die Tangente an f(x) = x² bei x = 1 hat die Steigung 2. Schieb sie mit b so, dass sie die Bahn dort berührt.", "b", -4..4, -1, 0,
                tip = "t(x) = 2x + b muss durch den Punkt (1|1) gehen: 1 = 2 · 1 + b.", way = listOf("Berührpunkt: (1 | f(1)) = (1|1)", "1 = 2 · 1 + b", "b = −1 → t(x) = 2x − 1"), color = "yellow",
                formula = { a -> listOf(listOf(Txt("f(x) = x²,  f′(1) = 2")), listOf(Txt("t(x) = 2x + b,   b = "), Num(Geo.num(a), "a"))) },
                objects = { a, _ -> listOf(g3.curve("bahn", "cyan", sq), g3.axis(), Stage.point("stelle", g3.at(1.0, 1.0), "yellow", drop = false), g3.line("tangente", -0.5, -1.0 + a, 2.5, 5.0 + a, "yellow")) },
                check = { a -> Families.compare(a, -1, "Der Achsenabschnitt", "t(x) = 2x − 1 berührt die Bahn genau in (1|1).", mapOf(1 to "Vorzeichen: 1 = 2 + b heißt b = 1 − 2.")) }),
            Families.scalar("17-4", 17, "Größte Leinwand", "Unter der schrägen Dachkante y = 8 − 2x soll eine rechteckige Leinwand hängen: links unten in der Ecke, die obere rechte Ecke an der Dachkante. Bei welcher Breite x ist sie am größten?", "x", 0..4, 2, 1,
                tip = "Fläche A(x) = x · (8 − 2x) = 8x − 2x². Ableiten und null setzen.", way = listOf("A(x) = 8x − 2x²", "A′(x) = 8 − 4x = 0 → x = 2", "A″(x) = −4 < 0 → Maximum: A(2) = 8, größer geht es nicht"), color = "yellow",
                formula = { a -> listOf(listOf(Txt("Dachkante: y = 8 − 2x")), listOf(Txt("x = "), Num(Geo.num(a), "a"), Txt("   A = x · (8 − 2x) = ${a * (8 - 2 * a)}"))) },
                objects = { a, _ -> val q = listOf(g4.at(0.0, 0.0), g4.at(a.toDouble(), 0.0), g4.at(a.toDouble(), 8.0 - 2 * a), g4.at(0.0, 8.0 - 2 * a))
                    listOf(g4.line("dach", 0.0, 8.0, 4.0, 0.0, "cyan"), g4.axis(), Stage.edges("leinwand", q.indices.map { q[it] to q[(it + 1) % 4] }, "yellow", faces = listOf(q))) },
                check = { a -> if (a == 2) Outcome(true, "Bei x = 2 ist die Leinwand mit 8 Flächeneinheiten am größten.") else Outcome(false, "Bei x = $a hat die Leinwand ${a * (8 - 2 * a)} Flächeneinheiten. Es geht größer.") }),
            Families.scalar("17-5", 17, "Abi-Check: Wachstum", "Die Zahl der Lichter verdoppelt sich jede Stunde: N(t) = 100 · 2ᵗ. Nach wie vielen Stunden sind es 800?", "t", 0..4, 3, 0,
                tip = "100 · 2ᵗ = 800 heißt 2ᵗ = 8.", way = listOf("2ᵗ = 800 : 100 = 8", "2³ = 8", "t = 3 Stunden"), color = "yellow", abi = true,
                lux = { a, _ -> g5.at(a.toDouble(), grow(a.toDouble())) + V3(0.0, 0.0, 0.45) },
                formula = { a -> listOf(listOf(Txt("N(t) = 100 · 2ᵗ,   Ziel: 800   (Hochachse: N in Hundert)")), listOf(Txt("t = "), Num(Geo.num(a), "a"), Txt("   N = ${(100 * grow(a.toDouble())).toInt()}"))) },
                objects = { _, _ -> listOf(g5.curve("bahn", "cyan", grow), g5.axis(), g5.line("ziel", 0.0, 8.0, 4.0, 8.0, "pink", dashed = true)) },
                check = { a -> if (a == 3) Outcome(true, "Nach 3 Stunden sind es 800 Lichter.") else Outcome(false, "Nach ${if (a == 1) "1 Stunde" else "$a Stunden"} sind es ${(100 * grow(a.toDouble())).toInt()} Lichter.") }),
        )
    }

    val chapters = listOf(
        Chapter(15, "Funktionen bauen", "Gerade, Scheitelform, Steckbriefaufgabe", "Konstruktionsbüro", "🛠️", "cyan", listOf(
            "Bisher war die Bahn fertig. Jetzt baust du sie selbst.",
            "Jeder Regler ändert die Kurve sofort. Sie muss durch alle Tore führen.",
            "Im Abi heißt das Steckbriefaufgabe: aus Punkten die Funktion finden."), ch15, Area.ANALYSIS),
        Chapter(16, "Bäume, Tafeln, Binomial", "Baumdiagramm, Vierfeldertafel, bedingte Wahrscheinlichkeit, σ", "Festival-Einlass", "🌳", "pink", listOf(
            "Mehr Zufall: mehrere Stufen, mehrere Merkmale.",
            "Baumdiagramm: entlang des Pfades multiplizieren, Pfade addieren.",
            "Bei „unter der Bedingung …“ zählt nur noch ein Teil der Gäste."), ch16, Area.STOCHASTIK),
        Chapter(17, "Ableiten & Anwenden", "Steigung, Kettenregel, Tangente, Extremwert, Wachstum", "Werkstatt", "🧰", "yellow", listOf(
            "Die Ableitung ist die Steigung der Tangente. Dreh die gelbe Linie und sieh es selbst.",
            "Tangente: Steigung aus f′, dann durch den Berührpunkt legen.",
            "Größter oder kleinster Wert gesucht? Ableitung null setzen."), ch17, Area.ANALYSIS),
    )
}

// ───────────────────────── Abi-Aufgaben Analysis und Stochastik ─────────────────────────

internal object MoreExams {
    private val arc = { x: Double -> -3 * x * x + 6 * x }
    private val g = Graph(0.0, 2.0, 2.5, 1.5, 1.2, 1.0)
    private fun part(label: String, pts: Int, l: Level) = ExamPart(label, pts, l)
    private fun num(id: String, title: String, mission: String, name: String, range: IntRange, solution: Int, start: Int, way: List<String>, tip: String, what: String, mistakes: Map<Int, String> = emptyMap(),
                    step: Int = 1, suffix: String = "", play: Pair<Finale, List<Obj>>? = null, objects: () -> List<Obj>) =
        Families.scalar(id, 0, title, mission, name, range, solution / step, start, tip, way, "yellow", step = step, suffix = suffix,
            formula = { a -> listOf(listOf(Txt("$name = "), Num(Geo.num(a * step) + suffix, "a"))) }, objects = { _, ph -> if (ph == Phase.DONE && play != null) play.second else objects() },
            finale = play?.let { p -> { _: Int -> p.first } },
            check = { a -> Families.compare(a * step, solution, what, "Richtig: ${Geo.num(solution)}$suffix.", mistakes) })

    private val bow = Exam("bogen", "Lichtbogen", "🌈", "Ein Lichtbogen hat die Form des Graphen von f(x) = −3x² + 6x (x und f(x) in Metern).", listOf(
        part("a", 2, num("abi-bogen-a", "Fußpunkt", "Der Bogen beginnt bei x = 0. An welcher Stelle trifft er wieder auf den Boden?", "x", 0..4, 2, 0,
            listOf("−3x² + 6x = 0", "−3x · (x − 2) = 0", "x = 2"), "Ausklammern: −3x · (x − 2) = 0.", "Die Nullstelle") { listOf(g.curve("bahn", "cyan", arc), g.axis()) }),
        part("b", 3, num("abi-bogen-b", "Höchster Punkt", "Wie hoch ist der Bogen an seiner höchsten Stelle?", "h", 0..6, 3, 0, suffix = " m", way =
            listOf("f′(x) = −6x + 6 = 0 → x = 1", "f(1) = −3 + 6", "= 3 Meter"), tip = "Erst die Stelle mit f′(x) = 0 suchen, dann in f einsetzen.", what = "Die Höhe",
            mistakes = mapOf(1 to "x = 1 ist die Stelle. Gefragt ist die Höhe dort: f(1).")) { listOf(g.curve("bahn", "cyan", arc), g.axis()) }),
        part("c", 3, num("abi-bogen-c", "Startsteigung", "Wie steil steigt der Bogen am Anfang (x = 0) an?", "m", 0..8, 6, 0,
            listOf("f′(x) = −6x + 6", "f′(0) = 6"), "Ableitung bilden und x = 0 einsetzen.", "Die Steigung") { listOf(g.curve("bahn", "cyan", arc), g.axis(), g.line("tangente", 0.0, 0.0, 0.5, 3.0, "yellow")) }),
        part("d", 4, num("abi-bogen-d", "Leuchtfläche", "Die Fläche zwischen Bogen und Boden wird mit Licht gefüllt. Wie groß ist sie?", "A", 0..8, 4, 0, suffix = " m²", way =
            listOf("∫₀² (−3x² + 6x) dx = [−x³ + 3x²]₀²", "= −8 + 12", "= 4 m²"), tip = "Stammfunktion: F(x) = −x³ + 3x².", what = "Die Fläche",
            mistakes = mapOf(6 to "6 wäre ein Rechteck aus Breite 2 und Höhe 3. Der Bogen füllt es nicht aus – nimm das Integral.")) {
            val pts = (0..32).map { val x = 2.0 * it / 32; g.at(x, arc(x)) }
            listOf(Stage.edges("fläche", pts.zipWithNext() + listOf(pts.last() to pts.first()), "yellow", faces = listOf(pts)), g.axis()) }),
    ))

    private val drumUrn = Urn(5, 2, { V3(4.0, 1.6 + it * 1.2, 2.2) }, 0.5)
    private fun urn() = drumUrn.balls()
    private val drum = Exam("trommel", "Lostrommel", "🎰", "In einer Lostrommel liegen 5 Kugeln, 2 davon sind pink (Gewinn).", listOf(
        part("a", 2, num("abi-trommel-a", "Ein Zug", "Wie wahrscheinlich ist ein Gewinn bei einem Zug?", "P", 0..10, 40, 0, listOf("P = 2 : 5", "= 40 %"), "Günstige durch mögliche.", "Die Wahrscheinlichkeit", step = 10, suffix = " %", play = drumUrn.one(0)) { urn() }),
        part("b", 3, num("abi-trommel-b", "Zweimal mit Zurücklegen", "Zwei Züge mit Zurücklegen: Wie wahrscheinlich sind zwei Gewinne?", "P", 0..25, 16, 0, listOf("0,4 · 0,4", "= 0,16 = 16 %"), "Pfadregel: multiplizieren.", "Die Wahrscheinlichkeit",
            mapOf(80 to "Nicht addieren – entlang des Pfades wird multipliziert."), step = 4, suffix = " %", play = drumUrn.withReplacement(listOf(0, 1))) { urn() }),
        part("c", 3, num("abi-trommel-c", "Zweimal ohne Zurücklegen", "Zwei Züge ohne Zurücklegen: Wie wahrscheinlich sind jetzt zwei Gewinne?", "P", 0..20, 10, 0, listOf("erster Zug 2 : 5, zweiter 1 : 4", "⅖ · ¼ = 2 : 20", "= 10 %"), "Beim zweiten Zug fehlt eine pinke Kugel – und insgesamt eine.", "Die Wahrscheinlichkeit",
            step = 5, suffix = " %", play = drumUrn.without(listOf(0, 1))) { urn() }),
        part("d", 4, num("abi-trommel-d", "Viele Züge", "Es wird 20-mal mit Zurücklegen gezogen. Mit wie vielen Gewinnen ist im Mittel zu rechnen?", "μ", 0..12, 8, 0, listOf("μ = n · p", "= 20 · 0,4", "= 8"), "Erwartungswert der Binomialverteilung.", "Der Erwartungswert",
            play = drumUrn.explain("Bei jedem Zug gewinnen 2 von 5 Kugeln: p = 0,4.", "20 Züge mit Zurücklegen: im Mittel 20 · 0,4 = 8 Gewinne.")) { urn() }),
    ))
    val all = listOf(bow, drum)
}
