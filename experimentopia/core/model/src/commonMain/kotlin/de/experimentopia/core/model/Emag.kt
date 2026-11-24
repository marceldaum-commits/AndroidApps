package de.experimentopia.core.model

import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Elektromagnetismus: Elektromagnet (lange Spule), Kraft auf einen stromdurchflossenen Leiter, Gleichstrommotor,
 * Induktion (Magnet in der Spule, Generator) und Transformator. Alle Größen in SI-Einheiten:
 * Spannung U in V, Stromstärke I in A, Widerstand R in Ω, Länge in m, Flussdichte B in T (Tesla), magnetischer Fluss Φ in Wb (Weber = V·s), Zeit in s.
 * Die Zahlen der Versuche (Spulenlänge, Widerstand, Magnetfluss …) sind Annahmen des Labors und stehen unten als Konstanten.
 */
object Emag {
    /** Magnetische Feldkonstante μ₀ = 4π · 10⁻⁷ T·m/A */
    const val MU0 = 4 * PI * 1e-7
    /** Eisenkern: Das Feld wird etwa 200-mal so stark (Modell: „relative Permeabilität“ μr = 200 für weiches Eisen) */
    const val MU_R_IRON = 200.0
    /** Die Spule des Labors: 10 cm lang, Drahtwiderstand 2 Ω */
    const val COIL_LENGTH_M = 0.10
    const val COIL_OHM = 2.0

    // ── Elektromagnet (lange Spule) ────────────────────────────────────────────
    /** Stromstärke durch die Spule: I = U ÷ R */
    fun coilCurrent(volts: Double, ohm: Double = COIL_OHM) = volts / ohm
    /** Flussdichte im Inneren einer langen Spule: B = μ₀ · N · I ÷ l (Luftspule), mit Eisenkern mal μr */
    fun coilField(windings: Int, amps: Double, core: Boolean, lengthM: Double = COIL_LENGTH_M) =
        MU0 * windings * amps / lengthM * (if (core) MU_R_IRON else 1.0)
    /** Stromstärke, die ein gewünschtes Feld [teslas] bei [windings] Windungen braucht */
    fun currentForField(teslas: Double, windings: Int, core: Boolean, lengthM: Double = COIL_LENGTH_M) =
        teslas * lengthM / (MU0 * windings * (if (core) MU_R_IRON else 1.0))

    // ── Kraft auf einen Leiter (Leiterschaukel) ────────────────────────────────
    /** Lorentzkraft auf einen Leiter der Länge [lengthM] senkrecht zum Feld: F = B · I · l */
    fun conductorForce(teslas: Double, amps: Double, lengthM: Double) = teslas * amps * lengthM
    fun currentForForce(newtons: Double, teslas: Double, lengthM: Double) = newtons / (teslas * lengthM)

    // ── Gleichstrommotor (Leerlauf-Modell) ─────────────────────────────────────
    /** Der Modellmotor dreht sich im Leerlauf mit 600 Umdrehungen pro Minute je Volt (Drehzahl ∝ Spannung) */
    const val RPM_PER_VOLT = 600.0
    fun motorRpm(volts: Double) = RPM_PER_VOLT * volts
    fun voltsForRpm(rpm: Double) = rpm / RPM_PER_VOLT

    // ── Induktion ──────────────────────────────────────────────────────────────
    /** Der Stabmagnet bringt im Labor einen magnetischen Fluss von 2,5·10⁻⁵ Wb durch die Spule (B = 0,05 T, Spulenquerschnitt 5 cm²) */
    const val MAGNET_FLUX_WB = 2.5e-5
    /** Der Magnet wird auf 10 cm Weg durch die Spule geschoben */
    const val PASS_LENGTH_M = 0.10
    /** Mittlere Induktionsspannung: U = N · ΔΦ ÷ Δt */
    fun induced(windings: Int, fluxWb: Double, seconds: Double) = windings * fluxWb / seconds
    /** Beim Durchschieben mit Geschwindigkeit v: Δt = s ÷ v */
    fun inducedByMagnet(windings: Int, speedMs: Double) = induced(windings, MAGNET_FLUX_WB, PASS_LENGTH_M / speedMs)
    fun speedForInduced(volts: Double, windings: Int) = volts * PASS_LENGTH_M / (windings * MAGNET_FLUX_WB)

    /** Generator: eine Spule mit N Windungen und Fläche A (m²) dreht sich im Feld B mit Frequenz f (Hz). Scheitelspannung Û = N · B · A · 2π · f */
    fun generatorPeak(windings: Int, teslas: Double, areaM2: Double, hz: Double) = windings * teslas * areaM2 * 2 * PI * hz
    fun frequencyForPeak(peakV: Double, windings: Int, teslas: Double, areaM2: Double) = peakV / (windings * teslas * areaM2 * 2 * PI)
    /** Effektivwert einer Sinusspannung: U_eff = Û ÷ √2 */
    fun effective(peak: Double) = peak / sqrt(2.0)
    /** Augenblickswert u(t) = Û · sin(2π f t) */
    fun instant(peak: Double, hz: Double, seconds: Double) = peak * kotlin.math.sin(2 * PI * hz * seconds)

