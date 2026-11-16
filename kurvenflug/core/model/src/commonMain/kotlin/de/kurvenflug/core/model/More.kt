package de.kurvenflug.core.model

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Inhaltspaket 0.20: Maßstab (Kl. 5–6), Prozent & Zinseszins (Kl. 7–8), beschleunigte Bewegung (Physik 9–10),
 * Potenzfunktionen (Kl. 10), verschobene Wellen (Kl. 10–11), waagerechter Wurf (Physik 10), e-Funktion (Kl. 11–12).
 * Jedes Level hat Tipp, eigenen Rechenweg ([Level.walk]) und eigene Rechen-Frage ([Level.quiz]) als Erklärung.
 */
object More {
    private fun t(x: Double, y: Double, r: Double = 0.4) = Body(x, y, r)
    private fun p(key: String, label: String, min: Double, max: Double, step: Double, start: Double, unit: String = "") = Param(key, label, min, max, step, start, unit)
    private fun s(text: String, answer: Double, sets: String? = null, extra: List<Double> = emptyList()) = Walkthrough.step(text, answer, sets, extra)
    private fun f(x: Double, d: Int = 2) = num(x, d)

    // ------------------------------------------------------------------ 29 Maßstab & Karten (Kl. 5–6)

    private val mx = p("x", "x", -9.0, 9.0, 1.0, 0.0)
    private val my = p("y", "y", -9.0, 9.0, 1.0, 0.0)

    /** [scale] Meter je Kästchen; Station [km] als (rechts | hoch) in echten Einheiten */
    private fun map(n: Int, title: String, scale: Double, unit: String, rx: Double, ry: Double, intro: String): Level {
        val x = rx / scale; val y = ry / scale
        return Level("29-$n", 29, n, title, Kind.POINT,
            "$intro 1 Kästchen = ${f(scale)} $unit. Die Station liegt ${f(kotlin.math.abs(rx))} $unit ${if (rx >= 0) "östlich" else "westlich"} " +
                "und ${f(kotlin.math.abs(ry))} $unit ${if (ry >= 0) "nördlich" else "südlich"} der Basis (0 | 0).",
            listOf(t(x, y, 0.5)), listOf(mx, my), mapOf("x" to x, "y" to y),
            tip = "Echte Länge : Maßstab = Kästchen. ${f(rx)} : ${f(scale)} = ${f(x)}, ${f(ry)} : ${f(scale)} = ${f(y)}.",
            quiz = listOf("Kästchen = ${f(kotlin.math.abs(rx))} : ${f(scale)}", "Kästchen = ${f(kotlin.math.abs(rx))} · ${f(scale)}", "Kästchen = ${f(kotlin.math.abs(rx))} − ${f(scale)}",
                "Wie rechnest du ${f(kotlin.math.abs(rx))} $unit in Kästchen um?"),
            walk = listOf(s("x = ${f(rx)} : ${f(scale)} = ?", x, "x"), s("y = ${f(ry)} : ${f(scale)} = ?", y, "y")))
    }

    private val ch29 = Chapter(29, "Maßstab & Karten", "Klasse 5–6", "🗺️",
        listOf(
            "Karten sind verkleinert. Der Maßstab sagt, wie viel ein Kästchen in echt ist.",
            "Echte Länge geteilt durch den Maßstab = Kästchen auf der Karte.",
            "Osten ist rechts (x plus), Westen links (x minus), Norden oben (y plus), Süden unten (y minus).",
        ),
        listOf(
            map(1, "Erste Karte", 2.0, "km", 6.0, 4.0, "Funkspruch!"),
            map(2, "Westwärts", 5.0, "km", -20.0, 15.0, "Ein Signal aus dem Westen."),
            map(3, "Tief im Süden", 10.0, "km", 30.0, -50.0, "Großer Maßstab:"),
            map(4, "Kleiner Maßstab", 0.5, "km", -3.0, -2.5, "Ganz nah dran:"),
            map(5, "Sternenkarte", 100.0, "Lichtjahre", 700.0, 300.0, "Im All rechnet man in Lichtjahren!"),
        ))

