package de.kurvenflug.core.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ein Schritt im Rechenweg: [text] enthält ein „?“, das Kind tippt die passende Zahl aus [choices] an.
 * [sets] = dieser Schritt liefert den Wert für den Regler mit diesem Schlüssel.
 */
data class Step(val text: String, val answer: Double, val choices: List<Double>, val sets: String? = null) {
    val answerText get() = num(answer)
}

/**
 * Rechenweg zum Mitrechnen (nach dem dritten Fehlversuch).
 *
 * Recherche: Ausgearbeitete Beispiele, deren Schritte nach und nach ausgeblendet werden, helfen besonders Schwächeren
 * (ter Vrugte u. a. 2017, Mathe-Spiel mit 12- bis 15-Jährigen). Deshalb tippt das Kind jedes Zwischenergebnis selbst an;
 * die falschen Auswahlen sind typische Fehler (Vorzeichen, um eins daneben, verdoppelt/halbiert).
 * Jeder Rechenweg wird gegen die Lösung des Levels geprüft – passt er nicht, gibt es keinen (lieber keiner als ein falscher).
 */
object Walkthrough {
    fun of(level: Level): List<Step>? {
        val steps = runCatching { build(level) }.getOrNull() ?: return null
        if (steps.isEmpty()) return null
        // Probe: Jeder Regler-Schritt muss die Lösung treffen (bis auf eine halbe Regler-Stufe), jeder Regler vorkommen
        val keys = level.params.map { it.key }.toSet()
        if (steps.mapNotNull { it.sets }.toSet() != keys) return null
        for (s in steps) {
            val k = s.sets ?: continue
            val p = level.params.first { it.key == k }
            if (abs(s.answer - level.solution.getValue(k)) > p.step / 2 + 1e-9) return null
        }
        return steps
    }

    private fun Double.r(d: Int = 2): Double { val f = 10.0.pow(d); return Math.round(this * f) / f }
    private fun f(x: Double) = num(x)
    /** Negative Zahlen in Klammern, wenn sie in einer Rechnung stehen */
    private fun p(x: Double) = if (x < 0) "(${num(x)})" else num(x)

    /** Antwort + zwei typische Fehler, gemischt (fest je Text, damit es nicht bei jedem Öffnen springt). */
    private fun choices(answer: Double, text: String, extra: List<Double> = emptyList()): List<Double> {
        val a = answer.r()
        val cand = (extra + listOf(-a, a + 1, a - 1, a * 2, a / 2, a + 0.5)).map { it.r() }
            .filter { num(it) != num(a) }.distinctBy { num(it) }.take(2)
        val all = (cand + a).toMutableList()
        val rot = Math.floorMod(text.hashCode(), 3)
        return List(3) { all[(it + rot) % all.size] }
    }

    private fun step(text: String, answer: Double, sets: String? = null, extra: List<Double> = emptyList()) =
        Step(text, answer.r(), choices(answer, text, extra), sets)

