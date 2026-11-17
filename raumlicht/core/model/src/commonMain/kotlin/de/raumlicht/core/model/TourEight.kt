package de.raumlicht.core.model

import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt
import kotlin.math.E
import kotlin.math.exp
import kotlin.math.ln

/** Kapitel 24–25 (seit 2.8.0): Binomialverteilung (einzeln und kumuliert) und e-Funktion mit Logarithmus. */
internal object TourEight {
    // ───────────────────────── 24 · Binomialverteilung ─────────────────────────
    /** Die Verteilung als Lichtsäulen: Höhe = P(X = k); [lit] färbt die Säulen, die zum Ereignis gehören */
    private fun bars(b: Binomial, lit: (Int) -> Boolean): List<Obj> {
        val slot = 6.4 / (b.n + 1); val top = b.single.max()
        return (0..b.n).flatMap { k ->
            val y0 = 0.8 + k * slot + slot * 0.12; val y1 = 0.8 + (k + 1) * slot - slot * 0.12; val h = 0.08 + 3.6 * b.single[k] / top
            val q = listOf(V3(4.0, y0, 1.0), V3(4.0, y1, 1.0), V3(4.0, y1, 1.0 + h), V3(4.0, y0, 1.0 + h)); val color = if (lit(k)) "yellow" else "cyan"
            listOf(Stage.edges("s$k", q.indices.map { q[it] to q[(it + 1) % 4] }, color, faces = listOf(q)),
                Stage.point("k$k", V3(4.0, (y0 + y1) / 2, 0.55), color, "$k", style = "ghost", drop = false))
        } + Stage.podium()
    }
    private fun barTop(b: Binomial, k: Int): V3 { val slot = 6.4 / (b.n + 1); return V3(4.0, 0.8 + (k + 0.5) * slot, 1.08 + 3.6 * b.single[k] / b.single.max()) }
    /** Lux zeigt nacheinander auf die Säulen, um die es geht, und sagt dazu, was sie bedeuten */
    private fun show(b: Binomial, steps: List<Pair<String, List<Int>>>) = Finale(acts = steps.map { (say, ks) -> Act(say, beams = ks.map { Beam(V3(8, 0, 1), barTop(b, it), "yellow", false) }, ms = 1100) })
    private fun percent(id: String, title: String, mission: String, b: Binomial, lit: (Int) -> Boolean, solution: Int, step: Int, max: Int, start: Int, lines: List<String>, what: String, ok: String,
                        tip: String, way: List<String>, mistakes: Map<Int, String>, abi: Boolean = false, steps: List<Pair<String, List<Int>>>) =
        Families.scalar(id, 24, title, mission, "P", start / step..max / step, solution / step, start / step, tip, way, "pink", step = step, suffix = " %", abi = abi,
            formula = { a -> lines.map { listOf(Txt(it)) } + listOf(listOf(Txt("$what = "), Num("${a * step} %", "a"))) },
            finale = { show(b, steps) },
            objects = { _, _ -> bars(b, lit) },
            check = { a -> Families.compare(a * step, solution, "Die Wahrscheinlichkeit", ok, mistakes) })

