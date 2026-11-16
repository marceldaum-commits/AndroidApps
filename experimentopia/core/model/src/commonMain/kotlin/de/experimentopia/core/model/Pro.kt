package de.experimentopia.core.model

/**
 * Frei und Pro („Forscher-Pass“). **Vorbereitet, aber ausgeschaltet** ([ENABLED] = false): Solange ist alles frei,
 * es gibt keinen Kauf und keine Sperre. Eingeschaltet wird erst, wenn es einen Bezahlweg gibt (wie bei Raumlicht).
 * Grundsatz: Die freie Fassung muss für sich begeistern – jede Insel ist anspielbar.
 */
object Pro {
    /** Hauptschalter. Erst auf true setzen, wenn man Pro wirklich kaufen kann – und dann die Texte „keine Käufe“ anpassen. */
    const val ENABLED = false
    /** Von jeder Insel sind die ersten Versuche frei … */
    const val FREE_PER_ISLAND = 5
    /** … die Wasserinsel ganz */
    val FREE_ISLANDS = setOf(Island.WASSER)
    /** Labore, die man auch ohne Pro frei bespielen kann */
    val FREE_LABS = setOf(Lab.SCHWIMMEN, Lab.WIPPE, Lab.STROM, Lab.ROTKOHL, Lab.LICHT, Lab.FALLTURM)

    fun freeExperiment(e: Experiment) = e.island in FREE_ISLANDS || Catalog.of(e.island).indexOf(e) < FREE_PER_ISLAND
    fun freeLab(l: Lab) = l in FREE_LABS

    /** Gesperrt für diesen Spieler? [proOwned] = gekauft, [enabled] = Sperren aktiv */
    fun locked(e: Experiment, proOwned: Boolean, enabled: Boolean = ENABLED) = enabled && !proOwned && !freeExperiment(e)
    fun locked(l: Lab, proOwned: Boolean, enabled: Boolean = ENABLED) = enabled && !proOwned && !freeLab(l)

    /** Satz für Eltern: Was kostet die App? Ändert sich von selbst, wenn Pro eingeschaltet wird. */
    val purchaseNote get() = if (ENABLED) "Fast alles ist kostenlos. Wer mag, kann den Forscher-Pass einmal kaufen (kein Abo) – nur nach Bestätigung durch Erwachsene."
        else "Zurzeit ist alles kostenlos – es gibt nichts zu kaufen."
    val freeCount get() = Catalog.all.count(::freeExperiment)
    val benefits get() = listOf(
        "🧪 alle ${Catalog.all.size} Versuche auf ${Island.entries.size} Inseln",
        "🔬 alle ${Lab.entries.size} Geräte im Freien Labor – mit eigenen Werten",
        "⚛️ Atom-Baukasten mit ${Reactions.all.size} Reaktionen",
        "🎵 Klangröhren, Prisma, Pendel und Fallturm auf Mond und Jupiter",
        "💛 einmal kaufen, für immer – kein Abo, keine Werbung",
    )
    val free get() = listOf(
        "die ganze Wasserinsel",
        "von jeder anderen Insel die ersten $FREE_PER_ISLAND Versuche ($freeCount Versuche insgesamt)",
        "Rätsel des Tages und die Themensuche",
        "Forscherbuch und Abzeichen",
        "Freies Labor: ${FREE_LABS.joinToString { it.title }}",
    )
}
