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

    // Protokoll
    @Insert suspend fun insertLog(l: ReviewLogEntity)

    @Query("SELECT * FROM review_log WHERE time >= :since ORDER BY time")
    fun logsSince(since: Long): Flow<List<ReviewLogEntity>>
}
