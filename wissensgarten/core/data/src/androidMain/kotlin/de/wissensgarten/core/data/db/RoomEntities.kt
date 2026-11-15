package de.wissensgarten.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/*
 * Room-Tabellen – Felder, Reihenfolge, Indizes und Fremdschlüssel exakt wie bis Datenbank-Version 3,
 * damit bestehende Gärten unverändert weiterlaufen. Die App arbeitet mit den gemeinsamen Klassen aus commonMain.
 */

@Entity(tableName = "profiles")
data class ProfileRow(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String,
    val level: Int,
    val dewdrops: Int = 0,
    val seedDay: Long = 0,
    val seedsToday: Int = 0,
    val seedsPerDay: Int = 10,
    val createdAt: Long,
)

@Entity(
    tableName = "beds",
    foreignKeys = [ForeignKey(ProfileRow::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId")],
)
data class BedRow(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val emoji: String,
    val hue: Float,
    val packId: String? = null,
    val packVersion: Int = 0,
    val createdAt: Long,
    val sortIndex: Int = 0,
    @ColumnInfo(defaultValue = "klassik") val species: String = "klassik",
)

@Entity(
    tableName = "cards",
    foreignKeys = [ForeignKey(BedRow::class, ["id"], ["bedId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bedId"), Index("profileId")],
)
data class CardRow(
    @PrimaryKey val id: String,
    val bedId: String,
    val profileId: String,
    val sourceId: String? = null,
    val front: String,
    val back: String,
    val choices: String? = null,
    val info: String? = null,
    val stability: Double = 0.0,
    val difficulty: Double = 5.0,
    val lastReview: Long = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val createdAt: Long,
    val sortIndex: Int = 0,
    val img: String? = null,
)

@Entity(
    tableName = "inventory",
    primaryKeys = ["profileId", "itemId"],
    foreignKeys = [ForeignKey(ProfileRow::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class InventoryRow(
    val profileId: String,
    val itemId: String,
    val active: Boolean = true,
    val boughtAt: Long,
)

@Entity(tableName = "review_log", indices = [Index("profileId", "time")])
data class ReviewLogRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,
    val cardId: String,
    val time: Long,
    val rating: Int,
    val stabilityAfter: Double,
)

data class BedWithCardsRow(
    @Embedded val bed: BedRow,
    @Relation(parentColumn = "id", entityColumn = "bedId") val cards: List<CardRow>,
)

// ---- Umwandlung Tabelle ↔ gemeinsame Klasse
fun ProfileRow.toModel() = ProfileEntity(id, name, avatar, level, dewdrops, seedDay, seedsToday, seedsPerDay, createdAt)
fun ProfileEntity.toRow() = ProfileRow(id, name, avatar, level, dewdrops, seedDay, seedsToday, seedsPerDay, createdAt)
fun BedRow.toModel() = BedEntity(id, profileId, name, emoji, hue, packId, packVersion, createdAt, sortIndex, species)
fun BedEntity.toRow() = BedRow(id, profileId, name, emoji, hue, packId, packVersion, createdAt, sortIndex, species)
fun CardRow.toModel() = CardEntity(id, bedId, profileId, sourceId, front, back, choices, info, stability, difficulty, lastReview, reps, lapses, createdAt, sortIndex, img)
fun CardEntity.toRow() = CardRow(id, bedId, profileId, sourceId, front, back, choices, info, stability, difficulty, lastReview, reps, lapses, createdAt, sortIndex, img)
fun InventoryRow.toModel() = InventoryEntity(profileId, itemId, active, boughtAt)
fun InventoryEntity.toRow() = InventoryRow(profileId, itemId, active, boughtAt)
fun ReviewLogRow.toModel() = ReviewLogEntity(id, profileId, cardId, time, rating, stabilityAfter)
fun ReviewLogEntity.toRow() = ReviewLogRow(id, profileId, cardId, time, rating, stabilityAfter)
fun BedWithCardsRow.toModel() = BedWithCards(bed.toModel(), cards.map { it.toModel() })
