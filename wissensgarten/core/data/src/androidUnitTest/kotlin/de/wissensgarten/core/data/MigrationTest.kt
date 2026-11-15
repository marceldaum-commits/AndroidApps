package de.wissensgarten.core.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.wissensgarten.core.data.db.GardenDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Ein Garten aus Version 0.2/0.3 (DB v1) muss das Update ohne Verlust überstehen.
 * Die v1-Datenbank wird exakt aus dem exportierten Schema (schemas/…/1.json) gebaut;
 * Room prüft beim Öffnen, dass das Ergebnis der Migration dem v2-Schema entspricht.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private fun createV1(file: File) {
        val schema = Json.parseToJsonElement(
            File("schemas/de.wissensgarten.core.data.db.GardenDatabase/1.json").readText()
        ).jsonObject["database"]!!.jsonObject
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        for (e in schema["entities"]!!.jsonArray) {
            val o = e.jsonObject
            val table = o["tableName"]!!.jsonPrimitive.content
            db.execSQL(o["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            o["indices"]?.jsonArray?.forEach { i ->
                db.execSQL(i.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
        }
        for (q in schema["setupQueries"]!!.jsonArray) db.execSQL(q.jsonPrimitive.content)
        db.execSQL("INSERT INTO profiles (id, name, avatar, level, dewdrops, seedDay, seedsToday, seedsPerDay, createdAt) VALUES ('p', 'Mia', '🦊', 5, 120, 0, 0, 10, 0)")
        db.execSQL("INSERT INTO beds (id, profileId, name, emoji, hue, packId, packVersion, createdAt, sortIndex) VALUES ('b', 'p', 'Test', '➗', 1.0, 's1-test', 1, 0, 0)")
        db.execSQL("INSERT INTO cards (id, bedId, profileId, sourceId, front, back, choices, info, stability, difficulty, lastReview, reps, lapses, createdAt, sortIndex) VALUES ('c', 'b', 'p', '001', '1+1', '2', NULL, NULL, 5.0, 5.0, 1000, 2, 0, 0, 0)")
        db.version = 1
        db.close()
    }

    @Test fun v1ToCurrentKeepsGarden() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = ctx.getDatabasePath("migration-test.db").apply { parentFile?.mkdirs(); delete() }
        createV1(file)

        val db = Room.databaseBuilder(ctx, GardenDatabase::class.java, file.absolutePath)
            .addMigrations(GardenDatabase.MIGRATION_1_2, GardenDatabase.MIGRATION_2_3).build()
        val repo = GardenRepository(RoomGardenStore(db)) { 0L }
        val beds = repo.beds("p").first()
        assertEquals("klassik", beds[0].bed.species)
        assertEquals(5.0, beds[0].cards[0].stability, 0.0)
        assertEquals(120, repo.profile("p")!!.dewdrops)
        assertEquals(GardenRepository.BuyResult.OK, repo.buy("p", "zwerg"))
        assertEquals(40, repo.profile("p")!!.dewdrops)
        db.close()
    }
}
