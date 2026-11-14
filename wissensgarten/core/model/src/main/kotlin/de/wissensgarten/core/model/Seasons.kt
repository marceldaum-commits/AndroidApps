package de.wissensgarten.core.model

import java.time.LocalDate
import java.time.MonthDay

enum class Season(val label: String) { SPRING("Frühling"), SUMMER("Sommer"), AUTUMN("Herbst"), WINTER("Winter") }

/** Ein Fest im Garten mit versteckten Überraschungen zum Einsammeln. */
data class Festival(val id: String, val name: String, val greeting: String, val items: List<String>)

object Seasons {
    fun season(date: LocalDate): Season = when (date.monthValue) {
        3, 4, 5 -> Season.SPRING
        6, 7, 8 -> Season.SUMMER
        9, 10, 11 -> Season.AUTUMN
        else -> Season.WINTER
    }

    /** Ostersonntag (Gaußsche Osterformel, gregorianisch). */
    fun easter(year: Int): LocalDate {
        val a = year % 19; val b = year / 100; val c = year % 100
        val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30; val i = c / 4; val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31; val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    fun festival(date: LocalDate): Festival? {
        val md = MonthDay.from(date)
        val easter = easter(date.year)
        return when {
            !date.isBefore(easter.minusDays(7)) && !date.isAfter(easter.plusDays(1)) ->
                Festival("ostern", "Ostern", "Frohe Ostern! Im Garten sind Eier versteckt 🥚", listOf("🥚", "🥚", "🐣"))
            md >= MonthDay.of(10, 24) && md <= MonthDay.of(10, 31) ->
                Festival("halloween", "Halloween", "Huh! Kürbisse im Garten – findest du alle? 🎃", listOf("🎃", "🎃", "👻"))
            md >= MonthDay.of(12, 1) && md <= MonthDay.of(12, 26) ->
                Festival("advent", "Advent", "Im Schnee liegen Geschenke versteckt 🎁", listOf("🎁", "⭐", "🎁"))
            md == MonthDay.of(12, 31) || md == MonthDay.of(1, 1) ->
                Festival("silvester", "Silvester", "Frohes neues Jahr! 🎆", listOf("🎉", "🍀", "🐷"))
            else -> null
        }
    }
}
