package de.experimentopia.core.model

import kotlin.math.abs
import kotlin.math.min

/** Flüssigkeiten mit ihrer Dichte in g/cm³ (Werte aus Tabellenbüchern, bei Zimmertemperatur gerundet). */
enum class Liquid(val title: String, val rho: Double, val color: Long, val emoji: String) {
    WASSER("Wasser", 1.00, 0xFF4FA8FF, "💧"),
    SALZWASSER("Salzwasser", 1.20, 0xFF8FD3FF, "🧂"),   // gesättigte Kochsalzlösung ≈ 1,2 g/cm³
    OEL("Speiseöl", 0.92, 0xFFFFD24D, "🌻"),
    HONIG("Honig", 1.42, 0xFFE89A1C, "🍯"),
    SPUELI("Spülmittel", 1.06, 0xFF7CF29A, "🧴"),
    SPIRITUS("Spiritus", 0.81, 0xFFB98CFF, "🔥"),
    QUECKSILBER("Quecksilber (giftig!)", 13.53, 0xFFC8CCD6, "🥄"),
    ;
    companion object {
        /** Mischen sich sofort, wenn sie sich berühren (Wasser und Alkohol) */
        fun miscible(a: Liquid, b: Liquid) = setOf(a, b) == setOf(WASSER, SPIRITUS) || setOf(a, b) == setOf(SALZWASSER, SPIRITUS) || setOf(a, b) == setOf(WASSER, SALZWASSER)
    }
}

/**
 * Gegenstände mit (mittlerer) Dichte in g/cm³. „Mittlere“ Dichte heißt: Luft im Inneren zählt mit –
 * darum schwimmt ein Boot aus Knete und eine Orange mit Schale.
 */
enum class Thing(val title: String, val emoji: String, val rho: Double, val massG: Double, val note: String) {
    KORK("Korken", "🍾", 0.24, 4.0, "Kork ist voller winziger Luftkammern."),
    HOLZ("Holzklotz", "🌲", 0.55, 120.0, "Holz hat viele mit Luft gefüllte Zellen."),
    APFEL("Apfel", "🍎", 0.80, 180.0, "Ein Apfel besteht zu etwa einem Viertel aus Luft."),
    EIS("Eiswürfel", "🧊", 0.92, 25.0, "Gefrorenes Wasser dehnt sich aus und wird leichter als Wasser."),
    KERZE("Riesenkerze", "🕯️", 0.90, 900.0, "Wachs ist etwas leichter als Wasser – egal wie groß die Kerze ist."),
    EI("frisches Ei", "🥚", 1.03, 60.0, "Ein frisches Ei ist ein kleines bisschen dichter als Wasser."),
    ORANGE("Orange mit Schale", "🍊", 0.85, 200.0, "In der Schale stecken viele Luftbläschen – wie eine Schwimmweste."),
    ORANGE_OHNE("Orange ohne Schale", "🟠", 1.04, 150.0, "Ohne Schale fehlt die Schwimmweste."),
    KARTOFFEL("Kartoffel", "🥔", 1.08, 150.0, "Kartoffeln sind dichter als Wasser."),
    KNETE("Knetkugel", "🔴", 1.60, 50.0, "Knete ist viel dichter als Wasser."),
    KNETBOOT("Knetboot", "🛶", 0.40, 50.0, "Die gleiche Knete, aber als Boot verdrängt sie viel mehr Wasser."),
    NAGEL("kleiner Nagel", "📌", 7.87, 2.0, "Eisen ist fast achtmal so dicht wie Wasser."),
    STEIN("Kieselstein", "🌑", 2.65, 40.0, "Gestein ist gut zweieinhalbmal so dicht wie Wasser."),
    STYROPOR("Styropor", "⬜", 0.03, 1.0, "Styropor besteht fast nur aus Luft."),
    GUMMIENTE("Gummiente", "🦆", 0.30, 30.0, "Die Ente ist innen hohl."),
    MUENZE("Münze", "🥉", 7.8, 4.0, "Metall ist viel dichter als Wasser."),
    TRAUBE("Weintraube", "🍇", 1.08, 5.0, "Trauben sind voller Zuckersaft – dichter als Wasser."),
    EBENHOLZ("Ebenholz", "🟫", 1.2, 60.0, "Ebenholz ist so dicht, dass es untergeht – ein Holz, das sinkt!"),
    EICHE("Eichenholz", "🌳", 0.75, 75.0, "Eiche ist schwer, aber immer noch weniger dicht als Wasser."),
    BALSA("Balsaholz", "🍂", 0.16, 8.0, "Balsa ist eines der leichtesten Hölzer – daraus baut man Modellflugzeuge."),
    PE("Flaschendeckel (PE)", "🔵", 0.95, 2.0, "Polyethylen ist leichter als Wasser – so trennt man Kunststoffe im Recycling."),
    PVC("Rohrstück (PVC)", "⚪", 1.4, 20.0, "PVC ist dichter als Wasser und sinkt."),
    ALUWUERFEL("Metallwürfel (54 g, 20 cm³)", "🧊", 2.7, 54.0, "54 g ÷ 20 cm³ = 2,7 g/cm³ – das ist Aluminium."),
    GOLD("Goldbarren", "🥇", 19.3, 1000.0, "Gold ist eines der dichtesten Metalle."),
    MENSCH("Mensch (eingeatmet)", "🏊", 0.985, 60000.0, "Mit Luft in der Lunge ist der Körper knapp weniger dicht als Wasser."),
    UBOOT("U-Boot (Tanks halb voll)", "🚢", 1.0, 2000000.0, "Mit Wasser in den Tanks hat das U-Boot genau die Dichte von Wasser – es schwebt."),
    ;
    val volumeCm3 get() = massG / rho
}

