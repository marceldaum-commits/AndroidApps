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

/** Newtons Gesetze (ohne Reibung): F = m·a und actio = reactio */
object Newton {
    const val TRACK_M = 2.0
    /** 2. Gesetz: a = F / m */
    fun accel(forceN: Double, massKg: Double) = forceN / massKg
    /** Zeit für die Strecke s aus der Ruhe: s = ½·a·t² */
    fun timeFor(s: Double, a: Double) = kotlin.math.sqrt(2 * s / a)
    /** Geschwindigkeit am Ende der Strecke: v = a·t */
    fun speedAt(s: Double, a: Double) = a * timeFor(s, a)
    /** 3. Gesetz beim Abstoßen: Beide bekommen denselben Kraftstoß p, also v = p / m – der Leichtere wird schneller */
    const val PUSH_NS = 60.0
    fun pushSpeeds(m1: Double, m2: Double, p: Double = PUSH_NS) = (p / m1) to (p / m2)
}

/** Schiefe Ebene ohne Reibung: F = G · h / l */
object Ramp {
    fun forceN(massKg: Double, heightM: Double, lengthM: Double, g: Double = Planet.ERDE.g) = massKg * g * heightM / lengthM
}

/** Trägheit: ein Wagen bekommt einen Schubs und rollt dann ohne Antrieb – mit oder ohne Reibung */
object Glide {
    const val V0 = 0.8          // m/s nach dem Schubs
    const val TRACK = 3.0       // m Bahnlänge
    const val FRICTION = 0.25   // m/s² Bremsung durch Reibung (Rollreibung, gerundet)
    /** Ort und Geschwindigkeit nach t Sekunden */
    fun at(t: Double, friction: Boolean): Pair<Double, Double> {
        if (!friction) return V0 * t to V0
        val tStop = V0 / FRICTION
        val tt = minOf(t, tStop)
        return (V0 * tt - 0.5 * FRICTION * tt * tt) to (V0 - FRICTION * tt)
    }
    fun stopDistance() = V0 * V0 / (2 * FRICTION)
}
