package de.kurvenflug.core.model

/**
 * Kurvenflug Pro: Was frei bleibt und was Pro ist (Konzept Pro, Meldung an den Leitstand).
 * Frei: aller Schulstoff bis Klasse 10. Pro: Oberstufe, Extrem-Prüfungen, Sturm ab Profi, Andromeda ab Planet 7,
 * mehr als 2 Spieler, Pro-Aussehen in der Werkstatt.
 *
 * Bestandsschutz: Wer in einem Pro-Kapitel schon einen Stern hat, behält das ganze Kapitel; wer schon über Planet 6
 * hinaus in Andromeda war, behält Andromeda; schon geschaffte Extrem-Prüfungen bleiben offen. Kein Fortschritt geht verloren.
 * Gesperrt wird nur, wenn ein Kaufweg wirklich angeboten wird (sonst bleibt alles frei).
 */
object Pro {
    const val APP_ID = 1
    /** Oberstufen-Kapitel */
    val chapters = setOf(19, 20, 21, 22, 28, 30)
    const val FREE_PLANETS = 6
    const val FREE_PROFILES = 2
    const val FREE_STORM_TIER = Tier.HARD
    /** Preis auf der Website (brutto, inkl. 19 % USt.) – in der Play-Fassung zeigt Google den Preis an */
    const val WEB_PRICE = "7,99 €"
    const val PLAY_PRODUCT = "kurvenflug_pro"

    data class Perk(val emoji: String, val title: String, val text: String)

    val perks = listOf(
        Perk("🎓", "Oberstufe", "Ableitung, Extrempunkte, Integral, Vektoren, e-Funktion und Binomialverteilung – 6 Kapitel, 35 Level"),
        Perk("🔥", "Extrem-Prüfungen", "In jedem der 30 Kapitel die ultimative Herausforderung"),
        Perk("☄️", "Asteroidensturm Profi & Extrem", "Ab Welle 13 wird es richtig wild"),
        Perk("🌌", "Andromeda ab Planet 7", "Weiterfliegen, schwerere Planeten, das Schiff Nova fertig bauen"),
        Perk("👪", "Bis 6 Spieler", "Für Geschwister und die ganze Familie"),
        Perk("🎨", "Pro-Werkstatt", "Besondere Sonden, Spuren und Hüte für Volt"),
    )

    private fun andromedaPlanet(id: String) = if (Andromeda.isAndromeda(id)) id.removePrefix(Andromeda.PREFIX).substringBefore('-').toIntOrNull() else null

    fun isProChapter(chapter: Int) = chapter in chapters

    /** Gehört dieses Level zu Pro? */
    fun isProLevel(id: String): Boolean =
        ChapterExtreme.isExtreme(id) || (andromedaPlanet(id)?.let { it > FREE_PLANETS } ?: false) ||
            (Levels.byId(id)?.chapter?.let { isProChapter(it) } ?: false)

    private fun hasStar(stars: Map<String, Int>, ids: Iterable<String>) = ids.any { (stars[it] ?: 0) > 0 }

    /** Bestandsschutz: schon begonnenes Pro-Kapitel bzw. Andromeda jenseits Planet 6 bleibt offen. */
    fun keepsChapter(chapter: Int, stars: Map<String, Int>) =
        hasStar(stars, Levels.chapters.firstOrNull { it.number == chapter }?.levels?.map { it.id } ?: emptyList())

    private fun keepsAndromeda(stars: Map<String, Int>) =
        stars.any { (k, v) -> v > 0 && (andromedaPlanet(k) ?: 0) > FREE_PLANETS }

    /** Darf das Level gespielt werden? [locking] = ein Kaufweg wird angeboten. */
    fun open(id: String, pro: Boolean, locking: Boolean, stars: Map<String, Int>): Boolean {
        if (pro || !locking || !isProLevel(id)) return true
        if (ChapterExtreme.isExtreme(id)) return (stars[id] ?: 0) > 0
        if (andromedaPlanet(id) != null) return keepsAndromeda(stars)
        return Levels.byId(id)?.chapter?.let { keepsChapter(it, stars) } ?: true
    }

    fun chapterLocked(chapter: Int, pro: Boolean, locking: Boolean, stars: Map<String, Int>) =
        !pro && locking && isProChapter(chapter) && !keepsChapter(chapter, stars)

    fun planetLocked(n: Int, pro: Boolean, locking: Boolean, stars: Map<String, Int>) =
        !pro && locking && n > FREE_PLANETS && !keepsAndromeda(stars)

    /** Höchste Sturm-Stufe */
    fun maxStormTier(pro: Boolean, locking: Boolean) = if (pro || !locking) Tier.MAX else FREE_STORM_TIER

    /** Wie viele Spieler-Profile angelegt werden dürfen (vorhandene bleiben immer) */
    fun maxProfiles(pro: Boolean, locking: Boolean) = if (pro || !locking) Int.MAX_VALUE else FREE_PROFILES
}
