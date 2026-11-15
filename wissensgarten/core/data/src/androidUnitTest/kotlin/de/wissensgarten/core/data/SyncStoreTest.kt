package de.wissensgarten.core.data

import androidx.test.core.app.ApplicationProvider
import de.wissensgarten.core.data.db.GardenDatabase
import de.wissensgarten.core.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Zwei Geräte (zwei Datenbanken), die über einen gemeinsamen Drive-Stand abgleichen. */
@RunWith(RobolectricTestRunner::class)
class SyncStoreTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var phoneDb: GardenDatabase
    private lateinit var tabletDb: GardenDatabase
    private var now = 1_800_000_000_000L
    private val pack = Pack("s1-test", 1, "Test", "mathe", listOf(5), "➗", cards = (1..5).map { PackCard("00$it", "$it+$it", "${it * 2}") })

    @Before fun setUp() {
        phoneDb = GardenDatabase.create(ctx, inMemory = true)
        tabletDb = GardenDatabase.create(ctx, inMemory = true)
    }
    @After fun tearDown() { phoneDb.close(); tabletDb.close() }

    private fun store(db: GardenDatabase) = SyncStore(ctx, RoomGardenStore(db), Settings(AndroidKeyValueStore(ctx.getSharedPreferences("einstellungen", 0))), Tombstones(AndroidKeyValueStore(ctx.getSharedPreferences("sync_geloescht", 0)))) { now }

    @Test fun twoDevicesEndUpWithTheSameGarden() = runTest {
        val phone = GardenRepository(RoomGardenStore(phoneDb)) { now }
        val tablet = GardenRepository(RoomGardenStore(tabletDb)) { now }
        // Auf beiden Geräten getrennt eingerichtet, jeweils eine andere Karte gelernt
        val pp = phone.createProfile("Lena", "🦊", 7); phone.plantPack(pp.id, pack)
        val pc = phone.beds(pp.id).first()[0].cards.first { it.sourceId == "001" }
        phone.review(pp.id, pc.toStudy(), Rating.GOOD)
        val tp = tablet.createProfile("Lena", "🦊", 7); tablet.plantPack(tp.id, pack)
        now += 60_000
        val tc = tablet.beds(tp.id).first()[0].cards.first { it.sourceId == "002" }
        tablet.review(tp.id, tc.toStudy(), Rating.GOOD)

        // Handy lädt zuerst hoch, dann gleicht das Tablet ab, dann wieder das Handy
        val phoneStore = store(phoneDb); val tabletStore = store(tabletDb)
        val drive1 = Sync.merge(phoneStore.snapshot(), null, null, now)
        val drive2 = Sync.merge(tabletStore.snapshot(), drive1, null, now)
        tabletStore.apply(drive2, tabletStore.snapshot())
        val phoneTaken = phoneStore.snapshot()
        val drive3 = Sync.merge(phoneTaken, drive2, drive1, now)
        phoneStore.apply(drive3, phoneTaken)

        for (db in listOf(phoneDb, tabletDb)) {
            val profiles = RoomGardenStore(db).allProfiles()
            assertEquals("ein Profil, nicht zwei", 1, profiles.size)
            val cards = RoomGardenStore(db).allCards()
            assertEquals(5, cards.size)
            assertEquals(setOf("001", "002"), cards.filter { !it.memory.isSeed }.map { it.sourceId }.toSet())
            assertEquals(2, RoomGardenStore(db).logsSinceOnce(0).size)
        }
        // Tautropfen: beide Gewinne zählen
        assertEquals(6, RoomGardenStore(phoneDb).allProfiles().single().dewdrops)
    }

    @Test fun applyingTwiceChangesNothing() = runTest {
        val phone = GardenRepository(RoomGardenStore(phoneDb)) { now }
        val p = phone.createProfile("Ben", "🐸", 3); phone.plantPack(p.id, pack)
        val s = store(phoneDb)
        val snap = Sync.merge(s.snapshot(), null, null, now)
        val t = s.snapshot()
        s.apply(snap, t); s.apply(snap, t)
        assertEquals(5, RoomGardenStore(phoneDb).allCards().size)
        assertEquals(0, RoomGardenStore(phoneDb).logsSinceOnce(0).size)
        assertEquals(snap.copy(savedAt = 0, deleted = emptySet()), s.snapshot().copy(savedAt = 0, deleted = emptySet()))
    }

    /** Während des Abgleichs gegossen und eingekauft: nichts davon darf verloren gehen. */
    @Test fun changesDuringSyncSurvive() = runTest {
        val repo = GardenRepository(RoomGardenStore(phoneDb)) { now }
        val p = repo.createProfile("Mia", "🐸", 3); repo.plantPack(p.id, pack)
        repo.addDew(p.id, 100)
        val s = store(phoneDb)
        val taken = s.snapshot()                               // Abgleich beginnt …
        val merged = Sync.merge(taken, taken.copy(), taken, now)
        repo.addDew(p.id, 15)                                   // … währenddessen 15 💧 verdient
        s.apply(merged, taken)                                  // … Abgleich schreibt zurück
        assertEquals(115, RoomGardenStore(phoneDb).allProfiles().single().dewdrops)
    }

    @Test fun deletionDuringSyncIsKept() = runTest {
        val repo = GardenRepository(RoomGardenStore(phoneDb), Tombstones(AndroidKeyValueStore(ctx.getSharedPreferences("sync_geloescht", 0)))) { now }
        val p = repo.createProfile("Mia", "🐸", 3); repo.plantPack(p.id, pack)
        val s = store(phoneDb)
        val taken = s.snapshot()
        val bed = RoomGardenStore(phoneDb).allBeds().single()
        repo.deleteBed(bed.id)                                  // während des Abgleichs gelöscht
        s.apply(Sync.merge(taken, taken, taken, now), taken)
        assertTrue(RoomGardenStore(phoneDb).allBeds().isEmpty())
        assertTrue(bed.id in Tombstones(AndroidKeyValueStore(ctx.getSharedPreferences("sync_geloescht", 0))).all())
    }
}
