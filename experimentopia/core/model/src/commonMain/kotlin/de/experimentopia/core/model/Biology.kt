package de.experimentopia.core.model

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

/** Merkmale für Kreuzungen. Großbuchstabe = dominantes Allel. Intermediär: Mischerbige sehen anders aus (rosa). */
enum class Trait(val title: String, val organism: String, val emoji: String, val big: Char, val small: Char,
                 val dominantLook: String, val recessiveLook: String, val dominantColor: Long, val recessiveColor: Long,
                 val intermediate: String? = null, val intermediateColor: Long? = null) {
    ERBSE("Samenfarbe", "Erbse", "🟢", 'A', 'a', "gelb", "grün", 0xFFFFD84D, 0xFF6BEB7A),
    MEERSCHWEIN("Fellfarbe", "Meerschweinchen", "🐹", 'S', 's', "schwarz", "weiß", 0xFF3A3A3A, 0xFFF3EEFF),
    WUNDERBLUME("Blütenfarbe", "Wunderblume", "🌺", 'R', 'r', "rot", "weiß", 0xFFFF3B5C, 0xFFF3EEFF, intermediate = "rosa", intermediateColor = 0xFFFF9EC4),
    ;
    /** Aussehen eines Genotyps wie „Aa“ */
    fun look(g: String) = when {
        g.count { it == big } == 2 -> dominantLook
        g.count { it == big } == 1 -> intermediate ?: dominantLook
        else -> recessiveLook
    }
    fun color(g: String) = when {
        g.count { it == big } == 2 -> dominantColor
        g.count { it == big } == 1 -> intermediateColor ?: dominantColor
        else -> recessiveColor
    }
    fun genotypes() = listOf("$big$big", "$big$small", "$small$small")
}

/** Vererbung nach Mendel: Kreuzungsquadrat (Punnett) und Aussaat */
object Genetics {
    /** Ordnet Allele: Großbuchstabe zuerst („aA“ → „Aa“), bei Blutgruppen A/B vor 0 */
    fun norm(g: String) = g.toList().sortedWith(compareBy({ if (it == '0') 2 else if (it.isUpperCase()) 0 else 1 }, { it })).joinToString("")
    /** Kreuzungsquadrat: 4 Felder, Zeile = Allel von Elternteil 1, Spalte = Allel von Elternteil 2 */
    fun square(p1: String, p2: String) = p1.map { a -> p2.map { b -> norm("$a$b") } }
    /** Wahrscheinlichkeit je Genotyp */
    fun cross(p1: String, p2: String): Map<String, Double> = square(p1, p2).flatten().groupingBy { it }.eachCount().mapValues { it.value / 4.0 }
    /** Anteil der Nachkommen mit diesem Aussehen */
    fun share(t: Trait, p1: String, p2: String, look: String) = cross(p1, p2).filterKeys { t.look(it) == look }.values.sum()
    /** Aussaat von n Samen – echte Zufallszahlen, aber reproduzierbar (seed) */
    fun sow(p1: String, p2: String, n: Int, seed: Int): List<String> { val r = Random(seed); return List(n) { norm("${p1[r.nextInt(2)]}${p2[r.nextInt(2)]}") } }
}

/** Blutgruppen (AB0): A und B sind dominant über 0, A und B zusammen ergeben AB (kodominant) */
object Blood {
    fun group(g: String) = when { 'A' in g && 'B' in g -> "AB"; 'A' in g -> "A"; 'B' in g -> "B"; else -> "0" }
    fun share(p1: String, p2: String, group: String) = Genetics.cross(p1, p2).filterKeys { group(it) == group }.values.sum()
}

