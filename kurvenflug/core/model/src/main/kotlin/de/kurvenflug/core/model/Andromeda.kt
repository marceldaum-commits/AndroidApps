package de.kurvenflug.core.model

/** Ein Planet in Andromeda: Name, Bild und 3 Level der schweren Stufe. */
data class AndroPlanet(val n: Int, val name: String, val emoji: String, val levels: List<Level>)

/**
 * Andromeda – die zweite Galaxie, sobald die Aurora fertig ist. Endlos: Jeder Planet wird aus seiner Nummer
 * erzeugt (immer gleich), mit 3 schwereren Leveln aus dem Generator. Die ersten 18 Planeten bringen je ein Teil
 * für das zweite Schiff „Nova“; danach geht die Reise einfach weiter.
 */
object Andromeda {
    const val PREFIX = "andro-"
    const val SHIP = "Nova"
    const val PARTS = 18

    private val first = listOf("Kristall", "Nebel", "Eis", "Lava", "Funkel", "Wirbel", "Glüh", "Schatten", "Regenbogen", "Donner", "Zucker", "Spiegel", "Purpur", "Silber")
    private val second = listOf("welt", "mond", "riese", "zwerg", "perle", "kugel", "insel", "stern", "garten", "wüste")
    private val emojis = listOf("🪐", "🌑", "🌕", "🌍", "🌖", "☄️", "🌋", "❄️", "🌀", "💠", "🔮", "🌸")

    fun isAndromeda(id: String) = id.startsWith(PREFIX)
    fun unlocked(partsFound: Int) = partsFound >= Story.parts.size

    fun planet(n: Int): AndroPlanet {
        val name = first[Math.floorMod(n * 5 + 3, first.size)] + second[Math.floorMod(n * 7 + 1, second.size)]
        val levels = (1..3).map { k ->
            val kind = Gen.kinds[Math.floorMod(n * 3 + k * 2, Gen.kinds.size)]
            Gen.level(kind, 900_000L + n * 31_337L + k * 7_717L, 2).copy(id = "$PREFIX$n-$k", chapter = 0, number = k, title = "$name · Etappe $k")
        }
        return AndroPlanet(n, name, emojis[Math.floorMod(n * 5, emojis.size)], levels)
    }

    /** Level allein aus der Kennung „andro-<Planet>-<Etappe>“ */
    fun levelFor(id: String): Level? {
        if (!isAndromeda(id)) return null
        val (n, k) = id.removePrefix(PREFIX).split('-').mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: return null
        if (n < 1 || k !in 1..3) return null
        return planet(n).levels[k - 1]
    }

    /** Nächste Etappe auf demselben Planeten */
    fun next(id: String): Level? = levelFor(id)?.let { l -> if (l.number < 3) levelFor(id.substringBeforeLast('-') + "-" + (l.number + 1)) else null }

    fun done(n: Int, stars: Map<String, Int>) = (1..3).all { (stars["$PREFIX$n-$it"] ?: 0) > 0 }

    /** Geschaffte Planeten in Folge ab 1 – der nächste ist der aktuelle */
    fun completed(stars: Map<String, Int>): Int = generateSequence(1) { it + 1 }.takeWhile { done(it, stars) }.count()

    /** Nova-Teile: ein Teil je geschafftem Planeten, höchstens 18 */
    fun novaParts(stars: Map<String, Int>) = minOf(completed(stars), PARTS)

    /** Kapitel-Rahmen für die Anzeige eines Planeten (Kopfzeile, Level-Kacheln) */
    fun chapter(n: Int): Chapter = planet(n).let { p -> Chapter(1000 + n, "${p.name} (Andromeda $n)", "Andromeda", p.emoji, emptyList(), p.levels) }
}
