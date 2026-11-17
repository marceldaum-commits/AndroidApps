package de.wissensgarten.app.ui.common

import kotlin.math.roundToInt

fun formatDays(days: Double): String = when {
    days < 1.0 / 24 -> "< 1 Std"
    days < 1.0 -> "${(days * 24).roundToInt()} Std"
    days < 30 -> "${days.roundToInt()} T"
    days < 365 -> "${(days / 30).roundToInt()} Mon"
    else -> "${de.wissensgarten.app.ui.common.oneDecimal(days / 365)} J"
}

/**
 * Dauer ausgeschrieben für ganze Sätze (Kinder lesen „3 Tage“ statt „3 T“).
 * [dativ]: „nach 3 Tagen“, „in 2 Monaten“; sonst „hält 3 Tage“, „etwa 2 Monate“.
 */
fun formatDaysText(days: Double, dativ: Boolean = false): String {
    fun word(n: Int, one: String, many: String) = "$n ${if (n == 1) one else if (dativ) many + "n" else many}"
    return when {
        days < 1.0 / 24 -> if (dativ) "weniger als einer Stunde" else "weniger als eine Stunde"
        days < 1.0 -> (days * 24).roundToInt().coerceAtLeast(1).let { "$it ${if (it == 1) "Stunde" else "Stunden"}" }
        days < 30 -> word(days.roundToInt().coerceAtLeast(1), "Tag", "Tage")
        days < 365 -> word((days / 30).roundToInt().coerceAtLeast(1), "Monat", "Monate")
        else -> {
            val y = oneDecimal(days / 365).removeSuffix(",0")
            "$y ${if (y == "1") "Jahr" else if (dativ) "Jahren" else "Jahre"}"
        }
    }
}

fun plantsWord(n: Int) = if (n == 1) "Pflanze" else "Pflanzen"
