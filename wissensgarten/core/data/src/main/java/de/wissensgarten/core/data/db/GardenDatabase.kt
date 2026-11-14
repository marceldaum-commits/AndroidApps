package de.wissensgarten.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Datenbank-Version erhöhen und eine Migration ergänzen, wenn sich Tabellen ändern.
 * Die Schemata liegen unter core/data/schemas – so bleiben Updates nachvollziehbar.
 */
@Database(
    entities = [ProfileEntity::class, BedEntity::class, CardEntity::class, ReviewLogEntity::class, InventoryEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class GardenDatabase : RoomDatabase() {
    abstract fun dao(): GardenDao

    companion object {
        fun create(context: Context, inMemory: Boolean = false): GardenDatabase {
            val b = if (inMemory) Room.inMemoryDatabaseBuilder(context, GardenDatabase::class.java)
            else Room.databaseBuilder(context, GardenDatabase::class.java, "garten.db")
            return b.addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        }

        /** v3: Merkbilder für Karten. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cards` ADD COLUMN `img` TEXT")
            }
        }

        /** v2: Gartenladen – Inventar und Pflanzenart pro Beet. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `beds` ADD COLUMN `species` TEXT NOT NULL DEFAULT 'klassik'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `inventory` (`profileId` TEXT NOT NULL, `itemId` TEXT NOT NULL, " +
                        "`active` INTEGER NOT NULL, `boughtAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `itemId`), " +
                        "FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
            }
        }
    }
}
