package de.experimentopia.core.model

import kotlin.math.PI

/**
 * Druck & Luftdruck: Druck = Kraft ÷ Fläche, Wasserdruck in der Tiefe, Hydraulik, Luftdruck (Magdeburger Halbkugeln).
 * Größen in SI-Einheiten: Kraft F in N, Fläche A in m², Druck p in Pa (1 Pa = 1 N/m²; 1 bar = 100 000 Pa; 1 hPa = 100 Pa), Höhe/Tiefe in m, Masse in kg.
 */
object Druck {
    /** Fallbeschleunigung g in m/s² */
    const val G = 9.81
    /** Dichte von Wasser in kg/m³ */
    const val RHO_WATER = 1000.0
    /** Normaler Luftdruck auf Meereshöhe in Pa (1013,25 hPa) */
    const val P_ATM = 101_325.0
    /** 1 bar in Pa */
    const val BAR = 100_000.0
    /** Masse der Person auf dem Nagelbrett in kg und die Spitzenfläche eines Nagels in m² (4 mm²) */
    const val PERSON_KG = 70.0
    const val NAIL_TIP_M2 = 4e-6
    /** Das Auto der Hebebühne in kg */
    const val CAR_KG = 1200.0

    /** Druck p = F ÷ A (in Pa) */
    fun pressure(forceN: Double, areaM2: Double) = forceN / areaM2
    /** Gewichtskraft F = m · g (in N) */
    fun weight(massKg: Double) = massKg * G
    /** Druck auf die Haut beim Nagelbrett: Gewicht ÷ (Anzahl · Spitzenfläche) (in Pa) */
    fun nailPressure(nails: Int, massKg: Double = PERSON_KG, tipM2: Double = NAIL_TIP_M2) = pressure(weight(massKg), nails * tipM2)
    /** Wenigstens so viele Nägel brauchst du, damit der Druck höchstens [maxPa] beträgt */
    fun nailsFor(maxPa: Double, massKg: Double = PERSON_KG, tipM2: Double = NAIL_TIP_M2) = kotlin.math.ceil(weight(massKg) / (maxPa * tipM2) - 1e-9).toInt()

    /** Wasserdruck durch die Wassersäule: p = ρ · g · h (in Pa, ohne Luftdruck) */
    fun waterColumn(depthM: Double) = RHO_WATER * G * depthM
    /** Gesamtdruck in der Tiefe: Luftdruck + Wasserdruck (in Pa) */
    fun total(depthM: Double) = P_ATM + waterColumn(depthM)
    /** Tiefe, in der der Gesamtdruck [pa] herrscht: h = (p − p_Luft) ÷ (ρ · g) (in m) */
    fun depthFor(pa: Double) = (pa - P_ATM) / (RHO_WATER * G)

    /** Hydraulik: Druck ist überall gleich, also F₂ = F₁ · A₂ ÷ A₁ (in N) */
    fun hydraulicForce(f1: Double, a1: Double, a2: Double) = f1 * a2 / a1
    /** Kolbenfläche A₂, die man für die Kraft [f2] braucht: A₂ = F₂ · A₁ ÷ F₁ (in m²) */
    fun pistonAreaFor(f2: Double, f1: Double, a1: Double) = f2 * a1 / f1

    /** Kraft, mit der die Luft zwei leergepumpte Halbkugeln (Radius r) zusammendrückt: F = p · π · r² (in N, ideales Vakuum) */
    fun hemisphereForce(radiusM: Double) = P_ATM * PI * radiusM * radiusM
    /** Kleinster Radius für die Kraft [forceN] (in m) */
    fun hemisphereRadiusFor(forceN: Double) = kotlin.math.sqrt(forceN / (P_ATM * PI))
}

enum class PressureMode {
    /** Nägel im Brett so wählen, dass der Druck höchstens [PressureSetup.maxKPa] beträgt */
    NAILS,
    /** Tiefe so wählen, dass der Gesamtdruck [PressureSetup.targetBar] (± 3 %) beträgt */
    DIVE,
    /** Kolbenfläche so wählen, dass die Kraft mindestens [PressureSetup.minForceN] erreicht */
    HYDRAULIC,
    /** Radius der Halbkugeln so wählen, dass die Luft mit mindestens [PressureSetup.minForceN] zusammendrückt */
    HEMISPHERE,
}

data class PressureSetup(
    val mode: PressureMode, val maxKPa: Double? = null, val targetBar: Double? = null, val minForceN: Double? = null,
    val f1: Double = 200.0, val a1: Double = 0.002,
) : Setup { override val lab get() = Lab.DRUCK }

object PressureUi {
    /** Regler: Nägel 100…3000 (Schritt 100), Tiefe 0…100 m (Schritt 1), Kolbenfläche 0,01…0,30 m² (Schritt 0,01), Radius 1…20 cm (Schritt 1) */
    val NAILS = (1..30).map { it * 100 }
    val DEPTHS = (0..100).map { it.toDouble() }
    val AREAS = (1..30).map { it / 100.0 }
    val RADII_CM = (1..20).map { it.toDouble() }
    const val TOLERANCE = 0.03
    fun hit(value: Double, target: Double) = kotlin.math.abs(value - target) <= target * TOLERANCE
}