    // ------------------------------------------------------------------ 23 Prozent & Zinseszins (Kl. 7–8)

    private val zb = p("b", "Faktor b", 0.3, 2.0, 0.05, 1.0)
    private val za = p("a", "Start a", 0.5, 9.0, 0.5, 1.0)

    private fun pct(n: Int, title: String, a: Double, b: Double, x: Double, task: String, tip: String, findA: Boolean = false): Level {
        val y = a * powK(b, x)
        val pc = kotlin.math.round((b - 1) * 100).toInt()
        val change = if (pc >= 0) "+ $pc %" else "− ${-pc} %"
        return Level("23-$n", 23, n, title, Kind.EXP, task, listOf(t(x, y, 0.25)),
            if (findA) listOf(za) else listOf(zb), if (findA) mapOf("a" to a) else mapOf("b" to b),
            fixed = (if (findA) mapOf("b" to b) else mapOf("a" to a)) + ("d" to 0.0), tip = tip,
            quiz = if (findA) listOf("a = ${f(y)} : ${f(b)}^${f(x)}", "a = ${f(y)} · ${f(b)}^${f(x)}", "a = ${f(y)} − ${f(b)} · ${f(x)}", "Wie findest du den Startwert?")
                else listOf("b = 1 ${if (pc >= 0) "+" else "−"} ${kotlin.math.abs(pc)} : 100 = ${f(b)}",
                    if (kotlin.math.abs(pc) / 100.0 == b) "b = ${kotlin.math.abs(pc)}" else "b = ${kotlin.math.abs(pc)} : 100 = ${f(kotlin.math.abs(pc) / 100.0)}",
                    "b = 1 ${if (pc >= 0) "−" else "+"} ${kotlin.math.abs(pc)} : 100 = ${f(2 - b)}", "Welcher Faktor gehört zu $change?"),
            walk = if (findA) listOf(s("${f(b)}^${f(x)} = ?", powK(b, x)), s("a = ${f(y)} : ${f(powK(b, x))} = ?", a, "a"))
                else listOf(s("$change heißt: mal (1 ${if (pc >= 0) "+" else "−"} ${kotlin.math.abs(pc)} : 100). b = ?", b, "b", extra = listOf(kotlin.math.abs(pc) / 100.0, 2 - b))))
    }

    private val ch23 = Chapter(23, "Prozent & Zinseszins", "Klasse 7–8", "💶",
        listOf(
            "Prozent heißt „von Hundert“. 25 % mehr ist dasselbe wie mal 1,25.",
            "Wächst etwas jedes Jahr um p %, wird jedes Jahr mit b = 1 + p : 100 malgenommen. Weniger: b = 1 − p : 100.",
            "Nach x Jahren: y = a · bˣ. a ist der Startwert. Das ist Zinseszins!",
        ),
        listOf(
            pct(1, "Taschengeld-Boost", 4.0, 1.25, 2.0, "Volts Sparschwein (4 Taler) wächst jedes Jahr um 25 %. Stell den Faktor b ein!", "25 % mehr: b = 1 + 0,25 = 1,25."),
            pct(2, "Rabattschlacht", 8.0, 0.8, 2.0, "Ein Preis von 8 sinkt jedes Jahr um 20 %. Welcher Faktor?", "20 % weniger: b = 1 − 0,2 = 0,8."),
            pct(3, "Algenblüte", 2.0, 1.5, 3.0, "Algen wachsen jedes Jahr um 50 %. Start: 2. Faktor?", "50 % mehr: b = 1,5. Probe: 2 · 1,5³ = 6,75."),
            pct(4, "Wie viel am Anfang?", 5.0, 1.1, 2.0, "Mit 10 % Zinsen sind es nach 2 Jahren 6,05. Wie viel war es am Anfang?", "a = 6,05 : 1,1² = 6,05 : 1,21 = 5.", findA = true),
            pct(5, "Halbierung", 8.0, 0.5, 3.0, "Jedes Jahr halbiert sich der Vorrat (8). Welcher Faktor – und wie viel Prozent sind das?", "Halbieren = − 50 % → b = 0,5."),
            pct(6, "Zinseszins", 5.0, 1.1, 4.0, "5 Taler, 10 % Zinsen pro Jahr, 4 Jahre lang. Stell b ein!", "b = 1,1. Nach 4 Jahren: 5 · 1,1⁴ ≈ 7,32."),
        ))

