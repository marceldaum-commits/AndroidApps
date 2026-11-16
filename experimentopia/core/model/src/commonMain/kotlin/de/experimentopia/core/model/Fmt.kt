package de.experimentopia.core.model

import kotlin.math.abs
import kotlin.math.roundToLong

/** Zahlen deutsch schreiben: Komma statt Punkt, unnötige Nullen weg. */
object Fmt {
    fun num(v: Double, decimals: Int = 2): String {
        if (v.isNaN()) return "–"
        if (v.isInfinite()) return if (v > 0) "∞" else "−∞"
        var f = 1.0; repeat(decimals) { f *= 10 }
        // ohne String.format, damit es auch im Browser (Kotlin/Wasm) gleich aussieht
        val scaled = (abs(v) * f).roundToLong()
        val whole = (scaled / f.toLong()).toString()
        val frac = if (decimals == 0) "" else (scaled % f.toLong()).toString().padStart(decimals, '0').trimEnd('0')
        var s = (if (v < 0 && scaled != 0L) "-" else "") + whole + (if (frac.isNotEmpty()) ".$frac" else "")
        if (s == "-0") s = "0"
        return s.replace('.', ',').replace("-", "−")
    }
    fun unit(v: Double, unit: String, decimals: Int = 2) = "${num(v, decimals)} $unit"

    /** Liest eine Eingabe wie „2,5“ oder „2.5“ – null, wenn es keine Zahl ist. */
    fun parse(text: String): Double? = text.trim().replace(',', '.').replace('−', '-').toDoubleOrNull()?.takeIf { it.isFinite() }

    fun close(a: Double, b: Double, tol: Double) = abs(a - b) <= tol
}

/** Winkel umrechnen (gibt es im Browser nicht als Math.toDegrees) */
fun toDeg(rad: Double) = rad * 180.0 / kotlin.math.PI
fun toRad(deg: Double) = deg * kotlin.math.PI / 180.0

/** Ergebnis einer Eingabeprüfung – mit kindgerechter Rückmeldung. */
sealed interface Check {
    data class Ok(val value: Double) : Check
    data class Bad(val message: String) : Check
}

/** Prüft eigene Werte: muss eine Zahl im erlaubten Bereich sein. */
fun checkInput(text: String, min: Double, max: Double, unit: String): Check {
    val v = Fmt.parse(text) ?: return Check.Bad(if (text.isBlank()) "Tipp eine Zahl ein." else "„$text“ ist keine Zahl. Beispiel: ${Fmt.num((min + max) / 2, 1)}")
    if (v < min) return Check.Bad("Zu klein – mindestens ${Fmt.unit(min, unit)}.")
    if (v > max) return Check.Bad("Zu groß – höchstens ${Fmt.unit(max, unit)}.")
    return Check.Ok(v)
}
