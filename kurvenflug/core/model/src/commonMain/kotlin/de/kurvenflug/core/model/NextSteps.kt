package de.kurvenflug.core.model

/**
 * „Wie geht's weiter?“ (Entscheidung Marcel 18.11.): KurvenFlug endet mit Klasse 10. Für die Oberstufe zeigt die App den Weg
 * zu den passenden Lern-Apps – nur Links auf freie Inhalte, kein Preis, kein Kaufhinweis.
 */
object NextSteps {
    data class App(val emoji: String, val name: String, val what: String, val topics: String, val url: String)

    val apps = listOf(
        App("📘", "Mathe-Trainer", "Ausführlich üben mit Lösungsweg – bis zum Abi",
            "Ableitung, Kurvendiskussion, Integral, e-Funktion, Wahrscheinlichkeit und Binomialverteilung",
            "https://trainer.mathenachhilfe-online.com/?src=kurvenflug-weiter"),
        App("🔦", "Raumlicht", "Mathe im Raum zum Anfassen – Oberstufe",
            "Vektoren, Geraden und Ebenen in 3D, Kurven abfahren, Galton-Brett",
            "https://raumlicht.mathenachhilfe-online.com/?src=kurvenflug-weiter"),
    )

    const val TITLE = "Wie geht's weiter?"
    const val INTRO = "KurvenFlug begleitet dich bis Klasse 10. Für die Oberstufe gibt es diese Lern-Apps:"
}
