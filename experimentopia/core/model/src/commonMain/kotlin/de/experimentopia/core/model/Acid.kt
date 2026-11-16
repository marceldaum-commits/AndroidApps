package de.experimentopia.core.model

import kotlin.math.log10
import kotlin.math.sqrt

/** Stoffe aus Küche und Bad mit typischem pH-Wert (gerundete Werte; echte Produkte schwanken etwas). */
enum class Household(val title: String, val emoji: String, val pH: Double, val note: String) {
    ZITRONE("Zitronensaft", "🍋", 2.2, "Zitronensäure macht ihn sauer."),
    COLA("Cola", "🥤", 2.5, "Cola enthält Phosphorsäure."),
    ESSIG("Essig", "🍶", 2.9, "Essigsäure – darum riecht er so stechend."),
    APFELSAFT("Apfelsaft", "🧃", 3.5, "Fruchtsäuren aus dem Apfel."),
    MILCH("Milch", "🥛", 6.6, "Fast neutral."),
    KAFFEE("Kaffee", "☕", 5.0, "Kaffee ist leicht sauer."),
    ORANGENSAFT("Orangensaft", "🍊", 3.7, "Fruchtsäuren, vor allem Zitronensäure."),
    SPRUDEL("Sprudelwasser", "🍾", 5.5, "Die Kohlensäure macht Sprudel leicht sauer."),
    WASSER("Wasser", "💧", 7.0, "Reines Wasser ist neutral."),
    NATRON("Natron-Lösung", "🧂", 8.3, "Natron ist schwach basisch."),
    SEIFE("Seifenlauge", "🧼", 10.0, "Seife ist basisch."),
    WASCHSODA("Waschsoda", "🧺", 11.5, "Soda ist stark basisch – Handschuhe!"),
    ;
}

/** Rotkohlsaft als Zauber-Anzeiger (Indikator): Farbe je nach pH-Wert. */
object Indicator {
    private val stops = listOf(
        1.0 to 0xFFE0115FL, 2.0 to 0xFFE2185BL, 3.0 to 0xFFD6336CL, 4.0 to 0xFFB8478FL, 5.0 to 0xFF8E4FA8L,
        6.0 to 0xFF7151B5L, 7.0 to 0xFF5D55C2L, 8.0 to 0xFF3D6FD6L, 9.0 to 0xFF2E9BC0L, 10.0 to 0xFF2DB88AL,
        11.0 to 0xFF4CC75AL, 12.0 to 0xFF9ED13CL, 13.0 to 0xFFD8D435L, 14.0 to 0xFFE8D22AL,
    )
    /** ARGB-Farbe des Rotkohlsafts bei diesem pH */
    fun color(pH: Double): Long {
        val p = pH.coerceIn(1.0, 14.0)
        val i = stops.indexOfLast { it.first <= p }.coerceAtMost(stops.size - 2)
        val (x0, c0) = stops[i]; val (x1, c1) = stops[i + 1]
        val t = ((p - x0) / (x1 - x0)).coerceIn(0.0, 1.0)
        fun ch(c: Long, s: Int) = ((c shr s) and 0xFF).toDouble()
        fun mix(s: Int) = (ch(c0, s) + (ch(c1, s) - ch(c0, s)) * t).toLong()
        return 0xFF000000L or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }
    /** Farbname, wie Kinder ihn sagen würden */
    fun name(pH: Double) = when { pH < 4 -> "rot"; pH < 7.5 -> "violett"; pH < 9 -> "blau"; pH < 12 -> "grün"; else -> "gelb" }
    fun kind(pH: Double) = when { pH < 6.5 -> "sauer"; pH <= 7.5 -> "neutral"; else -> "basisch" }
}

/**
 * Mischen von starker Säure (Salzsäure) und starker Lauge (Natronlauge) – so, wie es im Chemieunterricht gerechnet wird.
 * Gespeichert wird der Überschuss n(H⁺) − n(OH⁻) in mol; das Wasser-Gleichgewicht (Kw = 10⁻¹⁴) liefert den genauen pH.
 */
data class Beaker(val volumeMl: Double = 0.0, val netMol: Double = 0.0) {
    fun addAcid(cMolL: Double, ml: Double) = Beaker(volumeMl + ml, netMol + cMolL * ml / 1000)
    fun addBase(cMolL: Double, ml: Double) = Beaker(volumeMl + ml, netMol - cMolL * ml / 1000)
    fun addWater(ml: Double) = Beaker(volumeMl + ml, netMol)
    val pH: Double get() = if (volumeMl <= 0) 7.0 else Acid.pH(netMol / (volumeMl / 1000))
}

object Acid {
    const val KW = 1e-14
    /** pH aus dem Überschuss x = c(H⁺) − c(OH⁻) in mol/L: c(H⁺) = (x + √(x² + 4Kw)) / 2 */
    fun pH(x: Double): Double { val h = (x + sqrt(x * x + 4 * KW)) / 2; return -log10(h) }
    /** pH einer Salzsäure der Konzentration c */
    fun acidPH(c: Double) = pH(c)
    /** Volumen Lauge (mL) bis zum Neutralpunkt: n(Säure) = n(Lauge) */
    fun neutralMl(acidC: Double, acidMl: Double, baseC: Double) = acidC * acidMl / baseC
}