    // ------------------------------------------------------------------ 24 Beschleunigte Bewegung (Physik 9–10)

    private val ka = p("a", "a", -4.0, 4.0, 0.5, 0.5, " m/s²")
    private val kv = p("v0", "v₀", -2.0, 8.0, 0.5, 1.0, " m/s")

    private fun accel(n: Int, title: String, a: Double, v0: Double, stations: List<Pair<Double, Double>>, keys: Set<String>, task: String, tip: String,
                      quiz: List<String>, walk: List<Step>): Level =
        Level("24-$n", 24, n, title, Kind.ACCEL, task, stations.map { t(it.first, it.second, 0.4) },
            listOfNotNull(ka.takeIf { "a" in keys }, kv.takeIf { "v0" in keys }),
            buildMap { if ("a" in keys) put("a", a); if ("v0" in keys) put("v0", v0) },
            fixed = buildMap { if ("a" !in keys) put("a", a); if ("v0" !in keys) put("v0", v0); put("s0", 0.0) }, tip = tip, quiz = quiz, walk = walk)

    private val ch24 = Chapter(24, "Beschleunigte Bewegung", "Physik Klasse 9–10", "🚗",
        listOf(
            "Ein Raumgleiter wird immer schneller – er beschleunigt. Der Weg wächst dann nicht gerade, sondern als Parabel.",
            "Weg nach t Sekunden: s = ½ · a · t² + v₀ · t. a ist die Beschleunigung, v₀ das Anfangstempo.",
            "Bremsen heißt: a ist negativ. Stell a oder v₀ so ein, dass die Bahn durch die Station geht!",
        ),
        listOf(
            accel(1, "Losdüsen", 2.0, 0.0, listOf(3.0 to 9.0), setOf("a"), "Aus dem Stand: Nach 3 s soll der Gleiter 9 m weit sein. Welche Beschleunigung a?",
                "s = ½ · a · t² → 9 = ½ · a · 9 → a = 2.",
                listOf("a = 2 · s : t² = 2 · 9 : 3²", "a = s : t = 9 : 3", "a = s : t² = 9 : 3²"), listOf(s("t² = 3² = ?", 9.0), s("a = 2 · 9 : 9 = ?", 2.0, "a"))),
            accel(2, "Sanfter Start", 1.0, 0.0, listOf(4.0 to 8.0), setOf("a"), "Aus dem Stand, nach 4 s genau 8 m. a = ?",
                "8 = ½ · a · 16 → a = 1.",
                listOf("a = 2 · 8 : 4²", "a = 8 : 4", "a = 8 : 4²"), listOf(s("½ · t² = ½ · 16 = ?", 8.0), s("a = 8 : 8 = ?", 1.0, "a"))),
            accel(3, "Mit Schwung", 2.0, 1.0, listOf(2.0 to 6.0), setOf("a"), "Startet mit v₀ = 1 m/s. Nach 2 s soll er bei 6 m sein. a = ?",
                "6 = ½ · a · 4 + 1 · 2 → 2a = 4 → a = 2.",
                listOf("6 − 1 · 2 = ½ · a · 2²", "6 = ½ · a · 2²", "6 − 1 = a · 2"), listOf(s("v₀ · t = 1 · 2 = ?", 2.0), s("Rest: 6 − 2 = ?", 4.0), s("a = 4 : (½ · 4) = ?", 2.0, "a"))),
            accel(4, "Bremsmanöver", -1.0, 4.0, listOf(4.0 to 8.0), setOf("v0"), "Der Gleiter bremst mit a = −1 m/s². Nach 4 s ist er bei 8 m. Wie schnell war er am Anfang?",
                "8 = ½ · (−1) · 16 + v₀ · 4 = −8 + 4 v₀ → v₀ = 4.",
                listOf("v₀ = (8 + 8) : 4", "v₀ = 8 : 4", "v₀ = (8 − 8) : 4"), listOf(s("½ · (−1) · 4² = ?", -8.0), s("v₀ = (8 − (−8)) : 4 = ?", 4.0, "v0"))),
            accel(5, "Zwei Messpunkte", 1.0, 2.0, listOf(1.0 to 2.5, 2.0 to 6.0), setOf("a", "v0"), "Messung: nach 1 s bei 2,5 m, nach 2 s bei 6 m. Finde a und v₀!",
                "½a + v₀ = 2,5 und 2a + 2v₀ = 6 → a = 1, v₀ = 2.",
                listOf("Zwei Gleichungen aufstellen und lösen", "a = 6 : 2", "v₀ = 2,5 : 1"),
                listOf(s("Gleichung 2 halbiert: a + v₀ = ?", 3.0), s("Minus Gleichung 1: ½ · a = 3 − 2,5. a = ?", 1.0, "a"), s("v₀ = 2,5 − ½ · 1 = ?", 2.0, "v0"))),
            accel(6, "Notbremsung", -2.0, 6.0, listOf(3.0 to 9.0), setOf("a"), "Mit 6 m/s heran, nach 3 s genau an der Station (9 m) – wie stark bremsen?",
                "9 = ½ · a · 9 + 18 → 4,5a = −9 → a = −2.",
                listOf("a = 2 · (9 − 6 · 3) : 3²", "a = 2 · 9 : 3²", "a = (9 − 6) : 3"), listOf(s("v₀ · t = 6 · 3 = ?", 18.0), s("9 − 18 = ?", -9.0), s("a = 2 · (−9) : 9 = ?", -2.0, "a"))),
        ))

