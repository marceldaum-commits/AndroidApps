package de.experimentopia.core.model

import kotlin.math.exp
import kotlin.math.pow

// ───────────── Mikroskop ─────────────

/** Kleinstlebewesen aus dem Teich. size = Länge in mm (typische Werte aus Bestimmungsbüchern). */
enum class Microbe(val title: String, val sizeMm: Double, val color: Long, val note: String, val seeksLight: Boolean = false) {
    WASSERFLOH("Wasserfloh", 2.0, 0xFFE8A04D, "Ein winziger Krebs mit Ruderfühlern. Man sieht sogar sein Herz schlagen."),
    PANTOFFEL("Pantoffeltierchen", 0.25, 0xFFD9A85A, "Ein Einzeller, der mit Tausenden Wimpern rudert."),
    AMOEBE("Amöbe", 0.3, 0xFF7FB8A0, "Ein Einzeller ohne feste Form – er fließt mit Scheinfüßchen."),
    AUGENTIER("Augentierchen", 0.05, 0xFF4CD06A, "Ein Einzeller mit grünen Blattgrün-Körnern und einem roten Augenfleck: halb Tier, halb Pflanze.", seeksLight = true),
    BAKTERIUM("Bakterium", 0.002, 0xFFB98CFF, "Bakterien sind die kleinsten Lebewesen hier – sie haben nicht einmal einen Zellkern."),
}

enum class Sight(val text: String) { NICHTS("nicht zu sehen"), PUNKT("nur ein winziger Punkt"), GUT("gut zu erkennen"), ZU_GROSS("zu groß – nur ein Teil passt ins Bild") }

object Microscope {
    /** Vergrößerungen: bloßes Auge, Lupe, Mikroskop-Stufen */
    val MAGS = listOf(1, 10, 40, 100, 400)
    /** Sehfeldzahl: Durchmesser des sichtbaren Kreises in mm = 22 / Vergrößerung (beim Auge: Wassertropfen ca. 5 mm) */
    fun fieldMm(mag: Int) = if (mag == 1) 5.0 else 22.0 / mag
    /** Scheinbare Größe in mm (so groß wirkt es) */
    fun apparentMm(m: Microbe, mag: Int) = m.sizeMm * mag
    /** Erkennbar: Ab 0,1 mm sieht man einen Punkt, ab 5 mm scheinbarer Größe Einzelheiten */
    fun sight(m: Microbe, mag: Int) = when {
        m.sizeMm > fieldMm(mag) -> Sight.ZU_GROSS
        apparentMm(m, mag) >= 5.0 -> Sight.GUT
        apparentMm(m, mag) >= 0.1 -> Sight.PUNKT
        else -> Sight.NICHTS
    }
    /** Kleinste Vergrößerung, bei der es gut zu erkennen ist (null = nie) */
    fun best(m: Microbe) = MAGS.firstOrNull { sight(m, it) == Sight.GUT }
    /** Wie viele passen nebeneinander durchs Sehfeld */
    fun across(m: Microbe, mag: Int) = fieldMm(mag) / m.sizeMm
}

// ───────────── Keimung ─────────────

/** Eine Schale mit Kresse-Samen */
data class Dish(val label: String, val water: Boolean, val tempC: Double, val air: Boolean = true, val light: Boolean = true)

object Germination {
    /** Kresse keimt nur mit Wasser, Luft (Sauerstoff) und zwischen etwa 2 und 38 °C. Licht ist zum Keimen nicht nötig. */
    fun canGerminate(d: Dish) = d.water && d.air && d.tempC > 2 && d.tempC < 38
    /** Tage bis zum Keimen (bei 22 °C etwa 1,5 Tage; Kälte bremst stark) */
    fun daysToSprout(d: Dish) = if (!canGerminate(d)) Double.POSITIVE_INFINITY else 1.5 / exp(-((d.tempC - 22) / 12).pow(2))
    /** Höhe in cm nach [days] Tagen. Im Dunkeln schießt der Keimling in die Höhe (Vergeilen), bleibt aber gelb und dünn. */
    fun heightCm(d: Dish, days: Double): Double {
        val grow = days - daysToSprout(d)
        if (grow <= 0) return 0.0
        val rate = (if (d.light) 0.7 else 1.2) * exp(-((d.tempC - 22) / 12).pow(2))
        return minOf(grow * rate, if (d.light) 6.0 else 10.0)
    }
    fun green(d: Dish) = d.light
}

// ───────────── Nahrungsketten ─────────────

