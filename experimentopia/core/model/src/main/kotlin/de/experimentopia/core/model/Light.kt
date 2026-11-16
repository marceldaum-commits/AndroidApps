package de.experimentopia.core.model

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sin

/** Farbe als Anteile Rot, Grün, Blau (0..1) */
data class Rgb(val r: Double, val g: Double, val b: Double) {
    operator fun plus(o: Rgb) = Rgb((r + o.r).coerceAtMost(1.0), (g + o.g).coerceAtMost(1.0), (b + o.b).coerceAtMost(1.0))
    fun close(o: Rgb, tol: Double = 0.13) = abs(r - o.r) <= tol && abs(g - o.g) <= tol && abs(b - o.b) <= tol
    fun argb(): Long = 0xFF000000L or ((r * 255).toLong() shl 16) or ((g * 255).toLong() shl 8) or (b * 255).toLong()
    companion object {
        val ROT = Rgb(1.0, 0.0, 0.0); val GRUEN = Rgb(0.0, 1.0, 0.0); val BLAU = Rgb(0.0, 0.0, 1.0)
        val GELB = Rgb(1.0, 1.0, 0.0); val CYAN = Rgb(0.0, 1.0, 1.0); val MAGENTA = Rgb(1.0, 0.0, 1.0)
        val WEISS = Rgb(1.0, 1.0, 1.0); val SCHWARZ = Rgb(0.0, 0.0, 0.0); val ORANGE = Rgb(1.0, 0.5, 0.0)
    }
}

object Colors {
    /** Lichtfarben addieren sich (additive Mischung): mehr Licht = heller */
    fun add(lamps: List<Rgb>) = lamps.fold(Rgb.SCHWARZ) { a, b -> a + b }

    /** Farbfilter nehmen Licht weg (subtraktive Mischung): Cyan schluckt Rot, Magenta schluckt Grün, Gelb schluckt Blau */
    fun filter(cyan: Double, magenta: Double, yellow: Double) = Rgb(1 - cyan, 1 - magenta, 1 - yellow)

    /** Name der Farbe (für Rückmeldungen) */
    fun name(c: Rgb): String {
        val named = listOf(Rgb.WEISS to "Weiß", Rgb.SCHWARZ to "Schwarz", Rgb.ROT to "Rot", Rgb.GRUEN to "Grün", Rgb.BLAU to "Blau",
            Rgb.GELB to "Gelb", Rgb.CYAN to "Türkis (Cyan)", Rgb.MAGENTA to "Pink (Magenta)", Rgb.ORANGE to "Orange")
        return named.firstOrNull { it.first.close(c, 0.2) }?.second ?: "eine Mischfarbe"
    }
}

/** Prisma aus Kronglas: Brechzahl hängt von der Wellenlänge ab (Cauchy: n = A + B/λ²) – darum fächert weißes Licht auf. */
object Prism {
    const val A = 1.5046
    const val B = 4200.0     // nm²
    const val APEX = 60.0    // gleichseitiges Prisma

    fun n(lambdaNm: Double) = A + B / (lambdaNm * lambdaNm)

    /** Ablenkung in Grad bei Einfallswinkel [inDeg] (gegen das Lot der ersten Fläche); null bei Totalreflexion */
    fun deviation(lambdaNm: Double, inDeg: Double, apex: Double = APEX): Double? {
        val n = n(lambdaNm)
        val t1 = Math.toRadians(inDeg)
        val t2 = asin(sin(t1) / n)
        val t3 = Math.toRadians(apex) - t2
        val s4 = n * sin(t3)
        if (abs(s4) > 1) return null
        return inDeg + Math.toDegrees(asin(s4)) - apex
    }
    /** Kleinste Ablenkung: δ = 2·asin(n·sin(A/2)) − A */
    fun minDeviation(lambdaNm: Double, apex: Double = APEX) = 2 * Math.toDegrees(asin(n(lambdaNm) * sin(Math.toRadians(apex / 2)))) - apex

    /** Regenbogenfarben mit Wellenlänge */
    val spectrum = listOf(700.0 to "Rot", 620.0 to "Orange", 580.0 to "Gelb", 530.0 to "Grün", 470.0 to "Blau", 420.0 to "Violett")
}
