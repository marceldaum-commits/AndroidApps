package de.experimentopia.core.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/** Fallbeschleunigung g in m/s² und Luftdichte in kg/m³ (Mars: sehr dünne Luft; Jupiter: Gasplanet, Werte auf Höhe von 1 bar) */
enum class Planet(val title: String, val emoji: String, val g: Double, val airRho: Double) {
    ERDE("Erde", "🌍", 9.81, 1.2),
    VAKUUM("Vakuumröhre", "🍶", 9.81, 0.0),
    MOND("Mond", "🌕", 1.62, 0.0),
    MARS("Mars", "🔴", 3.71, 0.020),
    JUPITER("Jupiter", "🟠", 24.79, 0.16),
    ;
    val air get() = airRho > 0
}

/** Fallende Dinge: Masse (kg) und Luftwiderstand c_w·A (m²) */
enum class Faller(val title: String, val emoji: String, val massKg: Double, val cwA: Double) {
    KUGEL("Stahlkugel", "⚫", 0.11, 0.47 * PI * 0.015 * 0.015),
    FEDER("Feder", "🍃", 0.00025, 0.0114),          // sinkt in Luft mit etwa 0,6 m/s
    PAPIER("Blatt Papier (flach)", "📄", 0.005, 0.081),
    KNAEUEL("Papier (zerknüllt)", "🗞️", 0.005, 0.5 * PI * 0.03 * 0.03),
    HAMMER("Hammer", "🔨", 1.32, 0.004),             // wie beim Apollo-15-Versuch auf dem Mond
    TENNIS("Tennisball", "🎾", 0.058, 0.55 * PI * 0.033 * 0.033),
    BALLON("Luftballon", "🎈", 0.003, 0.47 * PI * 0.12 * 0.12),
    FALLSCHIRM("Spielzeug-Fallschirm", "🪂", 0.05, 1.3 * 0.07),
}

object Fall {
    const val AIR_RHO = 1.2   // kg/m³

    /** Fallzeit ohne Luft: t = √(2h/g) */
    fun timeNoAir(h: Double, g: Double) = sqrt(2 * h / g)

    /** Ein Punkt der Fallkurve */
    data class Sample(val t: Double, val y: Double, val v: Double)

    /** Fallen mit Luftwiderstand F = ½·ρ·c_w·A·v² (numerisch, kleine Zeitschritte). Gibt die Kurve bis zum Boden. */
    fun simulate(f: Faller, h: Double, planet: Planet, dt: Double = 0.0005): List<Sample> = simulate(f, h, planet.g, planet.airRho, dt)

    /** Wie oben, mit eigener Fallbeschleunigung [g] und Luftdichte [rho] (für eigene Werte im Freien Labor) */
    fun simulate(f: Faller, h: Double, g: Double, rho: Double, dt: Double = 0.0005): List<Sample> {
        val planetG = g
        val k = 0.5 * rho * f.cwA
        var t = 0.0; var y = 0.0; var v = 0.0
        val out = mutableListOf(Sample(0.0, 0.0, 0.0))
        var next = 0.01
        while (y < h && t < 600) {
            // halb-implizit: zuerst Geschwindigkeit, dann Ort
            val a = planetG - k * v * v / f.massKg
            v += a * dt; y += v * dt; t += dt
            if (t >= next || y >= h) { out += Sample(t, minOf(y, h), v); next += 0.01 }
        }
        return out
    }
    fun time(f: Faller, h: Double, planet: Planet) = if (!planet.air) timeNoAir(h, planet.g) else simulate(f, h, planet).last().t

    /** Endgeschwindigkeit in Luft: v = √(2mg / (ρ·c_w·A)) */
    fun terminal(f: Faller, g: Double = Planet.ERDE.g, rho: Double = AIR_RHO) = sqrt(2 * f.massKg * g / (rho * f.cwA))

    /** Wer ist zuerst unten? −1 a, 0 gleichzeitig (Unterschied < 2 %), 1 b */
    fun winner(a: Faller, b: Faller, h: Double, planet: Planet): Int {
        val ta = time(a, h, planet); val tb = time(b, h, planet)
        return if (abs(ta - tb) <= 0.02 * maxOf(ta, tb)) 0 else if (ta < tb) -1 else 1
    }
}

object Pendulum {
    /** Kleine Ausschläge: T = 2π·√(l/g) – die Masse kommt nicht vor! */
    fun periodSmall(l: Double, g: Double) = 2 * PI * sqrt(l / g)

    /** Genau, auch bei großem Ausschlag: T = 4·√(l/g)·K(sin(θ₀/2)) mit dem vollständigen elliptischen Integral (AGM-Verfahren) */
    fun period(l: Double, g: Double, amplitudeDeg: Double): Double {
        val k = sin(toRad(amplitudeDeg) / 2)
        var a = 1.0; var b = sqrt(1 - k * k)
        repeat(30) { val an = (a + b) / 2; b = sqrt(a * b); a = an }
        return 2 * PI / a * sqrt(l / g)
    }

    /** Länge für eine gewünschte Periode: l = g·(T/2π)² */
    fun lengthFor(t: Double, g: Double) = g * (t / (2 * PI)) * (t / (2 * PI))

    /** Winkel zur Zeit t (numerisch gelöst, für die Animation) – wiederverwendbarer Schritt */
    fun step(theta: Double, omega: Double, l: Double, g: Double, dt: Double): Pair<Double, Double> {
        val w = omega - g / l * sin(theta) * dt
        return (theta + w * dt) to w
    }
}