/** Osmose: Wasser wandert durch die Zellwand dorthin, wo mehr gelöst ist. Kartoffelzellen entsprechen etwa 1 % Salz. */
object Osmosis {
    const val CELL_SALT = 1.0   // % Kochsalz, bei dem sich nichts ändert (isotonisch, gerundet)
    /** Massenänderung in % nach 30 Minuten (Modell, gerundet an Schulversuche): positiv = Zelle nimmt Wasser auf */
    fun massChange(saltPercent: Double) = (6.0 * (CELL_SALT - saltPercent)).coerceIn(-25.0, 12.0)
    fun direction(saltPercent: Double) = when { abs(saltPercent - CELL_SALT) < 0.2 -> 0; saltPercent < CELL_SALT -> 1; else -> -1 }
}

/** Lebensmittel und was in ihnen steckt – für die Nachweise */
enum class Food(val title: String, val emoji: String, val starch: Boolean, val glucose: Boolean, val fat: Boolean, val protein: Boolean, val note: String) {
    KARTOFFEL("Kartoffel", "🥔", true, false, false, false, "Kartoffeln speichern ihre Energie als Stärke."),
    BROT("Brot", "🍞", true, false, false, true, "Mehl ist vor allem Stärke, dazu etwas Eiweiß (Gluten)."),
    BUTTER("Butter", "🧈", false, false, true, false, "Butter ist fast nur Fett."),
    APFEL("Apfel", "🍎", false, true, false, false, "Äpfel enthalten Trauben- und Fruchtzucker."),
    HONIG("Honig", "🍯", false, true, false, false, "Honig ist vor allem Trauben- und Fruchtzucker."),
    ZUCKER("Haushaltszucker", "🍬", false, false, false, false, "Haushaltszucker ist Saccharose – der Teststreifen sucht nur Traubenzucker (Glukose)!"),
    EIKLAR("Eiklar", "🥚", false, false, false, true, "Eiklar ist fast reines Eiweiß und Wasser."),
    NUESSE("Nüsse", "🥜", false, false, true, true, "Nüsse sind voller Fett und Eiweiß."),
    ;
}

enum class FoodTest(val title: String, val emoji: String, val positive: String, val negative: String, val posColor: Long, val negColor: Long) {
    LUGOL("Iod-Lösung (Stärke)", "🟤", "blau-schwarz", "bleibt braun", 0xFF1E1A4A, 0xFFB5651D),
    STREIFEN("Glukose-Teststreifen", "🧾", "wird grün", "bleibt gelb", 0xFF2EAD5B, 0xFFFFE066),
    PAPIER("Fettfleck-Probe", "📄", "durchscheinender Fleck", "trocknet weg", 0xFFFFF3B0, 0xFFE8E8E8),
    BIURET("Biuret-Probe (Eiweiß)", "🧪", "wird violett", "bleibt blau", 0xFF8E44AD, 0xFF4FA8FF),
    ;
    fun detects(f: Food) = when (this) { LUGOL -> f.starch; STREIFEN -> f.glucose; PAPIER -> f.fat; BIURET -> f.protein }
}

/** Enzym Amylase im Speichel: baut Stärke ab. Am schnellsten bei Körpertemperatur, ab etwa 60 °C zerstört. */
object Enzyme {
    /** Aktivität 0..1 */
    fun activity(tempC: Double) = if (tempC >= 60) 0.0 else exp(-((tempC - 37) / 14).pow(2))
    /** Anteil Stärke, der nach t Minuten noch da ist */
    fun starchLeft(tempC: Double, minutes: Double, withSaliva: Boolean = true) = if (!withSaliva) 1.0 else exp(-0.35 * activity(tempC) * minutes)
    /** Minuten, bis nur noch 5 % Stärke da sind (Iod-Probe färbt sich nicht mehr) – unendlich, wenn das Enzym nicht arbeitet */
    fun minutesUntilGone(tempC: Double) = activity(tempC).let { if (it < 1e-6) Double.POSITIVE_INFINITY else kotlin.math.ln(20.0) / (0.35 * it) }
}

