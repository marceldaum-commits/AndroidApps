package de.kurvenflug.core.model

/**
 * Direkt-Links von anderen Lern-Apps: „…/#kapitel=<name>“ (auch „?kapitel=“).
 * Die Namen bleiben fest, auch wenn sich die angezeigte Reihenfolge oder Nummer ändert (Absprache Wochenrunde 18.11.).
 * Alte Zahlen-Links (#kapitel=7) gehen zusätzlich; Unbekanntes öffnet einfach die Startseite – nie ein toter Link.
 */
object ChapterLinks {
    /** Kapitel-Nummer (intern) → fester Name. Neue Kapitel hier ergänzen; Namen nie umbenennen. */
    val NAMES: Map<Int, String> = mapOf(
        1 to "koordinaten", 15 to "brueche", 16 to "winkel", 29 to "massstab", 8 to "zuordnungen",
        2 to "geraden", 23 to "prozent", 7 to "rendezvous", 9 to "pythagoras", 3 to "parabeln",
        18 to "nullstellen", 14 to "trigonometrie", 5 to "wellen", 26 to "verschobene-wellen", 25 to "potenzfunktionen",
        10 to "logarithmus", 13 to "weg-zeit", 24 to "beschleunigung", 4 to "wurf", 27 to "waagerechter-wurf",
        6 to "schleuder", 12 to "energie", 11 to "laser", 17 to "lichtbrechung",
        19 to "ableitung", 20 to "extrempunkte", 21 to "integral", 22 to "vektoren", 28 to "e-funktion", 30 to "binomial",
    )

    private val pattern = Regex("""[#?&\s]kapitel=([A-Za-z0-9-]{1,40})""")

    /** Name oder Nummer aus Adresse (search + hash) lesen; null, wenn kein Kapitel-Link */
    fun extract(location: String): String? = pattern.find(" $location")?.groupValues?.get(1)?.lowercase()

    /** Kapitel zum Namen oder zur Nummer – nur wenn es das Kapitel in der App wirklich gibt */
    fun chapter(key: String): Chapter? {
        val number = key.toIntOrNull() ?: NAMES.entries.firstOrNull { it.value == key }?.key ?: return null
        return Levels.chapters.firstOrNull { it.number == number }
    }

    /** Welches Level der Link öffnet: das erste noch nicht geschaffte (freigeschaltete), sonst das erste des Kapitels */
    fun level(chapter: Chapter, stars: Map<String, Int>): Level {
        val open = chapter.levels.indexOfFirst { (stars[it.id] ?: 0) == 0 }
        return if (open <= 0) chapter.levels.first() else chapter.levels[open]
    }
}