    private val two = Binomial(2, 0.4); private val pair = Binomial(2, 0.3); private val ten = Binomial(10, 0.5)
    private const val TABLE = "Tabelle: P(X ≤ 3) = 0,172 | P(X ≤ 4) = 0,377 | P(X ≤ 6) = 0,828 | P(X ≤ 7) = 0,945"
    private val ch24 = listOf(
        percent("24-1", "Genau ein Treffer", "Ein Blitzlicht zündet mit einer Wahrscheinlichkeit von 40 %. Ich löse zweimal aus. Wie wahrscheinlich zündet es genau einmal?", two, { it == 1 }, 48, 4, 100, 0,
            listOf("n = 2, p = 0,4", "P(X = k) = (n über k) · pᵏ · (1 − p)ⁿ⁻ᵏ"), "P(X = 1)", "48 % – zwei Reihenfolgen mit je 24 %.",
            tip = "Formel von Bernoulli: (2 über 1) · 0,4 · 0,6. Der Binomialkoeffizient zählt die Reihenfolgen.",
            way = listOf("(2 über 1) = 2 Reihenfolgen", "jede: 0,4 · 0,6 = 0,24", "P(X = 1) = 2 · 0,24 = 48 %"),
            mistakes = mapOf(24 to "0,4 · 0,6 = 24 % ist nur eine Reihenfolge. Es gibt zwei: Treffer–Niete und Niete–Treffer.", 16 to "16 % ist P(X = 2): beide zünden. Gesucht ist genau einmal."),
            steps = listOf("Die mittlere Säule: genau ein Treffer. Den gibt es in zwei Reihenfolgen." to listOf(1), "Zweimal 24 % macht 48 %." to listOf(1))),
        percent("24-2", "Höchstens ein Treffer", "Dieselben zwei Versuche. Wie wahrscheinlich zündet es höchstens einmal? Jetzt wird aufsummiert.", two, { it <= 1 }, 84, 4, 100, 0,
            listOf("n = 2, p = 0,4", "P(X = 0) = 0,36 | P(X = 1) = 0,48 | P(X = 2) = 0,16"), "P(X ≤ 1)", "84 % – die Säulen für 0 und 1 zusammen.",
            tip = "Kumuliert heißt aufsummiert: P(X ≤ 1) = P(X = 0) + P(X = 1).",
            way = listOf("P(X = 0) = 0,6 · 0,6 = 0,36", "P(X = 1) = 0,48", "P(X ≤ 1) = 0,36 + 0,48 = 84 %"),
            mistakes = mapOf(48 to "48 % ist nur „genau einmal“. „Höchstens einmal“ schließt „keinmal“ mit ein – aufsummieren.", 36 to "36 % ist nur „keinmal“. Dazu kommt „genau einmal“."),
            steps = listOf("Keinmal: 36 %." to listOf(0), "Dazu genau einmal: 48 %. Zusammen 84 %." to listOf(0, 1))),
        percent("24-3", "Mindestens ein Treffer", "Ein anderes Blitzlicht zündet nur mit 30 %. Zwei Versuche: Wie wahrscheinlich zündet es mindestens einmal?", pair, { it >= 1 }, 51, 3, 99, 0,
            listOf("n = 2, p = 0,3", "P(X ≥ 1) = 1 − P(X = 0)"), "P(X ≥ 1)", "51 % – alles außer „keinmal“.",
            tip = "Mindestens einmal ist das Gegenteil von keinmal: 1 − P(X = 0) = 1 − 0,7².",
            way = listOf("P(X = 0) = 0,7 · 0,7 = 0,49", "P(X ≥ 1) = 1 − 0,49", "= 51 %"),
            mistakes = mapOf(42 to "42 % ist „genau einmal“. Bei „mindestens einmal“ zählt „zweimal“ auch mit.", 48 to "Fast: 49 % ist P(X = 0). Gesucht ist das Gegenereignis.", 60 to "0,3 + 0,3 = 60 % zählt den Fall „beide“ doppelt. Rechne über das Gegenereignis."),
            steps = listOf("Nur diese Säule zählt nicht: keinmal, 49 %." to listOf(0), "Alles andere bleibt übrig: 51 %." to listOf(1, 2))),
        percent("24-4", "Von … bis", "Zehn Blitzlichter, jedes zündet mit einer Wahrscheinlichkeit von 50 %. Wie wahrscheinlich zünden mindestens 4 und höchstens 6 davon? Lies aus der kumulierten Tabelle ab (auf ganze Prozent).", ten, { it in 4..6 }, 66, 1, 90, 40,
            listOf("n = 10, p = 0,5", TABLE), "P(4 ≤ X ≤ 6)", "66 % – die drei mittleren Säulen.",
            tip = "P(4 ≤ X ≤ 6) = P(X ≤ 6) − P(X ≤ 3). Abgezogen wird alles unterhalb von 4 – also bis einschließlich 3.",
            way = listOf("P(X ≤ 6) = 0,828", "P(X ≤ 3) = 0,172", "0,828 − 0,172 = 0,656 ≈ 66 %"),
            mistakes = mapOf(45 to "Du hast P(X ≤ 4) abgezogen – dann fehlt die 4. Abziehen musst du P(X ≤ 3).", 83 to "83 % ist P(X ≤ 6). Zieh noch ab, was unter 4 liegt: P(X ≤ 3)."),
            steps = listOf("Bis 6 sind es 82,8 %." to listOf(0, 1, 2, 3, 4, 5, 6), "Davon weg, was unter 4 liegt: 17,2 %. Es bleiben die drei mittleren Säulen." to listOf(4, 5, 6))),
        Families.scalar("24-5", 24, "Abi-Check: Wie viele reichen?", "Zehn Blitzlichter, jedes zündet mit einer Wahrscheinlichkeit von 50 %. Ich will mit mindestens 90 % Sicherheit sagen: „Es zünden höchstens k.“ Wie klein darf k sein?", "k", 0..10, 7, 10, abi = true,
            tip = "Gesucht ist das kleinste k mit P(X ≤ k) ≥ 0,9. In der kumulierten Tabelle von links nach rechts suchen.",
            way = listOf("P(X ≤ 6) = 0,828 – noch unter 90 %", "P(X ≤ 7) = 0,945 – reicht", "kleinstes k: 7"), color = "pink",
            formula = { a -> listOf(listOf(Txt("n = 10, p = 0,5")), listOf(Txt(TABLE)), listOf(Txt("P(X ≤ k) ≥ 90 % ab k = "), Num(Geo.num(a), "a"))) },
            finale = { show(ten, listOf("Bis 6 zusammen: 82,8 % – das reicht noch nicht." to (0..6).toList(), "Mit der Säule für 7 sind es 94,5 %. Also k = 7." to listOf(7))) },
            objects = { a, _ -> bars(ten) { it <= a } },
            check = { a -> when {
                a == 7 -> Outcome(true, "k = 7: P(X ≤ 7) = 94,5 % – das kleinste k mit mindestens 90 %.")
                a < 7 -> Outcome(false, "P(X ≤ $a) = ${Geo.num(ten.atMost(a) * 100)} % – das sind noch keine 90 %.")
                else -> Outcome(false, "P(X ≤ $a) = ${Geo.num(ten.atMost(a) * 100)} % reicht – aber es geht mit einem kleineren k.") } }),
    )

