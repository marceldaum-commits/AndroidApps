package de.raumlicht.core.model

/**
 * „Kommt bald“: Themen aus dem Lehrplan-Abgleich (LEHRPLAN-ABGLEICH.md), die als Nächstes gebaut werden – ohne Datum, ohne Kaufhinweis. Seit 2.18.32 nur Themen aus dem Profil „Geometrie im Raum“.
 * Ist ein Thema fertig, verschwindet es hier (die wöchentliche Aufgabe nimmt es heraus).
 */
object Soon {
    class Topic(val emoji: String, val title: String, val sub: String)
    val topics = listOf(
        Topic("💡", "Schattenwurf", "Licht, Körper, Schatten – auf Boden und Wand"),
        Topic("🧊", "Körper im Raum", "Pyramide, Prisma, Zelt: Kanten, Flächen, Volumen"),
        Topic("🎛️", "Geraden- und Ebenenscharen", "ein Parameter, viele Geraden: welche trifft den Punkt?"),
    )
}