/** Fotosynthese der Wasserpest: Bläschen pro Minute je nach Licht, CO₂ und Temperatur (Sättigung – vereinfachtes Modell) */
object Photosynthesis {
    /** Lichtstärke bei Abstand d (cm) zur Lampe, bezogen auf 10 cm: I ∼ 1/d² */
    fun light(distanceCm: Double) = (10.0 / distanceCm).pow(2)
    fun bubbles(distanceCm: Double, co2: Boolean, tempC: Double = 20.0, lampOn: Boolean = true): Double {
        if (!lampOn) return 0.0
        val i = light(distanceCm)
        val c = if (co2) 1.0 else 0.35
        val tf = if (tempC >= 45) 0.0 else exp(-((tempC - 25) / 12).pow(2))
        return 40.0 * i / (i + 0.5) * c * tf
    }
}

/** Räuber und Beute (Lotka-Volterra): Hasen und Luchse */
object PredatorPrey {
    data class Point(val year: Double, val hares: Double, val lynx: Double)
    fun simulate(hares0: Double, lynx0: Double, years: Double = 30.0, withLynx: Boolean = true, dt: Double = 0.002): List<Point> {
        val a = 0.9; val b = 0.025; val c = 0.8; val d = 0.008; val k = 1500.0   // Hasen wachsen bis zur Tragfähigkeit k
        var h = hares0; var l = if (withLynx) lynx0 else 0.0; var t = 0.0
        val out = mutableListOf(Point(0.0, h, l)); var next = 0.1
        while (t < years) {
            val dh = a * h * (1 - h / k) - b * h * l; val dl = d * h * l - c * l
            h = (h + dh * dt).coerceAtLeast(0.0); l = (l + dl * dt).coerceAtLeast(0.0); t += dt
            if (t >= next) { out += Point(t, h, l); next += 0.1 }
        }
        return out
    }
    /** Jahr des ersten Höhepunkts einer Kurve */
    fun firstPeak(values: List<Double>, step: Double = 0.1): Double { for (i in 1 until values.size - 1) if (values[i] > values[i - 1] && values[i] >= values[i + 1]) return i * step; return -1.0 }
}

/** Stoffe trennen: Welches Verfahren trennt was? */
enum class Component(val title: String, val emoji: String) { SAND("Sand", "🏖️"), SALZ("Salz", "🧂"), WASSER("Wasser", "💧"), EISEN("Eisenspäne", "🔩"), SCHWEFEL("Schwefel", "🟡"), OEL("Öl", "🌻"), KIES("Kies", "🌑"), KAFFEESATZ("Kaffeesatz", "☕") }
enum class Method(val title: String, val emoji: String, val how: String) {
    SIEB("Sieben", "🧺", "Große Körner bleiben im Sieb hängen."),
    MAGNET("Magnet", "🧲", "Der Magnet zieht Eisen heraus."),
    FILTER("Filtern", "☕", "Feste Körner bleiben im Filterpapier, Gelöstes läuft durch."),
    VERDAMPFEN("Eindampfen", "🔥", "Das Wasser verdampft, Gelöstes bleibt als Kruste zurück."),
    ABSCHEIDEN("Scheidetrichter", "⏳", "Öl schwimmt oben, unten lässt man das Wasser ab."),
}
data class Mixture(val title: String, val emoji: String, val parts: Set<Component>)
object Separation {
    /** Was ein Verfahren aus dem Gemisch herausholt (leer = nichts) */
    fun extract(m: Method, parts: Set<Component>): Set<Component> = when (m) {
        Method.SIEB -> if (Component.KIES in parts && parts.size > 1) setOf(Component.KIES) else emptySet()
        Method.MAGNET -> if (Component.EISEN in parts && parts.size > 1) setOf(Component.EISEN) else emptySet()
        Method.FILTER -> if (Component.WASSER in parts) parts.filter { it == Component.SAND || it == Component.KAFFEESATZ || it == Component.SCHWEFEL || it == Component.KIES || it == Component.EISEN }.toSet() else emptySet()
        Method.VERDAMPFEN -> if (Component.WASSER in parts && parts.size > 1 && parts.none { it == Component.OEL }) setOf(Component.WASSER) else emptySet()
        Method.ABSCHEIDEN -> if (Component.OEL in parts && Component.WASSER in parts) setOf(Component.OEL) else emptySet()
    }
    /** Kleinste Zahl an Schritten, um alles zu trennen (Breitensuche) – für die Tests */
    fun steps(parts: Set<Component>): Int {
        var level = listOf(listOf(parts)); var n = 0
        while (n < 6) {
            if (level.any { groups -> groups.all { it.size <= 1 } }) return n
            level = level.flatMap { groups -> groups.flatMap { g -> Method.entries.mapNotNull { m -> extract(m, g).takeIf { it.isNotEmpty() }?.let { e -> groups - setOf(g) + listOf(e, g - e) } } } }.distinct()
            n++
        }
        return -1
    }
}

