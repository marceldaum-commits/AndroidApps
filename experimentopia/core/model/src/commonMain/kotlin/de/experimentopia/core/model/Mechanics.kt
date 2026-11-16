package de.experimentopia.core.model

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sign

/** Ein Gewicht auf der Wippe: Masse in kg, Platz in Feldern vom Drehpunkt (negativ = links). */
data class Load(val massKg: Double, val pos: Int, val emoji: String = "🧱", val label: String = "${Fmt.num(massKg)} kg")

/** Hebelgesetz: Die Wippe ist im Gleichgewicht, wenn Masse·Abstand links = Masse·Abstand rechts. */
object Lever {
    const val SLOTS = 5          // Felder je Seite
    const val FIELD_M = 0.5      // ein Feld = 50 cm

    /** Drehmoment in kg·Felder: positiv = rechts schwerer */
    fun torque(loads: List<Load>) = loads.sumOf { it.massKg * it.pos }
    fun leftTorque(loads: List<Load>) = loads.filter { it.pos < 0 }.sumOf { it.massKg * -it.pos }
    fun rightTorque(loads: List<Load>) = loads.filter { it.pos > 0 }.sumOf { it.massKg * it.pos }
    fun balanced(loads: List<Load>) = abs(torque(loads)) < 1e-9

    /** −1 links unten, 0 Gleichgewicht, 1 rechts unten */
    fun tilt(loads: List<Load>): Int = if (balanced(loads)) 0 else sign(torque(loads)).toInt()

    /** Neigung zum Zeichnen in Grad (begrenzt, wie eine echte Wippe am Boden anschlägt) */
    fun angleDeg(loads: List<Load>, max: Double = 14.0): Double {
        val t = torque(loads)
        if (abs(t) < 1e-9) return 0.0
        return (toDeg(atan(t / 6.0))).coerceIn(-max, max)
    }

    /** Wo muss ein Gewicht der Masse m hin, damit Gleichgewicht herrscht? null, wenn kein Feld passt. */
    fun balancePos(fixed: List<Load>, massKg: Double): Int? {
        val need = -torque(fixed) / massKg
        val p = kotlin.math.round(need).toInt()
        return p.takeIf { abs(need - p) < 1e-9 && p != 0 && abs(p) <= SLOTS }
    }

    /** Kraft in N, die man am Abstand armOut aufbringen muss, um die Last am Abstand armIn zu halten: F₁·a₁ = F₂·a₂ */
    fun effortN(loadKg: Double, armIn: Double, armOut: Double, g: Double = Planet.ERDE.g) = loadKg * g * armIn / armOut
}

/** Rollen und Flaschenzug (ideal: Rollen und Seil ohne Masse und Reibung). */
object Pulley {
    /** Zugkraft F = G / n, wobei n die Zahl der tragenden Seilstücke ist */
    fun forceN(massKg: Double, ropes: Int, g: Double = Planet.ERDE.g) = massKg * g / ropes
    /** Seil, das man ziehen muss: s = n · h (die Arbeit F·s bleibt gleich – „Goldene Regel der Mechanik“) */
    fun ropeM(heightM: Double, ropes: Int) = heightM * ropes
    fun workJ(massKg: Double, heightM: Double, g: Double = Planet.ERDE.g) = massKg * g * heightM
    /** Kleinste Seilzahl aus [options], mit der die Kraft höchstens maxN beträgt */
    fun minRopes(massKg: Double, maxN: Double, options: List<Int>) = options.sorted().firstOrNull { forceN(massKg, it) <= maxN + 1e-9 }
}

/** Schiefe Ebene ohne Reibung: F = G · h / l */
object Ramp {
    fun forceN(massKg: Double, heightM: Double, lengthM: Double, g: Double = Planet.ERDE.g) = massKg * g * heightM / lengthM
}
