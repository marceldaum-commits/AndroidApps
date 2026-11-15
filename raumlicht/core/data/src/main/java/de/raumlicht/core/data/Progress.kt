package de.raumlicht.core.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Bestes Ergebnis pro Level. */
@Entity(tableName = "progress")
data class LevelProgress(
    @PrimaryKey val levelId: String,
    val stars: Int,
    val attempts: Int = 0,
    val solvedAt: Long = 0,
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress") fun all(): Flow<List<LevelProgress>>
    @Query("SELECT * FROM progress WHERE levelId = :id") suspend fun get(id: String): LevelProgress?
    @Upsert suspend fun put(p: LevelProgress)
    @Query("DELETE FROM progress") suspend fun clear()
}

@Database(entities = [LevelProgress::class], version = 1, exportSchema = true)
abstract class StageDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao

    companion object {
        fun create(ctx: Context, inMemory: Boolean = false): StageDatabase =
            (if (inMemory) Room.inMemoryDatabaseBuilder(ctx, StageDatabase::class.java)
            else Room.databaseBuilder(ctx, StageDatabase::class.java, "raumlicht.db")).build()
    }
}

/** Speichert Ergebnisse – das Beste bleibt (Sterne sinken nie). */
class ProgressRepository(private val dao: ProgressDao, private val clock: () -> Long = System::currentTimeMillis) {
    val all: Flow<List<LevelProgress>> = dao.all()

    /** Neu anfangen: alle Sterne und Versuche löschen (nur auf ausdrücklichen Wunsch) */
    suspend fun clear() = dao.clear()

    suspend fun record(levelId: String, stars: Int, attempts: Int) {
        val old = dao.get(levelId)
        dao.put(LevelProgress(
            levelId = levelId,
            stars = maxOf(stars, old?.stars ?: 0),
            attempts = (old?.attempts ?: 0) + attempts,
            solvedAt = old?.solvedAt?.takeIf { it > 0 } ?: clock(),
        ))
    }
}