    // ------------------------------------------------------------------ 25 Potenzfunktionen (Kl. 10)

    private val qa = p("a", "a", -3.0, 3.0, 0.5, 1.0)
    private val qn = p("n", "Hochzahl n", 1.0, 5.0, 1.0, 1.0)
    private val qd = p("d", "d", -8.0, 8.0, 1.0, 0.0)

    private fun power(n: Int, title: String, a: Double, k: Double, d: Double, x: Double, keys: Set<String>, task: String, tip: String, quiz: List<String>, walk: List<Step>, start: Map<String, Double> = emptyMap()): Level {
        val y = a * powK(x, k) + d
        val params = listOfNotNull(qa.takeIf { "a" in keys }?.let { it.copy(start = start["a"] ?: it.start) }, qn.takeIf { "n" in keys }, qd.takeIf { "d" in keys })
        return Level("25-$n", 25, n, title, Kind.POWER, task, listOf(t(x, y, 0.4)), params,
            buildMap { if ("a" in keys) put("a", a); if ("n" in keys) put("n", k); if ("d" in keys) put("d", d) },
            fixed = buildMap { if ("a" !in keys) put("a", a); if ("n" !in keys) put("n", k); if ("d" !in keys) put("d", d) }, tip = tip, quiz = quiz, walk = walk)
    }

