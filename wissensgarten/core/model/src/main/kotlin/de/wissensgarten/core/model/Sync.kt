package de.wissensgarten.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/*
 * Geräte-Sync über den versteckten App-Ordner im Google Drive der Familie.
 *
 * Jedes Gerät lädt den Stand aus dem Drive, führt ihn mit dem eigenen zusammen und lädt das Ergebnis wieder hoch.
 * Zusätzlich merkt sich jedes Gerät den Stand der letzten Synchronisierung („Basis“). Damit lässt sich erkennen,
 * WO sich etwas geändert hat – z. B. Tautropfen: Ausgaben und Gewinne beider Geräte werden verrechnet.
 *
 * Grundsatz: Lernfortschritt darf nie verloren gehen. Im Zweifel bleibt etwas lieber doppelt als weg.
 */

@Serializable
data class SyncSnapshot(
    val format: Int = 1,
    val savedAt: Long = 0,
    val profiles: List<SProfile> = emptyList(),
    val beds: List<SBed> = emptyList(),
    val cards: List<SCard> = emptyList(),
    val inventory: List<SItem> = emptyList(),
    val logs: List<SLog> = emptyList(),
    /** Fortschritt außerhalb der Datenbank, pro Profil (Kapitel, Truhen, Kreuzungen …) */
    val extras: Map<String, SExtras> = emptyMap(),
    val familyWeek: Long = -1,
    /** IDs gelöschter Profile, Beete und Karten – damit sie nicht vom anderen Gerät zurückkommen */
    val deleted: Set<String> = emptySet(),
    /** Einmal zugeordnete IDs (z. B. „Lena“ auf zwei Geräten getrennt angelegt) – bleiben verbunden, auch nach Umbenennen */
    val aliases: Map<String, String> = emptyMap(),
)

@Serializable
data class SProfile(
    val id: String, val name: String, val avatar: String, val level: Int, val dewdrops: Int,
    val seedDay: Long, val seedsToday: Int, val seedsPerDay: Int, val createdAt: Long,
)

@Serializable
data class SBed(
    val id: String, val profileId: String, val name: String, val emoji: String, val hue: Float,
    val packId: String?, val packVersion: Int, val createdAt: Long, val sortIndex: Int, val species: String,
)

@Serializable
data class SCard(
    val id: String, val bedId: String, val profileId: String, val sourceId: String?, val front: String, val back: String,
    val choices: String?, val info: String?, val stability: Double, val difficulty: Double, val lastReview: Long,
    val reps: Int, val lapses: Int, val createdAt: Long, val sortIndex: Int, val img: String?,
)

@Serializable
data class SItem(val profileId: String, val itemId: String, val active: Boolean, val boughtAt: Long)

@Serializable
data class SLog(val profileId: String, val cardId: String, val time: Long, val rating: Int, val stabilityAfter: Double)

@Serializable
data class SExtras(
    val intros: Set<String> = emptySet(),
    val chestsOpened: Int = 0,
    val friends: Set<String>? = null,
    val chapters: Set<Int> = emptySet(),
    val hybrids: Set<String> = emptySet(),
    val goodNightDay: Long = -1,
)

