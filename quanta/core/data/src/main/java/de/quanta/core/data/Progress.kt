package de.quanta.core.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Bestes Ergebnis pro Level. */
@Entity(tableName = "progress")
data class LevelProgress(
    @PrimaryKey val levelId: String,
    val stars: Int,
    /** Energiezellen, die in diesem Level schon eingesammelt wurden (Bit je Zelle) */
    val cellMask: Int = 0,
    val attempts: Int = 0,
    val solvedAt: Long = 0,
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress") fun all(): Flow<List<LevelProgress>>
    @Query("SELECT * FROM progress WHERE levelId = :id") suspend fun get(id: String): LevelProgress?
    @Upsert suspend fun put(p: LevelProgress)
}

@Database(entities = [LevelProgress::class], version = 1, exportSchema = true)
abstract class QuantaDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao

    companion object {
        fun create(ctx: Context, inMemory: Boolean = false): QuantaDatabase =
            (if (inMemory) Room.inMemoryDatabaseBuilder(ctx, QuantaDatabase::class.java)
            else Room.databaseBuilder(ctx, QuantaDatabase::class.java, "quanta.db")).build()
    }
}

/** Speichert Ergebnisse – das Beste bleibt (Sterne sinken nie, Energiezellen bleiben gesammelt). */
class ProgressRepository(private val dao: ProgressDao, private val clock: () -> Long = System::currentTimeMillis) {
    val all: Flow<List<LevelProgress>> = dao.all()

    suspend fun record(levelId: String, stars: Int, cells: Set<Int>, attempts: Int) {
        val old = dao.get(levelId)
        val mask = cells.fold(old?.cellMask ?: 0) { m, i -> m or (1 shl i) }
        dao.put(LevelProgress(
            levelId = levelId,
            stars = maxOf(stars, old?.stars ?: 0),
            cellMask = mask,
            attempts = (old?.attempts ?: 0) + attempts,
            solvedAt = old?.solvedAt?.takeIf { it > 0 } ?: clock(),
        ))
    }
}