    private val ch25 = Chapter(25, "Potenzfunktionen", "Klasse 10", "📊",
        listOf(
            "y = a · xⁿ: n ist die Hochzahl. n = 2 gibt eine Parabel, n = 3 eine S-Kurve, n = ½ eine Wurzel.",
            "Gerade Hochzahl: beide Seiten zeigen gleich (nach oben, wenn a > 0). Ungerade: links runter, rechts rauf.",
            "a streckt die Kurve, d schiebt sie nach oben oder unten.",
        ),
        listOf(
            power(1, "S-Kurve", 0.5, 3.0, 0.0, 2.0, setOf("a"), "y = a · x³. Die Station liegt bei (2 | 4). Wie groß ist a?", "2³ = 8 → a · 8 = 4 → a = 0,5.",
                listOf("a = 4 : 2³", "a = 4 : (3 · 2)", "a = 4 − 2³"), listOf(s("2³ = ?", 8.0), s("a = 4 : 8 = ?", 0.5, "a"))),
            power(2, "Welche Hochzahl?", 1.0, 3.0, 0.0, 2.0, setOf("n"), "y = xⁿ geht durch (2 | 8). Welche Hochzahl n?", "2 · 2 · 2 = 8 → n = 3.",
                listOf("2ⁿ = 8 → n = 3", "2 · n = 8 → n = 4", "n² = 8"), listOf(s("2 hoch wie viel ergibt 8? n = ?", 3.0, "n"))),
            power(3, "Kopfüber", -0.5, 4.0, 0.0, 2.0, setOf("a"), "y = a · x⁴. Die Station liegt unten bei (2 | −8). a = ?", "2⁴ = 16 → a = −8 : 16 = −0,5.",
                listOf("a = −8 : 2⁴", "a = −8 : (4 · 2)", "a = 8 : 2⁴"), listOf(s("2⁴ = ?", 16.0), s("a = −8 : 16 = ?", -0.5, "a"))),
            power(4, "Angehoben", 0.5, 3.0, -3.0, 2.0, setOf("d"), "y = 0,5 · x³ + d. Station bei (2 | 1). Wie weit verschieben?", "0,5 · 8 = 4 → d = 1 − 4 = −3.",
                listOf("d = 1 − 0,5 · 2³", "d = 1 + 0,5 · 2³", "d = 1 − 0,5 · 2"), listOf(s("0,5 · 2³ = ?", 4.0), s("d = 1 − 4 = ?", -3.0, "d"))),
            power(5, "Wurzelbogen", 3.0, 0.5, 0.0, 4.0, setOf("a"), "y = a · √x (Hochzahl ½). Station bei (4 | 6). a = ?", "√4 = 2 → a = 6 : 2 = 3.",
                listOf("a = 6 : √4", "a = 6 : 4", "a = 6 · √4"), listOf(s("√4 = ?", 2.0), s("a = 6 : 2 = ?", 3.0, "a"))),
        ))

    // ------------------------------------------------------------------ 26 Verschobene Wellen (Kl. 10–11)

    private val wc = p("c", "c", -4.0, 4.0, 0.5, 0.0)
    private val wa = p("a", "a", -5.0, 5.0, 0.5, 1.0)
    private val wd = p("d", "d", -5.0, 5.0, 0.5, 0.0)
    private val wb = p("b", "b", 0.25, 3.0, 0.25, 1.0)
    private const val HALF_PI = kotlin.math.PI / 2

    private fun wave(n: Int, title: String, a: Double, b: Double, c: Double, d: Double, keys: Set<String>, xs: List<Double>, task: String, tip: String, quiz: List<String>, walk: List<Step>): Level {
        fun y(x: Double) = a * kotlin.math.sin(b * (x - c)) + d
        val all = mapOf("a" to a, "b" to b, "c" to c, "d" to d)
        val params = listOfNotNull(wa.takeIf { "a" in keys }, wb.takeIf { "b" in keys }, wc.takeIf { "c" in keys }, wd.takeIf { "d" in keys })
        return Level("26-$n", 26, n, title, Kind.SINE, task, xs.map { t(it, y(it), 0.3) }, params, all.filterKeys { it in keys },
            fixed = all.filterKeys { it !in keys }, tip = tip, quiz = quiz, walk = walk)
    }

