package de.wissensgarten.core.data.db

import de.wissensgarten.core.model.Memory
import de.wissensgarten.core.model.StudyCard
import kotlinx.serialization.Serializable

/*
 * Gemeinsame Datenklassen für Android und Web. Auf Android speichert Room sie in denselben Tabellen wie bisher
 * (siehe androidMain/RoomEntities.kt), im Web liegen sie als JSON im Browser.
 */

/** Ein Lernender. Mehrere Profile pro Gerät (Familie). */
@Serializable
data class ProfileEntity(
    val id: String,
    val name: String,
    val avatar: String,
    /** 1–13 Klasse, 14 Erwachsene, 0 ohne Angabe */
    val level: Int,
    val dewdrops: Int = 0,
    val seedDay: Long = 0,
    val seedsToday: Int = 0,
    val seedsPerDay: Int = 10,
    val createdAt: Long,
) {
    /** Grundschulkinder bewerten nur mit „Wusste ich / Wusste ich nicht“. */
    val simpleRating: Boolean get() = level in 1..4
}

@Serializable
data class BedEntity(
    val id: String,
    val profileId: String,
    val name: String,
    val emoji: String,
    val hue: Float,
    /** Herkunft aus einem Wissenspaket (null = eigenes Beet) */
    val packId: String? = null,
    val packVersion: Int = 0,
    val createdAt: Long,
    val sortIndex: Int = 0,
    /** Pflanzenart aus dem Gartenladen */
    val species: String = "klassik",
)

@Serializable
data class CardEntity(
    val id: String,
    val bedId: String,
    val profileId: String,
    /** Karten-ID im Paket – darüber laufen Paket-Updates, ohne Fortschritt zu verlieren */
    val sourceId: String? = null,
    val front: String,
    val back: String,
    /** falsche Antworten, getrennt durch \u001F */
    val choices: String? = null,
    val info: String? = null,
    val stability: Double = 0.0,
    val difficulty: Double = 5.0,
    val lastReview: Long = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val createdAt: Long,
    val sortIndex: Int = 0,
    /** Merkbild aus dem Paket */
    val img: String? = null,
) {
    val memory: Memory get() = Memory(stability, difficulty, lastReview, reps, lapses)
    val choiceList: List<String>? get() = choices?.split(SEP)?.filter { it.isNotBlank() }?.ifEmpty { null }

    fun withMemory(m: Memory) = copy(
        stability = m.stability, difficulty = m.difficulty, lastReview = m.lastReview, reps = m.reps, lapses = m.lapses,
    )

    fun toStudy() = StudyCard(id, bedId, front, back, choiceList, info, memory, img)

    companion object {
        const val SEP = "\u001F"
        fun joinChoices(list: List<String>?): String? = list?.takeIf { it.isNotEmpty() }?.joinToString(SEP)
    }
}

/** Gekaufte Artikel aus dem Gartenladen. */
@Serializable
data class InventoryEntity(
    val profileId: String,
    val itemId: String,
    /** Deko: im Garten aufgestellt? */
    val active: Boolean = true,
    val boughtAt: Long,
)

/** Protokoll jeder Wiederholung – Grundlage für Eltern-Übersicht und Statistiken. */
@Serializable
data class ReviewLogEntity(
    val id: Long = 0,
    val profileId: String,
    val cardId: String,
    val time: Long,
    val rating: Int,
    val stabilityAfter: Double,
)

data class BedWithCards(val bed: BedEntity, val cards: List<CardEntity>)
