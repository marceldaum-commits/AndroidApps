package de.wissensgarten.core.data

import android.content.Context
import androidx.room.withTransaction
import de.wissensgarten.core.data.db.*
import de.wissensgarten.core.model.*
import java.io.File

/**
 * Übersetzt zwischen Datenbank/Einstellungen und dem Sync-Format. Der eigentliche Abgleich
 * (Zusammenführen) steckt in [Sync.merge], der Transport (Google Drive) in der App.
 */
class SyncStore(
    context: Context,
    private val db: GardenDatabase,
    private val settings: Settings,
    private val tombstones: Tombstones,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.dao()
    private val baseFile = File(context.filesDir, "sync_basis.gz")

    /** Stand der letzten erfolgreichen Synchronisierung (für die Drei-Wege-Zusammenführung). */
    fun base(): SyncSnapshot? = runCatching { Sync.decode(baseFile.readBytes()) }.getOrNull()
    fun saveBase(s: SyncSnapshot) { baseFile.writeBytes(Sync.encode(s)) }
    fun forgetBase() { baseFile.delete() }

    suspend fun snapshot(): SyncSnapshot {
        val now = clock()
        val profiles = dao.allProfiles()
        return SyncSnapshot(
            savedAt = now,
            profiles = profiles.map { SProfile(it.id, it.name, it.avatar, it.level, it.dewdrops, it.seedDay, it.seedsToday, it.seedsPerDay, it.createdAt) },
            beds = dao.allBeds().map { SBed(it.id, it.profileId, it.name, it.emoji, it.hue, it.packId, it.packVersion, it.createdAt, it.sortIndex, it.species) },
            cards = dao.allCards().map {
                SCard(it.id, it.bedId, it.profileId, it.sourceId, it.front, it.back, it.choices, it.info, it.stability, it.difficulty,
                    it.lastReview, it.reps, it.lapses, it.createdAt, it.sortIndex, it.img)
            },
            inventory = dao.allInventory().map { SItem(it.profileId, it.itemId, it.active, it.boughtAt) },
            logs = dao.logsSinceOnce(now - Sync.LOG_DAYS * 86_400_000L).map { SLog(it.profileId, it.cardId, it.time, it.rating, it.stabilityAfter) },
            extras = profiles.associate { p ->
                p.id to SExtras(
                    intros = settings.introsOf(p.id),
                    chestsOpened = settings.chestsOpened(p.id),
                    friends = settings.gardenFriends(p.id),
                    chapters = settings.chaptersRead(p.id),
                    hybrids = settings.hybrids(p.id),
                    goodNightDay = settings.goodNightDay(p.id),
                )
            },
            familyWeek = settings.familyClaimedWeek(),
            deleted = tombstones.all(),
            aliases = base()?.aliases.orEmpty(),
        )
    }

    /** Schreibt einen (zusammengeführten) Stand in Datenbank und Einstellungen. Nichts wird gelöscht außer Gelöschtem. */
    suspend fun apply(s: SyncSnapshot) {
        db.withTransaction {
            dao.upsertProfiles(s.profiles.map { ProfileEntity(it.id, it.name, it.avatar, it.level, it.dewdrops, it.seedDay, it.seedsToday, it.seedsPerDay, it.createdAt) })
            dao.upsertBeds(s.beds.map { BedEntity(it.id, it.profileId, it.name, it.emoji, it.hue, it.packId, it.packVersion, it.createdAt, it.sortIndex, it.species) })
            // Während des Abgleichs gegossen? Dann bleibt der neuere Stand aus der Datenbank.
            val current = dao.allCards().associate { it.id to it.lastReview }
            dao.upsertCards(s.cards.filter { (current[it.id] ?: Long.MIN_VALUE) <= it.lastReview }.map {
                CardEntity(it.id, it.bedId, it.profileId, it.sourceId, it.front, it.back, it.choices, it.info, it.stability, it.difficulty,
                    it.lastReview, it.reps, it.lapses, it.createdAt, it.sortIndex, it.img)
            })
            dao.upsertItems(s.inventory.map { InventoryEntity(it.profileId, it.itemId, it.active, it.boughtAt) })
            val since = s.logs.minOfOrNull { it.time } ?: Long.MAX_VALUE
            val have = dao.logsSinceOnce(since).map { Triple(it.profileId, it.cardId, it.time) }.toHashSet()
            dao.insertLogs(s.logs.filter { Triple(it.profileId, it.cardId, it.time) !in have }
                .map { ReviewLogEntity(profileId = it.profileId, cardId = it.cardId, time = it.time, rating = it.rating, stabilityAfter = it.stabilityAfter) })
            if (s.deleted.isNotEmpty()) {
                val ids = s.deleted.toList()
                ids.chunked(500).forEach { dao.deleteCardsById(it); dao.deleteBedsById(it); dao.deleteProfilesById(it) }
            }
        }
        tombstones.set(s.deleted)
        s.extras.forEach { (pid, e) ->
            settings.setIntros(pid, e.intros)
            if (e.chestsOpened > settings.chestsOpened(pid)) settings.setChestsOpened(pid, e.chestsOpened)
            if (e.friends != null) settings.setGardenFriends(pid, e.friends!!)
            settings.setChaptersRead(pid, e.chapters)
            settings.setHybrids(pid, e.hybrids)
            settings.setGoodNightDay(pid, e.goodNightDay)
        }
        if (s.familyWeek > settings.familyClaimedWeek()) settings.setFamilyClaimedWeek(s.familyWeek)
        // Aktives Profil weg (auf anderem Gerät gelöscht)? Dann das erste nehmen.
        if (s.profiles.none { it.id == settings.activeProfileId.value }) settings.setActiveProfile(s.profiles.firstOrNull()?.id)
        if (s.profiles.isNotEmpty()) settings.setOnboarded(true)
        settings.syncChanged()
    }
}
