package de.kurvenflug.core.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Rechen-Frage nach dem Treffer: „Wie hast du's gerechnet?“ – drei Rechnungen mit den Zahlen des Levels,
 * eine richtig, zwei typische Verwechslungen (sin statt tan, Bruch umgedreht, Plus statt Minus …).
 */
data class CalcCheck(val question: String, val options: List<String>, val correct: Int)

/**
 * Damit Kinder nicht nur schieben, sondern die Rechnung verstehen (Recherche: Selbsterklärung und
 * ausgearbeitete Beispiele, siehe docs/RECHERCHE.md):
 * - [live]: die Rechnung mit den eingestellten Zahlen läuft beim Schieben mit („17 · tan 13° ≈ 3,9 → Station bei 4“)
 * - [check]: nach dem Treffer die passende Rechnung erkennen – das gibt den 3. Stern („Rechen-Stern“)
 * - [Walkthrough] liefert danach den ganzen Lösungsweg als Beispiel.
 */
object Understand {

    // ------------------------------------------------------------------ Live-Rechnung

    /** Eine Zeile: die Rechnung mit den aktuellen Reglerwerten und was gebraucht wird (null = für diese Bahnart keine). */
    fun live(l: Level, values: Map<String, Double>): String? = runCatching { liveOrNull(l, values) }.getOrNull()

