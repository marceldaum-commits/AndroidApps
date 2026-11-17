package de.raumlicht.core.model

import kotlin.math.*

/**
 * Liest einen Funktionsterm in x, wie man ihn auf dem Handy tippt: „x^3-3x“, „2sin(x)“, „e^(-x)·cos(3x)“, „√(x+1)“, „1/x“, „2πx“.
 * Erlaubt: + − · / ^, Klammern, Komma oder Punkt als Dezimaltrenner, weggelassenes Malzeichen (2x, 3(x+1), πx, x√x),
 * ², ³, π, e und die Funktionen sin, cos, tan, exp, ln, log (Zehnerlogarithmus), sqrt/wurzel/√, abs.
 *
 * Regeln, damit nichts still anders verstanden wird als gemeint:
 * - Namen werden als Anfangsstück erkannt (längster zuerst): „πx“ = π · x, „ex“ = e · x, „xsin(x)“ = x · sin(x), „lnx“ = ln(x).
 * - Eine Funktion ohne Klammer nimmt das ganze folgende Produkt aus Zahlen, π, e und x samt Hochzahlen:
 *   „sin 2x“ = sin(2x), „ln x“ = ln(x), „sin x^2“ = sin(x²), „√2x“ = √(2x), „sin x cos x“ = sin(x) · cos(x).
 *   Folgt darauf ·, / oder eine Klammer („sin x/2“, „sin 2(x+1)“), wäre die Lesart nicht eindeutig – dann wird um Klammern gebeten.
 * - Das weggelassene Malzeichen bindet wie „·“: „e^-0,3x“ ist e^(−0,3) · x und „1/2x“ ist (1/2) · x. [read] zeigt den Term so,
 *   wie er verstanden wurde (mit allen Klammern), damit man ein solches Missverständnis sofort sieht.
 * - Ein Leerzeichen zwischen zwei Zahlen („2 2“) ist ein Fehler, keine 22.
 */