object Sync {
    /** Protokolle älter als das bleiben nur auf dem Gerät (die Eltern-Übersicht braucht 7 Tage). */
    const val LOG_DAYS = 120L
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun encode(s: SyncSnapshot): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(json.encodeToString(SyncSnapshot.serializer(), s).toByteArray()) }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): SyncSnapshot =
        json.decodeFromString(SyncSnapshot.serializer(), String(GZIPInputStream(bytes.inputStream()).use { it.readBytes() }))

    fun encodeText(s: SyncSnapshot) = json.encodeToString(SyncSnapshot.serializer(), s)
    fun decodeText(t: String) = json.decodeFromString(SyncSnapshot.serializer(), t)

    /**
     * Führt den Stand dieses Geräts ([local]) mit dem aus dem Drive ([remote]) zusammen.
     * [base] ist der zuletzt gemeinsam synchronisierte Stand (null beim allerersten Mal).
     * Lokale IDs haben Vorrang – das Ergebnis lässt sich direkt in die eigene Datenbank schreiben.
     */
    fun merge(local: SyncSnapshot, remote: SyncSnapshot?, base: SyncSnapshot?, now: Long): SyncSnapshot {
        if (remote == null) return local.copy(savedAt = now)
        val deleted = local.deleted + remote.deleted
        val knownAliases = local.aliases + remote.aliases
        val edges = HashMap<String, MutableSet<String>>()
        knownAliases.forEach { (a, b) -> edges.getOrPut(a) { HashSet() } += b; edges.getOrPut(b) { HashSet() } += a }
        /** Alle IDs, die schon einmal als „dasselbe“ erkannt wurden */
        fun linked(id: String): Set<String> {
            val seen = hashSetOf(id); val todo = ArrayDeque(listOf(id))
            while (todo.isNotEmpty()) edges[todo.removeFirst()]?.forEach { if (seen.add(it)) todo += it }
            return seen
        }

        // ---- Zuordnung: gleiche Dinge mit verschiedenen IDs (auf zwei Geräten getrennt angelegt)
        val profileMap = HashMap<String, String>()
        for (rp in remote.profiles) {
            profileMap[rp.id] = when {
                local.profiles.any { it.id == rp.id } -> rp.id
                else -> linked(rp.id).firstOrNull { a -> local.profiles.any { it.id == a } } ?: local.profiles.firstOrNull { lp ->
                    lp.name.trim().equals(rp.name.trim(), ignoreCase = true) && remote.profiles.none { it.id == lp.id } &&
                        profileMap.values.none { it == lp.id }
                }?.id ?: rp.id
            }
        }
        fun pid(id: String) = profileMap[id] ?: id
        // Über den Namen zugeordnet = auf zwei Geräten unabhängig voneinander angefangen
        val joinedByName = profileMap.filter { (r, l) -> r != l }.values.toSet()

        val bedMap = HashMap<String, String>()
        for (rb in remote.beds) {
            val p = pid(rb.profileId)
            bedMap[rb.id] = when {
                local.beds.any { it.id == rb.id } -> rb.id
                else -> linked(rb.id).firstOrNull { a -> local.beds.any { it.id == a } } ?: local.beds.firstOrNull { lb ->
                    lb.profileId == p && remote.beds.none { it.id == lb.id } && bedMap.values.none { it == lb.id } &&
                        (if (rb.packId != null) lb.packId == rb.packId else lb.packId == null && lb.name.trim().equals(rb.name.trim(), true))
                }?.id ?: rb.id
            }
        }
        fun bid(id: String) = bedMap[id] ?: id

        val localCardsByKey = local.cards.associateBy { cardKey(it.bedId, it) }
        val localCardIds = local.cards.map { it.id }.toHashSet()
        val cardMap = HashMap<String, String>()
        for (rc in remote.cards) {
            cardMap[rc.id] = when {
                rc.id in localCardIds -> rc.id
                else -> linked(rc.id).firstOrNull { it in localCardIds } ?: localCardsByKey[cardKey(bid(rc.bedId), rc)]?.takeIf { lc -> remote.cards.none { it.id == lc.id } && cardMap.values.none { it == lc.id } }?.id ?: rc.id
            }
        }
        fun cid(id: String) = cardMap[id] ?: id

        // Remote-Stand auf lokale IDs umschreiben
        val r = remote.copy(
            profiles = remote.profiles.map { it.copy(id = pid(it.id)) },
            beds = remote.beds.map { it.copy(id = bid(it.id), profileId = pid(it.profileId)) },
            cards = remote.cards.map { it.copy(id = cid(it.id), bedId = bid(it.bedId), profileId = pid(it.profileId)) },
            inventory = remote.inventory.map { it.copy(profileId = pid(it.profileId)) },
            logs = remote.logs.map { it.copy(profileId = pid(it.profileId), cardId = cid(it.cardId)) },
            extras = remote.extras.mapKeys { pid(it.key) },
        )

        // ---- Profile: Einstellungen „wer zuletzt geändert hat“, Tautropfen werden verrechnet
        val baseProfiles = base?.profiles?.associateBy { it.id }.orEmpty()
        val profiles = unionBy(local.profiles, r.profiles, { it.id }) { l, rp ->
            val b = baseProfiles[l.id]
            fun <T> pick(get: (SProfile) -> T): T = if (b != null && get(l) == get(b)) get(rp) else get(l)
            val dew = when {
                b != null -> (l.dewdrops + rp.dewdrops - b.dewdrops).coerceAtLeast(0)   // Änderungen beider Seiten verrechnen
                l.id in joinedByName -> l.dewdrops + rp.dewdrops                         // zwei getrennte Anfänge: zusammenzählen
                else -> maxOf(l.dewdrops, rp.dewdrops)                                   // gleicher Stand ohne Basis: nichts doppelt zählen
            }
            val (seedDay, seedsToday) = when {
                l.seedDay > rp.seedDay -> l.seedDay to l.seedsToday
                rp.seedDay > l.seedDay -> rp.seedDay to rp.seedsToday
                else -> l.seedDay to maxOf(l.seedsToday, rp.seedsToday)
            }
            l.copy(
                name = pick { it.name }, avatar = pick { it.avatar }, level = pick { it.level }, seedsPerDay = pick { it.seedsPerDay },
                dewdrops = dew, seedDay = seedDay, seedsToday = seedsToday, createdAt = minOf(l.createdAt, rp.createdAt),
            )
        }.filter { it.id !in deleted }
        val profileIds = profiles.map { it.id }.toSet()

        // ---- Beete: geänderte Felder vom Gerät, das sie geändert hat
        val baseBeds = base?.beds?.associateBy { it.id }.orEmpty()
        val beds = unionBy(local.beds, r.beds, { it.id }) { l, rb ->
            val b = baseBeds[l.id]
            if (b != null && l == b) rb.copy(id = l.id, profileId = l.profileId) else l
        }.filter { it.id !in deleted && it.profileId in profileIds }
        val bedIds = beds.map { it.id }.toSet()

        // ---- Karten: der neueste Lernstand gewinnt
        val cards = unionBy(local.cards, r.cards, { it.id }) { l, rc ->
            if (rc.lastReview > l.lastReview || (rc.lastReview == l.lastReview && rc.reps > l.reps))
                l.copy(stability = rc.stability, difficulty = rc.difficulty, lastReview = rc.lastReview, reps = rc.reps, lapses = rc.lapses)
            else l
        }.filter { it.id !in deleted && it.bedId in bedIds }

        // ---- Gekauftes bleibt gekauft; „aufgestellt“ vom Gerät, das es geändert hat
        val baseItems = base?.inventory?.associateBy { it.profileId to it.itemId }.orEmpty()
        val inventory = unionBy(local.inventory, r.inventory, { it.profileId to it.itemId }) { l, ri ->
            val b = baseItems[l.profileId to l.itemId]
            l.copy(active = if (b != null && l.active == b.active) ri.active else l.active, boughtAt = minOf(l.boughtAt, ri.boughtAt))
        }.filter { it.profileId in profileIds }

        // ---- Protokoll: alles von beiden, doppelte nur einmal
        val since = now - LOG_DAYS * DAY
        val logs = (local.logs + r.logs).filter { it.time >= since && it.profileId in profileIds }
            .distinctBy { Triple(it.profileId, it.cardId, it.time) }.sortedBy { it.time }

        // ---- Extras: Sammlungen vereinigen, Zähler maximal
        val baseExtras = base?.extras.orEmpty()
        val extras = (local.extras.keys + r.extras.keys).filter { it in profileIds }.associateWith { id ->
            val l = local.extras[id] ?: SExtras()
            val re = r.extras[id] ?: SExtras()
            val b = baseExtras[id]
            SExtras(
                intros = l.intros + re.intros,
                chestsOpened = maxOf(l.chestsOpened, re.chestsOpened),
                friends = if (b != null && l.friends == b.friends) re.friends else l.friends ?: re.friends,
                chapters = l.chapters + re.chapters,
                hybrids = l.hybrids + re.hybrids,
                goodNightDay = maxOf(l.goodNightDay, re.goodNightDay),
            )
        }

        return SyncSnapshot(
            savedAt = now, profiles = profiles, beds = beds, cards = cards, inventory = inventory, logs = logs,
            extras = extras, familyWeek = maxOf(local.familyWeek, remote.familyWeek), deleted = deleted,
            aliases = knownAliases + (profileMap + bedMap + cardMap).filter { (r, l) -> r != l },
        )
    }

    private const val DAY = 86_400_000L

    /** Paketkarten über ihre Paket-ID, eigene Karten über Frage + Antwort. */
    private fun cardKey(bedId: String, c: SCard) = bedId + "|" + (c.sourceId ?: "${c.front.trim()}→${c.back.trim()}")

    /** Vereinigung nach Schlüssel; kommt etwas auf beiden Seiten vor, entscheidet [both]. Reihenfolge: erst lokal. */
    private fun <T, K> unionBy(local: List<T>, remote: List<T>, key: (T) -> K, both: (T, T) -> T): List<T> {
        val r = remote.associateBy(key)
        val lKeys = HashSet<K>()
        val out = ArrayList<T>(local.size + remote.size)
        for (l in local) { val k = key(l); lKeys += k; out += r[k]?.let { both(l, it) } ?: l }
        for (x in remote) if (key(x) !in lKeys) out += x
        return out
    }
}
