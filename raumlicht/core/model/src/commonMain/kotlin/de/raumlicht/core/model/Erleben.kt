package de.raumlicht.core.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * „Analysis & Zufall erleben“ (seit 2.18.32): Raumlicht rechnet Analysis und Stochastik nicht mehr als Aufgaben – das übt man im Mathe-Trainer –,
 * sondern zeigt sie im Raum: Kurven abfahren, Bahnen bauen, Körper drehen, Kugeln fallen lassen. Hier stehen die Verweise zum Üben.
 */
object Erleben {
    const val TITLE = "Analysis & Zufall erleben"
    const val INTRO = "Hier wird nicht gerechnet, sondern ausprobiert: Kurven abfahren, Bahnen bauen, Körper drehen, Kugeln fallen lassen. Rechnen übst du im Mathe-Trainer."
    const val TRAINER = "https://trainer.mathenachhilfe-online.com/#/u/"
    /** „Mehr üben im Mathe-Trainer“: Titel und Einheit dort (Kennungen vom Mathe-Trainer am 18.11.2026 genannt) */
    class Link(val title: String, val unit: String) { val url get() = TRAINER + unit }
    val links = listOf(Link("Ableiten", "regeln"), Link("Kurvendiskussion", "kurve"), Link("Extremwertaufgaben", "extremwert"), Link("Integrale", "integral"),
        Link("Rotationskörper", "a-rotation"), Link("e-Funktion und Logarithmus", "expln"), Link("Baumdiagramme", "s-baum"), Link("Bedingte Wahrscheinlichkeit", "s-bedingt"),
        Link("Binomialverteilung", "s-verteilung"), Link("Hypothesentest", "s-test"))
    const val GONE = "Die Rechenaufgaben zu Analysis und Stochastik sind jetzt im Mathe-Trainer."
}

/**
 * Rotationskörper-Labor: Eine Kurve dreht sich um die x-Achse, der Körper entsteht im Raum. Kurve, Zahlen und Grenzen sind frei wählbar;
 * das Volumen wird Schritt für Schritt hergeleitet (V = π · ∫ f(x)² dx) – exakt als Vielfaches von π und gerundet.
 */
object RotationLab {
    enum class Shape(val title: String, val emoji: String, val pLabel: String, val qLabel: String?) {
        /** f(x) = p · x + q – Zylinder (p = 0), Kegel (q = 0) oder Kegelstumpf */
        GERADE("Gerade", "📏", "Steigung m", "Abschnitt c"),
        /** f(x) = √(p · x + q) – Vase, Trichter (Paraboloid) */
        WURZEL("Wurzel", "🏺", "Faktor k", "Summand c"),
        /** f(x) = √(p² − x²) – Kugel oder Kugelschicht */
        KREIS("Halbkreis", "🔮", "Radius r", null),
        /** f(x) = p/10 · x² – Kelch */
        PARABEL("Parabel", "🍷", "Zehntel a", null),
    }
    /** Eine Einstellung: Form, Zahlen [p] und [q] (ganze Zahlen; bei der Parabel p Zehntel), Grenzen [a] < [b] */
    data class Setup(val shape: Shape, val p: Int, val q: Int, val a: Int, val b: Int)
    val presets = listOf(
        "Zylinder" to Setup(Shape.GERADE, 0, 2, 0, 4), "Kegel" to Setup(Shape.GERADE, 1, 0, 0, 3), "Kegelstumpf" to Setup(Shape.GERADE, 1, 1, 0, 2),
        "Vase" to Setup(Shape.WURZEL, 1, 0, 0, 4), "Trichter" to Setup(Shape.WURZEL, 1, 1, 0, 3), "Kugel" to Setup(Shape.KREIS, 3, 0, -3, 3), "Kelch" to Setup(Shape.PARABEL, 5, 0, 0, 2))
    /** Erlaubte Bereiche der Regler */
    val RANGE_P = mapOf(Shape.GERADE to -2..2, Shape.WURZEL to 1..4, Shape.KREIS to 1..3, Shape.PARABEL to 1..10)
    val RANGE_Q = mapOf(Shape.GERADE to 0..3, Shape.WURZEL to 0..4)
    val RANGE_X = -4..4
    /** So dick darf der Körper höchstens werden – sonst passt er nicht auf die Bühne */
    const val MAX_RADIUS = 3.0