    private val ch26 = Chapter(26, "Verschobene Wellen", "Klasse 10–11", "🌊",
        listOf(
            "Bekannt: y = a · sin(b · x) + d. Neu ist c: y = a · sin(b · (x − c)) + d.",
            "c schiebt die Welle nach rechts (c > 0) oder links (c < 0). Dort, wo vorher der Nulldurchgang bei 0 war, ist er jetzt bei x = c.",
            "Eine Welle ist 2π : b lang, der Wellenberg liegt eine Viertelwelle (π : 2b) nach dem Nulldurchgang.",
        ),
        listOf(
            wave(1, "Verschoben", 2.0, 1.0, 1.0, 0.0, setOf("c"), listOf(1.0, 1.0 + HALF_PI), "y = 2 · sin(x − c). Die Welle soll bei x = 1 aufsteigend durch 0 gehen.",
                "Nulldurchgang bei x = c → c = 1.", listOf("c = 1 (Nulldurchgang wandert nach x = c)", "c = −1", "c = 2 (Wellenberg)"), listOf(s("Der Nulldurchgang liegt bei x = c. c = ?", 1.0, "c"))),
            wave(2, "Doppelt so schnell", 1.0, 2.0, 0.5, 1.0, setOf("c"), listOf(0.5, 0.5 + HALF_PI / 2), "y = sin(2 · (x − c)) + 1. Aufsteigend durch die Mittellinie bei x = 0,5.",
                "Mittellinie y = 1 wird bei x = c gekreuzt → c = 0,5.", listOf("c = 0,5", "c = 0,25", "c = 1"), listOf(s("c = ?", 0.5, "c"))),
            wave(3, "Hoch und verschoben", 2.0, 1.0, 2.0, 1.0, setOf("c", "d"), listOf(2.0, 2.0 + HALF_PI), "y = 2 · sin(x − c) + d. Mittellinie bei y = 1, aufsteigend bei x = 2.",
                "d = Mittellinie = 1, c = 2.", listOf("d = Mittellinie, c = Stelle des Nulldurchgangs", "d = Wellenberg, c = Periode", "d = 2, c = 1"),
                listOf(s("Mittellinie: d = ?", 1.0, "d"), s("c = ?", 2.0, "c"))),
            wave(4, "Lange Welle", 2.0, 0.5, 1.0, 0.0, setOf("b"), listOf(1.0, 1.0 + kotlin.math.PI), "y = 2 · sin(b · (x − 1)). Der Wellenberg ist π nach dem Nulldurchgang. b = ?",
                "Viertelwelle π : (2b) = π → b = 0,5.", listOf("π : (2 · b) = π → b = 0,5", "b = π", "2π : b = π → b = 2"), listOf(s("π : (2b) = π → b = ?", 0.5, "b"))),
            wave(5, "Links herum", 3.0, 1.0, -1.0, 0.0, setOf("a", "c"), listOf(-1.0, -1.0 + HALF_PI), "y = a · sin(x − c). Aufsteigend durch 0 bei x = −1, Wellenberg auf Höhe 3.",
                "c = −1 (nach links), a = 3.", listOf("c = −1, a = 3", "c = 1, a = 3", "c = −1, a = −3"), listOf(s("c = ?", -1.0, "c"), s("a = Höhe des Bergs = ?", 3.0, "a"))),
        ))

    // ------------------------------------------------------------------ 27 Waagerechter Wurf (Physik 10)

    private val hv = p("v", "Tempo", 1.0, 15.0, 0.5, 2.0, "m/s")

