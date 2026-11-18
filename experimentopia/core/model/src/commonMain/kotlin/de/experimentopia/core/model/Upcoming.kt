package de.experimentopia.core.model

/** Ein Thema aus dem Lehrplan, das noch gebaut wird – erscheint als „Kommt bald“ (ohne Datum, ohne Kaufhinweis). */
data class Upcoming(val subject: Subject, val title: String, val emoji: String, val grades: IntRange, val teaser: String) {
    val gradeText get() = "Kl. ${grades.first}–${grades.last}"
}

/** Geplante Themen aus dem Lehrplan-Abgleich (docs/LEHRPLAN-ABGLEICH.md), in der geplanten Reihenfolge je Fach */
object Coming {
    val all = listOf(
        Upcoming(Subject.PHYSIK, "Motor, Dynamo & Trafo", "🧲", 8..10, "Magnet in die Spule – und das Lämpchen glimmt."),
        Upcoming(Subject.PHYSIK, "Energie-Achterbahn", "🎢", 7..10, "Lageenergie wird Bewegungsenergie – und wohin verschwindet der Rest?"),
        Upcoming(Subject.PHYSIK, "Druck & Luftdruck", "🤿", 7..9, "Abtauchen, Hebebühne pumpen, Halbkugeln auseinanderziehen."),
        Upcoming(Subject.PHYSIK, "Bewegung & Diagramme", "🏎️", 7..10, "Fahr los – und das Diagramm zeichnet mit."),
        Upcoming(Subject.PHYSIK, "Wärme wandert", "🏠", 5..9, "Haus dämmen, Tee warm halten, Treibhauseffekt."),
        Upcoming(Subject.PHYSIK, "Ladung & Elektrostatik", "🎈", 7..8, "Luftballon-Haare und fliegende Papierschnipsel."),
        Upcoming(Subject.PHYSIK, "Echo & Lautstärke", "🦇", 5..7, "Wie die Fledermaus mit Schall sieht."),
        Upcoming(Subject.PHYSIK, "Radioaktivität", "☢️", 9..10, "Geigerzähler, Abschirmung und Halbwertszeit."),
        Upcoming(Subject.PHYSIK, "Weltall & Jahreszeiten", "🌍", 5..10, "Erdachse kippen, Mondphasen und Finsternisse."),
        Upcoming(Subject.CHEMIE, "Atom-Werkstatt & Periodensystem", "⚛️", 8..10, "Atome bauen – und das Element leuchtet im Periodensystem auf."),
        Upcoming(Subject.CHEMIE, "Salze & Ionen", "🧂", 8..10, "Kristallgitter bauen, Salzwasser leitet Strom."),
        Upcoming(Subject.CHEMIE, "Feuerwehr-Labor", "🧯", 6..8, "Brenndreieck: Nimm eine Ecke weg, und das Feuer geht aus."),
        Upcoming(Subject.CHEMIE, "Metalle & Redox", "🏆", 8..10, "Welches Metall gewinnt das Elektronen-Turnier?"),
        Upcoming(Subject.CHEMIE, "Zitronen-Batterie", "🍋", 9..10, "Strom aus Obst und zwei Metallen."),
        Upcoming(Subject.CHEMIE, "Erdöl & Kunststoffe", "🛢️", 9..10, "Ketten verlängern und den Siedeturm sortieren."),
        Upcoming(Subject.BIOLOGIE, "Tier-Safari", "🦒", 5..6, "Wirbeltiere sortieren, Fliegen und Schwimmen im Windkanal."),
        Upcoming(Subject.BIOLOGIE, "Blüten & Bestäubung", "🐝", 5..6, "Als Biene Blüten bestäuben, Flugsamen fliegen lassen."),
        Upcoming(Subject.BIOLOGIE, "Atmung & Lunge", "🌬️", 5..8, "Zwerchfell ziehen – und die Lunge atmet."),
        Upcoming(Subject.BIOLOGIE, "Skelett & Muskeln", "🦴", 5..6, "Beuger und Strecker bewegen den Arm."),
        Upcoming(Subject.BIOLOGIE, "Sinne & Nerven", "👁️", 7..9, "Blinder Fleck, Reaktionszeit und Reflexe."),
        Upcoming(Subject.BIOLOGIE, "Abwehr-Burg (Immunsystem)", "🛡️", 8..10, "Fresszellen und Antikörper gegen Erreger."),
        Upcoming(Subject.BIOLOGIE, "Evolution", "🦋", 6..10, "Birkenspanner auf heller und dunkler Rinde."),
        Upcoming(Subject.BIOLOGIE, "DNA & Zellteilung", "🧬", 9..10, "Chromosomen sortieren, DNA-Reißverschluss öffnen."),
    )
    fun of(s: Subject, grade: Int = 0) = all.filter { it.subject == s && (grade == 0 || grade in it.grades) }
}
