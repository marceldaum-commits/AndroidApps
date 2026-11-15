package de.kurvenflug.core.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest

/** Bestes Ergebnis pro Level und Spieler. */
@Entity(tableName = "progress", primaryKeys = ["profileId", "levelId"])
data class LevelProgress(
    val profileId: Int = 0,
    val levelId: String,
    val stars: Int,
    /** Energiezellen, die in diesem Level schon eingesammelt wurden (Bit je Zelle) */
    val cellMask: Int = 0,
    val attempts: Int = 0,
    val solvedAt: Long = 0,
)

/**
 * Ein Durchgang für die Eltern-Ansicht: geschafft ([stars] > 0) oder hängen geblieben (stars = 0 nach mehreren Versuchen).
 * Nur Level-Kennung, Sterne, Versuche, Dauer und Zeitpunkt – nichts Persönliches.
 */
@Entity(tableName = "plays", indices = [Index("profileId")])
data class Play(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Int,
    val levelId: String,
    val stars: Int,
    val attempts: Int,
    val seconds: Int,
    val at: Long,
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE profileId = :profile") fun all(profile: Int): Flow<List<LevelProgress>>
    @Query("SELECT * FROM progress WHERE profileId = :profile AND levelId = :id") suspend fun get(profile: Int, id: String): LevelProgress?
    @Upsert suspend fun put(p: LevelProgress)
    /** Neu anfangen / Profil löschen: Spielstand eines Spielers löschen */
    @Query("DELETE FROM progress WHERE profileId = :profile") suspend fun clear(profile: Int)

    @Insert suspend fun log(p: Play)
    @Query("SELECT * FROM plays WHERE profileId = :profile AND at >= :since ORDER BY at") fun plays(profile: Int, since: Long): Flow<List<Play>>
    @Query("DELETE FROM plays WHERE profileId = :profile") suspend fun clearPlays(profile: Int)
}

@Database(entities = [LevelProgress::class, Play::class], version = 2, exportSchema = true)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao

    companion object {
        /**
         * Version 1 → 2: Spielstand bekommt eine Profil-Spalte (alles Bisherige gehört Profil 0) und es gibt das
         * Durchgangs-Protokoll für die Eltern-Ansicht. Kein Stern geht verloren; geschaffte Level stehen gleich im Protokoll.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `progress_new` (`profileId` INTEGER NOT NULL, `levelId` TEXT NOT NULL, `stars` INTEGER NOT NULL, " +
                    "`cellMask` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `solvedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `levelId`))")
                db.execSQL("INSERT INTO `progress_new` (`profileId`, `levelId`, `stars`, `cellMask`, `attempts`, `solvedAt`) " +
                    "SELECT 0, `levelId`, `stars`, `cellMask`, `attempts`, `solvedAt` FROM `progress`")
                db.execSQL("DROP TABLE `progress`")
                db.execSQL("ALTER TABLE `progress_new` RENAME TO `progress`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `plays` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, " +
                    "`levelId` TEXT NOT NULL, `stars` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `seconds` INTEGER NOT NULL, `at` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plays_profileId` ON `plays` (`profileId`)")
                db.execSQL("INSERT INTO `plays` (`profileId`, `levelId`, `stars`, `attempts`, `seconds`, `at`) " +
                    "SELECT 0, `levelId`, `stars`, `attempts`, 0, `solvedAt` FROM `progress` WHERE `stars` > 0")
            }
        }

        fun create(ctx: Context, inMemory: Boolean = false, name: String = "kurvenflug.db"): FlightDatabase =
            (if (inMemory) Room.inMemoryDatabaseBuilder(ctx, FlightDatabase::class.java)
            else Room.databaseBuilder(ctx, FlightDatabase::class.java, name)).addMigrations(MIGRATION_1_2).build()
    }
}

/** Speichert Ergebnisse des aktuellen Spielers – das Beste bleibt (Sterne sinken nie, Energiezellen bleiben gesammelt). */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepository(private val dao: ProgressDao, private val profile: StateFlow<Int>, private val clock: () -> Long = System::currentTimeMillis) {
    val all: Flow<List<LevelProgress>> = profile.flatMapLatest { dao.all(it) }

    /** Durchgänge des aktuellen Spielers seit [since] (für die Eltern-Ansicht) */
    fun plays(since: Long): Flow<List<Play>> = profile.flatMapLatest { dao.plays(it, since) }

    suspend fun record(levelId: String, stars: Int, cells: Set<Int>, attempts: Int, seconds: Int = 0) {
        val p = profile.value
        val old = dao.get(p, levelId)
        val mask = cells.fold(old?.cellMask ?: 0) { m, i -> m or (1 shl i) }
        dao.put(LevelProgress(
            profileId = p,
            levelId = levelId,
            stars = maxOf(stars, old?.stars ?: 0),
            cellMask = mask,
            attempts = (old?.attempts ?: 0) + attempts,
            solvedAt = old?.solvedAt?.takeIf { it > 0 } ?: clock(),
        ))
        dao.log(Play(profileId = p, levelId = levelId, stars = stars, attempts = attempts, seconds = seconds, at = clock()))
    }

    /** Hängen geblieben (mehrere Fehlversuche, noch nicht geschafft) – damit Eltern sehen, wo Hilfe gut täte. */
    suspend fun stuck(levelId: String, attempts: Int, seconds: Int) =
        dao.log(Play(profileId = profile.value, levelId = levelId, stars = 0, attempts = attempts, seconds = seconds, at = clock()))

    /** Alles des aktuellen Spielers löschen – nur über „Neu anfangen“ (mit Gedrückthalten bestätigt). */
    suspend fun clear() = clear(profile.value)
    suspend fun clear(profileId: Int) { dao.clear(profileId); dao.clearPlays(profileId) }
}
