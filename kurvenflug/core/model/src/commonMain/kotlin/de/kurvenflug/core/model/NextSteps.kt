package de.kurvenflug.core.model

/**
 * „Wie geht's weiter?“ (Entscheidung Marcel 18.11.): KurvenFlug endet mit Klasse 10. Für die Oberstufe zeigt die App den Weg
 * zu den passenden Lern-Apps – nur Links auf freie Inhalte, kein Preis, kein Kaufhinweis.
 * Recht V10b (19.11.): Mathe-Trainer hat selbst ein Tor vor Preisen → Startseite ok; Raumlicht nur auf einen preisfreien Einstieg.
 */
object NextSteps {
    data class App(val emoji: String, val name: String, val what: String, val topics: String, val url: String)

    val apps = listOf(
        App("📘", "Mathe-Trainer", "Ausführlich üben mit Lösungsweg – bis zum Abi",
            "Ableitung, Kurvendiskussion, Integral, e-Funktion, Wahrscheinlichkeit und Binomialverteilung",
            "https://trainer.mathenachhilfe-online.com/?src=kurvenflug-weiter"),
        App("🔦", "Raumlicht", "Mathe im Raum zum Anfassen – Oberstufe",
            "Vektoren, Geraden und Ebenen in 3D, Kurven abfahren, Galton-Brett",
            "https://raumlicht.mathenachhilfe-online.com/?ziel=level/3-1&src=kurvenflug-weiter"   // preisfreier Einstieg (Recht V10b): auch für Kl. < 9 ohne Tor),
    )

    const val TITLE = "Wie geht's weiter?"
    const val INTRO = "KurvenFlug begleitet dich bis Klasse 10. Für die Oberstufe gibt es diese Lern-Apps:"
}
