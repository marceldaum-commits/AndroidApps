package de.wissensgarten.app.ui.common

import kotlin.math.roundToInt

fun formatDays(days: Double): String = when {
    days < 1.0 / 24 -> "< 1 Std"
    days < 1.0 -> "${(days * 24).roundToInt()} Std"
    days < 30 -> "${days.roundToInt()} T"
    days < 365 -> "${(days / 30).roundToInt()} Mon"
    else -> "${"%.1f".format(days / 365)} J"
}

fun plantsWord(n: Int) = if (n == 1) "Pflanze" else "Pflanzen"
