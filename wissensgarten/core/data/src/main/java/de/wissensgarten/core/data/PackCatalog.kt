package de.wissensgarten.core.data

import android.content.res.AssetManager
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.PackJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Alle verfügbaren Wissenspakete. Heute aus den App-Assets (Modul :content).
 * Später kann hier eine zweite Quelle (Download) ergänzt werden – die höhere Version gewinnt.
 */
class PackCatalog(private val assets: AssetManager) {
    private val mutex = Mutex()
    private var cache: List<Pack>? = null

    suspend fun all(): List<Pack> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) { load() }.also { cache = it }
    }

    suspend fun byId(id: String): Pack? = all().firstOrNull { it.id == id }

    private fun load(): List<Pack> {
        val files = assets.list(DIR).orEmpty().filter { it.endsWith(".json") }
        return files.mapNotNull { f ->
            runCatching { assets.open("$DIR/$f").bufferedReader().use { PackJson.parse(it.readText()) } }.getOrNull()
        }.sortedWith(compareBy({ it.grades.minOrNull() ?: 99 }, { it.title }))
    }

    companion object { const val DIR = "packs" }
}