    private fun liveOrNull(l: Level, values: Map<String, Double>): String? {
        val t = l.targets.firstOrNull() ?: return null
        val v = l.values(values)
        return when (l.kind) {
            Kind.BINOM -> {
                val n = kotlin.math.round(v.getValue("n")).toInt(); val p = v.getValue("p"); val k = kotlin.math.round(t.x + 9).toInt()
                val cum = (v["cum"] ?: 0.0) > 0.5
                val y = Flight.f(l, values, t.x)
                (if (cum) "P(X ≤ $k) bei B(${n}; ${num(p)})" else "P(X = $k) = ${num(Stochastik.choose(n, k), 0)} · ${num(p)}^$k · ${num(1 - p)}^${n - k}") +
                    " ≈ ${Stochastik.percent(y / 10)}  ·  Station: ${Stochastik.percent(t.y / 10)} ${mark(y, t.y, t.r)}"
            }
            Kind.LINE, Kind.PARABOLA, Kind.SINE, Kind.EXP, Kind.PROP, Kind.ANTI, Kind.LOG, Kind.MOTION, Kind.ACCEL, Kind.POWER, Kind.EXPE -> {
                if (l.kind == Kind.LOG && t.x <= 0 || l.kind == Kind.ANTI && t.x == 0.0) return null
                val y = Flight.f(l, values, t.x)
                if (!y.isFinite()) return null
                val (xs, ys) = if (l.kind == Kind.MOTION || l.kind == Kind.ACCEL) "t" to "s" else "x" to "y"
                "Bei $xs = ${num(t.x)}: $ys = ${substituted(l, v, t.x)} ≈ ${num(y, 1)}  ·  Station: ${num(t.y)} ${mark(y, t.y, t.r)}"
            }
            Kind.POINT -> {
                if (l.params.size < 2) return null   // Zahlenstrahl (Brüche): Wert selbst finden, nicht vorsagen
                val x = v.getValue("x"); val y = v.getValue("y")
                "(${num(x)} | ${num(y)}): ${steps(x, "rechts", "links")}, ${steps(y, "oben", "unten")} " +
                    if (t.contains(Vec(x, y))) "✓" else "↔"
            }
            Kind.RENDEZVOUS -> {
                val x = v.getValue("x"); val y = v.getValue("y")
                val y1 = l.fixed.getValue("m1") * x + l.fixed.getValue("b1"); val y2 = l.fixed.getValue("m2") * x + l.fixed.getValue("b2")
                "Bei x = ${num(x)}: Bahn 1 → ${num(y1, 1)}, Bahn 2 → ${num(y2, 1)}, Sonde → ${num(y)} " +
                    if (abs(y1 - y2) < 0.05 && abs(y - y1) < 0.05) "✓" else if (abs(y1 - y2) < 0.05) "↕" else "↔"
            }
            Kind.LASER -> {
                if (l.triangle == null || l.mirrors.isNotEmpty()) return null
                val w = abs(t.x - l.launch.x); val h = t.y - l.launch.y
                val a = v.getValue("angle")
                val got = w * tan(toRadians(a))
                "Höhe bei Weite ${num(w)}: ${num(w)} · tan ${num(a)}° ≈ ${num(got, 1)}  ·  Station: ${num(h)} ${mark(got, h, t.r)}"
            }
            Kind.RADIUS -> {
                val r = v.getValue("r"); val dx = abs(t.x - l.launch.x); val dy = abs(t.y - l.launch.y)
                val tri = l.triangle
                if (tri == null) "Kreis: r² = ${num(r, 1)}² ≈ ${num(r * r, 1)}  ·  Station: Δx² + Δy² = ${num(dx)}² + ${num(dy)}² ${mark(r * r, dx * dx + dy * dy, 2 * r * t.r)}"
                else {
                    val alpha = toDegrees(atan2(dy, dx))
                    if (tri.height != null) "Höhe: r · sin ${num(alpha, 0)}° = ${num(r, 1)} · ${num(sin(toRadians(alpha)), 2)} ≈ ${num(r * sin(toRadians(alpha)), 1)}  ·  Station: ${num(dy)} ${mark(r * sin(toRadians(alpha)), dy, t.r)}"
                    else "Weite: r · cos ${num(alpha, 0)}° = ${num(r, 1)} · ${num(kotlin.math.cos(toRadians(alpha)), 2)} ≈ ${num(r * kotlin.math.cos(toRadians(alpha)), 1)}  ·  Station: ${num(dx)} ${mark(r * kotlin.math.cos(toRadians(alpha)), dx, t.r)}"
                }
            }
            Kind.THROW -> {
                // Waagerechter Wurf von der Klippe: x = v · √(2h : g)
                if (v.getValue("angle") == 0.0 && l.params.none { it.key == "angle" }) {
                    val h = l.launch.y - t.y; val g = l.planet.g; val sp = v.getValue("v")
                    val w = sp * sqrt(2 * h / g)
                    return "Weite: v · √(2h : g) = ${num(sp)} · √(2 · ${num(h)} : ${num(g)}) ≈ ${num(w, 1)}  ·  Ziel: ${num(t.x - l.launch.x, 1)} ${mark(w, t.x - l.launch.x, t.r)}"
                }
                // Nur auf ebenem Boden: Wurfweite-Formel gilt, wenn Start und Ziel gleich hoch sind
                if (abs(t.y - l.launch.y) > 0.6) return null
                val a = v.getValue("angle"); val s = v.getValue("v"); val g = l.planet.g
                val w = s * s * sin(toRadians(2 * a)) / g
                "Weite: v² · sin(2α) : g = ${num(s)}² · sin ${num(2 * a)}° : ${num(g)} ≈ ${num(w, 1)}  ·  Ziel: ${num(t.x - l.launch.x, 1)} ${mark(w, t.x - l.launch.x, t.r)}"
            }
            Kind.TANGENT -> {
                val c = Upper.c(v); val x0 = v.getValue("x0"); val y0 = Upper.f(c, x0); val m = v.getValue("m")
                val y = y0 + m * (t.x - x0)
                "Tangente bei x = ${num(t.x)}: ${num(y0)} + ${num(m)} · ${num(t.x - x0)} ≈ ${num(y, 1)}  ·  Station: ${num(t.y)} ${mark(y, t.y, t.r)}"
            }
            Kind.PEAK -> {
                val c = Upper.c(v); val x = v.getValue("x")
                if (l.title == "Wendepunkt") { val d2 = Upper.ddf(c, x); "f''(${num(x)}) = ${num(d2, 2)}" + if (abs(d2) < 0.05) "  ✓" else "  ≠ 0" }
                else { val d = Upper.df(c, x); "Steigung f'(${num(x)}) = ${num(d, 2)}" + if (abs(d) < 0.05) "  ✓" else "  ≠ 0" }
            }
            Kind.AREA -> {
                val end = Upper.path(l, values).lastOrNull() ?: return null
                "Treibstoff ${num(v.getValue("A"))} reicht bis x ≈ ${num(end.x, 2)}  ·  Station: x = ${num(t.x)} ${mark(end.x, t.x, t.r)}"
            }
            Kind.VECTOR -> {
                val px = v.getValue("px"); val py = v.getValue("py"); val tt = v.getValue("T"); val v1 = v.getValue("v1"); val v2 = v.getValue("v2")
                val q = l.targets.last(); val ex = px + tt * v1; val ey = py + tt * v2
                val ok = abs(ex - q.x) <= q.r && abs(ey - q.y) <= q.r
                "P + ${num(tt)} · v = (${num(ex)} | ${num(ey)})  ·  Station: (${num(q.x)} | ${num(q.y)}) ${if (ok) "✓" else "↔"}"
            }
            else -> null
        }
    }

