package de.wissensgarten.core.data

import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.PackJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Neue oder verbesserte Wissenspakete nachladen – **nur Daten** (JSON mit Fragen, Texten und Emoji-Merkbildern),
 * niemals ausführbarer Code. Quelle ist die freigegebene WebApp-Fassung (`packs/versions.json` + `packs/<name>.json`).
 *
 * @param get lädt eine Datei (relativer Name) und liefert ihren Text oder null
 * @param save legt ein geprüftes Paket im lokalen Speicher ab
 */
class PackUpdates(
    private val get: suspend (String) -> String?,
    private val save: suspend (String, String) -> Unit,
) {
    /** Lädt alles, was neuer ist als [known] (id → Version). Liefert die Zahl neuer/aktualisierter Pakete. */
    suspend fun fetch(known: Map<String, Int>): Int {
        val versions = parseVersions(get(VERSIONS) ?: return 0) ?: return 0
        var n = 0
        for ((file, version) in versions) {
            val id = file.removeSuffix(".json")
            if (version <= (known[id] ?: 0)) continue
            val text = get(file) ?: continue
            validate(text, id, version) ?: continue
            save(file, text)
            n++
        }
        return n
    }

    companion object {
        const val VERSIONS = "versions.json"
        const val MAX_BYTES = 1_000_000
        private val NAME = Regex("^[a-z0-9][a-z0-9-]{0,60}\\.json$")

        /** `{"gs-tiere.json": 3, …}` → nur gültige Dateinamen, höchstens 300 Einträge. */
        fun parseVersions(text: String): Map<String, Int>? = runCatching {
            Json.parseToJsonElement(text).jsonObject.entries.take(300)
                .filter { NAME.matches(it.key) && it.key != VERSIONS }
                .associate { it.key to it.value.jsonPrimitive.int }
        }.getOrNull()

        /** Nur annehmen, was ein gültiges Paket mit passender id/Version und Karten ist. */
        fun validate(text: String, id: String, version: Int): Pack? {
            if (text.length > MAX_BYTES) return null
            val p = runCatching { PackJson.parse(text) }.getOrNull() ?: return null
            return p.takeIf { it.id == id && it.version == version && it.cards.isNotEmpty() }
        }
    }
}
