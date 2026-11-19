package de.wissensgarten.core.data

import de.wissensgarten.core.data.db.BedEntity
import de.wissensgarten.core.data.db.BedWithCards
import de.wissensgarten.core.data.db.CardEntity
import de.wissensgarten.core.data.db.InventoryEntity
import de.wissensgarten.core.data.db.ProfileEntity
import de.wissensgarten.core.data.db.ReviewLogEntity
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.Rating
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.ShopItems
import de.wissensgarten.core.model.StudyCard
import de.wissensgarten.core.model.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GardenRepository(
    private val dao: GardenStore,
    private val tombstones: Tombstones? = null,
    private val clock: () -> Long = de.wissensgarten.core.model.Platform::nowMs,
) {
    @OptIn(ExperimentalUuidApi::class)
    private fun newId() = Uuid.random().toString()

    val profiles: Flow<List<ProfileEntity>> = dao.profiles()
    // Sprach-Beete (an Quasselquest abgegeben, Marcel 19.11.2026) werden ausgeblendet, aber NICHT gelöscht:
    // Sicherung, Sync und Paket-Updates (dao direkt) behalten sie, damit ein Umzug zu Quasselquest sie übernehmen kann.
    private fun sichtbar(l: List<BedWithCards>) = l.filterNot { de.wissensgarten.core.model.Angebot.ausgeblendet(it.bed.packId) }
    fun beds(profileId: String): Flow<List<BedWithCards>> = dao.bedsWithCards(profileId).map(::sichtbar)
    fun allBeds(): Flow<List<BedWithCards>> = dao.allBedsWithCards().map(::sichtbar)
    /** Anzahl ausgeblendeter Sprach-Beete eines Profils (für den Hinweis auf Quasselquest) */
    fun ausgeblendeteBeete(profileId: String): Flow<Int> = dao.bedsWithCards(profileId).map { l -> l.count { de.wissensgarten.core.model.Angebot.ausgeblendet(it.bed.packId) } }
    fun logsSince(since: Long) = dao.logsSince(since)

    suspend fun profile(id: String) = dao.profile(id)

    suspend fun createProfile(name: String, avatar: String, level: Int): ProfileEntity {
        val p = ProfileEntity(
            id = newId(), name = name.trim(), avatar = avatar, level = level,
            seedsPerDay = if (level in 1..4) 5 else 10, createdAt = clock(),
        )
        dao.insertProfile(p)
        return p
    }

    suspend fun updateProfile(p: ProfileEntity) = dao.updateProfile(p)
    suspend fun deleteProfile(id: String) {
        val beds = dao.bedsWithCardsOnce(id)
        tombstones?.add(listOf(id) + beds.map { it.bed.id } + beds.flatMap { b -> b.cards.map { it.id } })
        dao.deleteProfile(id)
    }

    /** Pflanzt ein Wissenspaket als neues Beet. */
    suspend fun plantPack(profileId: String, pack: Pack): String = dao.transaction {
        val now = clock()
        val bed = BedEntity(
            id = newId(), profileId = profileId, name = pack.title, emoji = pack.emoji,
            hue = pack.subjectInfo.hue, packId = pack.id, packVersion = pack.version, createdAt = now,
            sortIndex = dao.maxBedSort(profileId) + 1,
        )
        dao.insertBed(bed)
        dao.insertCards(pack.cards.mapIndexed { i, c ->
            CardEntity(
                id = newId(), bedId = bed.id, profileId = profileId, sourceId = c.id,
                front = c.q, back = c.a, choices = CardEntity.joinChoices(c.choices), info = c.info,
                createdAt = now, sortIndex = i, img = c.img,
            )
        })
        bed.id
    }

    suspend fun addOwnBed(profileId: String, name: String, emoji: String): String {
        val hues = listOf(330f, 45f, 200f, 280f, 15f, 160f, 90f)
        val sort = dao.maxBedSort(profileId) + 1
        val bed = BedEntity(
            id = newId(), profileId = profileId, name = name.trim(), emoji = emoji,
            hue = hues[sort % hues.size], createdAt = clock(), sortIndex = sort,
        )
        dao.insertBed(bed)
        return bed.id
    }

    suspend fun deleteBed(bedId: String) {
        tombstones?.add(listOf(bedId) + dao.cardsOfBed(bedId).map { it.id })
        dao.deleteBed(bedId)
    }

    suspend fun addCard(bed: BedEntity, front: String, back: String, info: String? = null) {
        val existing = dao.cardsOfBed(bed.id).size
        dao.insertCards(listOf(
            CardEntity(
                id = newId(), bedId = bed.id, profileId = bed.profileId,
                front = front.trim(), back = back.trim(), info = info?.trim()?.ifEmpty { null },
                createdAt = clock(), sortIndex = existing,
            )
        ))
    }

    /** Mehrere Karten auf einmal (Klassen-Code, Wissens-Safari). */
    /** @return IDs der neuen Karten (z. B. um ein Foto als Merkbild zuzuordnen) */
    suspend fun addCards(bed: BedEntity, cards: List<de.wissensgarten.core.model.ClassCard>): List<String> {
        val existing = dao.cardsOfBed(bed.id).size
        val now = clock()
        val entities = cards.mapIndexed { i, c ->
            CardEntity(
                id = newId(), bedId = bed.id, profileId = bed.profileId,
                front = c.q.trim(), back = c.a.trim(), info = c.info?.trim()?.ifEmpty { null },
                createdAt = now, sortIndex = existing + i, img = c.img,
            )
        }
        dao.insertCards(entities)
        return entities.map { it.id }
    }

    suspend fun bedEntity(bedId: String) = dao.bed(bedId)
    suspend fun logCount(profileId: String) = dao.logCount(profileId)
    suspend fun profileLogsSince(profileId: String, since: Long) = dao.profileLogsSince(profileId, since)
    suspend fun bedsOnce(profileId: String) = sichtbar(dao.bedsWithCardsOnce(profileId))

    suspend fun deleteCard(cardId: String) {
        tombstones?.add(listOf(cardId))
        dao.deleteCard(cardId)
    }

    /** Eine Wiederholung speichern: Gedächtnis, Protokoll, Tautropfen, Samen-Zähler. */
    suspend fun review(profileId: String, card: StudyCard, rating: Rating, dew: Int = baseDew(rating)) = dao.transaction {
        val now = clock()
        val entity = dao.card(card.id) ?: return@transaction
        val wasSeed = entity.memory.isSeed
        val m = Scheduler.review(entity.memory, rating, now)
        dao.updateCard(entity.withMemory(m))
        dao.insertLog(ReviewLogEntity(profileId = profileId, cardId = card.id, time = now, rating = rating.ordinal, stabilityAfter = m.stability))
        val p = dao.profile(profileId) ?: return@transaction
        val today = Scheduler.localDay(now)
        val seedsToday = if (p.seedDay == today) p.seedsToday else 0
        dao.updateProfile(
            p.copy(
                dewdrops = p.dewdrops + dew,
                seedDay = today,
                seedsToday = seedsToday + if (wasSeed) 1 else 0,
            )
        )
    }

    suspend fun addDew(profileId: String, amount: Int) = dao.transaction {
        dao.profile(profileId)?.let { dao.updateProfile(it.copy(dewdrops = it.dewdrops + amount)) }
    }

    // --- Gartenladen ---

    fun inventory(profileId: String) = dao.inventory(profileId)

    enum class BuyResult { OK, TOO_EXPENSIVE, ALREADY_OWNED, UNKNOWN }

    /** Kauft einen Artikel mit Tautropfen. Preis kommt immer aus dem Katalog. */
    suspend fun buy(profileId: String, itemId: String): BuyResult = dao.transaction {
        val item = ShopItems.byId(itemId) ?: return@transaction BuyResult.UNKNOWN
        if (item.price == 0 || dao.owned(profileId, itemId) != null) return@transaction BuyResult.ALREADY_OWNED
        val p = dao.profile(profileId) ?: return@transaction BuyResult.UNKNOWN
        if (p.dewdrops < item.price) return@transaction BuyResult.TOO_EXPENSIVE
        dao.updateProfile(p.copy(dewdrops = p.dewdrops - item.price))
        dao.insertItem(InventoryEntity(profileId, itemId, active = true, boughtAt = clock()))
        BuyResult.OK
    }

    suspend fun setDecoActive(profileId: String, itemId: String, active: Boolean) {
        dao.owned(profileId, itemId)?.let { dao.updateItem(it.copy(active = active)) }
    }

    suspend fun setBedSpecies(bedId: String, species: String) = dao.setBedSpecies(bedId, species)

    /**
     * Paket-Updates einspielen: neue Karten kommen als Samen dazu, geänderte Texte werden übernommen,
     * entfernte Karten verschwinden. Der Lernfortschritt bleibt über die Karten-ID erhalten.
     * @return Anzahl aktualisierter Beete
     */
    suspend fun syncPacks(catalog: List<Pack>): Int {
        val byId = catalog.associateBy { it.id }
        var updated = 0
        for (bed in dao.packBeds()) {
            val pack = byId[bed.packId] ?: continue
            if (pack.version <= bed.packVersion) continue
            dao.transaction {
                val now = clock()
                val existing = dao.cardsOfBed(bed.id).associateBy { it.sourceId }
                val packIds = pack.cards.map { it.id }.toSet()
                val changed = mutableListOf<CardEntity>()
                val added = mutableListOf<CardEntity>()
                pack.cards.forEachIndexed { i, c ->
                    val e = existing[c.id]
                    if (e == null) {
                        added += CardEntity(
                            id = newId(), bedId = bed.id, profileId = bed.profileId, sourceId = c.id,
                            front = c.q, back = c.a, choices = CardEntity.joinChoices(c.choices), info = c.info,
                            createdAt = now, sortIndex = i, img = c.img,
                        )
                    } else {
                        val n = e.copy(front = c.q, back = c.a, choices = CardEntity.joinChoices(c.choices), info = c.info, sortIndex = i, img = c.img)
                        if (n != e) changed += n
                    }
                }
                val removed = existing.values.filter { it.sourceId != null && it.sourceId !in packIds }
                if (added.isNotEmpty()) dao.insertCards(added)
                if (changed.isNotEmpty()) dao.updateCards(changed)
                if (removed.isNotEmpty()) {
                    // aus dem Paket entfernte Karten: auch auf anderen Geräten weg (sonst holt der Sync sie zurück)
                    tombstones?.add(removed.map { it.id })
                    dao.deleteCards(removed)
                }
                dao.updateBed(bed.copy(packVersion = pack.version, name = pack.title, emoji = pack.emoji))
            }
            updated++
        }
        return updated
    }

    companion object {
        fun baseDew(r: Rating) = if (r == Rating.AGAIN) 1 else 3

        /** Serien-Bonus: ab 5 richtigen in Folge doppelt, ab 10 dreifach. */
        fun comboMultiplier(streak: Int) = when {
            streak >= 10 -> 3
            streak >= 5 -> 2
            else -> 1
        }

        val OWN_BED_EMOJIS = listOf("✏️", "🌻", "📐", "🧪", "🌍", "📜", "💬", "🎵", "🧠", "⚽", "🍎", "🚗")
        val OWN_HUE = Subject.EIGENE.hue
    }
}
