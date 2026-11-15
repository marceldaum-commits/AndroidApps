package de.wissensgarten.core.data

import de.wissensgarten.core.data.db.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

/** Gesamter Lernstand als ein Wert – im Web als JSON im Browser gespeichert. */
@Serializable
data class StoreState(
    val profiles: List<ProfileEntity> = emptyList(),
    val beds: List<BedEntity> = emptyList(),
    val cards: List<CardEntity> = emptyList(),
    val inventory: List<InventoryEntity> = emptyList(),
    val logs: List<ReviewLogEntity> = emptyList(),
    val nextLogId: Long = 1,
)

/**
 * [GardenStore] im Speicher: verhält sich wie die Room-Datenbank (Sortierung, Löschen mit allem Dazugehörigen).
 * [onChange] wird nach jeder Änderung aufgerufen (Web: im Browser speichern).
 */
class MemoryGardenStore(initial: StoreState = StoreState(), private val onChange: (StoreState) -> Unit = {}) : GardenStore {
    private val state = MutableStateFlow(initial)
    val current: StoreState get() = state.value

    private fun update(f: (StoreState) -> StoreState) { state.value = f(state.value); onChange(state.value) }
    private fun bwc(s: StoreState, beds: List<BedEntity>): List<BedWithCards> {
        val byBed = s.cards.groupBy { it.bedId }
        return beds.map { BedWithCards(it, byBed[it.id].orEmpty()) }
    }
    private fun ownBeds(s: StoreState, pid: String) = s.beds.filter { it.profileId == pid }.sortedWith(compareBy({ it.sortIndex }, { it.createdAt }))

    override suspend fun <T> transaction(block: suspend () -> T): T = block()

    override fun profiles() = state.map { s -> s.profiles.sortedBy { it.createdAt } }.distinctUntilChanged()
    override suspend fun profile(id: String) = current.profiles.firstOrNull { it.id == id }
    override suspend fun insertProfile(p: ProfileEntity) = update { s ->
        require(s.profiles.none { it.id == p.id }) { "Profil gibt es schon" }; s.copy(profiles = s.profiles + p)
    }
    override suspend fun updateProfile(p: ProfileEntity) = update { s -> s.copy(profiles = s.profiles.map { if (it.id == p.id) p else it }) }
    override suspend fun deleteProfile(id: String) = deleteProfilesById(listOf(id))

    override fun bedsWithCards(profileId: String) = state.map { s -> bwc(s, ownBeds(s, profileId)) }.distinctUntilChanged()
    override fun allBedsWithCards() = state.map { s -> bwc(s, s.beds.sortedBy { it.createdAt }) }.distinctUntilChanged()
    override suspend fun bedsWithCardsOnce(profileId: String) = current.let { bwc(it, ownBeds(it, profileId)) }
    override suspend fun packBeds() = current.beds.filter { it.packId != null }
    override suspend fun maxBedSort(profileId: String) = current.beds.filter { it.profileId == profileId }.maxOfOrNull { it.sortIndex } ?: 0
    override suspend fun insertBed(b: BedEntity) = update { s -> s.copy(beds = s.beds + b) }
    override suspend fun updateBed(b: BedEntity) = update { s -> s.copy(beds = s.beds.map { if (it.id == b.id) b else it }) }
    override suspend fun bed(id: String) = current.beds.firstOrNull { it.id == id }
    override suspend fun deleteBed(id: String) = deleteBedsById(listOf(id))

    override suspend fun cardsOfBed(bedId: String) = current.cards.filter { it.bedId == bedId }
    override suspend fun card(id: String) = current.cards.firstOrNull { it.id == id }
    override suspend fun insertCards(c: List<CardEntity>) = update { s -> s.copy(cards = s.cards + c) }
    override suspend fun updateCard(c: CardEntity) = updateCards(listOf(c))
    override suspend fun updateCards(c: List<CardEntity>) {
        val m = c.associateBy { it.id }
        update { s -> s.copy(cards = s.cards.map { m[it.id] ?: it }) }
    }
    override suspend fun deleteCards(c: List<CardEntity>) = deleteCardsById(c.map { it.id })
    override suspend fun deleteCard(id: String) = deleteCardsById(listOf(id))

