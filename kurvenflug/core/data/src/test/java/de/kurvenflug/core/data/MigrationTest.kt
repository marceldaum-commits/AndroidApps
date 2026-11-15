package de.kurvenflug.core.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migration 1 → 2: Eine echte Datenbank im Format von Version 1 (Schema aus schemas/…/1.json) wird angelegt,
 * dann öffnet die neue App sie. Jeder Stern, jede Energiezelle muss danach noch da sein.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    @Test fun version1ProgressSurvivesAndAppearsInThePlayLog() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "migration-test.db"
        ctx.deleteDatabase(name)
        val file = ctx.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `progress` (`levelId` TEXT NOT NULL, `stars` INTEGER NOT NULL, `cellMask` INTEGER NOT NULL, " +
                "`attempts` INTEGER NOT NULL, `solvedAt` INTEGER NOT NULL, PRIMARY KEY(`levelId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0b33fba1c7aa03603429910e5110bd27')")
            db.execSQL("INSERT INTO progress VALUES ('2-1', 3, 5, 1, 1000)")
            db.execSQL("INSERT INTO progress VALUES ('3-4', 2, 0, 4, 2000)")
            db.execSQL("INSERT INTO progress VALUES ('daily-20500', 1, 0, 3, 3000)")
            db.version = 1
        }
        val db = FlightDatabase.create(ctx, name = name)
        val repo = ProgressRepository(db.progress(), MutableStateFlow(0))
        val all = repo.all.first().associateBy { it.levelId }
        assertEquals(3, all.size)
        assertEquals(LevelProgress(0, "2-1", 3, 5, 1, 1000), all["2-1"])
        assertEquals(2, all["3-4"]!!.stars)
        assertEquals(3, repo.plays(0).first().size)
        // neue Profile starten leer, Weiterspielen funktioniert
        assertTrue(ProgressRepository(db.progress(), MutableStateFlow(1)).all.first().isEmpty())
        repo.record("2-1", 1, setOf(1), 2)
        assertEquals(3, repo.all.first().first { it.levelId == "2-1" }.stars)
        db.close()
    }
}
