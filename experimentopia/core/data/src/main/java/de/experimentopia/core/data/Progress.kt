package de.experimentopia.core.data

import android.content.Context
import androidx.room.*
import de.experimentopia.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

/** Android-Speicher (Room) hinter der gemeinsamen Schnittstelle – die Regeln stehen in [Progress]. */
class ProgressRepository(private val dao: ResultDao, private val clock: () -> Long = System::currentTimeMillis) : LabStore {
    override val results: Flow<List<ResultRow>> = dao.all().map { l -> l.map { ResultRow(it.experimentId, it.stars, it.firstGuessRight, it.plays, it.firstAt, it.lastAt) } }
    override val journal: Flow<List<NoteRow>> = dao.journal().map { l -> l.map { NoteRow(it.id, it.experimentId, it.at, it.guess, it.result, it.right, it.sure, it.explainedFirstTry, it.daily) } }

    override suspend fun record(f: Finished) {
        val now = clock()
        val old = dao.get(f.id)?.let { ResultRow(it.experimentId, it.stars, it.firstGuessRight, it.plays, it.firstAt, it.lastAt) }
        val r = Progress.merge(old, f, now)
        dao.put(ExperimentResult(r.experimentId, r.stars, r.firstGuessRight, r.plays, r.firstAt, r.lastAt))
        val n = Progress.note(f, now)
        dao.add(JournalEntry(experimentId = n.experimentId, at = n.at, guess = n.guess, result = n.result, right = n.right, sure = n.sure, explainedFirstTry = n.explainedFirstTry, daily = n.daily))
    }

    override suspend fun reset() { dao.clearResults(); dao.clearJournal() }
}