enum class FloatState(val title: String) { SCHWIMMT("schwimmt"), SCHWEBT("schwebt"), SINKT("sinkt") }

object Buoyancy {
    /** Ab diesem Dichteunterschied (g/cm³) zählt es als „schweben“ */
    const val HOVER = 0.005

    fun state(rhoObject: Double, rhoLiquid: Double): FloatState = when {
        abs(rhoObject - rhoLiquid) <= HOVER -> FloatState.SCHWEBT
        rhoObject < rhoLiquid -> FloatState.SCHWIMMT
        else -> FloatState.SINKT
    }
    fun state(t: Thing, l: Liquid) = state(t.rho, l.rho)

    /** Anteil, der unter der Oberfläche liegt (0..1): beim Schwimmen ρ(Gegenstand)/ρ(Flüssigkeit) – Archimedes */
    fun submerged(rhoObject: Double, rhoLiquid: Double) = min(1.0, rhoObject / rhoLiquid)

    /** Auftriebskraft in N bei ganz eingetauchtem Volumen V (cm³): F = ρ · V · g */
    fun buoyantForceN(volumeCm3: Double, rhoLiquid: Double, g: Double = Planet.ERDE.g) = rhoLiquid * volumeCm3 / 1000.0 * g

    /** Dichte aus eigenen Werten: Masse (g) durch Volumen (cm³) */
    fun density(massG: Double, volumeCm3: Double) = massG / volumeCm3
}

/** Dichteturm: Flüssigkeiten ordnen sich nach Dichte – schwerste unten. Wasser und Spiritus mischen sich, wenn nichts dazwischen liegt. */
object DensityTower {
    data class Layer(val liquids: List<Liquid>, val rho: Double) {
        val title get() = liquids.joinToString(" + ") { it.title }
        val mixed get() = liquids.size > 1
    }

    /** Schichten von unten nach oben – gleiche Mengen je Flüssigkeit angenommen */
    fun layers(poured: List<Liquid>): List<Layer> {
        var ls = poured.distinct().sortedByDescending { it.rho }.map { Layer(listOf(it), it.rho) }
        // Benachbarte mischbare Schichten verschmelzen, bis sich nichts mehr ändert
        var changed = true
        while (changed) {
            changed = false
            for (i in 0 until ls.size - 1) {
                val a = ls[i]; val b = ls[i + 1]
                if (a.liquids.any { x -> b.liquids.any { y -> Liquid.miscible(x, y) } }) {
                    val all = a.liquids + b.liquids
                    ls = (ls.take(i) + Layer(all, all.map { it.rho }.average()) + ls.drop(i + 2)).sortedByDescending { it.rho }
                    changed = true; break
                }
            }
        }
        return ls
    }
    fun top(poured: List<Liquid>) = layers(poured).last()
    fun bottom(poured: List<Liquid>) = layers(poured).first()
}