    override fun inventory(profileId: String) = state.map { s -> s.inventory.filter { it.profileId == profileId } }.distinctUntilChanged()
    override suspend fun owned(profileId: String, itemId: String) = current.inventory.firstOrNull { it.profileId == profileId && it.itemId == itemId }
    override suspend fun insertItem(i: InventoryEntity) = update { s -> s.copy(inventory = s.inventory + i) }
    override suspend fun updateItem(i: InventoryEntity) = update { s ->
        s.copy(inventory = s.inventory.map { if (it.profileId == i.profileId && it.itemId == i.itemId) i else it })
    }
    override suspend fun setBedSpecies(bedId: String, species: String) = update { s ->
        s.copy(beds = s.beds.map { if (it.id == bedId) it.copy(species = species) else it })
    }

    override suspend fun insertLog(l: ReviewLogEntity) = insertLogs(listOf(l))
    override suspend fun logCount(profileId: String) = current.logs.count { it.profileId == profileId }
    override suspend fun profileLogsSince(profileId: String, since: Long) = current.logs.filter { it.profileId == profileId && it.time >= since }
    override fun logsSince(since: Long) = state.map { s -> s.logs.filter { it.time >= since }.sortedBy { it.time } }.distinctUntilChanged()

    override suspend fun allProfiles() = current.profiles
    override suspend fun allBeds() = current.beds
    override suspend fun allCards() = current.cards
    override suspend fun allInventory() = current.inventory
    override suspend fun logsSinceOnce(since: Long) = current.logs.filter { it.time >= since }
    override suspend fun upsertProfiles(p: List<ProfileEntity>) = update { s -> s.copy(profiles = upsert(s.profiles, p) { it.id }) }
    override suspend fun upsertBeds(b: List<BedEntity>) = update { s -> s.copy(beds = upsert(s.beds, b) { it.id }) }
    override suspend fun upsertCards(c: List<CardEntity>) = update { s -> s.copy(cards = upsert(s.cards, c) { it.id }) }
    override suspend fun upsertItems(i: List<InventoryEntity>) = update { s -> s.copy(inventory = upsert(s.inventory, i) { it.profileId to it.itemId }) }
    override suspend fun insertLogs(l: List<ReviewLogEntity>) = update { s ->
        var id = s.nextLogId
        s.copy(logs = s.logs + l.map { it.copy(id = if (it.id == 0L) id++ else it.id) }, nextLogId = id)
    }
    override suspend fun deleteCardsById(ids: List<String>) { val set = ids.toSet(); update { s -> s.copy(cards = s.cards.filter { it.id !in set }) } }
    override suspend fun deleteBedsById(ids: List<String>) {
        val set = ids.toSet()
        update { s -> s.copy(beds = s.beds.filter { it.id !in set }, cards = s.cards.filter { it.bedId !in set }) }
    }
    override suspend fun deleteProfilesById(ids: List<String>) {
        val set = ids.toSet()
        update { s ->
            val beds = s.beds.filter { it.profileId in set }.map { it.id }.toSet()
            s.copy(profiles = s.profiles.filter { it.id !in set }, beds = s.beds.filter { it.id !in beds },
                cards = s.cards.filter { it.bedId !in beds }, inventory = s.inventory.filter { it.profileId !in set })
        }
    }
    override suspend fun clearAll() = update { StoreState() }

    private fun <T, K> upsert(list: List<T>, new: List<T>, key: (T) -> K): List<T> {
        val m = new.associateBy(key)
        val replaced = list.map { m[key(it)] ?: it }
        val have = list.map(key).toSet()
        return replaced + new.filter { key(it) !in have }
    }
}