    private fun horizontal(n: Int, title: String, h: Double, v: Double, planet: Planet, task: String, rocks: List<Body> = emptyList()): Level {
        val g = planet.g
        val tt = sqrt(2 * h / g)
        val x = -9.0 + v * tt
        return Level("27-$n", 27, n, title, Kind.THROW, task, listOf(t(x, -9.0, 0.4)), listOf(hv), mapOf("v" to v), fixed = mapOf("angle" to 0.0),
            planet = planet, launch = Vec(-9.0, -9.0 + h), ground = -9.5, asteroids = rocks,
            tip = "Fallzeit t = √(2 · ${f(h)} : ${f(g)}) ≈ ${f(tt)} s. v = Weite : t = ${f(v * tt)} : ${f(tt)} ≈ ${f(v)} m/s.",
            quiz = listOf("Erst Fallzeit t = √(2h : g), dann v = Weite : t", "v = Weite : h", "v = √(2 · g · h)", "Wie hast du's gerechnet?"),
            walk = listOf(s("Fallzeit t = √(2 · ${f(h)} : ${f(g)}) ≈ ?", tt), s("v = ${f(v * tt)} : ${f(tt)} ≈ ?", v, "v")))
    }

    private val ch27 = Chapter(27, "Waagerechter Wurf", "Physik Klasse 10", "🏃",
        listOf(
            "Die Sonde wird waagerecht von einer Klippe abgeschossen. Nach vorne fliegt sie gleichmäßig, nach unten fällt sie immer schneller.",
            "Die Fallzeit hängt nur von der Höhe ab: t = √(2 · h : g). Auf dem Mond (g = 1,62) fällt man viel langsamer!",
            "In dieser Zeit fliegt sie die Weite x = v · t. Also: v = x : t.",
        ),
        listOf(
            horizontal(1, "Klippensprung", 5.0, 4.0, Planet.EARTH, "Erde, Klippe 5 m hoch. Die Station liegt gut 4 m entfernt. Wie schnell abschießen?"),
            horizontal(2, "Hoher Turm", 10.0, 6.0, Planet.EARTH, "Erde, Turm 10 m hoch. Die Station liegt etwa 8,6 m entfernt."),
            horizontal(3, "Mondklippe", 10.0, 3.0, Planet.MOON, "Mond, Klippe 10 m. Ziel etwa 10,5 m entfernt – auf dem Mond fällt man langsam!"),
            horizontal(4, "Marsschlucht", 15.0, 4.0, Planet.MARS, "Mars, Schlucht 15 m tief. Ziel etwa 11,4 m entfernt."),
            horizontal(5, "Weitschuss", 18.0, 9.0, Planet.EARTH, "Erde, 18 m hoch. Die Station liegt über 17 m entfernt!"),
        ))

    // ------------------------------------------------------------------ 28 e-Funktion (Kl. 11–12)

    private val ea = p("a", "a", 0.5, 9.0, 0.5, 1.0)
    private val ek = p("k", "k", -2.0, 2.0, 0.1, 0.0)

    private fun expe(n: Int, title: String, a: Double, k: Double, d: Double, xs: List<Double>, keys: Set<String>, task: String, tip: String, quiz: List<String>, walk: List<Step>): Level {
        fun y(x: Double) = a * exp(k * x) + d
        val all = mapOf("a" to a, "k" to k, "d" to d)
        return Level("28-$n", 28, n, title, Kind.EXPE, task, xs.map { t(it, y(it), 0.3) }, listOfNotNull(ea.takeIf { "a" in keys }, ek.takeIf { "k" in keys }),
            all.filterKeys { it in keys }, fixed = all.filterKeys { it !in keys }, tip = tip, quiz = quiz, walk = walk)
    }

