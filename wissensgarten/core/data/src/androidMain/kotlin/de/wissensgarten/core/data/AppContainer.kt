package de.wissensgarten.core.data

import android.content.Context
import de.wissensgarten.core.data.db.GardenDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android: Room-Datenbank, SharedPreferences, Pakete aus den App-Assets; dazu der Geräte-Sync. */
class AppContainer private constructor(context: Context, val database: GardenDatabase) : DataContainer(
    store = RoomGardenStore(database),
    prefs = { name -> AndroidKeyValueStore(context.getSharedPreferences(name, Context.MODE_PRIVATE)) },
    packTexts = {
        withContext(Dispatchers.IO) {
            val assets = context.assets
            assets.list(PackCatalog.DIR).orEmpty().filter { it.endsWith(".json") }.mapNotNull { f ->
                runCatching { assets.open("${PackCatalog.DIR}/$f").bufferedReader().use { it.readText() } }.getOrNull()
            }
        }
    },
) {
    constructor(context: Context) : this(context.applicationContext, GardenDatabase.create(context.applicationContext))
    val syncStore: SyncStore by lazy { SyncStore(appContext, store, settings, tombstones) }
    private val appContext = context.applicationContext
}
