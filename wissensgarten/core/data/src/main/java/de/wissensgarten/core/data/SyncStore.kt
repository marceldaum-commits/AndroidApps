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
                    exams = settings.exams(p.id),
                )
            },
            familyWeek = settings.familyClaimedWeek(),
            deleted = tombstones.all(),
            aliases = base()?.aliases.orEmpty(),
        )
    }

    /**
     * Schreibt einen zusammengeführten Stand in Datenbank und Einstellungen.
     * [taken] ist der eigene Stand, der in die Zusammenführung einging. Was sich SEITDEM auf dem Gerät geändert hat
     * (während des Abgleichs gegossen, gekauft, gelöscht …), bleibt erhalten und wird eingerechnet.
     */
    suspend fun apply(s: SyncSnapshot, taken: SyncSnapshot) {
        val tookProfiles = taken.profiles.associateBy { it.id }
        val tookBeds = taken.beds.associateBy { it.id }
        val tookItems = taken.inventory.associateBy { it.profileId to it.itemId }
        db.withTransaction {
            // Inzwischen Gelöschtes zählt mit
            val tombs = tombstones.all() + s.deleted
            val curProfiles = dao.allProfiles().associateBy { it.id }
            dao.upsertProfiles(s.profiles.filter { it.id !in tombs }.map { m ->
                val cur = curProfiles[m.id]; val t = tookProfiles[m.id]
                if (cur == null || t == null) ProfileEntity(m.id, m.name, m.avatar, m.level, m.dewdrops, m.seedDay, m.seedsToday, m.seedsPerDay, m.createdAt)
                else {
                    // Tautropfen: Änderung seit dem Schnappschuss dazurechnen; Einstellungen: lokale Änderung gewinnt
                    val dew = (m.dewdrops + (cur.dewdrops - t.dewdrops)).coerceAtLeast(0)
                    val (seedDay, seedsToday) = when {
                        cur.seedDay > m.seedDay -> cur.seedDay to cur.seedsToday
                        cur.seedDay == m.seedDay -> m.seedDay to maxOf(m.seedsToday, cur.seedsToday)
                        else -> m.seedDay to m.seedsToday
                    }
                    ProfileEntity(m.id,
                        if (cur.name != t.name) cur.name else m.name,
                        if (cur.avatar != t.avatar) cur.avatar else m.avatar,
                        if (cur.level != t.level) cur.level else m.level,
                        dew, seedDay, seedsToday,
                        if (cur.seedsPerDay != t.seedsPerDay) cur.seedsPerDay else m.seedsPerDay,
                        minOf(cur.createdAt, m.createdAt))
                }
            })
            val curBeds = dao.allBeds().associateBy { it.id }
            dao.upsertBeds(s.beds.filter { it.id !in tombs }.filter { m ->
                // während des Abgleichs geändertes Beet (z. B. neue Pflanzenart) nicht zurückdrehen
                val cur = curBeds[m.id]; val t = tookBeds[m.id]
                cur == null || t == null || (cur.species == t.species && cur.name == t.name && cur.packVersion == t.packVersion)
            }.map { BedEntity(it.id, it.profileId, it.name, it.emoji, it.hue, it.packId, it.packVersion, it.createdAt, it.sortIndex, it.species) })
            // Während des Abgleichs gegossen? Dann bleibt der neuere Stand aus der Datenbank.
            val current = dao.allCards().associate { it.id to it.lastReview }
            dao.upsertCards(s.cards.filter { it.id !in tombs && (current[it.id] ?: Long.MIN_VALUE) <= it.lastReview }.map {
                CardEntity(it.id, it.bedId, it.profileId, it.sourceId, it.front, it.back, it.choices, it.info, it.stability, it.difficulty,
                    it.lastReview, it.reps, it.lapses, it.createdAt, it.sortIndex, it.img)
            })
            val curItems = dao.allInventory().associateBy { it.profileId to it.itemId }
            dao.upsertItems(s.inventory.filter { it.profileId !in tombs }.map { m ->
                val k = m.profileId to m.itemId
                val cur = curItems[k]; val t = tookItems[k]
                val active = if (cur != null && t != null && cur.active != t.active) cur.active else m.active
                InventoryEntity(m.profileId, m.itemId, active, m.boughtAt)
            })
            val since = s.logs.minOfOrNull { it.time } ?: Long.MAX_VALUE
            val have = dao.logsSinceOnce(since).map { Triple(it.profileId, it.cardId, it.time) }.toHashSet()
            dao.insertLogs(s.logs.filter { Triple(it.profileId, it.cardId, it.time) !in have && it.profileId !in tombs }
                .map { ReviewLogEntity(profileId = it.profileId, cardId = it.cardId, time = it.time, rating = it.rating, stabilityAfter = it.stabilityAfter) })
            if (tombs.isNotEmpty()) {
                tombs.toList().chunked(500).forEach { dao.deleteCardsById(it); dao.deleteBedsById(it); dao.deleteProfilesById(it) }
            }
        }
        // Gelöschtes nur ergänzen – nie Einträge verlieren, die während des Abgleichs dazukamen
        tombstones.add(s.deleted)
        val tombs = tombstones.all()
        s.extras.forEach { (pid, e) ->
            if (pid in tombs) return@forEach
            val t = taken.extras[pid]
            settings.setIntros(pid, e.intros)
            if (e.chestsOpened > settings.chestsOpened(pid)) settings.setChestsOpened(pid, e.chestsOpened)
            // Gartenfreunde: lokal inzwischen umgestellt? Dann bleibt das.
            val curFriends = settings.gardenFriends(pid)
            if (e.friends != null && (t == null || curFriends == t.friends)) settings.setGardenFriends(pid, e.friends!!)
            settings.setChaptersRead(pid, settings.chaptersRead(pid) + e.chapters)
            settings.setHybrids(pid, settings.hybrids(pid) + e.hybrids)
            settings.setGoodNightDay(pid, maxOf(settings.goodNightDay(pid), e.goodNightDay))
            settings.setExams(pid, (settings.exams(pid) + e.exams).distinctBy { it.id }.filter { it.id !in tombs })
        }
        if (s.familyWeek > settings.familyClaimedWeek()) settings.setFamilyClaimedWeek(s.familyWeek)
        // Aktives Profil weg (auf anderem Gerät gelöscht)? Dann das erste nehmen.
        val alive = dao.allProfiles()
        if (alive.none { it.id == settings.activeProfileId.value }) settings.setActiveProfile(alive.firstOrNull()?.id)
        if (alive.isNotEmpty()) settings.setOnboarded(true)
        settings.syncChanged()
    }

    // ---- Abgleich in zwei Schritten absichern: erst „vorgemerkt“, nach Erfolg zur Basis

    private val pendingFile = File(context.filesDir, "sync_vorgemerkt.gz")
    private val pendingTakenFile = File(context.filesDir, "sync_vorgemerkt_eigen.gz")

    /** Vor dem Hochladen merken, was hochgeladen wird – falls die App mittendrin beendet wird. */
    fun savePending(merged: SyncSnapshot, taken: SyncSnapshot) {
        pendingTakenFile.writeBytes(Sync.encode(taken))
        pendingFile.writeBytes(Sync.encode(merged))
    }
    fun pending(): Pair<SyncSnapshot, SyncSnapshot>? = runCatching {
        Sync.decode(pendingFile.readBytes()) to Sync.decode(pendingTakenFile.readBytes())
    }.getOrNull()
    fun clearPending() { pendingFile.delete(); pendingTakenFile.delete() }
}
