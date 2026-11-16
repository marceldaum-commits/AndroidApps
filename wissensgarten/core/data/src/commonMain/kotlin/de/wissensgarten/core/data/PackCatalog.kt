package de.wissensgarten.core.data

import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.PackJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Alle verfügbaren Wissenspakete. [texts] liefert die mitgelieferten JSON-Texte (Android: App-Assets, Web: Dateien),
 * [extra] nachgeladene Pakete (nur Daten, siehe [PackUpdates]). Gibt es ein Paket doppelt, gewinnt die höhere Version.
 */
class PackCatalog(
    private val texts: suspend () -> List<String>,
    private val extra: suspend () -> List<String> = { emptyList() },
    private val previews: suspend () -> String? = { null },
) {
    private val mutex = Mutex()
    private var cache: List<Pack>? = null

    suspend fun all(): List<Pack> = mutex.withLock {
        cache ?: merge(parse(texts()) + runCatching { previews()?.let(PackJson::parseList) }.getOrNull().orEmpty().map { it.copy(preview = true) }, parse(extra()))
            .also { cache = it }
    }

    /** Nach dem Nachladen neuer Pakete: Liste neu aufbauen. */
    suspend fun reload(): List<Pack> { mutex.withLock { cache = null }; return all() }

    suspend fun byId(id: String): Pack? = all().firstOrNull { it.id == id }

    companion object {
        const val DIR = "packs"

        private fun parse(texts: List<String>) = texts.mapNotNull { runCatching { PackJson.parse(it) }.getOrNull() }

        /** Mitgelieferte + nachgeladene Pakete: je id das volle Paket vor der Vorschau, dann die höchste Version; sortiert nach Klasse und Titel. */
        fun merge(bundled: List<Pack>, downloaded: List<Pack>): List<Pack> =
            (bundled + downloaded).groupBy { it.id }.values.map { same -> same.maxWith(compareBy<Pack>({ !it.preview }, { it.version })) }
                .sortedWith(compareBy({ it.grades.minOrNull() ?: 99 }, { it.title }))
    }
}
