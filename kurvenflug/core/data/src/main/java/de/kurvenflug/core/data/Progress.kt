package de.kurvenflug.core.data

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
    /** Neu anfangen: ganzen Spielstand löschen (nur auf ausdrücklichen Wunsch in den Einstellungen) */
    @Query("DELETE FROM progress") suspend fun clear()
}

@Database(entities = [LevelProgress::class], version = 1, exportSchema = true)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao

    companion object {
        fun create(ctx: Context, inMemory: Boolean = false): FlightDatabase =
            (if (inMemory) Room.inMemoryDatabaseBuilder(ctx, FlightDatabase::class.java)
            else Room.databaseBuilder(ctx, FlightDatabase::class.java, "kurvenflug.db")).build()
    }
}

/** Speichert Ergebnisse – das Beste bleibt (Sterne sinken nie, Energiezellen bleiben gesammelt). */
class ProgressRepository(private val dao: ProgressDao, private val clock: () -> Long = System::currentTimeMillis) {
    val all: Flow<List<LevelProgress>> = dao.all()

    /** Alles löschen – nur über „Neu anfangen“ in den Einstellungen (mit Gedrückthalten bestätigt). */
    suspend fun clear() = dao.clear()

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