    fun f(s: Setup, x: Double): Double = when (s.shape) {
        Shape.GERADE -> s.p * x + s.q
        Shape.WURZEL -> sqrt((s.p * x + s.q).coerceAtLeast(0.0))
        Shape.KREIS -> sqrt((s.p.toDouble() * s.p - x * x).coerceAtLeast(0.0))
        Shape.PARABEL -> s.p / 10.0 * x * x
    }
    private fun n(v: Int) = if (v < 0) "−${-v}" else "$v"
    private fun term(k: Int, x: String, first: Boolean = false): String = when {
        k == 0 -> ""; first -> (if (k == 1) "" else if (k == -1) "−" else n(k) + " · ").let { if (x.isEmpty()) n(k) else it + x }
        else -> (if (k > 0) " + " else " − ") + (if (x.isEmpty()) "${abs(k)}" else if (abs(k) == 1) x else "${abs(k)} · $x") }
    fun text(s: Setup): String = when (s.shape) {
        Shape.GERADE -> "f(x) = " + (term(s.p, "x", true) + term(s.q, "", s.p == 0)).ifEmpty { "0" }
        Shape.WURZEL -> "f(x) = √(" + (term(s.p, "x", true) + term(s.q, "")) + ")"
        Shape.KREIS -> "f(x) = √(${s.p * s.p} − x²)"
        Shape.PARABEL -> "f(x) = ${Geo.num(s.p / 10.0)} · x²"
    }
    /** Warum diese Einstellung keinen Körper ergibt – oder null, wenn alles passt. Verständlich, ohne Fachchinesisch. */
    fun problem(s: Setup): String? = when {
        s.a >= s.b -> "Die linke Grenze muss kleiner sein als die rechte."
        s.shape == Shape.WURZEL && (s.p * s.a + s.q < 0) -> "Unter der Wurzel steht bei x = ${n(s.a)} eine negative Zahl. Schieb die linke Grenze nach rechts oder mach c größer."
        s.shape == Shape.KREIS && (abs(s.a) > s.p || abs(s.b) > s.p) -> "Der Halbkreis reicht nur von −${s.p} bis ${s.p}. Zieh die Grenzen in diesen Bereich."
        s.shape == Shape.GERADE && s.p == 0 && s.q == 0 -> "f(x) = 0 liegt auf der Achse – da dreht sich nichts. Stell m oder c anders ein."
        s.shape == Shape.GERADE && (f(s, s.a.toDouble()) < 0 || f(s, s.b.toDouble()) < 0) -> "Die Gerade taucht hier unter die Achse. Für einen klaren Körper bleib über der Achse: Grenzen oder c ändern."
        profile(s).maxOf { it.second } > MAX_RADIUS + 1e-9 -> "So wird der Körper zu dick für die Bühne (Radius über 3). Nimm kleinere Zahlen oder zieh die Grenzen enger."
        else -> null
    }
    /** Größter gemeinsamer Teiler – zum Kürzen */
    private fun gcd(a: Long, b: Long): Long = if (b == 0L) abs(a) else gcd(b, a % b)
    /** Bruch „z/n“ gekürzt als Text (ganze Zahlen ohne Nenner) */
    class Frac(z: Long, n: Long) {
        private val g = gcd(z, n).coerceAtLeast(1) * (if (n < 0) -1 else 1)
        val num = z / g; val den = n / g
        val value get() = num.toDouble() / den
        override fun toString() = if (den == 1L) (if (num < 0) "−${-num}" else "$num") else (if (num < 0) "−" else "") + "${abs(num)}/$den"
    }
    /** Stammfunktion von f(x)² an der Stelle x als Bruch (alle Formen haben eine mit ganzen bzw. rationalen Zahlen) */
    private fun anti(s: Setup, x: Long): Frac = when (s.shape) {
        // (px + q)² = p²x² + 2pqx + q²  →  p²x³/3 + pq·x² + q²·x
        Shape.GERADE -> Frac(s.p.toLong() * s.p * x * x * x + 3L * s.p * s.q * x * x + 3L * s.q * s.q * x, 3)
        // px + q  →  p·x²/2 + q·x
        Shape.WURZEL -> Frac(s.p * x * x + 2L * s.q * x, 2)
        // r² − x²  →  r²·x − x³/3
        Shape.KREIS -> Frac(3L * s.p * s.p * x - x * x * x, 3)
        // (p/10)²·x⁴  →  p²·x⁵/500
        Shape.PARABEL -> Frac(s.p.toLong() * s.p * x * x * x * x * x, 500)
    }
    /** Das Volumen als Vielfaches von π (exakt, gekürzt) */
    fun piTimes(s: Setup): Frac { val hi = anti(s, s.b.toLong()); val lo = anti(s, s.a.toLong()); return Frac(hi.num * lo.den - lo.num * hi.den, hi.den * lo.den) }
    fun volume(s: Setup): Double = piTimes(s).value * PI
    /** Zur Kontrolle: dasselbe Volumen mit vielen dünnen Scheiben gerechnet */
    fun numeric(s: Setup, slices: Int = 20000): Double { val h = (s.b - s.a).toDouble() / slices
        return (0 until slices).sumOf { val x = s.a + (it + 0.5) * h; PI * f(s, x) * f(s, x) * h } }

