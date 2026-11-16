package de.experimentopia.core.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Bestes Ergebnis je Versuch. Sterne sinken nie. */
@Entity(tableName = "results")
data class ExperimentResult(
    @PrimaryKey val experimentId: String,
    val stars: Int,
    /** Lag die allererste Vermutung richtig? (danach kennt man die Lösung) */
    val firstGuessRight: Boolean,
    val plays: Int = 1,
    val firstAt: Long = 0,
    val lastAt: Long = 0,
)

/** Ein Eintrag im Forscherbuch: was ich vermutet habe, wie sicher ich war, was herauskam. */
@Entity(tableName = "journal")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: String,
    val at: Long,
    val guess: String,
    val result: String,
    val right: Boolean,
    /** Sicherheit: 0 geraten, 1 ziemlich sicher, 2 ganz sicher */
    val sure: Int,
    val explainedFirstTry: Boolean,
    /** War es das Rätsel des Tages? */
    val daily: Boolean = false,
)

@Dao
interface ResultDao {
    @Query("SELECT * FROM results") fun all(): Flow<List<ExperimentResult>>
    @Query("SELECT * FROM results WHERE experimentId = :id") suspend fun get(id: String): ExperimentResult?
    @Upsert suspend fun put(r: ExperimentResult)
    @Query("SELECT * FROM journal ORDER BY at DESC, id DESC") fun journal(): Flow<List<JournalEntry>>
    @Insert suspend fun add(e: JournalEntry)
    @Query("DELETE FROM results") suspend fun clearResults()
    @Query("DELETE FROM journal") suspend fun clearJournal()
}

@Database(entities = [ExperimentResult::class, JournalEntry::class], version = 1, exportSchema = true)
abstract class LabDatabase : RoomDatabase() {
    abstract fun results(): ResultDao

    companion object {
        fun create(ctx: Context, inMemory: Boolean = false): LabDatabase =
            (if (inMemory) Room.inMemoryDatabaseBuilder(ctx, LabDatabase::class.java)
            else Room.databaseBuilder(ctx, LabDatabase::class.java, "experimentopia.db")).build()
    }
}

class ProgressRepository(private val dao: ResultDao, private val clock: () -> Long = System::currentTimeMillis) {
    val results: Flow<List<ExperimentResult>> = dao.all()
    val journal: Flow<List<JournalEntry>> = dao.journal()

    /** Einen fertigen Versuch speichern: bestes Ergebnis behalten, erste Vermutung merken, Forscherbuch ergänzen. */
    suspend fun record(id: String, stars: Int, guess: String, result: String, right: Boolean, sure: Int, explainedFirstTry: Boolean, daily: Boolean = false) {
        val now = clock()
        val old = dao.get(id)
        dao.put(ExperimentResult(
            experimentId = id,
            stars = maxOf(stars, old?.stars ?: 0),
            firstGuessRight = old?.firstGuessRight ?: right,
            plays = (old?.plays ?: 0) + 1,
            firstAt = old?.firstAt ?: now,
            lastAt = now,
        ))
        dao.add(JournalEntry(experimentId = id, at = now, guess = guess, result = result, right = right, sure = sure, explainedFirstTry = explainedFirstTry, daily = daily))
    }

    suspend fun reset() { dao.clearResults(); dao.clearJournal() }
}
