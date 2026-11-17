package de.wissensgarten.core.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class Season(val label: String) { SPRING("Frühling"), SUMMER("Sommer"), AUTUMN("Herbst"), WINTER("Winter") }

/** Ein Fest im Garten mit versteckten Überraschungen zum Einsammeln. */
data class Festival(val id: String, val name: String, val greeting: String, val items: List<String>)

object Seasons {
    /** Heute nach Ortszeit. */
    fun today(): LocalDate = LocalDate.fromEpochDays(Scheduler.localDay(Platform.nowMs()).toInt())

    fun season(date: LocalDate): Season = when (date.month.ordinal + 1) {
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
        return LocalDate(year, month, day)
    }

    /** Ostern, Halloween, Advent, Silvester */
    val FESTIVALS = listOf(
        Festival("ostern", "Ostern", "Frohe Ostern! Im Garten sind Eier versteckt 🥚", listOf("🥚", "🥚", "🐣")),
        Festival("halloween", "Halloween", "Huh! Kürbisse im Garten – findest du alle? 🎃", listOf("🎃", "🎃", "👻")),
        Festival("advent", "Advent", "Im Schnee liegen Geschenke versteckt 🎁", listOf("🎁", "⭐", "🎁")),
        Festival("silvester", "Silvester", "Frohes neues Jahr! 🎆", listOf("🎉", "🍀", "🐷")),
    )

    fun festival(date: LocalDate): Festival? {
        val md = (date.month.ordinal + 1) * 100 + date.day
        val easter = easter(date.year)
        return when {
            date >= easter.minus(DatePeriod(days = 7)) && date <= easter.plus(DatePeriod(days = 1)) -> FESTIVALS[0]
            md in 1024..1031 -> FESTIVALS[1]
            md in 1201..1226 -> FESTIVALS[2]
            md == 1231 || md == 101 -> FESTIVALS[3]
            else -> null
        }
    }
}
