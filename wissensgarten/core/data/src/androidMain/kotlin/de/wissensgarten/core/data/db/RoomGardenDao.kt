package de.wissensgarten.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomGardenDao {
    // Profile
    @Query("SELECT * FROM profiles ORDER BY createdAt")
    fun profiles(): Flow<List<ProfileRow>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun profile(id: String): ProfileRow?

    @Insert
    suspend fun insertProfile(p: ProfileRow)

    /** Achtung: nie REPLACE verwenden – das löscht per CASCADE alle Beete des Profils. */
    @Update
    suspend fun updateProfile(p: ProfileRow)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    // Beete
    @Transaction
    @Query("SELECT * FROM beds WHERE profileId = :profileId ORDER BY sortIndex, createdAt")
    fun bedsWithCards(profileId: String): Flow<List<BedWithCardsRow>>

    @Transaction
    @Query("SELECT * FROM beds ORDER BY createdAt")
    fun allBedsWithCards(): Flow<List<BedWithCardsRow>>

    @Transaction
    @Query("SELECT * FROM beds WHERE profileId = :profileId ORDER BY sortIndex, createdAt")
    suspend fun bedsWithCardsOnce(profileId: String): List<BedWithCardsRow>

    @Query("SELECT * FROM beds WHERE packId IS NOT NULL")
    suspend fun packBeds(): List<BedRow>

    @Query("SELECT COALESCE(MAX(sortIndex), 0) FROM beds WHERE profileId = :profileId")
    suspend fun maxBedSort(profileId: String): Int

    @Insert suspend fun insertBed(b: BedRow)
    @Update suspend fun updateBed(b: BedRow)

    @Query("SELECT * FROM beds WHERE id = :id")
    suspend fun bed(id: String): BedRow?

    @Query("DELETE FROM beds WHERE id = :id")
    suspend fun deleteBed(id: String)

    // Karten
    @Query("SELECT * FROM cards WHERE bedId = :bedId")
    suspend fun cardsOfBed(bedId: String): List<CardRow>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun card(id: String): CardRow?

    @Insert suspend fun insertCards(c: List<CardRow>)
    @Update suspend fun updateCard(c: CardRow)
    @Update suspend fun updateCards(c: List<CardRow>)
    @Delete suspend fun deleteCards(c: List<CardRow>)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCard(id: String)

    // Gartenladen
    @Query("SELECT * FROM inventory WHERE profileId = :profileId")
    fun inventory(profileId: String): Flow<List<InventoryRow>>

    @Query("SELECT * FROM inventory WHERE profileId = :profileId AND itemId = :itemId")
    suspend fun owned(profileId: String, itemId: String): InventoryRow?

    @Insert suspend fun insertItem(i: InventoryRow)
    @Update suspend fun updateItem(i: InventoryRow)

    @Query("UPDATE beds SET species = :species WHERE id = :bedId")
    suspend fun setBedSpecies(bedId: String, species: String)

    // Protokoll
    @Insert suspend fun insertLog(l: ReviewLogRow)

    // ---- Postkarte und Urkunde
    @Query("SELECT COUNT(*) FROM review_log WHERE profileId = :profileId") suspend fun logCount(profileId: String): Int
    @Query("SELECT * FROM review_log WHERE profileId = :profileId AND time >= :since") suspend fun profileLogsSince(profileId: String, since: Long): List<ReviewLogRow>

    // ---- Geräte-Sync: alles lesen, zusammengeführten Stand zurückschreiben
    @Query("SELECT * FROM profiles") suspend fun allProfiles(): List<ProfileRow>
    @Query("SELECT * FROM beds") suspend fun allBeds(): List<BedRow>
    @Query("SELECT * FROM cards") suspend fun allCards(): List<CardRow>
    @Query("SELECT * FROM inventory") suspend fun allInventory(): List<InventoryRow>
    @Query("SELECT * FROM review_log WHERE time >= :since") suspend fun logsSinceOnce(since: Long): List<ReviewLogRow>
    @androidx.room.Upsert suspend fun upsertProfiles(p: List<ProfileRow>)
    @androidx.room.Upsert suspend fun upsertBeds(b: List<BedRow>)
    @androidx.room.Upsert suspend fun upsertCards(c: List<CardRow>)
    @androidx.room.Upsert suspend fun upsertItems(i: List<InventoryRow>)
    @Insert suspend fun insertLogs(l: List<ReviewLogRow>)
    @Query("DELETE FROM cards WHERE id IN (:ids)") suspend fun deleteCardsById(ids: List<String>)
    @Query("DELETE FROM beds WHERE id IN (:ids)") suspend fun deleteBedsById(ids: List<String>)
    @Query("DELETE FROM profiles WHERE id IN (:ids)") suspend fun deleteProfilesById(ids: List<String>)

    @Query("SELECT * FROM review_log WHERE time >= :since ORDER BY time")
    fun logsSince(since: Long): Flow<List<ReviewLogRow>>

    @Query("DELETE FROM review_log") suspend fun deleteAllLogs()
}
