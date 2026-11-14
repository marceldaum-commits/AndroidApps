package de.wissensgarten.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenDao {
    // Profile
    @Query("SELECT * FROM profiles ORDER BY createdAt")
    fun profiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun profile(id: String): ProfileEntity?

    @Insert
    suspend fun insertProfile(p: ProfileEntity)

    /** Achtung: nie REPLACE verwenden – das löscht per CASCADE alle Beete des Profils. */
    @Update
    suspend fun updateProfile(p: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    // Beete
    @Transaction
    @Query("SELECT * FROM beds WHERE profileId = :profileId ORDER BY sortIndex, createdAt")
    fun bedsWithCards(profileId: String): Flow<List<BedWithCards>>

    @Transaction
    @Query("SELECT * FROM beds ORDER BY createdAt")
    fun allBedsWithCards(): Flow<List<BedWithCards>>

    @Transaction
    @Query("SELECT * FROM beds WHERE profileId = :profileId ORDER BY sortIndex, createdAt")
    suspend fun bedsWithCardsOnce(profileId: String): List<BedWithCards>

    @Query("SELECT * FROM beds WHERE packId IS NOT NULL")
    suspend fun packBeds(): List<BedEntity>

    @Query("SELECT COALESCE(MAX(sortIndex), 0) FROM beds WHERE profileId = :profileId")
    suspend fun maxBedSort(profileId: String): Int

    @Insert suspend fun insertBed(b: BedEntity)
    @Update suspend fun updateBed(b: BedEntity)

    @Query("SELECT * FROM beds WHERE id = :id")
    suspend fun bed(id: String): BedEntity?

    @Query("DELETE FROM beds WHERE id = :id")
    suspend fun deleteBed(id: String)

    // Karten
    @Query("SELECT * FROM cards WHERE bedId = :bedId")
    suspend fun cardsOfBed(bedId: String): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun card(id: String): CardEntity?

    @Insert suspend fun insertCards(c: List<CardEntity>)
    @Update suspend fun updateCard(c: CardEntity)
    @Update suspend fun updateCards(c: List<CardEntity>)
    @Delete suspend fun deleteCards(c: List<CardEntity>)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCard(id: String)

    // Gartenladen
    @Query("SELECT * FROM inventory WHERE profileId = :profileId")
    fun inventory(profileId: String): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE profileId = :profileId AND itemId = :itemId")
    suspend fun owned(profileId: String, itemId: String): InventoryEntity?

    @Insert suspend fun insertItem(i: InventoryEntity)
    @Update suspend fun updateItem(i: InventoryEntity)

    @Query("UPDATE beds SET species = :species WHERE id = :bedId")
    suspend fun setBedSpecies(bedId: String, species: String)

    // Protokoll
    @Insert suspend fun insertLog(l: ReviewLogEntity)

    // ---- Postkarte und Urkunde
    @Query("SELECT COUNT(*) FROM review_log WHERE profileId = :profileId") suspend fun logCount(profileId: String): Int
    @Query("SELECT * FROM review_log WHERE profileId = :profileId AND time >= :since") suspend fun profileLogsSince(profileId: String, since: Long): List<ReviewLogEntity>

    // ---- Geräte-Sync: alles lesen, zusammengeführten Stand zurückschreiben
    @Query("SELECT * FROM profiles") suspend fun allProfiles(): List<ProfileEntity>
    @Query("SELECT * FROM beds") suspend fun allBeds(): List<BedEntity>
    @Query("SELECT * FROM cards") suspend fun allCards(): List<CardEntity>
    @Query("SELECT * FROM inventory") suspend fun allInventory(): List<InventoryEntity>
    @Query("SELECT * FROM review_log WHERE time >= :since") suspend fun logsSinceOnce(since: Long): List<ReviewLogEntity>
    @androidx.room.Upsert suspend fun upsertProfiles(p: List<ProfileEntity>)
    @androidx.room.Upsert suspend fun upsertBeds(b: List<BedEntity>)
    @androidx.room.Upsert suspend fun upsertCards(c: List<CardEntity>)
    @androidx.room.Upsert suspend fun upsertItems(i: List<InventoryEntity>)
    @Insert suspend fun insertLogs(l: List<ReviewLogEntity>)
    @Query("DELETE FROM cards WHERE id IN (:ids)") suspend fun deleteCardsById(ids: List<String>)
    @Query("DELETE FROM beds WHERE id IN (:ids)") suspend fun deleteBedsById(ids: List<String>)
    @Query("DELETE FROM profiles WHERE id IN (:ids)") suspend fun deleteProfilesById(ids: List<String>)

    @Query("SELECT * FROM review_log WHERE time >= :since ORDER BY time")
    fun logsSince(since: Long): Flow<List<ReviewLogEntity>>
}