    private fun square(s: Setup): String = when (s.shape) {
        Shape.GERADE -> (term(s.p * s.p, "x²", true) + term(2 * s.p * s.q, "x", s.p == 0) + term(s.q * s.q, "", s.p == 0)).ifEmpty { "0" }
        Shape.WURZEL -> term(s.p, "x", true) + term(s.q, "")
        Shape.KREIS -> "${s.p * s.p} − x²"
        Shape.PARABEL -> "${Geo.num(s.p * s.p / 100.0)} · x⁴"
    }
    private fun antiText(s: Setup): String = when (s.shape) {
        Shape.GERADE -> listOfNotNull(if (s.p != 0) "${Frac(s.p.toLong() * s.p, 3)} · x³" else null, if (s.p * s.q != 0) "${n(s.p * s.q)} · x²" else null, if (s.q != 0) "${s.q * s.q} · x" else null).joinToString(" + ").replace("+ −", "− ")
        Shape.WURZEL -> listOfNotNull("${Frac(s.p.toLong(), 2)} · x²", if (s.q != 0) "${s.q} · x" else null).joinToString(" + ")
        Shape.KREIS -> "${s.p * s.p} · x − 1/3 · x³"
        Shape.PARABEL -> "${Frac(s.p.toLong() * s.p, 500)} · x⁵"
    }
    /** Der Rechenweg in allen Schritten – von der Formel bis zum gerundeten Wert */
    fun way(s: Setup): List<String> { val hi = anti(s, s.b.toLong()); val lo = anti(s, s.a.toLong()); val v = piTimes(s)
        return listOf(
            "Formel: V = π · ∫ f(x)² dx von ${n(s.a)} bis ${n(s.b)}",
            "Quadrieren: f(x)² = ${square(s)}",
            "Stammfunktion: F(x) = ${antiText(s)}",
            "Grenzen einsetzen: F(${n(s.b)}) = $hi und F(${n(s.a)}) = $lo",
            "Abziehen: F(${n(s.b)}) − F(${n(s.a)}) = $v",
            "Mit π malnehmen: V = $v · π ≈ ${Geo.num(kotlin.math.round(volume(s) * 100) / 100)}")
    }
    /** Umriss für die Bühne: (x, Radius) in [steps] Stücken */
    fun profile(s: Setup, steps: Int = 32): List<Pair<Double, Double>> = (0..steps).map { val x = s.a + (s.b - s.a) * it / steps.toDouble(); x to abs(f(s, x)) }
    /** Welcher bekannte Körper das ist – für den Satz unter dem Bild (leer, wenn keiner) */
    fun name(s: Setup): String = when {
        s.shape == Shape.GERADE && s.p == 0 -> "Zylinder mit Radius ${s.q} und Höhe ${s.b - s.a}"
        s.shape == Shape.GERADE && (f(s, s.a.toDouble()) == 0.0 || f(s, s.b.toDouble()) == 0.0) -> "Kegel mit Höhe ${s.b - s.a}"
        s.shape == Shape.GERADE -> "Kegelstumpf"
        s.shape == Shape.KREIS && s.a == -s.p && s.b == s.p -> "Kugel mit Radius ${s.p}"
        s.shape == Shape.KREIS && (s.a == 0 && s.b == s.p || s.a == -s.p && s.b == 0) -> "Halbkugel mit Radius ${s.p}"
        s.shape == Shape.KREIS -> "Kugelschicht"
        s.shape == Shape.WURZEL -> "Paraboloid (Vase)"
        else -> "Kelch"
    }
    /** Probe mit der Schulformel, wo es eine gibt (Zylinder, Kegel, Kugel) – oder null */
    fun check(s: Setup): String? = when {
        s.shape == Shape.GERADE && s.p == 0 -> "Probe: Zylinder V = π · r² · h = π · ${s.q * s.q} · ${s.b - s.a} = ${s.q * s.q * (s.b - s.a)}π ✓"
        s.shape == Shape.GERADE && s.q == 0 && s.a == 0 -> "Probe: Kegel V = ⅓ · π · r² · h mit r = ${abs(s.p) * s.b}, h = ${s.b} → ${Frac(s.p.toLong() * s.p * s.b * s.b * s.b, 3)}π ✓"
        s.shape == Shape.KREIS && s.a == -s.p && s.b == s.p -> "Probe: Kugel V = ⁴⁄₃ · π · r³ = ${Frac(4L * s.p * s.p * s.p, 3)}π ✓"
        else -> null
    }
}
