package de.wissensgarten.app.data

import kotlinx.serialization.Serializable

/** Eine Lernkarte – im Garten ist sie eine Pflanze. */
@Serializable
data class Card(
    val id: String,
    val front: String,
    val back: String,
    /** Tage, nach denen die Erinnerung auf 90 % gesunken ist. 0 = noch nie gelernt (Samen). */
    val stability: Double = 0.0,
    /** Schwierigkeit 1 (leicht) … 10 (schwer). */
    val difficulty: Double = 5.0,
    /** Zeitpunkt der letzten Wiederholung (ms seit 1970), 0 = nie. */
    val lastReview: Long = 0L,
    val reps: Int = 0,
    val lapses: Int = 0,
) {
    val isSeed: Boolean get() = reps == 0
}

/** Ein Beet sammelt Karten zu einem Thema. */
@Serializable
data class Bed(
    val id: String,
    val name: String,
    val emoji: String,
    /** Farbton (0–360) für die Blüten dieses Beets. */
    val hue: Float,
    val cards: List<Card> = emptyList(),
)

@Serializable
data class GardenState(
    val beds: List<Bed> = emptyList(),
    /** Gesammelte Tautropfen – sanfte Belohnung, kein Verlust bei Pausen. */
    val dewdrops: Int = 0,
    /** Tag (Epoch-Tag) und Zahl der heute gesäten Samen, für das Tageslimit. */
    val seedDay: Long = 0L,
    val seedsToday: Int = 0,
)

enum class Grade(val label: String) {
    AGAIN("Vergessen"),
    HARD("Schwer"),
    GOOD("Gut"),
    EASY("Leicht"),
}

/** Wachstumsstufe einer Pflanze, abgeleitet aus der Stabilität. */
enum class Stage(val label: String) {
    SEED("Samen"),
    SPROUT("Keimling"),
    SHOOT("Spross"),
    BUSH("Busch"),
    BLOOM("Blüte"),
    TREE("Baum");

    companion object {
        fun of(card: Card): Stage = when {
            card.isSeed -> SEED
            card.stability < 2 -> SPROUT
            card.stability < 7 -> SHOOT
            card.stability < 21 -> BUSH
            card.stability < 60 -> BLOOM
            else -> TREE
        }
    }
}
