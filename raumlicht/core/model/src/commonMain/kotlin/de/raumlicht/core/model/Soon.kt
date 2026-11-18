package de.raumlicht.core.model

/**
 * „Kommt bald“: Themen aus dem Lehrplan-Abgleich (LEHRPLAN-ABGLEICH.md), die als Nächstes gebaut werden – ohne Datum, ohne Kaufhinweis.
 * Ist ein Thema fertig, verschwindet es hier (die wöchentliche Aufgabe nimmt es heraus).
 */
object Soon {
    class Topic(val emoji: String, val title: String, val sub: String)
    val topics = listOf(
        Topic("🧩", "Lineare Gleichungssysteme", "Gauß-Verfahren: drei Gleichungen, drei Unbekannte"),
        Topic("🔗", "Unabhängigkeit", "Hängen zwei Ereignisse zusammen – oder nicht?"),
        Topic("🎰", "Zufallsgrößen", "Verteilung, Erwartungswert und Varianz"),
        Topic("🔔", "Normalverteilung", "Die Glockenkurve und ihre Flächen"),
        Topic("🌊", "Sinus und Kosinus", "ableiten, verschieben, untersuchen"),
        Topic("🎛️", "Funktionenscharen", "Eine Formel, viele Kurven: der Parameter"),
    )
}
