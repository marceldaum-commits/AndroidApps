package de.wissensgarten.core.data

import android.content.Context
import de.wissensgarten.core.data.db.GardenDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android: Room-Datenbank, SharedPreferences, Pakete aus den App-Assets; dazu der Geräte-Sync. */
class AppContainer private constructor(context: Context, val database: GardenDatabase, plus: PlusConfig) : DataContainer(
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
    downloadedPacks = {
        withContext(Dispatchers.IO) {
            remotePackDir(context).listFiles { f -> f.name.endsWith(".json") }.orEmpty().mapNotNull { runCatching { it.readText() }.getOrNull() }
        }
    },
    plus = plus,
    plusPreviews = { withContext(Dispatchers.IO) { runCatching { context.assets.open("plus_katalog.json").bufferedReader().use { it.readText() } }.getOrNull() } },
) {
    constructor(context: Context, plus: PlusConfig = PlusConfig()) : this(context.applicationContext, GardenDatabase.create(context.applicationContext), plus)
    val syncStore: SyncStore by lazy { SyncStore(appContext, store, settings, tombstones) }
    private val appContext = context.applicationContext

    companion object {
        /** Nachgeladene Wissenspakete (nur JSON-Daten) – liegen im privaten App-Speicher. */
        fun remotePackDir(context: Context): java.io.File = java.io.File(context.applicationContext.filesDir, "packs_nachgeladen").apply { mkdirs() }
    }
}
