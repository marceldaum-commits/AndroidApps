package de.kurvenflug.core.model

/**
 * Lehrplan-Abgleich (Marcel 18.11.): Themen der Lehrpläne von Gymnasium und Realschule (Kl. 5–10, KMK-Bildungsstandards;
 * BY, BW, NRW, NI, HE), die Kurvenflug noch nicht als eigenes Kapitel hat. Sie erscheinen in der Galaxie als „Kommt bald“ –
 * ohne Datum, ohne Kaufhinweis. Wird ein Thema gebaut, wandert es hier heraus und ist ein normales Kapitel.
 */
data class Upcoming(val emoji: String, val title: String, val subject: Subject, val grades: IntRange, val what: String) {
    val gradeText get() = if (grades.first == grades.last) "Klasse ${grades.first}" else "Klasse ${grades.first}–${grades.last}"
}

object Curriculum {
    val upcoming: List<Upcoming> = listOf(
        // Mathematik – nur Funktionen/Zahlengerade/Koordinaten (Profil Kl. 5–10 „sehen und fliegen“, Abgleich 18./19.11.;
        // Flächen, Körper, Daten, Wahrscheinlichkeit, Potenzen → Mathe-Trainer/Rechenblitz, Oberstufe → Mathe-Trainer/Raumlicht)
        Upcoming("➖", "Negative Zahlen", Subject.MATH, 6..7, "Am Zahlenstrahl fliegen: Plus und Minus, Rechengesetze"),
        Upcoming("🧮", "Terme & Gleichungen", Subject.MATH, 7..8, "Terme vereinfachen, lineare Gleichungen umformen und lösen"),
        Upcoming("🪞", "Spiegeln & Verschieben", Subject.MATH, 6..7, "Achsen- und Punktsymmetrie, Spiegeln, Drehen, Verschieben im Koordinatensystem"),
        Upcoming("🎛️", "Funktionen formen", Subject.MATH, 9..10, "Graphen strecken, spiegeln, verschieben: a · f(b · (x − c)) + d"),
        // Physik – Nachrechnen mit Formeln und Einheiten (Ausprobieren: Experimentopia)
        Upcoming("⚙️", "Kräfte & Hebel", Subject.PHYSICS, 7..8, "Kraft, Hebelgesetz, Flaschenzug, Reibung"),
        Upcoming("💧", "Druck & Auftrieb", Subject.PHYSICS, 8..9, "Druck in Flüssigkeiten und Gasen, Archimedes"),
        Upcoming("🔌", "Strom & Ohmsches Gesetz", Subject.PHYSICS, 7..9, "Stromkreise, Spannung, Stromstärke, Widerstand U = R · I"),
        Upcoming("🧲", "Magnetismus & Induktion", Subject.PHYSICS, 8..10, "Magnetfeld, Elektromagnet, Induktion, Generator – nachrechnen"),
        Upcoming("🌡️", "Wärme", Subject.PHYSICS, 7..9, "Temperatur, Wärmeenergie, Wärmetransport"),
        Upcoming("🛰️", "Kreisbahnen & Satelliten", Subject.PHYSICS, 10..10, "Zentripetalkraft, Umlaufzeit, geostationäre Bahn"),
        Upcoming("🎱", "Impuls & Stöße", Subject.PHYSICS, 10..10, "Impulserhaltung, elastische und unelastische Stöße"),
        Upcoming("🕰️", "Schwingungen", Subject.PHYSICS, 10..10, "Pendel und Federschwinger, Periode und Frequenz"),
    )

    fun upcoming(subject: Subject?): List<Upcoming> = upcoming.filter { subject == null || it.subject == subject }
}