enum class Organism(val title: String, val emoji: String, val producer: Boolean = false) {
    GRAS("Gras", "🌱", true), LOEWENZAHN("Löwenzahn", "🌼", true),
    RAUPE("Raupe", "🐛"), HEUSCHRECKE("Heuschrecke", "🦗"), HASE("Hase", "🐇"), MAUS("Maus", "🐭"),
    AMSEL("Amsel", "🐦"), FROSCH("Frosch", "🐸"), SCHLANGE("Ringelnatter", "🐍"), FUCHS("Fuchs", "🦊"), EULE("Waldkauz", "🦉"),
}

object FoodWeb {
    /** Wer frisst wen (vereinfachte Wiese am Waldrand) */
    val eats: Map<Organism, Set<Organism>> = mapOf(
        Organism.RAUPE to setOf(Organism.LOEWENZAHN, Organism.GRAS),
        Organism.HEUSCHRECKE to setOf(Organism.GRAS),
        Organism.HASE to setOf(Organism.GRAS, Organism.LOEWENZAHN),
        Organism.MAUS to setOf(Organism.GRAS, Organism.HEUSCHRECKE),
        Organism.AMSEL to setOf(Organism.RAUPE),
        Organism.FROSCH to setOf(Organism.HEUSCHRECKE, Organism.RAUPE),
        Organism.SCHLANGE to setOf(Organism.FROSCH, Organism.MAUS),
        Organism.FUCHS to setOf(Organism.MAUS, Organism.HASE, Organism.AMSEL),
        Organism.EULE to setOf(Organism.MAUS),
    )
    fun food(o: Organism) = eats[o] ?: emptySet()
    fun canFollow(prev: Organism, next: Organism) = prev in food(next)
    fun valid(chain: List<Organism>) = chain.isNotEmpty() && chain.first().producer && chain.zipWithNext().all { (a, b) -> canFollow(a, b) }
    /** Längste mögliche Kette (Anzahl Glieder) */
    fun longest(): Int {
        fun from(o: Organism): Int = 1 + (Organism.entries.filter { o in food(it) }.maxOfOrNull { from(it) } ?: 0)
        return Organism.entries.filter { it.producer }.maxOf { from(it) }
    }
    /** Wer hungert, wenn [gone] verschwindet (frisst nichts anderes) */
    fun starving(gone: Organism) = eats.filter { (o, f) -> o != gone && gone in f && (f - gone).isEmpty() }.keys
    /** 10-%-Regel: Energie auf Stufe i (0 = Pflanze) */
    fun energy(startKj: Double, level: Int) = startKj * 0.1.pow(level)
    /** Anreicherung von Gift: jede Stufe frisst das Zehnfache ihrer Masse – das Gift bleibt im Körper */
    fun poison(level: Int) = 10.0.pow(level)
}

// ───────────── Herz und Puls ─────────────

enum class Activity(val title: String, val emoji: String, val factor: Double) {
    RUHE("Ruhe", "🛋️", 1.0), GEHEN("Gehen", "🚶", 1.3), JOGGEN("Joggen", "🏃", 1.9), SPRINT("Sprinten", "⚡", 2.4),
}

object Heart {
    /** Ruhepuls nach Alter (Mittelwerte, Schläge pro Minute) */
    fun restPulse(age: Int) = when { age <= 6 -> 95.0; age <= 10 -> 85.0; age <= 14 -> 78.0; else -> 70.0 }
    fun maxPulse(age: Int) = 220.0 - age
    fun pulse(age: Int, a: Activity) = minOf(restPulse(age) * a.factor, maxPulse(age))
    /** Blut pro Herzschlag in ml (Erwachsene in Ruhe etwa 70 ml) */
    fun strokeMl(age: Int) = when { age <= 10 -> 45.0; age <= 14 -> 60.0; else -> 70.0 }
    /** Liter pro Minute */
    fun litersPerMinute(age: Int, a: Activity) = pulse(age, a) * strokeMl(age) * (if (a == Activity.RUHE) 1.0 else 1.25) / 1000.0
    fun perDay(pulse: Double) = pulse * 60 * 24
    /** Erholung nach Belastung: Puls nach t Minuten. Trainierte erholen sich schneller (kleinere Zeitkonstante). */
    fun recovery(rest: Double, peak: Double, minutes: Double, trained: Boolean) = rest + (peak - rest) * exp(-minutes / (if (trained) 0.8 else 1.8))
    fun fromCount(beats15s: Int) = beats15s * 4
}