    private fun steps(v: Double, plus: String, minus: String) = when {
        v == 0.0 -> "0"
        v > 0 -> "${num(v)} nach $plus"
        else -> "${num(-v)} nach $minus"
    }

    /** ✓ getroffen, ↑ zu wenig, ↓ zu viel */
    private fun mark(got: Double, want: Double, tol: Double) = when {
        abs(got - want) <= maxOf(tol, 0.05) -> "✓"
        got < want -> "↑"
        else -> "↓"
    }

    /** Die Formel mit eingesetzten Zahlen, z. B. „1,5 · 4 + 2“ */
    private fun substituted(l: Level, v: Map<String, Double>, x: Double): String {
        fun g(k: String) = v.getValue(k)
        fun p(d: Double) = if (d < 0) "(${num(d)})" else num(d)
        val xs = p(x)
        return when (l.kind) {
            Kind.LINE -> "${p(g("m"))} · $xs + ${p(g("b"))}"
            Kind.MOTION -> "${p(g("v"))} · $xs + ${p(g("s0"))}"
            Kind.PARABOLA -> "${p(g("a"))} · ($xs − ${p(g("d"))})² + ${p(g("e"))}"
            Kind.SINE -> if ((v["c"] ?: 0.0) != 0.0 || l.params.any { it.key == "c" }) "${p(g("a"))} · sin(${p(g("b"))} · ($xs − ${p(g("c"))})) + ${p(g("d"))}"
                else "${p(g("a"))} · sin(${p(g("b"))} · $xs) + ${p(g("d"))}"
            Kind.ACCEL -> "½ · ${p(g("a"))} · $xs² + ${p(g("v0"))} · $xs"
            Kind.POWER -> "${p(g("a"))} · $xs^${num(g("n"))}" + if ((v["d"] ?: 0.0) != 0.0 || l.params.any { it.key == "d" }) " + ${p(g("d"))}" else ""
            Kind.EXPE -> "${p(g("a"))} · e^(${p(g("k"))} · $xs)" + if ((v["d"] ?: 0.0) != 0.0) " + ${p(g("d"))}" else ""
            Kind.EXP -> "${p(g("a"))} · ${p(g("b"))}^$xs" + if (g("d") != 0.0) " + ${p(g("d"))}" else ""
            Kind.PROP -> "${p(g("k"))} · $xs"
            Kind.ANTI -> "${p(g("k"))} : $xs"
            Kind.LOG -> "log${sub(g("b"))}($xs) + ${p(g("d"))}"
            else -> ""
        }
    }

    // ------------------------------------------------------------------ Rechen-Frage

    /** Die Rechen-Frage zu diesem Level (null = Bahnart ohne feste Rechnung, z. B. Koordinaten oder Schleuder). */
    fun check(l: Level): CalcCheck? = runCatching { checkOrNull(l) }.getOrNull()

