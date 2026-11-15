package de.wissensgarten.core.data

import de.wissensgarten.core.data.db.*
import kotlinx.coroutines.flow.Flow

/**
 * Speicher für Profile, Beete, Karten, Laden und Protokoll. Android: Room-Datenbank (unverändertes Schema),
 * Web: [MemoryGardenStore], dauerhaft im Browser gespeichert. Die Lern-Logik ([GardenRepository]) ist für beide gleich.
 */
interface GardenStore {
    suspend fun <T> transaction(block: suspend () -> T): T

    fun profiles(): Flow<List<ProfileEntity>>
    suspend fun profile(id: String): ProfileEntity?
    suspend fun insertProfile(p: ProfileEntity)
    suspend fun updateProfile(p: ProfileEntity)
    /** Löscht auch Beete, Karten und Laden-Artikel des Profils. */
    suspend fun deleteProfile(id: String)

    fun bedsWithCards(profileId: String): Flow<List<BedWithCards>>
    fun allBedsWithCards(): Flow<List<BedWithCards>>
    suspend fun bedsWithCardsOnce(profileId: String): List<BedWithCards>
    suspend fun packBeds(): List<BedEntity>
    suspend fun maxBedSort(profileId: String): Int
    suspend fun insertBed(b: BedEntity)
    suspend fun updateBed(b: BedEntity)
    suspend fun bed(id: String): BedEntity?
    /** Löscht auch die Karten des Beets. */
    suspend fun deleteBed(id: String)

    suspend fun cardsOfBed(bedId: String): List<CardEntity>
    suspend fun card(id: String): CardEntity?
    suspend fun insertCards(c: List<CardEntity>)
    suspend fun updateCard(c: CardEntity)
    suspend fun updateCards(c: List<CardEntity>)
    suspend fun deleteCards(c: List<CardEntity>)
    suspend fun deleteCard(id: String)

    fun inventory(profileId: String): Flow<List<InventoryEntity>>
    suspend fun owned(profileId: String, itemId: String): InventoryEntity?
    suspend fun insertItem(i: InventoryEntity)
    suspend fun updateItem(i: InventoryEntity)
    suspend fun setBedSpecies(bedId: String, species: String)

    suspend fun insertLog(l: ReviewLogEntity)
    suspend fun logCount(profileId: String): Int
    suspend fun profileLogsSince(profileId: String, since: Long): List<ReviewLogEntity>
    fun logsSince(since: Long): Flow<List<ReviewLogEntity>>

    // Geräte-Sync
    suspend fun allProfiles(): List<ProfileEntity>
    suspend fun allBeds(): List<BedEntity>
    suspend fun allCards(): List<CardEntity>
    suspend fun allInventory(): List<InventoryEntity>
    suspend fun logsSinceOnce(since: Long): List<ReviewLogEntity>
    suspend fun upsertProfiles(p: List<ProfileEntity>)
    suspend fun upsertBeds(b: List<BedEntity>)
    suspend fun upsertCards(c: List<CardEntity>)
    suspend fun upsertItems(i: List<InventoryEntity>)
    suspend fun insertLogs(l: List<ReviewLogEntity>)
    suspend fun deleteCardsById(ids: List<String>)
    suspend fun deleteBedsById(ids: List<String>)
    suspend fun deleteProfilesById(ids: List<String>)

    /** „Alle Daten löschen“ */
    suspend fun clearAll()
}
