package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class SyncTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    private fun profile(id: String, name: String = "Lena", dew: Int = 100) = SProfile(id, name, "🦊", 7, dew, 0, 0, 10, 1)
    private fun bed(id: String, p: String, pack: String? = "s1-englisch", name: String = "Englisch") =
        SBed(id, p, name, "💬", 340f, pack, 2, 1, 0, "klassik")
    private fun card(id: String, bed: String, p: String, src: String?, last: Long = 0, reps: Int = 0, front: String = "dog", back: String = "Hund") =
        SCard(id, bed, p, src, front, back, null, null, if (reps > 0) 3.0 else 0.0, 5.0, last, reps, 0, 1, 0, null)

    private val phone = SyncSnapshot(
        profiles = listOf(profile("p1")),
        beds = listOf(bed("b1", "p1")),
        cards = listOf(card("c1", "b1", "p1", "001", last = now - 5 * day, reps = 2), card("c2", "b1", "p1", "002")),
    )

    @Test fun firstSyncUploadsLocalAsIs() {
        val m = Sync.merge(phone, null, null, now)
        assertEquals(phone.cards, m.cards)
        assertEquals(now, m.savedAt)
    }

    @Test fun newestLearningStateWins() {
        val tablet = phone.copy(cards = listOf(card("c1", "b1", "p1", "001", last = now - 1 * day, reps = 3), card("c2", "b1", "p1", "002")))
        val phoneLater = phone.copy(cards = listOf(card("c1", "b1", "p1", "001", last = now - 5 * day, reps = 2), card("c2", "b1", "p1", "002", last = now, reps = 1)))
        val m = Sync.merge(phoneLater, tablet, phone, now)
        assertEquals(3, m.cards.first { it.id == "c1" }.reps) // auf dem Tablet gelernt
        assertEquals(1, m.cards.first { it.id == "c2" }.reps) // auf dem Handy gelernt
    }

    @Test fun dewIsAddedUpFromBothDevices() {
        val base = phone
        val local = phone.copy(profiles = listOf(profile("p1", dew = 130)))   // +30 auf dem Handy
        val remote = phone.copy(profiles = listOf(profile("p1", dew = 80)))   // −20 (Deko gekauft) auf dem Tablet
        assertEquals(110, Sync.merge(local, remote, base, now).profiles.single().dewdrops)
    }

    @Test fun separatelyCreatedProfileIsMatchedByNameNotDuplicated() {
        // Tablet: Lena neu eingerichtet, gleiches Paket gepflanzt, eine Karte dort gelernt
        val tablet = SyncSnapshot(
            profiles = listOf(profile("t-p", name = "lena ")),
            beds = listOf(bed("t-b", "t-p")),
            cards = listOf(card("t-c1", "t-b", "t-p", "001", last = now, reps = 5), card("t-c3", "t-b", "t-p", "003")),
            inventory = listOf(SItem("t-p", "tulpe", true, 5)),
            logs = listOf(SLog("t-p", "t-c1", now - 1000, 3, 9.0)),
            extras = mapOf("t-p" to SExtras(chapters = setOf(1, 2))),
        )
        val m = Sync.merge(phone, tablet, null, now)
        assertEquals(listOf("p1"), m.profiles.map { it.id })
        assertEquals(listOf("b1"), m.beds.map { it.id })
        assertEquals(setOf("c1", "c2", "t-c3"), m.cards.map { it.id }.toSet())
        assertEquals(5, m.cards.first { it.id == "c1" }.reps)
        assertEquals("b1", m.cards.first { it.id == "t-c3" }.bedId)
        assertEquals(SItem("p1", "tulpe", true, 5), m.inventory.single())
        assertEquals("c1", m.logs.single().cardId)
        assertEquals(setOf(1, 2), m.extras["p1"]!!.chapters)
    }

    @Test fun differentPeopleStaySeparate() {
        val tablet = SyncSnapshot(profiles = listOf(profile("t-p", name = "Ben")))
        assertEquals(setOf("p1", "t-p"), Sync.merge(phone, tablet, null, now).profiles.map { it.id }.toSet())
    }

    @Test fun deletedBedDoesNotComeBack() {
        val local = phone.copy(beds = emptyList(), cards = emptyList(), deleted = setOf("b1", "c1", "c2"))
        val m = Sync.merge(local, phone, phone, now)
        assertTrue(m.beds.isEmpty()); assertTrue(m.cards.isEmpty())
        assertTrue("b1" in m.deleted)
    }

    @Test fun bedChangedOnOtherDeviceIsTaken() {
        val remote = phone.copy(beds = listOf(bed("b1", "p1").copy(species = "rose")))
        assertEquals("rose", Sync.merge(phone, remote, phone, now).beds.single().species)
        // lokal geändert → lokal bleibt
        val local = phone.copy(beds = listOf(bed("b1", "p1").copy(species = "kaktus")))
        assertEquals("kaktus", Sync.merge(local, remote, phone, now).beds.single().species)
    }

    @Test fun logsAreUnitedWithoutDuplicatesAndOldOnesDropped() {
        val a = SLog("p1", "c1", now - 1000, 3, 1.0)
        val old = SLog("p1", "c1", now - 200 * day, 3, 1.0)
        val local = phone.copy(logs = listOf(a, old))
        val remote = phone.copy(logs = listOf(a, SLog("p1", "c2", now - 500, 1, 0.5)))
        assertEquals(2, Sync.merge(local, remote, null, now).logs.size)
    }

    @Test fun ownCardsMatchByText() {
        val local = phone.copy(beds = listOf(bed("o1", "p1", pack = null, name = "Vokabeln")),
            cards = listOf(card("x1", "o1", "p1", null, front = "Haus", back = "house")))
        val remote = SyncSnapshot(profiles = listOf(profile("t")), beds = listOf(bed("o9", "t", pack = null, name = "vokabeln")),
            cards = listOf(card("y1", "o9", "t", null, front = "Haus", back = "house", last = now, reps = 2), card("y2", "o9", "t", null, front = "Baum", back = "tree")))
        val m = Sync.merge(local, remote, null, now)
        assertEquals(setOf("x1", "y2"), m.cards.map { it.id }.toSet())
        assertEquals(2, m.cards.first { it.id == "x1" }.reps)
    }

    @Test fun roundTripCompressed() {
        val m = Sync.merge(phone, null, null, now)
        assertEquals(m, Sync.decode(Sync.encode(m)))
    }

    @Test fun mergeIsStableWhenRepeated() {
        val tablet = phone.copy(cards = listOf(card("c1", "b1", "p1", "001", last = now, reps = 4), card("c2", "b1", "p1", "002")))
        val m1 = Sync.merge(phone, tablet, phone, now)
        val m2 = Sync.merge(m1, m1, m1, now)
        assertEquals(m1, m2)
    }

    @Test fun separateStartsAddUpDew() {
        val tablet = SyncSnapshot(profiles = listOf(profile("t-p", dew = 40)))
        assertEquals(140, Sync.merge(phone, tablet, null, now).profiles.single().dewdrops)
        // gleiche ID ohne Basis (z. B. Basis verloren): nicht doppelt zählen
        assertEquals(100, Sync.merge(phone, phone, null, now).profiles.single().dewdrops)
    }

    @Test fun matchedProfileStaysMatchedAfterRename() {
        val tablet = SyncSnapshot(profiles = listOf(profile("t-p", name = "Lena")))
        val first = Sync.merge(phone, tablet, null, now)
        assertEquals("p1", first.aliases["t-p"])
        // Später auf dem Tablet umbenannt – trotzdem kein zweites Profil
        val renamed = SyncSnapshot(profiles = listOf(profile("t-p", name = "Lena M.")), aliases = first.aliases)
        val m = Sync.merge(first, renamed, first, now)
        assertEquals(listOf("p1"), m.profiles.map { it.id })
        assertEquals("Lena M.", m.profiles.single().name)
    }

    @Test fun reconnectWithoutBaseDoesNotDoubleDew() {
        // A und B haben „Lena“ getrennt angelegt und schon einmal abgeglichen (Alias t-p ↔ p1), beide 200 💧
        val first = Sync.merge(phone.copy(profiles = listOf(profile("p1", dew = 100))),
            SyncSnapshot(profiles = listOf(profile("t-p", dew = 100))), null, now)
        assertEquals(200, first.profiles.single().dewdrops)
        // B trennt und verbindet neu (Basis gelöscht) – Drive enthält den Stand mit Alias
        val tablet = SyncSnapshot(profiles = listOf(profile("t-p", dew = 200)))
        assertEquals(200, Sync.merge(tablet, first, null, now).profiles.single().dewdrops)
    }

    @Test fun deletionFollowsAliases() {
        val tablet = SyncSnapshot(profiles = listOf(profile("t-p")))
        val first = Sync.merge(phone, tablet, null, now)          // p1 ↔ t-p verbunden
        // Tablet löscht „seine“ Lena (t-p)
        val tabletAfter = SyncSnapshot(deleted = setOf("t-p"), aliases = first.aliases)
        val m = Sync.merge(first, tabletAfter, first, now)
        assertTrue(m.profiles.isEmpty())
        assertTrue("p1" in m.deleted)
    }

    @Test fun mergeCarriesFormat() = assertEquals(Sync.FORMAT, Sync.merge(phone, phone, null, now).format)
}
