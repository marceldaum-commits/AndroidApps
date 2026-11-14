package de.wissensgarten.core.model

/**
 * Fehler werden zu Kristallen: Eine Karte, die mindestens zweimal vergessen wurde und jetzt
 * mindestens drei Wochen hält, ist ein Kristall. Fehler sind so etwas Wertvolles, kein Makel.
 */
object Crystals {
    const val MIN_LAPSES = 2
    const val MIN_STABILITY_DAYS = 21.0

    fun isCrystal(m: Memory) = m.lapses >= MIN_LAPSES && m.stability >= MIN_STABILITY_DAYS

    /** Wurde mit dieser Wiederholung gerade ein neuer Kristall? */
    fun formed(before: Memory, after: Memory) = !isCrystal(before) && isCrystal(after)

    /** Wie weit ist eine „schwere“ Karte auf dem Weg zum Kristall? (0..1, null wenn nicht schwer) */
    fun progress(m: Memory): Double? = if (m.lapses < MIN_LAPSES) null else (m.stability / MIN_STABILITY_DAYS).coerceIn(0.0, 1.0)
}