class Expr private constructor(private val root: (Double) -> Double, private val err: (Double) -> Double,
                               /** der Term, wie er getippt wurde (mit ², ³, · und echtem Minus) */
                               val text: String,
                               /** der Term, wie er verstanden wurde – vollständig geklammert, z. B. „e^(−0,3) · x“ */
                               val read: String) {
    /** Funktionswert; außerhalb des Definitionsbereichs NaN oder ±unendlich */
    fun at(x: Double): Double = root(x)
    /**
     * Geschätzter Rundungsfehler von [at] an der Stelle x (mitlaufende Fehlerrechnung über alle Rechenschritte).
     * Damit lässt sich Rechenrauschen von echten Nullstellen, Extrema und Wendestellen unterscheiden – auch wenn sich große Zwischenwerte fast aufheben.
     */
    fun noise(x: Double): Double = err(x)

    /** Teilterm: Funktion, Schreibweise und Rang (für die Klammern in der Schreibweise) */
    private class N(val f: (Double) -> Double, val s: String, val p: Int, val number: Boolean = false,
                    /** absoluter Rundungsfehler des Teilterms (Schätzung) */
                    val e: (Double) -> Double = { x -> EPS * abs(f(x)) })

    /** [s]: zum Lesen vereinheitlicht (gleich lang wie [shown]), [shown]: wie getippt – nur daraus wird in Meldungen zitiert */
    private class Parser(val s: String, val shown: String) {
        var i = 0
        fun fail(msg: String): Nothing = throw IllegalArgumentException(msg)
        fun peek(): Char? { while (i < s.length && s[i] == ' ') i++; return s.getOrNull(i) }
        fun eat(c: Char) = if (peek() == c) { i++; true } else false
        private fun typed(from: Int, to: Int) = (if (shown.length == s.length) shown else s).substring(from, to)

        private fun wrap(n: N, need: Boolean) = if (need) "(${n.s})" else n.s
        private fun mul(a: N, b: N): N {
            val left = wrap(a, a.p < MUL && a.p != NEG); val right = wrap(b, b.p < MUL)
            val tight = a.number && b.p >= POW && right[0].let { it.isLetter() || it == '√' }
            return N({ x -> a.f(x) * b.f(x) }, if (tight) left + right else "$left · $right", MUL) { x -> val u = a.f(x); val w = b.f(x); abs(w) * a.e(x) + abs(u) * b.e(x) + EPS * abs(u * w) }
        }
        private fun div(a: N, b: N) = N({ x -> a.f(x) / b.f(x) }, "${wrap(a, a.p == SUM || a.p == DIV)} / ${wrap(b, b.p < POW)}", DIV) { x ->
            val u = a.f(x); val w = b.f(x); a.e(x) / abs(w) + abs(u) * b.e(x) / (w * w) + EPS * abs(u / w) }
        private fun neg(a: N) = N({ x -> -a.f(x) }, "−${wrap(a, a.p < MUL)}", NEG, e = a.e)
        private fun add(a: N, b: N, sign: Int, s: String) = N({ x -> a.f(x) + sign * b.f(x) }, s, SUM) { x -> a.e(x) + b.e(x) + EPS * abs(a.f(x) + sign * b.f(x)) }
        private fun call(nm: String, a: N, s: String): N { val f = FUNCTIONS.getValue(nm)
            return N({ x -> f(a.f(x)) }, s, ATOM) { x -> val u = a.f(x); val r = f(u); val ea = a.e(x)
                EPS * abs(r) + when (nm) { "sin", "cos", "abs" -> ea; "tan" -> (1 + r * r) * ea; "exp" -> abs(r) * ea; "ln" -> ea / abs(u); "log" -> ea / (abs(u) * LN10)
                    else -> if (r == 0.0) sqrt(ea) else ea / (2 * abs(r)) } } }
        private fun pow(b: N, e: N): N {
            val base = wrap(b, b.p < ATOM)
            return N({ x -> b.f(x).pow(e.f(x)) }, if (e.number && e.s == "2") "$base²" else if (e.number && e.s == "3") "$base³" else "$base^${wrap(e, e.p < ATOM)}", POW) { x ->
                val u = b.f(x); val w = e.f(x); val r = u.pow(w)
                if (u == 0.0) (if (w > 0) b.e(x).pow(w) else Double.POSITIVE_INFINITY) else abs(w * r / u) * b.e(x) + abs(r * ln(abs(u))) * e.e(x) + 2 * EPS * abs(r) }
        }

        fun sum(): N {
            var l = term()
            while (true) l = when {
                eat('+') -> { val a = l; val b = term(); add(a, b, 1, "${a.s} + ${wrap(b, b.p <= NEG)}") }
                eat('-') -> { val a = l; val b = term(); add(a, b, -1, "${a.s} − ${wrap(b, b.p <= NEG)}") }
                else -> return l
            }
        }
        fun term(): N {
            var l = unary()
            while (true) l = when {
                eat('*') -> mul(l, unary())
                eat('/') -> div(l, unary())
                // weggelassenes Malzeichen: 2x, 3(x+1), x sin(x), 3√x
                peek()?.let { it.isLetterOrDigit() || it == '(' || it == '√' || it == '.' } == true -> mul(l, power())
                else -> return l
            }
        }
        fun unary(): N = if (eat('-')) neg(unary()) else { eat('+'); power() }
        fun power(): N = raised(primary())
        /** Hochzahlen hinter einem Teilterm: ², ³ oder ^… (rechts gebunden: 2^3^2 = 2^(3^2)) */
        private fun raised(base: N): N {
            var b = base
            while (true) b = when { eat('²') -> pow(b, N({ 2.0 }, "2", ATOM, true) { 0.0 }); eat('³') -> pow(b, N({ 3.0 }, "3", ATOM, true) { 0.0 }); else -> break }
            return if (eat('^')) pow(b, if (eat('-')) neg(power()) else { eat('+'); power() }) else b
        }
        private fun number(): N {
            val start = i; while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            val end = i; val v = s.substring(start, end).toDoubleOrNull() ?: fail("„${typed(start, end)}“ ist keine Zahl.")
            // „2 2“ ist keine 22: zwischen zwei Zahlen muss ein Rechenzeichen stehen
            if (peek()?.let { it.isDigit() || it == '.' } == true && i > end) { val b = i; while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                fail("Zwischen ${typed(start, end)} und ${typed(b, i)} fehlt ein Rechenzeichen.") }
            return N({ v }, s.substring(start, end).replace('.', ','), ATOM, true) { 0.0 }
        }
        /** Bekannter Name an der Lesestelle (längster zuerst) – sonst eine Meldung mit genau dem Wort, das getippt wurde */
        private fun name(): String {
            NAMES.firstOrNull { s.startsWith(it, i) }?.let { return it }
            var a = i; while (a > 0 && s[a - 1].isLetter()) a--
            var b = i; while (b < s.length && s[b].isLetter()) b++
            fail("„${typed(a, b)}“ kenne ich nicht. Es gibt: sin, cos, tan, exp, ln, log, sqrt, abs.")
        }
        private fun constant(name: String): N = when (name) { "x" -> N({ x -> x }, "x", ATOM); "e" -> N({ E }, "e", ATOM); else -> N({ PI }, "π", ATOM) }
        private fun brackets(name: String): Nothing { val n = if (name in ROOTS) "√" else name; fail("Bitte mit Klammer schreiben: $n(…), zum Beispiel $n(2x).") }
        /** Argument einer Funktion ohne Klammer: das Produkt aus Zahlen, π, e und x samt Hochzahlen, das direkt folgt */
        private fun bare(fn: String): N {
            var acc: N? = null
            while (true) {
                val c = peek() ?: break
                val fac = when {
                    c.isDigit() || c == '.' -> number()
                    c == '√' -> if (acc == null) brackets(fn) else break
                    c.isLetter() -> { val nm = name(); if (nm in FUNCTIONS) { if (acc == null) brackets(fn) else break }; i += nm.length; constant(nm) }
                    else -> break
                }
                val f = raised(fac); acc = acc?.let { mul(it, f) } ?: f
            }
            if (acc == null) { if (peek() == null) fail("Der Term hört mittendrin auf.") else brackets(fn) }
            if (peek().let { it == '*' || it == '/' || it == '(' }) brackets(fn)          // „sin x/2“, „sin 2(x+1)“: nicht eindeutig
            return acc
        }
        fun primary(): N {
            val c = peek() ?: fail("Der Term hört mittendrin auf.")
            if (c == '(') { i++; val e = sum(); if (!eat(')')) fail("Hier fehlt eine schließende Klammer."); return e }
            if (c.isDigit() || c == '.') return number()
            if (c.isLetter() || c == '√') {
                val nm = if (c == '√') "√" else name()
                i += nm.length
                if (nm !in FUNCTIONS) return constant(nm)
                val label = if (nm in ROOTS) "√" else nm
                if (peek() == '(') { i++; val a = sum(); if (!eat(')')) fail("Hier fehlt eine schließende Klammer."); return call(nm, a, "$label(${a.s})") }
                if (peek().let { it == '^' || it == '²' || it == '³' }) fail("Bitte so schreiben: $label(x)^2 – erst die Klammer, dann die Hochzahl.")
                val a = bare(nm); return call(nm, a, "$label(${a.s})")
            }
            fail(if (c == ')') "Eine Klammer zu viel." else "Mit „${typed(i, i + 1)}“ kann ich hier nichts anfangen.")
        }
    }

    companion object {
        /** halbe Maschinengenauigkeit: so groß ist der relative Fehler eines einzelnen Rechenschritts höchstens */
        private const val EPS = 1.2e-16
        private val LN10 = ln(10.0)
        private const val SUM = 0; private const val NEG = 1; private const val DIV = 2; private const val MUL = 3; private const val POW = 4; private const val ATOM = 5
        private val ROOTS = setOf("sqrt", "wurzel", "√")
        private val FUNCTIONS: Map<String, (Double) -> Double> = mapOf("sin" to ::sin, "cos" to ::cos, "tan" to ::tan, "exp" to ::exp, "ln" to ::ln, "log" to ::log10,
            "sqrt" to ::sqrt, "wurzel" to ::sqrt, "√" to ::sqrt, "abs" to ::abs)
        /** alle bekannten Namen, die längsten zuerst – so gewinnt „exp“ gegen „e“ */
        private val NAMES = (FUNCTIONS.keys + setOf("x", "pi", "π", "e")).sortedByDescending { it.length }

        /** Anzeige wie getippt, nur hübscher: „·“ statt „*“, echtes Minus, ² und ³ – aber nur, wenn die Hochzahl genau 2 oder 3 ist (nicht bei x^23 oder x^2,5) */
        private fun pretty(shown: String): String {
            val out = StringBuilder(); var i = 0
            while (i < shown.length) {
                val c = shown[i]; val d = shown.getOrNull(i + 1); val after = shown.getOrNull(i + 2)
                if (c == '^' && (d == '2' || d == '3') && !(after != null && (after.isDigit() || after == ',' || after == '.'))) { out.append(if (d == '2') '²' else '³'); i += 2; continue }
                out.append(when (c) { '*' -> " · "; '-' -> "−"; else -> c.toString() }); i++
            }
            return out.toString()
        }

        /** @return der gelesene Term oder eine Meldung, was nicht verstanden wurde */
        fun parse(src: String): Result<Expr> = runCatching {
            val shown = src.trim().removePrefix("f(x)").trim().removePrefix("=").trim()
            if (shown.isEmpty()) throw IllegalArgumentException("Tipp einen Term ein, zum Beispiel x^2 − 2x.")
            // Zeichen für Zeichen vereinheitlichen (die Länge bleibt gleich, damit Meldungen das Getippte zitieren können)
            val s = buildString { for (c in shown) append(when (c) { '−', '–' -> '-'; '·', '×' -> '*'; ':', '÷' -> '/'; ',' -> '.'; '\t', ' ' -> ' '; else -> c.lowercaseChar() }) }
            val p = Parser(s, shown); val f = p.sum()
            if (p.peek() != null) p.fail(if (p.peek() == ')') "Eine Klammer zu viel." else "Mit „${shown.substring(p.i, p.i + 1)}“ kann ich hier nichts anfangen.")
            Expr(f.f, f.e, pretty(shown), f.s)
        }
    }
}