    // ───────────────────────── 25 · e-Funktion und Logarithmus ─────────────────────────
    private fun curve(id: String, title: String, mission: String, g: TourSeven.Graph, text: String, name: String, range: IntRange, solution: Int, start: Int, ok: String,
                      tip: String, way: List<String>, abi: Boolean = false, mistakes: Map<Int, String> = emptyMap(), extra: List<List<Part>> = emptyList(), hint: (Int) -> String) =
        Families.scalar(id, 25, title, mission, name, range, solution, start, tip, way, "yellow", abi = abi,
            lux = { a, _ -> g.at(a.toDouble()) + V3(0.0, 0.0, 0.45) },
            formula = { a -> listOf(listOf(Txt("f(x) = $text"))) + extra + listOf(listOf(Txt("$name = "), Num(Geo.num(a), "a"))) },
            objects = { a, _ -> listOf(g.curve(), g.axis(), Stage.point("stelle", g.at(a.toDouble()), "yellow", drop = false)) },
            check = { a -> MistakeBook.add(mistakes.values); if (a == solution) Outcome(true, ok) else Outcome(false, mistakes[a] ?: hint(a)) })
    private fun area(id: String, title: String, mission: String, g: TourSeven.Graph, from: Double, to: Double, lines: List<String>, solution: Int, max: Int, ok: String,
                     tip: String, way: List<String>, mistakes: Map<Int, String>, abi: Boolean = false) =
        Families.scalar(id, 25, title, mission, "I", 0..max, solution, 0, tip, way, "yellow", abi = abi,
            formula = { a -> lines.map { listOf(Txt(it)) } + listOf(listOf(Txt("∫ f(x) dx = "), Num(Geo.num(a), "a"))) },
            objects = { _, _ -> listOf(g.area("fläche", from, to), g.curve(), g.axis()) },
            check = { a -> Families.compare(a, solution, "Der Wert", ok, mistakes) })

