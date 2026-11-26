package de.experimentopia.core.model

import kotlin.math.sqrt

/**
 * Energie-Achterbahn: Lageenergie wird Bewegungsenergie, ein Teil wird durch Reibung zu Wärme.
 * Größen in SI-Einheiten: Masse m in kg, Höhe h in m, Geschwindigkeit v in m/s (1 m/s = 3,6 km/h), Energie in Joule (J = N·m).
 * Modell: Der Wagen startet in Ruhe. Reibung nimmt in diesem Labor einen festen Anteil der Startenergie als Wärme weg (Reibungsanteil).
 */
object Energie {
    /** Fallbeschleunigung g auf der Erde in m/s² */
    const val G = 9.81
    /** Masse des Achterbahn-Wagens mit Fahrgästen in kg */
    const val WAGON_KG = 400.0
    /** Radius des Loopings in m; der Wagen muss oben (2 · R hoch) noch mindestens mit v² = g · R fahren, um nicht herauszufallen */
    const val LOOP_RADIUS_M = 5.0
    /** Höhe des zweiten Hügels in m */
    const val HILL2_M = 10.0

    /** Lageenergie E = m · g · h (in J) */
    fun potential(massKg: Double, heightM: Double) = massKg * G * heightM
    /** Bewegungsenergie E = ½ · m · v² (in J) */
    fun kinetic(massKg: Double, speedMs: Double) = 0.5 * massKg * speedMs * speedMs
    /** Geschwindigkeit nach Absinken um [dropM] Meter, wenn der Anteil [friction] (0…1) der Energie als Wärme verloren geht: v = √(2 · g · Δh · (1 − Reibung)) (in m/s) */
    fun speed(dropM: Double, friction: Double = 0.0) = sqrt((2 * G * dropM * (1 - friction)).coerceAtLeast(0.0))
    fun toKmh(ms: Double) = ms * 3.6
    fun toMs(kmh: Double) = kmh / 3.6
    /** Höhe, aus der man starten muss, um unten die Geschwindigkeit [speedMs] zu haben: h = v² ÷ (2 · g · (1 − Reibung)) (in m) */
    fun heightForSpeed(speedMs: Double, friction: Double = 0.0) = speedMs * speedMs / (2 * G * (1 - friction))

    /** Kleinste Starthöhe für den Looping ohne Reibung: h = 2,5 · R (in m). Herleitung: oben v² ≥ g · R und v² = 2 · g · (h − 2R) ergibt h ≥ 2,5 · R */
    fun loopMinHeight(radiusM: Double = LOOP_RADIUS_M, friction: Double = 0.0) = 2.5 * radiusM / (1 - friction)
    /** Schafft der Wagen den Looping? (Reibungsanteil verringert die nutzbare Energie) */
    fun passesLoop(startM: Double, radiusM: Double = LOOP_RADIUS_M, friction: Double = 0.0) = startM * (1 - friction) >= 2.5 * radiusM - 1e-9
    /** Kleinste Starthöhe, damit der Wagen über einen Hügel der Höhe [hillM] kommt: h = Hügelhöhe ÷ (1 − Reibung) (in m) */
    fun hillMinHeight(hillM: Double = HILL2_M, friction: Double = 0.0) = hillM / (1 - friction)
    fun passesHill(startM: Double, hillM: Double = HILL2_M, friction: Double = 0.0) = startM * (1 - friction) >= hillM - 1e-9
    /** Anteil der Bewegungsenergie an der Gesamtenergie an einer Stelle in Höhe [posM] bei Start in [startM] (ohne Reibung): (H − h) ÷ H */
    fun kineticShare(startM: Double, posM: Double) = if (startM <= 0) 0.0 else ((startM - posM) / startM).coerceIn(0.0, 1.0)
}

/** Was in der Achterbahn-Werkstatt zu tun ist */
enum class CoasterMode {
    /** Starthöhe so wählen, dass die Lageenergie mindestens [CoasterSetup.minEnergyKJ] erreicht */
    ENERGY,
    /** Starthöhe so wählen, dass die Geschwindigkeit unten [CoasterSetup.targetKmh] (± 3 %) beträgt */
    SPEED,
    /** Starthöhe höchstens [CoasterSetup.maxStartM] und der Wagen muss den Looping schaffen */
    LOOP,
    /** Starthöhe höchstens [CoasterSetup.maxStartM] und der Wagen muss den zweiten Hügel überqueren */
    HILL,
    /** Bei Start in [CoasterSetup.startM] die Stelle finden, an der Lage- und Bewegungsenergie gleich groß sind (± 7 % der Gesamtenergie) */
    SPLIT,
}

data class CoasterSetup(
    val mode: CoasterMode, val startM: Double = 10.0, val massKg: Double = Energie.WAGON_KG, val frictionPct: Int = 0,
    val adjustStart: Boolean = true, val adjustMass: Boolean = false, val adjustFriction: Boolean = false,
    val minEnergyKJ: Double? = null, val targetKmh: Double? = null, val maxStartM: Double? = null,
) : Setup { override val lab get() = Lab.ACHTERBAHN }

object CoasterUi {
    /** Starthöhe 1…60 m (Schritt 1), Masse 100…1000 kg (Schritt 100), Reibung 0, 10, 20 % */
    val HEIGHTS = (1..60).map { it.toDouble() }
    val MASSES = (1..10).map { it * 100.0 }
    val FRICTIONS = listOf(0, 10, 20)
    const val TOLERANCE = 0.03
    fun hit(value: Double, target: Double) = kotlin.math.abs(value - target) <= target * TOLERANCE
    /** Die Stelle auf der Fahrt (SPLIT): bei gleichem Verhältnis von Lage- und Bewegungsenergie */
    const val SPLIT_TOLERANCE = 0.07
}