    private val ch28 = Chapter(28, "e-Funktion", "Klasse 11–12", "📈",
        listOf(
            "Die Eulersche Zahl e ≈ 2,718 ist die Basis für natürliches Wachstum: y = a · e^(k·x).",
            "a ist der Startwert bei x = 0. k > 0: Wachstum, k < 0: Zerfall.",
            "Umkehrung mit dem natürlichen Logarithmus ln: Aus e^(k·x) = q folgt k · x = ln(q).",
        ),
        listOf(
            expe(1, "Startwert", 3.0, 0.5, 0.0, listOf(0.0), setOf("a"), "y = a · e^(0,5·x). Die Station liegt bei (0 | 3). a = ?", "e⁰ = 1 → y(0) = a = 3.",
                listOf("y(0) = a · e⁰ = a", "y(0) = a · e", "y(0) = 0"), listOf(s("e⁰ = ?", 1.0), s("a = 3 : 1 = ?", 3.0, "a"))),
            expe(2, "Wachstumsrate", 1.0, 0.5, 0.0, listOf(2.0), setOf("k"), "y = e^(k·x) geht durch (2 | e) – also y ≈ 2,72. k = ?", "e^(2k) = e¹ → 2k = 1 → k = 0,5.",
                listOf("2 · k = ln(e) = 1", "2 · k = e", "k = e : 2"), listOf(s("ln(e) = ?", 1.0), s("k = 1 : 2 = ?", 0.5, "k"))),
            expe(3, "Zerfall", 8.0, -0.5, 0.0, listOf(2.0), setOf("k"), "y = 8 · e^(k·x). Nach x = 2 ist nur noch 8 · e⁻¹ ≈ 2,94 übrig. k = ?", "e^(2k) = e⁻¹ → k = −0,5.",
                listOf("2 · k = ln(2,94 : 8) = −1", "2 · k = 2,94 : 8", "k = −2,94 : 8"), listOf(s("ln(2,94 : 8) = ln(e⁻¹) = ?", -1.0), s("k = −1 : 2 = ?", -0.5, "k"))),
            expe(4, "Zwei Messpunkte", 2.0, 0.5, 0.0, listOf(0.0, 2.0), setOf("a", "k"), "Gemessen: (0 | 2) und (2 | 2e ≈ 5,44). Finde a und k!", "a = y(0) = 2; e^(2k) = e → k = 0,5.",
                listOf("a = y(0), dann k = ln(y(2) : a) : 2", "a = y(2), k = 2", "k = y(2) − y(0)"), listOf(s("a = y(0) = ?", 2.0, "a"), s("ln(5,44 : 2) = ln(e) = ?", 1.0), s("k = 1 : 2 = ?", 0.5, "k"))),
            expe(5, "Tee kühlt ab", 6.0, -0.5, 2.0, listOf(2.0), setOf("k"), "Temperatur y = 6 · e^(k·x) + 2 (2 = Raum). Nach x = 2 sind es 6 · e⁻¹ + 2 ≈ 4,21. k = ?",
                "Erst die 2 abziehen: 6 · e^(2k) = 2,21 → e^(2k) = e⁻¹ → k = −0,5.", listOf("Erst d abziehen, dann ln", "Gleich ln(4,21) nehmen", "k = 4,21 : 6"),
                listOf(s("4,21 − 2 = ?", 2.21), s("ln(2,21 : 6) = ln(e⁻¹) = ?", -1.0), s("k = −1 : 2 = ?", -0.5, "k"))),
            expe(6, "Medikament", 6.0, -0.3, 0.0, listOf(2.0), setOf("k"), "Ein Wirkstoff (6 mg) baut sich ab: y = 6 · e^(k·x). Nach 2 Stunden sind 6 · e^(−0,6) ≈ 3,29 mg übrig. k = ?",
                "e^(2k) = e^(−0,6) → k = −0,3.", listOf("2 · k = ln(3,29 : 6) = −0,6", "2 · k = 3,29 : 6", "k = ln(3,29) : 2"),
                listOf(s("ln(3,29 : 6) ≈ ?", -0.6), s("k = −0,6 : 2 = ?", -0.3, "k"))),
        ))

    val chapters = listOf(ch29, ch23, ch24, ch25, ch26, ch27, ch28)
}
