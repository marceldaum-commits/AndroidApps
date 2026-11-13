package de.wissensgarten.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Datenbank-Version erhöhen und eine Migration ergänzen, wenn sich Tabellen ändern.
 * Die Schemata liegen unter core/data/schemas – so bleiben Updates nachvollziehbar.
 */
@Database(
    entities = [ProfileEntity::class, BedEntity::class, CardEntity::class, ReviewLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GardenDatabase : RoomDatabase() {
    abstract fun dao(): GardenDao

    companion object {
        fun create(context: Context, inMemory: Boolean = false): GardenDatabase {
            val b = if (inMemory) Room.inMemoryDatabaseBuilder(context, GardenDatabase::class.java)
            else Room.databaseBuilder(context, GardenDatabase::class.java, "garten.db")
            return b.build()
        }
    }
}
