package de.wissensgarten.core.data

import androidx.test.core.app.ApplicationProvider
import de.wissensgarten.core.data.db.GardenDatabase
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.PackCard
import de.wissensgarten.core.model.Rating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Gleichstand Android ↔ Web: Derselbe Ablauf über die Room-Datenbank (Android) und den Browser-Speicher
 * (MemoryGardenStore, Web) muss denselben Garten ergeben.
 */
@RunWith(RobolectricTestRunner::class)
class StoreParityTest {
    private val v1 = Pack("s1-test", 1, "Test", "mathe", listOf(5), "➗", cards = listOf(
        PackCard("001", "1+1", "2", listOf("3", "4")), PackCard("002", "2+2", "4"), PackCard("003", "3+3", "6")))
    private val v2 = v1.copy(version = 2, cards = listOf(PackCard("001", "1 + 1", "2"), PackCard("003", "3+3", "6"), PackCard("004", "4+4", "8")))

    /** Spielt einen typischen Garten-Verlauf durch und liefert eine vergleichbare Beschreibung (ohne Zufalls-IDs). */
    private suspend fun story(store: GardenStore, kv: KeyValueStore): String {
        var now = 1_700_000_000_000L
        val repo = GardenRepository(store, Tombstones(kv)) { now }
        val mia = repo.createProfile("Mia", "🦊", 5); now += 10
        val ben = repo.createProfile("Ben", "🐻", 2); now += 10
        repo.plantPack(mia.id, v1); now += 10
        val own = repo.addOwnBed(mia.id, "Mein Beet", "✏️"); now += 10
        repo.addCard(repo.bedEntity(own)!!, "Hund", "dog")
        val cards = repo.beds(mia.id).first().flatMap { it.cards }.sortedBy { it.front }
        cards.forEachIndexed { i, c -> now += 60_000; repo.review(mia.id, c.toStudy(), if (i % 2 == 0) Rating.GOOD else Rating.AGAIN) }
        repo.addDew(mia.id, 100)
        repo.buy(mia.id, "sonnenblume"); repo.setBedSpecies(repo.beds(mia.id).first().first().bed.id, "sonnenblume"); repo.buy(mia.id, "nicht_da")
        repo.syncPacks(listOf(v2))
        repo.plantPack(ben.id, v1)
        repo.deleteProfile(ben.id)
        repo.deleteBed(own)
        val p = repo.profiles.first()
        val beds = repo.beds(mia.id).first()
        return buildString {
            p.forEach { appendLine("P ${it.name} ${it.avatar} L${it.level} dew=${it.dewdrops} seeds=${it.seedsToday}/${it.seedsPerDay}") }
            beds.forEach { b ->
                appendLine("B ${b.bed.name} ${b.bed.emoji} v${b.bed.packVersion} sort=${b.bed.sortIndex} ${b.bed.species}")
                b.cards.sortedBy { it.sortIndex }.forEach { c -> appendLine("  C ${c.sourceId} ${c.front}=${c.back} ${c.choices} s=${c.stability} r=${c.reps} l=${c.lapses}") }
            }
            appendLine("inv=" + repo.inventory(mia.id).first().map { it.itemId })
            appendLine("logs=" + repo.profileLogsSince(mia.id, 0).map { it.rating })
        }
    }

    @Test fun androidAndWebStoreBehaveTheSame() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = GardenDatabase.create(ctx, inMemory = true)
        val android = story(RoomGardenStore(db), AndroidKeyValueStore(ctx.getSharedPreferences("paritaet", 0)))
        val web = story(MemoryGardenStore(), MemoryKeyValueStore())
        db.close()
        println(android)
        assertEquals(android, web)
    }

    @Test fun webStateSurvivesJson() = runTest {
        var saved: StoreState? = null
        val store = MemoryGardenStore(onChange = { saved = it })
        story(store, MemoryKeyValueStore())
        val json = kotlinx.serialization.json.Json.encodeToString(StoreState.serializer(), saved!!)
        val back = kotlinx.serialization.json.Json.decodeFromString(StoreState.serializer(), json)
        assertEquals(saved, back)
    }
}