/** Papier-Chromatografie: Farbstoffe wandern verschieden weit (Rf-Wert = Strecke Farbe ÷ Strecke Laufmittel) */
enum class Dye(val title: String, val color: Long, val rf: Double) { GELB("Gelb", 0xFFFFD84D, 0.85), ROT("Rot", 0xFFFF3B5C, 0.55), BLAU("Blau", 0xFF3D6FD6, 0.25) }
enum class Marker(val title: String, val color: Long, val dyes: List<Dye>) {
    SCHWARZ("schwarzer Filzstift", 0xFF222222, listOf(Dye.GELB, Dye.ROT, Dye.BLAU)),
    GRUEN("grüner Filzstift", 0xFF2EAD5B, listOf(Dye.GELB, Dye.BLAU)),
    ORANGE("oranger Filzstift", 0xFFFF8A3D, listOf(Dye.GELB, Dye.ROT)),
    BLAU("blauer Filzstift", 0xFF3D6FD6, listOf(Dye.BLAU)),
}

/** Malkasten: Farben mischen heißt Licht wegnehmen. Jede Farbe wirft einen Teil von Rot, Grün und Blau zurück; gemischt wird multipliziert. */
enum class Paint(val title: String, val emoji: String, val refl: Rgb) {
    GELB("Gelb", "🟡", Rgb(0.95, 0.9, 0.08)), CYAN("Blau (Cyan)", "🔵", Rgb(0.08, 0.6, 0.9)), MAGENTA("Rot (Magenta)", "🔴", Rgb(0.92, 0.12, 0.45)), WEISS("Weiß", "⚪", Rgb(0.97, 0.97, 0.97)),
}
object Painting {
    /** Mischfarbe aus Tropfen: gewichtetes geometrisches Mittel der Rückstrahlung (vereinfachtes subtraktives Modell) */
    fun mix(drops: Map<Paint, Int>): Rgb {
        val n = drops.values.sum().takeIf { it > 0 } ?: return Rgb.WEISS
        fun ch(f: (Rgb) -> Double) = drops.entries.fold(1.0) { acc, (p, k) -> acc * f(p.refl).pow(k.toDouble() / n) }
        return Rgb(ch { it.r }, ch { it.g }, ch { it.b })
    }
    fun name(c: Rgb): String {
        val mx = maxOf(c.r, c.g, c.b); val mn = minOf(c.r, c.g, c.b)
        if (mx < 0.3) return "fast Schwarz (Braun)"
        if (mx - mn < 0.12) return if (mx > 0.85) "Weiß" else "matschiges Grau-Braun"
        return when {
            c.g >= c.r && c.g >= c.b && c.b < c.g * 0.6 && c.r < c.g * 0.6 -> "Grün"
            c.r >= c.g && c.b > c.g && c.b > c.r * 0.35 -> if (c.b > c.r * 0.75) "Violett" else "Pink"
            c.r >= c.g && c.g > c.b && c.g > c.r * 0.7 -> "Gelb"
            c.r >= c.g && c.g > c.b -> "Orange"
            c.b >= c.r && c.b >= c.g -> if (c.g > c.b * 0.6) "Blau (Cyan)" else "Blauviolett"
            c.r > c.g && c.r > c.b -> "Rot"
            else -> "eine Mischfarbe"
        }
    }
}
