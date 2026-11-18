package de.kurvenflug.core.model

/**
 * Lehrplan-Abgleich (Marcel 18.11.): Themen der Lehrpläne von Gymnasium und Realschule (Kl. 5–13, KMK-Bildungsstandards;
 * BY, BW, NRW, NI, HE), die Kurvenflug noch nicht als eigenes Kapitel hat. Sie erscheinen in der Galaxie als „Kommt bald“ –
 * ohne Datum, ohne Kaufhinweis. Wird ein Thema gebaut, wandert es hier heraus und ist ein normales Kapitel.
 */
data class Upcoming(val emoji: String, val title: String, val subject: Subject, val grades: IntRange, val what: String) {
    val gradeText get() = if (grades.first == grades.last) "Klasse ${grades.first}" else "Klasse ${grades.first}–${grades.last}"
}

object Curriculum {
    val upcoming: List<Upcoming> = listOf(
        // Mathematik
        Upcoming("➖", "Negative Zahlen", Subject.MATH, 6..7, "Rechnen mit Plus und Minus, Zahlengerade, Rechengesetze"),
        Upcoming("🧮", "Terme & Gleichungen", Subject.MATH, 7..8, "Terme vereinfachen, lineare Gleichungen umformen und lösen"),
        Upcoming("⭕", "Kreis & Flächen", Subject.MATH, 5..9, "Umfang und Fläche von Rechteck, Dreieck, Trapez und Kreis (π)"),
        Upcoming("🧊", "Körper & Volumen", Subject.MATH, 5..10, "Quader, Prisma, Zylinder, Kegel, Kugel: Oberfläche und Volumen"),
        Upcoming("🪞", "Symmetrie & Abbildungen", Subject.MATH, 5..7, "Achsen- und Punktsymmetrie, Spiegeln, Drehen, Verschieben"),
        Upcoming("📏", "Dreiecke konstruieren", Subject.MATH, 7..7, "Kongruenzsätze, Konstruktionen mit Zirkel und Geodreieck"),
        Upcoming("🔺", "Ähnlichkeit & Strahlensätze", Subject.MATH, 9..9, "Zentrische Streckung, Strahlensätze, Maßstab im Raum"),
        Upcoming("📊", "Daten & Diagramme", Subject.MATH, 5..8, "Mittelwert, Median, Spannweite, Säulen- und Boxplots"),
        Upcoming("🎲", "Wahrscheinlichkeit", Subject.MATH, 6..9, "Laplace-Experimente, Baumdiagramme, Pfadregeln"),
        Upcoming("🔟", "Potenzen & Wurzeln", Subject.MATH, 9..10, "Potenzgesetze, Wurzeln, Zehnerpotenzen und wissenschaftliche Schreibweise"),
        Upcoming("〰️", "Kurvendiskussion", Subject.MATH, 11..12, "Ganzrationale Funktionen: Symmetrie, Wendepunkte, Funktionsscharen"),
        Upcoming("🧊", "Geraden & Ebenen im Raum", Subject.MATH, 12..13, "Analytische Geometrie in 3D, Skalarprodukt, Abstände und Winkel"),
        Upcoming("🔔", "Normalverteilung & Testen", Subject.MATH, 12..13, "Glockenkurve, Hypothesentest, Signifikanz"),
        // Physik
        Upcoming("⚙️", "Kräfte & Hebel", Subject.PHYSICS, 7..8, "Kraft, Hebelgesetz, Flaschenzug, Reibung"),
        Upcoming("💧", "Druck & Auftrieb", Subject.PHYSICS, 8..9, "Druck in Flüssigkeiten und Gasen, Archimedes"),
        Upcoming("🔌", "Strom & Ohmsches Gesetz", Subject.PHYSICS, 7..9, "Stromkreise, Spannung, Stromstärke, Widerstand U = R · I"),
        Upcoming("🧲", "Magnetismus & Induktion", Subject.PHYSICS, 8..10, "Magnetfeld, Elektromagnet, Induktion, Generator"),
        Upcoming("🌡️", "Wärme", Subject.PHYSICS, 7..9, "Temperatur, Wärmeenergie, Wärmetransport"),
        Upcoming("🛰️", "Kreisbahnen & Satelliten", Subject.PHYSICS, 10..11, "Zentripetalkraft, Umlaufzeit, geostationäre Bahn"),
        Upcoming("🎱", "Impuls & Stöße", Subject.PHYSICS, 10..11, "Impulserhaltung, elastische und unelastische Stöße"),
        Upcoming("🕰️", "Schwingungen", Subject.PHYSICS, 10..11, "Pendel und Federschwinger, Periode und Frequenz"),
        Upcoming("☢️", "Radioaktivität", Subject.PHYSICS, 9..10, "Atomkern, Strahlung, Halbwertszeit"),
    )

    fun upcoming(subject: Subject?): List<Upcoming> = upcoming.filter { subject == null || it.subject == subject }
}
