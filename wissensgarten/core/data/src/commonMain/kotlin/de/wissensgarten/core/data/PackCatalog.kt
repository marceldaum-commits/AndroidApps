package de.wissensgarten.core.data

import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.PackJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Alle verfügbaren Wissenspakete. [texts] liefert die JSON-Texte (Android: App-Assets, Web: mitgelieferte Dateien).
 * Später kann hier eine zweite Quelle (Download) ergänzt werden – die höhere Version gewinnt.
 */
class PackCatalog(private val texts: suspend () -> List<String>) {
    private val mutex = Mutex()
    private var cache: List<Pack>? = null

    suspend fun all(): List<Pack> = mutex.withLock {
        cache ?: texts().mapNotNull { runCatching { PackJson.parse(it) }.getOrNull() }
            .sortedWith(compareBy({ it.grades.minOrNull() ?: 99 }, { it.title }))
            .also { cache = it }
    }

    suspend fun byId(id: String): Pack? = all().firstOrNull { it.id == id }

    companion object { const val DIR = "packs" }
}
