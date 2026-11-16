package de.experimentopia.core.model

/** Dinge für den Magnet-Test. Angezogen werden Eisen (Stahl), Nickel und Kobalt – nicht jedes Metall! */
enum class MagnetItem(val title: String, val emoji: String, val magnetic: Boolean, val note: String) {
    BUEROKLAMMER("Büroklammer", "📎", true, "Büroklammern sind aus Stahl – das ist Eisen."),
    NAGEL("Eisennagel", "📌", true, "Eisen wird vom Magneten angezogen."),
    CENT("1-Cent-Münze", "🥉", true, "Kupferfarben, aber innen Stahl: 1-, 2- und 5-Cent-Münzen haben einen Eisenkern."),
    SCHERE("Schere", "✂️", true, "Die Klingen sind aus Stahl."),
    WEISSBLECH("Konservendose", "🥫", true, "Konservendosen sind aus verzinntem Stahlblech."),
    ALUDOSE("Getränkedose aus Alu", "🥤", false, "Aluminium ist ein Metall – aber nicht magnetisch."),
    KUPFER("Kupferdraht", "🧵", false, "Kupfer leitet super Strom, wird aber nicht angezogen."),
    GOLDRING("Goldring", "💍", false, "Gold ist nicht magnetisch."),
    HOLZ("Holzklotz", "🌲", false, "Holz ist kein Metall."),
    RADIERER("Radiergummi", "🧽", false, "Gummi wird nicht angezogen."),
    GLAS("Glasmurmel", "🔮", false, "Glas wird nicht angezogen."),
    PAPIER("Papier", "📄", false, "Papier wird nicht angezogen – aber Magnetkraft geht durch Papier hindurch!"),
    ;
}

object Magnets {
    /** Zwei Pole: gleiche stoßen sich ab, verschiedene ziehen sich an. */
    fun attract(a: Char, b: Char) = a != b
}
