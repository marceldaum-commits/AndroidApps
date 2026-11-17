package de.wissensgarten.app.ui

import de.wissensgarten.app.ui.common.formatDaysText
import org.junit.Assert.assertEquals
import org.junit.Test

/** Dauern in ganzen Sätzen: ausgeschrieben und grammatisch passend (nicht „nach 3 T“). */
class FormatTest {
    @Test fun ausgeschrieben() {
        assertEquals("3 Tage", formatDaysText(3.2))
        assertEquals("3 Tagen", formatDaysText(3.2, dativ = true))
        assertEquals("1 Tag", formatDaysText(1.1, dativ = true))
        assertEquals("2 Monate", formatDaysText(61.0))
        assertEquals("2 Monaten", formatDaysText(61.0, dativ = true))
        assertEquals("1 Monat", formatDaysText(31.0))
        assertEquals("7 Stunden", formatDaysText(7.0 / 24, dativ = true))
        assertEquals("1 Stunde", formatDaysText(1.0 / 24 + 0.001))
        assertEquals("weniger als einer Stunde", formatDaysText(0.01, dativ = true))
        assertEquals("1 Jahr", formatDaysText(365.0))
        assertEquals("1,5 Jahren", formatDaysText(548.0, dativ = true))
    }
}
