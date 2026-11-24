package de.experimentopia.core.model

/**
 * Alles, was Blubb in der Oberfläche sagt – an einer Stelle, damit jeder Satz vorab im Tonstudio vertont werden kann.
 * Auch die Varianten (je Insel, je Fach, je Gerät) stehen in [all]; zur Laufzeit muss dann nichts mehr nachvertont werden.
 */
object Say {
    const val PREDICT = "Was glaubst du? Es gibt kein Falsch – Forscher vermuten immer zuerst."
    const val TRANSFER = "Jetzt bist du der Profi. Was passiert hier?"
    const val EXPLAIN_ASK = "Ich verstehe das nicht. Kannst du es mir erklären? Warum ist das so?"
    const val EXPLAIN_WRONG = "Hmm … das glaube ich nicht ganz. Erklär's mir noch mal anders?"
    const val EXPLAIN_RIGHT = "Ahhh! Jetzt versteh ich's! Danke, Professor!"
    const val JOURNAL_EMPTY = "Dein Forscherbuch ist noch leer. Mach deinen ersten Versuch – ich schreibe alles mit!"
    const val FREE_LAB = "Hier gibt es keine Aufgaben. Stell eigene Fragen, gib eigene Werte ein und probier aus, was passiert!"
    const val PRO_HAS = "Du hast den Forscher-Pass! Alle Inseln und Geräte gehören dir. Danke! 💛"
    const val PRO_ALL = "Hier steht, was zum Forscher-Pass gehört."
    fun proWhat(what: String) = "„$what“ gehört zum Forscher-Pass."

    /** Zahl der Inseln als Wort – damit der Begrüßungstext nie veraltet */
    fun islandCountWord() = listOf("null", "einer", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn", "elf", "zwölf").getOrElse(Island.entries.size) { "${Island.entries.size}" }
    fun welcome() = "Hallo! Ich bin Blubb, der neugierigste Kolben der Welt. Auf ${islandCountWord()} Inseln warten Rätsel. Du vermutest, probierst aus – und erklärst mir dann, warum. Falsch vermuten ist hier super: Das sind Aha-Momente!"
    fun subject(title: String) = "Hier lernst du $title Schritt für Schritt – in der Reihenfolge wie in der Schule. Wähl deine Klasse, dann zeige ich dir, was für dich dran ist."
    fun island(i: Island) = when (i) {
        Island.WASSER -> "Willkommen auf der Wasserinsel! Hier geht es ums Schwimmen und Sinken – und um ein Geheimnis namens Dichte."
        Island.KRAFT -> "Auf der Kraftwerk-Insel hebst du Papas, Klaviere und Steine – mit Köpfchen statt Muskeln."
        Island.STROM -> "Achtung, Blitzinsel! Lampen, Schalter, Magnete. Hier ist alles sicher – zu Hause bitte nie an die Steckdose!"
        Island.CHEMIE -> "Im Chemie-Vulkan blubbert, zischt und knallt es. Hier darfst du alles mischen – zu Hause nur mit Erwachsenen."
        Island.WAERME -> "Feuer & Eis: Was schmilzt, was kocht, und warum bleibt das Thermometer manchmal stehen?"
        Island.LICHTKLANG -> "Auf dieser Insel mischst du Licht, zauberst Regenbögen und spielst Musik mit Wasser."
        Island.FALL -> "Vom Fallturm bis zum Mond: Was fällt schneller – und warum tickt eine Pendeluhr so gleichmäßig?"
        Island.BIO -> "Willkommen im Bio-Dschungel! Hier kreuzt du Erbsen wie Mendel, lässt Kartoffeln schrumpfen, jagst Nährstoffe, mikroskopierst Teichwasser, baust Nahrungsketten und hörst dein Herz schlagen."
    }

    /** Startseite am PC: Wissens-Häppchen zum Thema des Tagesrätsels */
    fun didYouKnow(t: Topic) = "Wusstest du? ${Theory.of(t).idea}"

    /** Jeder mögliche Satz (für die Vertonung vorab) */
    fun all(): List<String> = listOf(PREDICT, TRANSFER, EXPLAIN_ASK, EXPLAIN_WRONG, EXPLAIN_RIGHT, JOURNAL_EMPTY, FREE_LAB, PRO_HAS, PRO_ALL, welcome()) +
        Subject.entries.map { subject(it.title) } + Island.entries.map { island(it) } +
        (Catalog.all.map { it.title } + Lab.entries.map { it.title }).distinct().map { proWhat(it) } +
        Calibration.ALL + Topic.entries.map { Theory.of(it).idea } + Topic.entries.map { didYouKnow(it) }
}
