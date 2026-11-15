package de.kurvenflug.core.model

/** Ein Raum der eigenen Raumstation. [cost] in Sternenstaub ✨ (0 = von Anfang an da). */
data class Room(val id: Int, val name: String, val emoji: String, val cost: Int)

/**
 * Raumstation: ein ruhiger Ort zum Einrichten. Sternenstaub = alle je gesammelten Sterne (auch Tagesmissionen,
 * Rettungen, Andromeda). Räume freischalten, mit Stickern schmücken; gerettete Crew-Mitglieder ziehen ein.
 * Kein Kaufen, kein Verfall – was einmal freigeschaltet ist, bleibt.
 */
object Station {
    const val SLOTS = 6
    val rooms = listOf(
        Room(0, "Kommandobrücke", "🎛️", 0), Room(1, "Labor", "🧪", 30), Room(2, "Garten", "🌱", 60),
        Room(3, "Werkstatt", "🔧", 100), Room(4, "Sternwarte", "🔭", 150), Room(5, "Lounge", "🛋️", 220),
    )

    fun earned(allStars: Map<String, Int>) = allStars.values.sum()
    fun spent(unlocked: Set<Int>) = rooms.filter { it.id in unlocked }.sumOf { it.cost }
    fun balance(allStars: Map<String, Int>, unlocked: Set<Int>) = earned(allStars) - spent(unlocked)
    fun open(unlocked: Set<Int>) = unlocked + 0
    fun canUnlock(room: Room, allStars: Map<String, Int>, unlocked: Set<Int>) = room.id !in open(unlocked) && balance(allStars, unlocked) >= room.cost

    /** Wer wohnt wo: Crew-Mitglied n zieht der Reihe nach in die offenen Räume */
    fun residents(room: Int, rescued: Set<Int>, unlocked: Set<Int>): List<CrewMember> {
        val o = open(unlocked).sorted()
        return Season2.crew.filter { it.n in rescued }.filter { o[floorModK(it.n - 1, o.size)] == room }
    }
}

/** Gast-Planet einer Jahreszeit – kommt jedes Jahr wieder. */
enum class SeasonEvent(val key: String, val title: String, val emoji: String, val planet: String, val sticker: Sticker, val during: String) {
    HALLOWEEN("halloween", "Halloween", "🎃", "Kürbis-Komet", Sticker("s_halloween", "🎃", "Kürbis-Komet", Rarity.EPIC), "zu Halloween"),
    ADVENT("advent", "Advent", "🎄", "Lichterplanet", Sticker("s_advent", "🎄", "Sternenbaum", Rarity.EPIC), "im Advent"),
    EASTER("ostern", "Ostern", "🐣", "Eierplanet", Sticker("s_ostern", "🐣", "Weltraum-Küken", Rarity.EPIC), "zu Ostern"),
    SUMMER("sommer", "Sommerferien", "🏖️", "Sonnenstrand", Sticker("s_sommer", "🏖️", "Strandplanet", Rarity.EPIC), "in den Sommerferien"),
}

/**
 * Jahreszeiten-Besuch: Zu Halloween, in der Adventszeit, rund um Ostern und in den Sommerferien kommt ein Gast-Planet
 * mit 5 Etappen und einem Themen-Sticker. Bewusst ohne „jetzt oder nie“: Er kommt jedes Jahr wieder.
 */
object Seasons {
    const val PREFIX = "season-"
    const val STAGES = 5

    /** Ostersonntag (Gaußsche Osterformel, gregorianisch) */
    fun easter(year: Int): CalDate {
        val a = year % 19; val b = year / 100; val c = year % 100; val d = b / 4; val e = b % 4
        val f = (b + 8) / 25; val g = (b - f + 1) / 3; val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4; val k = c % 4; val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31; val day = (h + l - 7 * m + 114) % 31 + 1
        return CalDate.of(year, month, day)
    }

    fun active(date: CalDate): SeasonEvent? {
        val md = date.monthValue * 100 + date.dayOfMonth
        val e = easter(date.year)
        return when {
            md in 1020..1103 -> SeasonEvent.HALLOWEEN
            md >= 1201 || md <= 106 -> SeasonEvent.ADVENT
            !date.isBefore(e.minusDays(7)) && !date.isAfter(e.plusDays(7)) -> SeasonEvent.EASTER
            md in 701..831 -> SeasonEvent.SUMMER
            else -> null
        }
    }

    /** Das „Saison-Jahr“: Advent im Januar gehört noch zum Vorjahr */
    fun year(date: CalDate, s: SeasonEvent) = if (s == SeasonEvent.ADVENT && date.monthValue == 1) date.year - 1 else date.year

    fun isSeason(id: String) = id.startsWith(PREFIX)
    fun id(s: SeasonEvent, year: Int, k: Int) = "$PREFIX${s.key}-$year-$k"

    fun levels(s: SeasonEvent, year: Int): List<Level> = (1..STAGES).map { k ->
        val kind = Gen.kinds[floorModK(s.ordinal * 2 + k, Gen.kinds.size)]
        Gen.level(kind, year * 1_000L + s.ordinal * 97L + k * 13L, if (k >= 4) 2 else 1)
            .copy(id = id(s, year, k), chapter = 0, number = k, title = "${s.emoji} ${s.planet} · Etappe $k")
    }

    fun levelFor(id: String): Level? {
        if (!isSeason(id)) return null
        val p = id.removePrefix(PREFIX).split('-')
        val s = SeasonEvent.entries.firstOrNull { it.key == p.getOrNull(0) } ?: return null
        val year = p.getOrNull(1)?.toIntOrNull() ?: return null
        val k = p.getOrNull(2)?.toIntOrNull()?.takeIf { it in 1..STAGES } ?: return null
        return levels(s, year)[k - 1]
    }

    fun next(id: String): Level? = levelFor(id)?.let { l -> if (l.number < STAGES) levelFor(id.substringBeforeLast('-') + "-" + (l.number + 1)) else null }

    fun done(s: SeasonEvent, year: Int, stars: Map<String, Int>) = (1..STAGES).all { (stars[id(s, year, it)] ?: 0) > 0 }

    fun chapter(s: SeasonEvent, year: Int) = Chapter(2000 + s.ordinal, "${s.planet} ($year)", "Gast-Planet", s.emoji, emptyList(), levels(s, year))
}
