package de.wissensgarten.app.ui.share

import de.wissensgarten.app.widget.Flower

/** Zahlen und Beispiele für Postkarte und Urkunde eines Profils. */
data class KeepsakeData(
    val name: String,
    val avatar: String,
    val grown: Int,
    val trees: Int,
    val reviewsTotal: Int,
    val reviewsWeek: Int,
    val newThisWeek: Int,
    /** Beispiele, was diese Woche gelernt wurde: (Emoji, Antwort) */
    val learned: List<Pair<String, String>>,
    val flowers: List<Flower>,
    val beds: List<String>,
)
