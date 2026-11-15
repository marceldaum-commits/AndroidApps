package de.raumlicht.core.model

import kotlin.math.*

/**
 * Liest einen Funktionsterm in x, wie man ihn auf dem Handy tippt: „x^3-3x“, „2sin(x)“, „e^(-x)·cos(3x)“, „√(x+1)“, „1/x“.
 * Erlaubt: + − · / ^, Klammern, Komma oder Punkt als Dezimaltrenner, weggelassenes Malzeichen (2x, 3(x+1)),
 * ², ³, π, e und die Funktionen sin, cos, tan, exp, ln, log (Zehnerlogarithmus), sqrt/wurzel/√, abs.
 */
class Expr private constructor(private val root: (Double) -> Double, val text: String) {
    /** Funktionswert; außerhalb des Definitionsbereichs NaN oder ±unendlich */
    fun at(x: Double): Double = root(x)

    private class Parser(val s: String) {
        var i = 0
        fun fail(msg: String): Nothing = throw IllegalArgumentException(msg)
        fun peek() = s.getOrNull(i)
        fun eat(c: Char) = if (peek() == c) { i++; true } else false

        fun expr(): (Double) -> Double {
            var l = term()
            while (true) l = when { eat('+') -> { val a = l; val b = term(); { x -> a(x) + b(x) } }; eat('-') -> { val a = l; val b = term(); { x -> a(x) - b(x) } }; else -> return l }
        }
        fun term(): (Double) -> Double {
            var l = unary()
            while (true) l = when {
                eat('*') -> { val a = l; val b = unary(); { x -> a(x) * b(x) } }
                eat('/') -> { val a = l; val b = unary(); { x -> a(x) / b(x) } }
                // weggelassenes Malzeichen: 2x, 3(x+1), x sin(x)
                peek()?.let { it.isLetterOrDigit() || it == '(' } == true -> { val a = l; val b = power(); { x -> a(x) * b(x) } }
                else -> return l
            }
        }
        fun unary(): (Double) -> Double = if (eat('-')) { val a = unary(); { x -> -a(x) } } else { eat('+'); power() }
        fun power(): (Double) -> Double {
            val b = primary()
            return if (eat('^')) { val e = if (eat('-')) { val a = power(); { x: Double -> -a(x) } } else power(); { x -> b(x).pow(e(x)) } } else b
        }
        fun primary(): (Double) -> Double {
            val c = peek() ?: fail("Der Term hört mittendrin auf.")
            if (c == '(') { i++; val e = expr(); if (!eat(')')) fail("Hier fehlt eine schließende Klammer."); return e }
            if (c.isDigit() || c == '.') {
                val start = i; while (peek()?.let { it.isDigit() || it == '.' } == true) i++
                val v = s.substring(start, i).toDoubleOrNull() ?: fail("„${s.substring(start, i)}“ ist keine Zahl."); return { v }
            }
            if (c.isLetter()) {
                val start = i; while (peek()?.isLetter() == true) i++
                var name = s.substring(start, i)
                // „xsin“ gibt es nicht – ein führendes x ist die Variable, der Rest wird neu gelesen
                if (name.length > 1 && name !in NAMES && name[0] == 'x') { i = start + 1; name = "x" }
                return when (name) {
                    "x" -> { x -> x }
                    "pi" -> { _ -> PI }
                    "e" -> { _ -> E }
                    in FUNCTIONS -> { val f = FUNCTIONS.getValue(name); val a = if (peek() == '(') primary() else power(); { x -> f(a(x)) } }
                    else -> fail("„$name“ kenne ich nicht. Es gibt: sin, cos, tan, exp, ln, log, sqrt, abs.")
                }
            }
            fail(if (c == ')') "Eine Klammer zu viel." else "Mit „$c“ kann ich hier nichts anfangen.")
        }
    }

    companion object {
        private val FUNCTIONS: Map<String, (Double) -> Double> = mapOf("sin" to ::sin, "cos" to ::cos, "tan" to ::tan, "exp" to ::exp, "ln" to ::ln, "log" to ::log10,
            "sqrt" to ::sqrt, "wurzel" to ::sqrt, "abs" to ::abs)
        private val NAMES = FUNCTIONS.keys + setOf("x", "pi", "e")

        /** @return der gelesene Term oder eine Meldung, was nicht verstanden wurde */
        fun parse(src: String): Result<Expr> = runCatching {
            val shown = src.trim().removePrefix("f(x)").trim().removePrefix("=").trim()
            if (shown.isEmpty()) throw IllegalArgumentException("Tipp einen Term ein, zum Beispiel x^2 − 2x.")
            val s = shown.lowercase().replace(" ", "").replace('−', '-').replace('–', '-').replace('·', '*').replace('×', '*').replace(':', '/').replace('÷', '/').replace(',', '.')
                .replace("²", "^2").replace("³", "^3").replace("√", "sqrt").replace("π", "pi")
            val p = Parser(s); val f = p.expr()
            if (p.i < s.length) p.fail(if (p.peek() == ')') "Eine Klammer zu viel." else "Mit „${p.peek()}“ kann ich hier nichts anfangen.")
            Expr(f, shown.replace("*", " · ").replace("^2", "²").replace("^3", "³").replace('-', '−'))
        }
    }
}
