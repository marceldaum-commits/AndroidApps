package de.wissensgarten.core.data

import androidx.test.core.app.ApplicationProvider
import de.wissensgarten.core.data.db.GardenDatabase
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.PackCard
import de.wissensgarten.core.model.Rating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GardenRepositoryTest {
    private lateinit var db: GardenDatabase
    private lateinit var repo: GardenRepository
    private var now = 1_700_000_000_000L

    private val v1 = Pack(
        "s1-test", 1, "Test", "mathe", listOf(5), "➗", cards = listOf(
            PackCard("001", "1+1", "2", listOf("3", "4")),
            PackCard("002", "2+2", "4"),
            PackCard("003", "3+3", "6"),
        )
    )

    @Before fun setUp() {
        db = GardenDatabase.create(ApplicationProvider.getApplicationContext(), inMemory = true)
        repo = GardenRepository(db) { now }
    }

    @After fun tearDown() = db.close()

    @Test fun plantPackCreatesSeeds() = runTest {
        val p = repo.createProfile("Mia", "🦊", 5)
        repo.plantPack(p.id, v1)
        val beds = repo.beds(p.id).first()
        assertEquals(1, beds.size)
        assertEquals(3, beds[0].cards.size)
        assertTrue(beds[0].cards.all { it.memory.isSeed })
        assertEquals(listOf("3", "4"), beds[0].cards.first { it.sourceId == "001" }.choiceList)
    }

    @Test fun reviewUpdatesMemoryDewAndSeedCounter() = runTest {
        val p = repo.createProfile("Mia", "🦊", 5)
        repo.plantPack(p.id, v1)
        val card = repo.beds(p.id).first()[0].cards.first().toStudy()
        repo.review(p.id, card, Rating.GOOD)
        val after = repo.beds(p.id).first()[0].cards.first { it.id == card.id }
        assertFalse(after.memory.isSeed)
        val prof = repo.profile(p.id)!!
        assertEquals(3, prof.dewdrops)
        assertEquals(1, prof.seedsToday)
        assertEquals(1, repo.logsSince(0).first().size)
    }

    @Test fun packUpdateKeepsProgressAddsAndRemovesCards() = runTest {
        val p = repo.createProfile("Mia", "🦊", 5)
        repo.plantPack(p.id, v1)
        val learned = repo.beds(p.id).first()[0].cards.first { it.sourceId == "002" }
        repo.review(p.id, learned.toStudy(), Rating.EASY)

        val v2 = v1.copy(
            version = 2, cards = listOf(
                PackCard("002", "2+2", "4", info = "verdoppeln"), // geändert, Fortschritt bleibt
                PackCard("003", "3+3", "6"),
                PackCard("004", "4+4", "8"), // neu
            )
        )
        assertEquals(1, repo.syncPacks(listOf(v2)))
        val cards = repo.beds(p.id).first()[0].cards
        assertEquals(setOf("002", "003", "004"), cards.map { it.sourceId }.toSet())
        val kept = cards.first { it.sourceId == "002" }
        assertEquals(learned.id, kept.id)
        assertEquals("verdoppeln", kept.info)
        assertFalse("Lernstand muss erhalten bleiben", kept.memory.isSeed)
        assertEquals(2, repo.beds(p.id).first()[0].bed.packVersion)
        assertEquals("zweiter Sync ändert nichts", 0, repo.syncPacks(listOf(v2)))
    }

    @Test fun shopBuyingRules() = runTest {
        val p = repo.createProfile("Mia", "🦊", 5)
        assertEquals(GardenRepository.BuyResult.TOO_EXPENSIVE, repo.buy(p.id, "zwerg"))
        repo.updateProfile(repo.profile(p.id)!!.copy(dewdrops = 100))
        assertEquals(GardenRepository.BuyResult.OK, repo.buy(p.id, "zwerg"))      // 80
        assertEquals(20, repo.profile(p.id)!!.dewdrops)
        assertEquals(GardenRepository.BuyResult.ALREADY_OWNED, repo.buy(p.id, "zwerg"))
        assertEquals(GardenRepository.BuyResult.UNKNOWN, repo.buy(p.id, "gibtsnicht"))
        assertEquals(GardenRepository.BuyResult.ALREADY_OWNED, repo.buy(p.id, "klassik")) // gratis, immer da
        repo.setDecoActive(p.id, "zwerg", false)
        assertEquals(false, repo.inventory(p.id).first().single().active)
        assertEquals(20, repo.profile(p.id)!!.dewdrops)
    }

    @Test fun bedSpeciesCanBeChanged() = runTest {
        val p = repo.createProfile("Mia", "🦊", 5)
        val id = repo.plantPack(p.id, v1)
        assertEquals("klassik", repo.beds(p.id).first()[0].bed.species)
        repo.setBedSpecies(id, "rose")
        assertEquals("rose", repo.beds(p.id).first()[0].bed.species)
    }

    @Test fun deletingProfileRemovesItsGarden() = runTest {
        val a = repo.createProfile("Mia", "🦊", 5)
        val b = repo.createProfile("Papa", "🐻", 14)
        repo.plantPack(a.id, v1)
        repo.plantPack(b.id, v1)
        repo.deleteProfile(a.id)
        assertEquals(1, repo.allBeds().first().size)
        assertEquals(10, b.seedsPerDay)
        assertEquals(5, repo.createProfile("Ben", "🐸", 2).seedsPerDay)
    }
}
