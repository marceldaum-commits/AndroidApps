package de.experimentopia.core.model

/** Wärmelehre mit Tabellenwerten für Wasser. */
object Heat {
    const val C_ICE = 2.09       // J/(g·K)
    const val C_WATER = 4.19
    const val C_STEAM = 2.01
    const val L_MELT = 334.0     // J/g
    const val L_BOIL = 2257.0

    /** Mischtemperatur (Richmannsche Mischungsregel) – ohne Verluste an die Umgebung */
    fun mix(m1: Double, c1: Double, t1: Double, m2: Double, c2: Double, t2: Double) = (m1 * c1 * t1 + m2 * c2 * t2) / (m1 * c1 + m2 * c2)
    fun mixWater(m1: Double, t1: Double, m2: Double, t2: Double) = mix(m1, C_WATER, t1, m2, C_WATER, t2)

    /** Zustand von Wasser (Masse m, Start als Eis bei startC < 0) nach zugeführter Energie (J) */
    data class State(val tempC: Double, val ice: Double, val water: Double, val steam: Double) {
        val phase get() = when { ice > 0 && water > 0 -> "Eis schmilzt"; ice > 0 -> "Eis"; water > 0 && steam > 0 -> "Wasser verdampft"; steam > 0 -> "Dampf"; else -> "Wasser" }
    }

    fun heat(massG: Double, startC: Double, energyJ: Double): State {
        var e = energyJ
        val toZero = massG * C_ICE * (0 - startC)
        if (e < toZero) return State(startC + e / (massG * C_ICE), 1.0, 0.0, 0.0)
        e -= toZero
        val melt = massG * L_MELT
        if (e < melt) { val f = e / melt; return State(0.0, 1 - f, f, 0.0) }
        e -= melt
        val toBoil = massG * C_WATER * 100
        if (e < toBoil) return State(e / (massG * C_WATER), 0.0, 1.0, 0.0)
        e -= toBoil
        val boil = massG * L_BOIL
        if (e < boil) { val f = e / boil; return State(100.0, 0.0, 1 - f, f) }
        e -= boil
        return State(100 + e / (massG * C_STEAM), 0.0, 0.0, 1.0)
    }
    /** Energie bis alles verdampft ist (für die Zeitachse) */
    fun energyToSteam(massG: Double, startC: Double) = massG * (C_ICE * -startC + L_MELT + C_WATER * 100 + L_BOIL)
}

/** Was auf der Heizplatte schmilzt – Schmelztemperaturen (gerundet). */
enum class Meltable(val title: String, val emoji: String, val meltC: Double, val note: String) {
    EIS("Eiswürfel", "🧊", 0.0, "Eis schmilzt schon bei 0 °C."),
    BUTTER("Butter", "🧈", 32.0, "Butter wird ab etwa 30 °C weich und flüssig."),
    SCHOKO("Schokolade", "🍫", 34.0, "Kakaobutter schmilzt knapp unter Körpertemperatur – darum schmilzt Schokolade im Mund."),
    WACHS("Kerzenwachs", "🕯️", 58.0, "Paraffin-Wachs schmilzt erst bei etwa 50–60 °C."),
    ZUCKER("Zucker", "🍬", 186.0, "Zucker schmilzt bei etwa 186 °C und wird zu Karamell."),
    ;
}

/** Stoffe für das Mischen: spezifische Wärmekapazität c in J/(g·K) */
enum class Stuff(val title: String, val emoji: String, val c: Double) {
    WASSER("Wasser", "💧", Heat.C_WATER), EISEN("Eisen", "🔩", 0.45), ALU("Aluminium", "🥈", 0.90), KUPFER("Kupfer", "🟫", 0.385),
}