    private val ch25 = run {
        val g1 = TourSeven.Graph(0.0, 3.0, 2.2, 0.8, 0.6, 1.6) { exp(2 * it - 4) - 1 }
        val g2 = TourSeven.Graph(2.2, 6.0, 1.2, 0.2, 1.2, 2.5) { ln(it - 2) }
        val g3 = TourSeven.Graph(-2.0, 2.2, 1.6, 3.6, 0.9, 3.6) { (it - 2) * exp(it) }
        val g4 = TourSeven.Graph(0.0, 5.0, 1.4, 0.6, 9.0, 1.0) { it * exp(-it) }
        val g5 = TourSeven.Graph(0.6, 6.2, 1.15, 0.2, 0.9, 2.6) { it - 4 * ln(it) }
        val g6 = TourSeven.Graph(0.0, 1.3, 5.0, 0.8, 1.2, 0.8) { exp(it) }
        val g7 = TourSeven.Graph(0.5, 8.0, 0.9, 0.3, 1.0, 0.8) { 2 / it }
        fun r(v: Double) = Geo.num(kotlin.math.round(v * 100) / 100)
        listOf(
            curve("25-1", "e hoch – wann ist es 1?", "Die Bahn f kreuzt die x-Achse genau einmal. An welcher Stelle?", g1, "e^(2x − 4) − 1", "x", 0..3, 2, 0,
                "Nullstelle bei x = 2: Dort ist der Exponent 0 und e⁰ = 1.", tip = "e^(2x − 4) = 1 gilt genau dann, wenn der Exponent 0 ist. Oder beide Seiten logarithmieren: ln(1) = 0.",
                way = listOf("e^(2x − 4) = 1", "ln auf beiden Seiten: 2x − 4 = 0", "x = 2")) { "f(${Geo.num(it)}) ${Geo.eq(exp(2.0 * it - 4) - 1)}. An der Nullstelle muss 0 herauskommen." },
            curve("25-2", "Nullstelle des Logarithmus", "Diese Bahn gibt es erst rechts von x = 2. Wo kreuzt sie die x-Achse?", g2, "ln(x − 2)", "x", 3..6, 3, 6,
                "Nullstelle bei x = 3, denn ln(1) = 0.", tip = "Der Logarithmus ist genau dann 0, wenn in der Klammer 1 steht: x − 2 = 1.",
                way = listOf("ln(x − 2) = 0", "x − 2 = e⁰ = 1", "x = 3 (Definitionsbereich: x > 2)"), extra = listOf(listOf(Txt("definiert für x > 2")))) { "f(${Geo.num(it)}) = ln(${it - 2}) ${Geo.eq(ln(it - 2.0))}. An der Nullstelle muss 0 herauskommen." },
            curve("25-3", "Tiefpunkt mit Produktregel", "Kurvendiskussion mit e-Funktion: Bei welchem x liegt der Tiefpunkt der Bahn?", g3, "(x − 2) · e^x", "x", -2..2, 1, -2,
                "Tiefpunkt bei x = 1: f′(1) = 0 und f″(1) = e > 0.", tip = "Produktregel: f′(x) = 1 · e^x + (x − 2) · e^x = (x − 1) · e^x. Da e^x nie null wird, entscheidet die Klammer.",
                way = listOf("f′(x) = e^x + (x − 2) · e^x = (x − 1) · e^x", "f′(x) = 0 → x = 1 (e^x ist nie 0)", "f″(x) = x · e^x, f″(1) = e > 0 → Tiefpunkt T(1|−e)"),
                mistakes = mapOf(2 to "x = 2 ist die Nullstelle (dort ist x − 2 = 0). Für den Tiefpunkt brauchst du f′(x) = 0.", 0 to "Bei x = 0 ist f″ = 0 – das ist die Wendestelle. Der Tiefpunkt liegt dort, wo f′ = 0 ist.")) { "f′(${Geo.num(it)}) ${Geo.eq((it - 1) * exp(it.toDouble()))}. Am Tiefpunkt ist die Steigung 0." },
            curve("25-4", "Wendestelle", "Die Bahn steigt kurz an und läuft dann flach aus. Wo wechselt sie die Krümmung?", g4, "x · e^(−x)", "x", 0..5, 2, 0,
                "Wendestelle bei x = 2: Dort fällt die Bahn am steilsten.", tip = "Zweimal ableiten (Produkt- und Kettenregel): f′(x) = (1 − x) · e^(−x), f″(x) = (x − 2) · e^(−x).",
                way = listOf("f′(x) = e^(−x) − x · e^(−x) = (1 − x) · e^(−x)", "f″(x) = −e^(−x) − (1 − x) · e^(−x) = (x − 2) · e^(−x)", "f″(x) = 0 → x = 2, und f″ wechselt dort das Vorzeichen (von − nach +)"),
                mistakes = mapOf(1 to "Bei x = 1 ist f′ = 0 – das ist der Hochpunkt. Für die Wendestelle brauchst du f″ = 0.")) { "f″(${Geo.num(it)}) ${Geo.eq((it - 2) * exp(-it.toDouble()))}. An der Wendestelle muss 0 herauskommen." },
            curve("25-5", "Tiefpunkt mit ln", "Jetzt steckt ein Logarithmus in der Bahn. Bei welchem x liegt ihr Tiefpunkt?", g5, "x − 4 · ln(x)", "x", 1..6, 4, 1,
                "Tiefpunkt bei x = 4: f′(4) = 1 − 1 = 0.", tip = "Die Ableitung von ln(x) ist 1 : x. Also f′(x) = 1 − 4 : x.",
                way = listOf("f′(x) = 1 − 4 : x", "f′(x) = 0 → x = 4", "f″(x) = 4 : x² > 0 → Tiefpunkt"), extra = listOf(listOf(Txt("definiert für x > 0")))) { "f′(${Geo.num(it)}) = 1 − 4 : $it ${Geo.eq(1 - 4.0 / it)}. Am Tiefpunkt ist die Steigung 0." },
            area("25-6", "Fläche unter e^x", "Unter der e-Funktion liegt ein Leuchtfeld von x = 0 bis x = ln(3). Wie groß ist es?", g6, 0.0, ln(3.0), listOf("f(x) = e^x,  x von 0 bis ln(3)"), 2, 6,
                "Genau 2 – denn e^(ln 3) = 3 und e⁰ = 1.", tip = "Die e-Funktion ist ihre eigene Stammfunktion: F(x) = e^x. Und e^(ln 3) ist einfach 3.",
                way = listOf("∫ e^x dx = [e^x] von 0 bis ln(3)", "= e^(ln 3) − e⁰", "= 3 − 1 = 2"),
                mistakes = mapOf(3 to "3 ist e^(ln 3), der Wert an der oberen Grenze. Zieh noch den an der unteren ab: e⁰ = 1.")),
            area("25-7", "Abi-Check: Integral mit ln", "Die Fläche unter f von x = 1 bis x = e² wird beleuchtet. Wie groß ist sie?", g7, 1.0, E * E, listOf("f(x) = 2 : x,  x von 1 bis e²"), 4, 8,
                "Genau 4 – denn ln(e²) = 2 und ln(1) = 0.", tip = "Die Stammfunktion von 1 : x ist ln(x). Also F(x) = 2 · ln(x).",
                way = listOf("∫ 2 : x dx = [2 · ln(x)] von 1 bis e²", "= 2 · ln(e²) − 2 · ln(1)", "= 2 · 2 − 0 = 4"), abi = true,
                mistakes = mapOf(2 to "ln(e²) = 2 stimmt – aber vor dem Bruch steht noch der Faktor 2.")),
        )
    }

