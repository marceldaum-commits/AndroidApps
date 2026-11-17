package de.experimentopia.core.model

/**
 * Frei und Pro („Forscher-Pass“). **Vorbereitet, aber ausgeschaltet** ([ENABLED] = false): Solange ist alles frei,
 * es gibt keinen Kauf und keine Sperre. Eingeschaltet wird erst, wenn es einen Bezahlweg gibt (wie bei Raumlicht).
 * Grundsatz: Die freie Fassung muss für sich begeistern – jede Insel ist anspielbar.
 */
object Pro {
    /** App-Nummer in den Freischalt-Codes (Vorgabe Leitstand) */
    const val APP_ID = 6
    /** Preis auf der Website (brutto, inkl. 19 % USt.: 6,71 € + 1,28 €) – freigegeben von Marcel am 17.11.2026 */
    const val WEB_PRICE = "7,99 €"
    /** Öffentlicher Schlüssel des Lizenz-Dienstes und Kasse (aus pro/pro.properties) – beim Start gesetzt */
    var publicKey = ""; private set
    var checkoutUrl = ""; private set
    fun configure(key: String, checkout: String) { publicKey = key.trim(); checkoutUrl = checkout.trim() }
    /** Gesperrt wird nur, wenn man Pro wirklich kaufen kann (Schlüssel und Kasse eingetragen) – sonst ist alles frei */
    val ENABLED: Boolean get() = publicKey.length == 64 && checkoutUrl.isNotBlank()
    /** Ist dieser Code ein gültiger Forscher-Pass? */
    fun valid(code: String?, today: Long) = License.check(code, publicKey, today, APP_ID) != null
    /** Von jeder Insel sind die ersten Versuche frei … */
    const val FREE_PER_ISLAND = 5
    /** … die Wasserinsel ganz */
    val FREE_ISLANDS = setOf(Island.WASSER)
    /** Labore, die man auch ohne Pro frei bespielen kann */
    val FREE_LABS = setOf(Lab.SCHWIMMEN, Lab.WIPPE, Lab.STROM, Lab.ROTKOHL, Lab.LICHT, Lab.FALLTURM)

    fun freeExperiment(e: Experiment) = e.island in FREE_ISLANDS || Catalog.of(e.island).indexOf(e) < FREE_PER_ISLAND
    fun freeLab(l: Lab) = l in FREE_LABS

    /** Gesperrt für diesen Spieler? [proOwned] = gekauft, [enabled] = Sperren aktiv */
    /** [done]: schon gemachte Versuche bleiben immer offen (Bestandsschutz) */
    fun locked(e: Experiment, proOwned: Boolean, enabled: Boolean = ENABLED, done: Set<String> = emptySet()) = enabled && !proOwned && !freeExperiment(e) && e.id !in done
    fun locked(l: Lab, proOwned: Boolean, enabled: Boolean = ENABLED) = enabled && !proOwned && !freeLab(l)

    /** Satz für Eltern: Was kostet die App? Ändert sich von selbst, wenn Pro eingeschaltet wird. */
    val purchaseNote get() = if (ENABLED) "Ein großer Teil ist kostenlos. Wer mag, kann den Forscher-Pass einmal kaufen ($WEB_PRICE, kein Abo) – nur nach einer Frage für Erwachsene."
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