    private fun build(l: Level): List<Step>? {
        val fixed = l.fixed
        val keys = l.params.map { it.key }.toSet()
        val t = l.targets
        return when (l.kind) {
            Kind.LINE -> when {
                keys == setOf("b") -> { val m = fixed.getValue("m"); val (x, y) = t[0].x to t[0].y
                    listOf(step("m · x = ${p(m)} · ${p(x)} = ?", m * x),
                        step("b = y − m · x = ${p(y)} − ${p(m * x)} = ?", y - m * x, "b")) }
                keys == setOf("m") -> { val b = fixed.getValue("b"); val (x, y) = t[0].x to t[0].y
                    listOf(step("y − b = ${p(y)} − ${p(b)} = ?", y - b),
                        step("m = (y − b) : x = ${p(y - b)} : ${p(x)} = ?", (y - b) / x, "m")) }
                keys == setOf("m", "b") && t.size >= 2 -> twoPoints(t[0], t[1], "m", "b", "x", "y")
                else -> null
            }
            Kind.MOTION -> when {
                keys == setOf("v") -> { val s0 = fixed.getValue("s0"); val (tt, s) = t[0].x to t[0].y
                    listOf(step("Weg: s − s₀ = ${p(s)} − ${p(s0)} = ?", s - s0),
                        step("v = Weg : Zeit = ${p(s - s0)} : ${p(tt)} = ?", (s - s0) / tt, "v")) }
                keys == setOf("v", "s0") && t.size >= 2 -> twoPoints(t[0], t[1], "v", "s0", "t", "s")
                else -> null
            }
            Kind.PARABOLA -> when {
                keys == setOf("e") -> { val a = fixed.getValue("a"); val d = fixed.getValue("d"); val (x, y) = t[0].x to t[0].y
                    val sq = (x - d) * (x - d)
                    listOf(step("(x − d)² = (${f(x)} − ${p(d)})² = ?", sq),
                        step("a · ${f(sq)} = ${p(a)} · ${f(sq)} = ?", a * sq),
                        step("e = y − ${p(a * sq)} = ${p(y)} − ${p(a * sq)} = ?", y - a * sq, "e")) }
                keys == setOf("a") -> { val d = fixed.getValue("d"); val e = fixed.getValue("e"); val (x, y) = t[0].x to t[0].y
                    val sq = (x - d) * (x - d)
                    listOf(step("y − e = ${p(y)} − ${p(e)} = ?", y - e),
                        step("(x − d)² = (${f(x)} − ${p(d)})² = ?", sq),
                        step("a = ${p(y - e)} : ${f(sq)} = ?", (y - e) / sq, "a")) }
                keys == setOf("d") -> {
                    // Nullstellen-Mitte: zwei Stationen auf gleicher Höhe → Scheitel-x in der Mitte
                    if (t.size < 2 || t[0].y != t[1].y) null
                    else listOf(step("Mitte zwischen ${p(t[0].x)} und ${p(t[1].x)}: (${p(t[0].x)} + ${p(t[1].x)}) : 2 = ?", (t[0].x + t[1].x) / 2, "d"))
                }
                keys == setOf("d", "e") && l.given != null -> {
                    // Normalform → Scheitelform (quadratische Ergänzung); a vorher ausklammern
                    val a = fixed.getValue("a"); val d = l.solution.getValue("d"); val e = l.solution.getValue("e")
                    val pp = -2 * d; val q = d * d + e / a
                    (if (a != 1.0) listOf(step("${l.given}: erst a = ${p(a)} ausklammern. Innen bleibt x² + p·x + q mit p = ?", pp)) else emptyList()) + listOf(
                        step("d = −p : 2 = −${p(pp)} : 2 = ?", d, "d"),
                        step("Innen: q − d² = ${p(q)} − ${f(d * d)} = ?", q - d * d),
                        step("e = a · ${p(q - d * d)} = ${p(a)} · ${p(q - d * d)} = ?", e, "e"))
                }
                keys == setOf("d", "e") && t.size >= 2 && t[0].y == 0.0 && t[1].y == 0.0 && fixed["a"] == 1.0 && (t[0].x + t[1].x) / 2 == l.solution["d"] -> {
                    val d = (t[0].x + t[1].x) / 2; val half = abs(t[1].x - d)
                    listOf(step("d = Mitte = (${p(t[0].x)} + ${p(t[1].x)}) : 2 = ?", d, "d"),
                        step("Abstand zur Nullstelle: ${f(half)}. ${f(half)}² = ?", half * half),
                        step("Der Scheitel liegt so tief: e = ?", -half * half, "e"))
                }
                keys == setOf("d", "e") -> {
                    // Scheitel-Aufgabe: Die erste Station sitzt auf dem Gipfel/Tal → (d | e) direkt ablesen
                    val top = t[0]
                    if (top.x != l.solution["d"] || top.y != l.solution["e"]) null
                    else listOf(step("Der Scheitel ist (d | e). Er liegt bei (${f(top.x)} | ${f(top.y)}). d = ?", top.x, "d"),
                        step("Und e = ?", top.y, "e"))
                }
                else -> null
            }
            Kind.PROP -> listOf(step("k = y : x = ${p(t[0].y)} : ${p(t[0].x)} = ?", t[0].y / t[0].x, "k"))
            Kind.ANTI -> listOf(step("k = x · y = ${p(t[0].x)} · ${p(t[0].y)} = ?", t[0].x * t[0].y, "k"))
            Kind.LOG -> when (keys) {
                setOf("d") -> { val b = fixed.getValue("b"); val (x, y) = t[0].x to t[0].y; val lg = ln(x) / ln(b)
                    listOf(step("log${sub(b)}(${f(x)}) = ?   (${f(b)} hoch wie viel ergibt ${f(x)}?)", lg),
                        step("d = y − ${f(lg)} = ${p(y)} − ${p(lg)} = ?", y - lg, "d")) }
                setOf("b") -> { val (x, y) = t[0].x to t[0].y
                    listOf(step("b hoch ${p(y)} = ${f(x)}  →  b = ?", x.pow(1 / y), "b")) }
                setOf("b", "d") -> {
                    val one = t.firstOrNull { it.x == 1.0 }; val other = t.firstOrNull { it.x != 1.0 }
                    if (one == null || other == null) null else {
                        val h = other.y - one.y
                        listOf(step("Bei x = 1 ist log immer 0. Also d = ?", one.y, "d"),
                            step("log_b(${f(other.x)}) = ${p(other.y)} − ${p(one.y)} = ?", h),
                            step("b hoch ${f(h)} = ${f(other.x)}  →  b = ?", other.x.pow(1 / h), "b"))
                    }
                }
                else -> null
            }
            Kind.RADIUS -> {
                val dx = abs(t[0].x - l.launch.x); val dy = abs(t[0].y - l.launch.y)
                val tri = l.triangle
                if (tri == null) listOf(
                    step("Δx = ?  (Kästchen nach rechts/links)", dx), step("Δy = ?  (Kästchen nach oben/unten)", dy),
                    step("Δx² + Δy² = ${f(dx)}² + ${f(dy)}² = ?", dx * dx + dy * dy),
                    step("r = √${f(dx * dx + dy * dy)} ≈ ?", sqrt(dx * dx + dy * dy).r(1), "r"),
                ) else {
                    val alpha = Math.toDegrees(atan2(dy, dx)).r(0)
                    if (tri.height != null) listOf(
                        step("sin ${f(alpha)}° ≈ ?", sin(Math.toRadians(alpha)), extra = listOf(cos(Math.toRadians(alpha)).r())),
                        step("r = ${f(dy.r())} : ${f(sin(Math.toRadians(alpha)).r())} ≈ ?", (dy / sin(Math.toRadians(alpha))).r(1), "r"))
                    else listOf(
                        step("cos ${f(alpha)}° ≈ ?", cos(Math.toRadians(alpha)), extra = listOf(sin(Math.toRadians(alpha)).r())),
                        step("r = ${f(dx.r())} : ${f(cos(Math.toRadians(alpha)).r())} ≈ ?", (dx / cos(Math.toRadians(alpha))).r(1), "r"))
                }
            }
            Kind.LASER -> if (l.protractor && l.hidden) listOf(step(l.tip.substringBefore("=").trim() + " = ?", l.solution.getValue("angle"), "angle"))
            else l.triangle?.let {
                val dx = t[0].x - l.launch.x; val dy = t[0].y - l.launch.y
                val ratio = abs(dy) / abs(dx)
                val deg = Math.toDegrees(atan2(abs(dy), abs(dx))).r(0)
                listOf(step("tan α = Höhe : Weite = ${f(abs(dy))} : ${f(abs(dx))} ≈ ?", ratio, extra = listOf((abs(dx) / abs(dy)).r())),
                    step("α = tan⁻¹(${f(ratio.r())}) ≈ ?°" + if (dy < 0) "  (nach unten: Minus!)" else "",
                        if (dy < 0) -deg else deg, "angle", extra = listOf(90 - deg, if (dy < 0) deg else -deg)))
            }
            Kind.RENDEZVOUS -> {
                val m1 = fixed.getValue("m1"); val b1 = fixed.getValue("b1"); val m2 = fixed.getValue("m2"); val b2 = fixed.getValue("b2")
                val x = (b2 - b1) / (m1 - m2)
                listOf(step("Gleichsetzen: ${p(m1)}x + ${p(b1)} = ${p(m2)}x + ${p(b2)}.  b₂ − b₁ = ?", b2 - b1),
                    step("m₁ − m₂ = ${p(m1)} − ${p(m2)} = ?", m1 - m2),
                    step("x = ${p(b2 - b1)} : ${p(m1 - m2)} = ?", x, "x"),
                    step("y = ${p(m1)} · ${p(x)} + ${p(b1)} = ?", m1 * x + b1, "y"))
            }
            else -> null
        }
    }

    /** Gerade durch zwei Punkte: Steigung aus dem Steigungsdreieck, dann Achsenabschnitt. */
    private fun twoPoints(a: Body, b: Body, slope: String, offset: String, xs: String, ys: String): List<Step> {
        val dy = b.y - a.y; val dx = b.x - a.x; val m = dy / dx
        return listOf(
            step("Δ$ys = ${p(b.y)} − ${p(a.y)} = ?", dy),
            step("Δ$xs = ${p(b.x)} − ${p(a.x)} = ?", dx),
            step("$slope = Δ$ys : Δ$xs = ${p(dy)} : ${p(dx)} = ?", m, slope),
            step("${offset.replace("s0", "s₀")} = $ys − $slope · $xs = ${p(a.y)} − ${p(m)} · ${p(a.x)} = ?", a.y - m * a.x, offset),
        )
    }

    private fun sub(b: Double) = num(b).map { c -> "₀₁₂₃₄₅₆₇₈₉".getOrNull(c - '0') ?: c }.joinToString("")
}
