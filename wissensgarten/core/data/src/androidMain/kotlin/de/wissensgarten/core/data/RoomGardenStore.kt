package de.wissensgarten.core.data

import android.content.SharedPreferences
import androidx.room.withTransaction
import de.wissensgarten.core.data.db.*
import kotlinx.coroutines.flow.map

/** [GardenStore] auf der Room-Datenbank (Android). */
class RoomGardenStore(private val db: GardenDatabase) : GardenStore {
    private val dao = db.dao()
    override suspend fun <T> transaction(block: suspend () -> T): T = db.withTransaction { block() }

    override fun profiles() = dao.profiles().map { l -> l.map { it.toModel() } }
    override suspend fun profile(id: String) = dao.profile(id)?.toModel()
    override suspend fun insertProfile(p: ProfileEntity) = dao.insertProfile(p.toRow())
    override suspend fun updateProfile(p: ProfileEntity) = dao.updateProfile(p.toRow())
    override suspend fun deleteProfile(id: String) = dao.deleteProfile(id)

    override fun bedsWithCards(profileId: String) = dao.bedsWithCards(profileId).map { l -> l.map { it.toModel() } }
    override fun allBedsWithCards() = dao.allBedsWithCards().map { l -> l.map { it.toModel() } }
    override suspend fun bedsWithCardsOnce(profileId: String) = dao.bedsWithCardsOnce(profileId).map { it.toModel() }
    override suspend fun packBeds() = dao.packBeds().map { it.toModel() }
    override suspend fun maxBedSort(profileId: String) = dao.maxBedSort(profileId)
    override suspend fun insertBed(b: BedEntity) = dao.insertBed(b.toRow())
    override suspend fun updateBed(b: BedEntity) = dao.updateBed(b.toRow())
    override suspend fun bed(id: String) = dao.bed(id)?.toModel()
    override suspend fun deleteBed(id: String) = dao.deleteBed(id)

    override suspend fun cardsOfBed(bedId: String) = dao.cardsOfBed(bedId).map { it.toModel() }
    override suspend fun card(id: String) = dao.card(id)?.toModel()
    override suspend fun insertCards(c: List<CardEntity>) = dao.insertCards(c.map { it.toRow() })
    override suspend fun updateCard(c: CardEntity) = dao.updateCard(c.toRow())
    override suspend fun updateCards(c: List<CardEntity>) = dao.updateCards(c.map { it.toRow() })
    override suspend fun deleteCards(c: List<CardEntity>) = dao.deleteCards(c.map { it.toRow() })
    override suspend fun deleteCard(id: String) = dao.deleteCard(id)

    override fun inventory(profileId: String) = dao.inventory(profileId).map { l -> l.map { it.toModel() } }
    override suspend fun owned(profileId: String, itemId: String) = dao.owned(profileId, itemId)?.toModel()
    override suspend fun insertItem(i: InventoryEntity) = dao.insertItem(i.toRow())
    override suspend fun updateItem(i: InventoryEntity) = dao.updateItem(i.toRow())
    override suspend fun setBedSpecies(bedId: String, species: String) = dao.setBedSpecies(bedId, species)

    override suspend fun insertLog(l: ReviewLogEntity) = dao.insertLog(l.toRow())
    override suspend fun logCount(profileId: String) = dao.logCount(profileId)
    override suspend fun profileLogsSince(profileId: String, since: Long) = dao.profileLogsSince(profileId, since).map { it.toModel() }
    override fun logsSince(since: Long) = dao.logsSince(since).map { l -> l.map { it.toModel() } }

    override suspend fun allProfiles() = dao.allProfiles().map { it.toModel() }
    override suspend fun allBeds() = dao.allBeds().map { it.toModel() }
    override suspend fun allCards() = dao.allCards().map { it.toModel() }
    override suspend fun allInventory() = dao.allInventory().map { it.toModel() }
    override suspend fun logsSinceOnce(since: Long) = dao.logsSinceOnce(since).map { it.toModel() }
    override suspend fun upsertProfiles(p: List<ProfileEntity>) = dao.upsertProfiles(p.map { it.toRow() })
    override suspend fun upsertBeds(b: List<BedEntity>) = dao.upsertBeds(b.map { it.toRow() })
    override suspend fun upsertCards(c: List<CardEntity>) = dao.upsertCards(c.map { it.toRow() })
    override suspend fun upsertItems(i: List<InventoryEntity>) = dao.upsertItems(i.map { it.toRow() })
    override suspend fun insertLogs(l: List<ReviewLogEntity>) = dao.insertLogs(l.map { it.toRow() })
    override suspend fun deleteCardsById(ids: List<String>) = dao.deleteCardsById(ids)
    override suspend fun deleteBedsById(ids: List<String>) = dao.deleteBedsById(ids)
    override suspend fun deleteProfilesById(ids: List<String>) = dao.deleteProfilesById(ids)
    override suspend fun clearAll() = db.clearAllTables()
}

/** [KeyValueStore] auf Androids SharedPreferences – gleiche Schlüssel wie bisher, nichts geht verloren. */
class AndroidKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String, def: String?) = prefs.getString(key, def)
    override fun getBoolean(key: String, def: Boolean) = prefs.getBoolean(key, def)
    override fun getInt(key: String, def: Int) = prefs.getInt(key, def)
    override fun getLong(key: String, def: Long) = prefs.getLong(key, def)
    override fun getStringSet(key: String, def: Set<String>?): Set<String>? = prefs.getStringSet(key, def)?.toSet()
    override fun contains(key: String) = prefs.contains(key)
    override fun keys(): Set<String> = prefs.all.keys.toSet()
    override fun edit(): KeyValueStore.Editor = object : KeyValueStore.Editor {
        private val e = prefs.edit()
        override fun putString(key: String, value: String?) = also { e.putString(key, value) }
        override fun putBoolean(key: String, value: Boolean) = also { e.putBoolean(key, value) }
        override fun putInt(key: String, value: Int) = also { e.putInt(key, value) }
        override fun putLong(key: String, value: Long) = also { e.putLong(key, value) }
        override fun putStringSet(key: String, value: Set<String>?) = also { e.putStringSet(key, value) }
        override fun remove(key: String) = also { e.remove(key) }
        override fun clear() = also { e.clear() }
        override fun apply() = e.apply()
        override fun commit() = e.commit()
    }
}