    // ── Transformator (ideal, ohne Verluste) ───────────────────────────────────
    /** U₂ = U₁ · N₂ ÷ N₁ */
    fun secondaryVolts(u1: Double, n1: Int, n2: Int) = u1 * n2 / n1
    fun windingsForVolts(u1: Double, n1: Int, u2: Double) = n1 * u2 / u1
    /** Stromstärke primär: I₁ = I₂ · N₂ ÷ N₁ (P₁ = P₂) */
    fun primaryCurrent(i2: Double, n1: Int, n2: Int) = i2 * n2 / n1

    /** Fernleitung: Leistung P (W) bei Spannung U (V) → Strom I = P ÷ U; Verlustleistung in der Leitung P_V = I² · R */
    fun lineCurrent(powerW: Double, volts: Double) = powerW / volts
    fun lineLoss(powerW: Double, volts: Double, ohm: Double) = lineCurrent(powerW, volts).let { it * it * ohm }
    fun lossShare(powerW: Double, volts: Double, ohm: Double) = lineLoss(powerW, volts, ohm) / powerW
}

/** Ziele der Spule: [min] mindestens, [target] genau (± 3 %), [free] ohne Ziel. Alle Felder in Millitesla (mT). */
data class CoilSetup(
    val windings: Int, val volts: Double, val core: Boolean,
    /** Was man verstellen darf */
    val adjustWindings: Boolean = false, val adjustVolts: Boolean = false, val adjustCore: Boolean = false,
    val minMilliT: Double? = null, val targetMilliT: Double? = null,
) : Setup { override val lab get() = Lab.SPULE }

enum class MotorMode { DIRECTION, FORCE, SPEED }
/**
 * Leiterschaukel und Motor. DIRECTION: Strom so polen, dass die Schaukel nach [wantRight] ausschlägt.
 * FORCE: Kraft F = B·I·l auf [targetN] (± 3 %) einstellen. SPEED: Drehzahl auf [targetRpm] (± 3 %) einstellen.
 */
data class MotorSetup(
    val mode: MotorMode, val teslas: Double = 0.1, val lengthM: Double = 0.05, val wantRight: Boolean = true,
    val targetN: Double? = null, val targetRpm: Double? = null,
) : Setup { override val lab get() = Lab.MOTOR }

enum class DynamoMode { MAGNET, GENERATOR }
/** MAGNET: Magnet durch die Spule schieben (U = N·Φ ÷ Δt); GENERATOR: Spule im Feld drehen (Û = N·B·A·2π·f). Ziel in V ([targetV], ± 3 %) bzw. Mindestwert [minV]. */
data class DynamoSetup(
    val mode: DynamoMode, val windings: Int, val adjustWindings: Boolean = false,
    val teslas: Double = 0.1, val areaM2: Double = 0.002,
    val targetV: Double? = null, val minV: Double? = null,
) : Setup { override val lab get() = Lab.DYNAMO }

enum class TrafoMode { RATIO, LINE }
/** RATIO: Sekundärwindungen so wählen, dass [targetU2] (± 3 %) bzw. Sekundärstrom [targetI2] (± 3 %) herauskommt. LINE: Fernleitung – Verlust höchstens [maxLossShare]. */
data class TrafoSetup(
    val mode: TrafoMode, val u1: Double = 12.0, val n1: Int = 600, val loadOhm: Double = 6.0,
    val targetU2: Double? = null, val targetI2: Double? = null,
    val powerW: Double = 2_000_000.0, val lineOhm: Double = 5.0, val maxLossShare: Double? = null,
) : Setup { override val lab get() = Lab.TRAFO }

object EmagUi {
    /** Regler: Windungen 10…500 (Schritt 10), Spannung der Spule 0…9 V (Schritt 0,1), Strom der Leiterschaukel 0…10 A (Schritt 0,5), Motorspannung 0…12 V (Schritt 0,5),
     *  Geschwindigkeit des Magneten 0,1…2,0 m/s (Schritt 0,1), Dynamo-Windungen 100…1000 (Schritt 100), Drehfrequenz 1…30 Hz (Schritt 1) */
    val WINDINGS = (1..50).map { it * 10 }
    val COIL_VOLTS = (0..90).map { it / 10.0 }
    val FORCE_AMPS = (0..20).map { it / 2.0 }
    val MOTOR_VOLTS = (0..24).map { it / 2.0 }
    val MAGNET_SPEEDS = (1..20).map { it / 10.0 }
    val DYNAMO_WINDINGS = (1..10).map { it * 100 }
    val GEN_HZ = (1..30).map { it.toDouble() }
    /** Ziel erreicht, wenn der Wert höchstens 3 % daneben liegt */
    const val TOLERANCE = 0.03
    fun hit(value: Double, target: Double) = kotlin.math.abs(value - target) <= target * TOLERANCE

    /** Spannungen der Fernleitung zur Auswahl (in kV) */
    val LINE_KV = listOf(10.0, 20.0, 30.0, 50.0, 110.0, 220.0, 380.0)
    /** Sekundärwindungen des Labor-Trafos (Schritte zu 50) */
    val N2_STEPS = (1..48).map { it * 50 }
}
