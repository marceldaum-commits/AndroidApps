package de.experimentopia.core.model

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sin

/** Stoffe, durch die Licht läuft – mit Brechzahl n */
enum class Medium(val title: String, val emoji: String, val n: Double) {
    LUFT("Luft", "💨", 1.0), WASSER("Wasser", "💧", 1.33), GLAS("Glas", "🔷", 1.5), DIAMANT("Diamant", "💎", 2.42)
}

/**
 * Geometrische Optik: Schatten (Strahlensatz), Spiegel (Reflexionsgesetz), Brechung (Snellius) und Sammellinse (Linsengleichung).
 * Längen in cm, Winkel in Grad.
 */
object Optics {
    /** Schattengröße an der Wand: Lampe (punktförmig) – Gegenstand (Höhe [objCm], Abstand [lampToObj]) – Wand (Abstand [lampToWall]) */
    fun shadow(objCm: Double, lampToObj: Double, lampToWall: Double) = objCm * lampToWall / lampToObj
    /** Lampenabstand für eine gewünschte Schattengröße */
    fun lampFor(objCm: Double, shadowCm: Double, lampToWall: Double) = objCm * lampToWall / shadowCm

    /** Reflexion: Richtung des Strahls (Winkel gegen die x-Achse) an einem Spiegel, der um [mirrorDeg] gedreht ist */
    fun reflectDir(beamDeg: Double, mirrorDeg: Double) = norm360(2 * mirrorDeg - beamDeg)
    /** Einfallswinkel gegen das Lot (0..90°) */
    fun incidence(beamDeg: Double, mirrorDeg: Double): Double { val d = abs(norm180(beamDeg - mirrorDeg)); return abs(90 - minOf(d, 180 - d)) }
    /** Reflexionsgesetz: Ausfallswinkel = Einfallswinkel */
    fun reflectAngle(inDeg: Double) = inDeg
    /** Spiegelstellung, damit der Strahl in Richtung [targetDeg] weiterläuft (0..180°) */
    fun mirrorFor(beamDeg: Double, targetDeg: Double) = norm360((targetDeg + beamDeg) / 2).let { if (it >= 180) it - 180 else it }
    fun norm360(a: Double) = ((a % 360) + 360) % 360
    fun norm180(a: Double) = norm360(a + 180) - 180
    fun angleDiff(a: Double, b: Double) = abs(norm180(a - b))

    /** Brechung (Snellius): n₁ · sin α = n₂ · sin β. null = Totalreflexion */
    fun refract(inDeg: Double, n1: Double, n2: Double): Double? {
        val s = n1 / n2 * sin(toRad(inDeg))
        return if (abs(s) > 1) null else toDeg(asin(s))
    }
    /** Grenzwinkel der Totalreflexion (nur von dichter nach dünner) */
    fun critical(n1: Double, n2: Double): Double? = if (n1 <= n2) null else toDeg(asin(n2 / n1))

    /** Sammellinse: 1/f = 1/g + 1/b → Bildweite b; null, wenn der Gegenstand innerhalb der Brennweite steht (Lupe, kein Bild auf dem Schirm) */
    fun imageDist(f: Double, g: Double): Double? = if (g <= f + 1e-9) null else 1 / (1 / f - 1 / g)
    /** Abbildungsmaßstab B/G = b/g */
    fun magnification(f: Double, g: Double): Double? = imageDist(f, g)?.let { it / g }
    /** Lupe: scheinbare Vergrößerung des aufrechten (virtuellen) Bildes, b' = f·g/(f−g) */
    fun virtualMag(f: Double, g: Double): Double? = if (g >= f) null else f / (f - g)
    /** Gegenstandsweite für einen gewünschten Maßstab m: g = f · (1 + 1/m) */
    fun objectFor(f: Double, mag: Double) = f * (1 + 1 / mag)
}

enum class ShadowGoal { SIZE, FREE }
/** Schattenbühne: Figur [objCm] hoch, Wand [wallCm] von der Lampe; Ziel = Schattengröße [targetCm] (± 1 cm) */
data class ShadowSetup(val objCm: Double = 10.0, val wallCm: Double = 100.0, val targetCm: Double? = null) : Setup { override val lab get() = Lab.SCHATTEN }

/** Spiegel-Laser: Strahl kommt mit Richtung [beamDeg]; alle Sterne in den Richtungen [targets] treffen (± 2°) */
data class MirrorSetup(val beamDeg: Double = -40.0, val targets: List<Double> = emptyList(), val showAngles: Boolean = false) : Setup { override val lab get() = Lab.SPIEGEL }

enum class RefractGoal { HIT, TOTAL, FREE }
/** Lichtbrechung an der Grenze [top] → [bottom]; HIT: Strahl soll unter [targetDeg] (± 1,5°) weiterlaufen; TOTAL: Totalreflexion finden */
data class RefractSetup(val top: Medium, val bottom: Medium, val goal: RefractGoal, val targetDeg: Double? = null) : Setup { override val lab get() = Lab.BRECHUNG }

enum class LensGoal { SHARP, SIZE, MAGNIFIER, FREE }
/** Optische Bank: Linse mit Brennweite [f] cm; Kerze bei [g] cm (null = verschiebbar); Ziel scharf (± 0,5 cm), Maßstab [mag] (± 0,05) oder Lupe */
data class LensSetup(val f: Double = 10.0, val g: Double? = null, val goal: LensGoal = LensGoal.SHARP, val mag: Double? = null) : Setup { override val lab get() = Lab.LINSE }
