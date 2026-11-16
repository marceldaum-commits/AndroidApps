package de.experimentopia.core.model

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Töne der Tonleiter (gleichstufig, Kammerton a′ = 440 Hz) */
enum class Note(val title: String, val hz: Double) {
    C5("c″", 523.25), D5("d″", 587.33), E5("e″", 659.26), F5("f″", 698.46), G5("g″", 783.99), A5("a″", 880.00), H5("h″", 987.77), C6("c‴", 1046.50);
    companion object {
        fun nearest(hz: Double) = entries.minBy { abs(Klang.cents(hz, it.hz)) }
    }
}

object Klang {
    const val C = 343.0   // Schallgeschwindigkeit in Luft bei 20 °C, m/s
    const val TUBE_M = 0.25 // Länge der Glasröhren

    /** Röhre, unten geschlossen (Wasser), oben offen: f = c / (4·L) mit L = Luftsäule (Modell ohne Randkorrektur) */
    fun tubeHz(airM: Double) = C / (4 * airM)
    fun airFor(hz: Double) = C / (4 * hz)
    /** Wasserhöhe in einer 25-cm-Röhre für einen Ton */
    fun waterFor(hz: Double) = TUBE_M - airFor(hz)

    /**
     * Angeschlagenes Glas: Je mehr Wasser, desto tiefer – das Wasser schwingt mit und macht die Wand „schwerer“.
     * Näherung nach French (1983): f = f₀ / √(1 + k·(h/H)⁴)
     */
    fun glassHz(fill: Double, f0: Double = 1400.0, k: Double = 1.6) = f0 / sqrt(1 + k * fill.coerceIn(0.0, 1.0).pow(4))

    /** Saite: f ist umgekehrt proportional zur Länge (Spannung und Saite gleich) */
    fun stringHz(lengthFraction: Double, f0: Double = 261.63) = f0 / lengthFraction

    /** Abstand zweier Töne in Cent (100 Cent = Halbton) */
    fun cents(hz: Double, ref: Double) = 1200 * ln(hz / ref) / ln(2.0)
    fun inTune(hz: Double, ref: Double, tolCents: Double = 15.0) = abs(cents(hz, ref)) <= tolCents
    fun round(hz: Double) = hz.roundToInt()
}