    val chapters = listOf(
        Chapter(24, "Binomialverteilung", "genau, höchstens, mindestens k Treffer – einzeln und kumuliert, Tabelle lesen", "Blitzlicht-Gewitter", "📊", "pink", listOf(
            "Immer derselbe Versuch, immer dieselbe Chance, n-mal hintereinander: Das ist eine Bernoulli-Kette.",
            "P(X = k) ist eine einzelne Säule. P(X ≤ k) zählt alle Säulen bis k zusammen – das heißt kumuliert.",
            "Mindestens k? Dann rechne über das Gegenteil: 1 − P(X ≤ k − 1). Im Binomial-Labor unter „Mehr“ kannst du alles selbst einstellen."), ch24, Area.STOCHASTIK),
        Chapter(25, "e-Funktion & Logarithmus", "Gleichungen, Kurvendiskussion mit Produkt- und Kettenregel, Integrale mit e^x und 1 : x", "Wachstumslabor", "🌱", "green", listOf(
            "Die e-Funktion ist ihre eigene Ableitung – und ihre eigene Stammfunktion. Der Logarithmus ln macht sie rückgängig.",
            "e^x wird nie null. Bei einer Kurvendiskussion entscheidet deshalb immer der Faktor davor.",
            "Die Ableitung von ln(x) ist 1 : x. Umgekehrt ist ln(x) die Stammfunktion von 1 : x."), ch25, Area.ANALYSIS),
    )
}