    private fun checkOrNull(l: Level): CalcCheck? {
        l.quiz?.let { q -> return make(if (q.size > 3) q[3] else "Wie hast du's gerechnet?", q[0], q[1], q[2], l.id) }
        val t = l.targets
        val keys = l.params.map { it.key }.toSet()
        val fx = l.fixed
        fun p(d: Double) = if (d < 0) "(${num(d)})" else num(d)
        val q = "Wie hast du's gerechnet?"
        return when (l.kind) {
            Kind.POINT -> {
                if (l.params.size < 2) return null
                val x = l.solution.getValue("x"); val y = l.solution.getValue("y")
                if (x == y || x == -y) make("Wie liest du (${num(x)} | ${num(y)})?", "Erst x: ${steps(x, "rechts", "links")}, dann y: ${steps(y, "oben", "unten")}",
                    "Erst x: ${steps(-x, "rechts", "links")}, dann y: ${steps(y, "oben", "unten")}", "Erst x: ${steps(x, "rechts", "links")}, dann y: ${steps(-y, "oben", "unten")}", l.id)
                else make("Wie liest du (${num(x)} | ${num(y)})?", "Erst x: ${steps(x, "rechts", "links")}, dann y: ${steps(y, "oben", "unten")}",
                    "Erst x: ${steps(y, "rechts", "links")}, dann y: ${steps(x, "oben", "unten")}", "Erst x: ${steps(-x, "rechts", "links")}, dann y: ${steps(y, "oben", "unten")}", l.id)
            }
            Kind.LASER -> {
                if (l.crystal != null) return make("Was passiert an der Grenze zum Glas/Wasser?", "n₁ · sin α₁ = n₂ · sin α₂ (Snellius)",
                    "n₁ · α₁ = n₂ · α₂", "Der Strahl läuft immer gerade weiter", l.id)
                if (l.mirrors.isNotEmpty()) return make("Welche Regel gilt am Spiegel?", "Einfallswinkel = Ausfallswinkel",
                    "Ausfallswinkel = 90° − Einfallswinkel", "Ausfallswinkel = 2 · Einfallswinkel", l.id)
                if (l.triangle == null) return null
                val w = num(abs(t[0].x - l.launch.x)); val h = num(abs(t[0].y - l.launch.y))
                make(q, "tan α = $h : $w", "sin α = $h : $w", "tan α = $w : $h", l.id)
            }
            Kind.RADIUS -> {
                val dx = num(abs(t[0].x - l.launch.x)); val dy = num(abs(t[0].y - l.launch.y))
                val tri = l.triangle
                when {
                    tri == null -> make(q, "r = √($dx² + $dy²)", "r = $dx + $dy", "r = √($dx + $dy)", l.id)
                    tri.height != null -> make(q, "r = $dy : sin α", "r = $dy · sin α", "r = $dy : cos α", l.id)
                    else -> make(q, "r = $dx : cos α", "r = $dx · cos α", "r = $dx : sin α", l.id)
                }
            }
            Kind.PROP -> make(q, "k = ${p(t[0].y)} : ${p(t[0].x)}", "k = ${p(t[0].x)} : ${p(t[0].y)}", "k = ${p(t[0].y)} − ${p(t[0].x)}", l.id)
            Kind.ANTI -> make(q, "k = ${p(t[0].x)} · ${p(t[0].y)}", "k = ${p(t[0].y)} : ${p(t[0].x)}", "k = ${p(t[0].x)} + ${p(t[0].y)}", l.id)
            Kind.LINE, Kind.MOTION -> {
                val (m, b, xs, ys) = if (l.kind == Kind.LINE) listOf("m", "b", "x", "y") else listOf("v", "s0", "t", "s")
                val bs = b.replace("s0", "s₀")
                when {
                    keys == setOf(b) && fx.getValue(m) == 0.0 ->
                        make(q, "$bs = ${p(t[0].y)}  (waagrecht: überall gleich hoch)", "$bs = ${p(t[0].x)}", "$bs = ${p(t[0].y)} − ${p(t[0].x)}", l.id)
                    keys == setOf(m) && fx.getValue(b) == 0.0 ->
                        make(q, "$m = ${p(t[0].y)} : ${p(t[0].x)}", "$m = ${p(t[0].x)} : ${p(t[0].y)}", "$m = ${p(t[0].y)} − ${p(t[0].x)}", l.id)
                    keys == setOf(b) -> { val mm = fx.getValue(m)
                        make(q, "$bs = ${p(t[0].y)} − ${p(mm)} · ${p(t[0].x)}", "$bs = ${p(t[0].y)} + ${p(mm)} · ${p(t[0].x)}", "$bs = ${p(mm)} · ${p(t[0].x)} − ${p(t[0].y)}", l.id) }
                    keys == setOf(m) -> { val bb = fx.getValue(b)
                        make(q, "$m = (${p(t[0].y)} − ${p(bb)}) : ${p(t[0].x)}", "$m = ${p(t[0].y)} : ${p(t[0].x)}", "$m = ${p(t[0].x)} : (${p(t[0].y)} − ${p(bb)})", l.id) }
                    t.size >= 2 -> make(q, "$m = Δ$ys : Δ$xs = (${p(t[1].y)} − ${p(t[0].y)}) : (${p(t[1].x)} − ${p(t[0].x)})",
                        "$m = Δ$xs : Δ$ys = (${p(t[1].x)} − ${p(t[0].x)}) : (${p(t[1].y)} − ${p(t[0].y)})",
                        "$m = ${p(t[1].y)} − ${p(t[0].y)}", l.id)
                    else -> null
                }
            }
            Kind.PARABOLA -> when {
                keys == setOf("e") && fx.getValue("d") == 0.0 -> { val a = fx.getValue("a")
                    make(q, "e = ${p(t[0].y)} − ${p(a)} · ${num(t[0].x)}²", "e = ${p(t[0].y)} + ${p(a)} · ${num(t[0].x)}²", "e = ${p(t[0].y)} − ${p(a)} · ${num(t[0].x)}", l.id) }
                keys == setOf("e") -> { val a = fx.getValue("a"); val d = fx.getValue("d")
                    make(q, "e = ${p(t[0].y)} − ${p(a)} · (${num(t[0].x)} − ${p(d)})²", "e = ${p(t[0].y)} + ${p(a)} · (${num(t[0].x)} − ${p(d)})²",
                        "e = ${p(t[0].y)} − ${p(a)} · (${num(t[0].x)} + ${p(d)})²", l.id) }
                keys == setOf("a") -> { val d = fx.getValue("d"); val e = fx.getValue("e")
                    val dx = if (d == 0.0) p(t[0].x) else "(${num(t[0].x)} − ${p(d)})"
                    val dy = if (e == 0.0) p(t[0].y) else "(${p(t[0].y)} − ${p(e)})"
                    make(q, "a = $dy : $dx²", "a = $dy : $dx", "a = $dx² : $dy", l.id) }
                keys == setOf("d") && t.size >= 2 -> make(q, "d = (${p(t[0].x)} + ${p(t[1].x)}) : 2", "d = ${p(t[0].x)} + ${p(t[1].x)}", "d = (${p(t[1].x)} − ${p(t[0].x)}) : 2", l.id)
                keys == setOf("d", "e") && l.given != null -> make("Wie findest du den Scheitel?", "d = −p : 2, dann e ausrechnen", "d = p : 2, dann e ausrechnen", "d = −p, dann e ausrechnen", l.id)
                keys == setOf("d", "e") && t.size >= 2 && t[0].y == 0.0 && t[1].y == 0.0 ->
                    make("Wie findest du den Scheitel?", "d = Mitte der Nullstellen", "d = linke Nullstelle", "d = Summe der Nullstellen", l.id)
                keys == setOf("d", "e") -> make("Wie liest du d und e ab?", "Scheitel (d | e) = (${num(t[0].x)} | ${num(t[0].y)})",
                    "Scheitel (d | e) = (${num(t[0].y)} | ${num(t[0].x)})", "Scheitel (d | e) = (${num(-t[0].x)} | ${num(t[0].y)})", l.id)
                else -> null
            }
            Kind.LOG -> when (keys) {
                setOf("d") -> { val b = fx.getValue("b")
                    make(q, "d = ${p(t[0].y)} − log${sub(b)}(${num(t[0].x)})", "d = ${p(t[0].y)} + log${sub(b)}(${num(t[0].x)})", "d = log${sub(b)}(${num(t[0].y)})", l.id) }
                setOf("b") -> make(q, "b hoch ${p(t[0].y)} = ${num(t[0].x)}", "${p(t[0].y)} hoch b = ${num(t[0].x)}", "b · ${p(t[0].y)} = ${num(t[0].x)}", l.id)
                else -> make("Wie findest du d?", "Bei x = 1 ist log immer 0 – also d = y dort", "Bei x = 0 ist log immer 0", "Bei x = b ist log immer 0", l.id)
            }
            Kind.SINE -> when (keys) {
                setOf("a") -> make("Was stellt a ein?", "Wie hoch die Welle ausschlägt (Amplitude)", "Wie weit zwei Wellenberge auseinander sind", "Wie hoch die Mittellinie liegt", l.id)
                setOf("b") -> make("Was macht b?", "Staucht die Welle: eine Welle ist 2π : b lang", "Eine Welle ist b : 2π lang", "Macht die Welle höher", l.id)
                setOf("d") -> make("Was macht d?", "Schiebt die ganze Welle nach oben/unten", "Macht die Welle breiter", "Schiebt die Welle nach rechts", l.id)
                else -> make("Was stellen a, b, d ein?", "a: Höhe · b: Stauchung · d: Mittellinie", "a: Mittellinie · b: Höhe · d: Stauchung", "a: Stauchung · b: Mittellinie · d: Höhe", l.id)
            }
            Kind.EXP -> when (keys) {
                setOf("a") -> make("Wie findest du a?", "Bei x = 0 ist y = a", "Bei x = 1 ist y = a", "a ist die Steigung", l.id)
                else -> make("Was macht b?", "Pro Kästchen nach rechts wird y mal b genommen", "Pro Kästchen nach rechts kommt b dazu", "b ist der Startwert bei x = 0", l.id)
            }
            Kind.ORBIT -> make("Was macht ein Planet mit der Sonde?", "Er zieht sie an – je näher, desto stärker die Kurve", "Er stößt sie ab", "Je näher, desto schwächer die Kurve", l.id)
            Kind.TANGENT -> { val x0 = num(fx.getValue("x0"))
                make(q, "m = f'($x0) – die Steigung der Kurve dort", "m = f($x0) – die Höhe der Kurve dort", "m = f($x0) : $x0", l.id) }
            Kind.PEAK -> if (l.title == "Wendepunkt") make("Woran erkennst du den Wendepunkt?", "f''(x) = 0", "f'(x) = 0", "f(x) = 0", l.id)
                else make("Woran erkennst du den Extrempunkt?", "f'(x) = 0 – die Kurve ist dort waagrecht", "f(x) = 0 – die Kurve schneidet die x-Achse", "f''(x) = 0 – die Kurve wechselt die Krümmung", l.id)
            Kind.AREA -> { val xs = num(t[0].x)
                make("Wie viel Treibstoff?", "A = ∫₀^$xs f(x) dx = F($xs) − F(0)", "A = f($xs)", "A = f'($xs)", l.id) }
            Kind.VECTOR -> { val tt = fx.getValue("T")
                if (tt == 1.0) make(q, "v = Q − P", "v = P − Q", "v = Q + P", l.id)
                else make(q, "v = (Q − P) : ${num(tt)}", "v = Q − P", "v = (P − Q) : ${num(tt)}", l.id) }
            Kind.RENDEZVOUS -> make("Wie findest du den Treffpunkt?", "Beide Gleichungen gleichsetzen", "Beide b addieren", "x = b₁ : m₁ ausrechnen", l.id)
            Kind.THROW -> make("Welche Formel steckt im Wurf?", "Weite = v² · sin(2α) : g", "Weite = v · sin(α) : g", "Weite = v² : g · α", l.id)
            Kind.RAMP -> make("Welche Formel steckt in der Rampe?", "m · g · h = ½ · m · v²", "m · g · h = m · v", "g · h = 2 · v²", l.id)
            else -> null
        }
    }

    /** Richtige Antwort an eine feste (je Level gleiche) Stelle mischen */
    private fun make(question: String, right: String, wrong1: String, wrong2: String, seed: String): CalcCheck? {
        if (right == wrong1 || right == wrong2 || wrong1 == wrong2) return null
        val pos = floorModK(stableHash(seed), 3)
        val opts = mutableListOf(wrong1, wrong2).apply { add(pos, right) }
        return CalcCheck(question, opts, pos)
    }

    private fun sub(b: Double) = num(b).map { c -> "₀₁₂₃₄₅₆₇₈₉".getOrNull(c - '0') ?: c }.joinToString("")
}

/** Sterne mit Rechen-Stern: Gibt es eine Rechen-Frage, braucht der 3. Stern die richtige Rechnung. [correct] = null: noch nicht beantwortet. */
fun starsWithCheck(flightStars: Int, hasCheck: Boolean, correct: Boolean?): Int =
    if (!hasCheck) flightStars else minOf(flightStars, if (correct == true) 3 else 2)

/** Lösungsweg zum Nachlesen: jeder Schritt mit eingesetztem Ergebnis */
fun Step.solved(): String = text.replaceFirst("?", answerText)
