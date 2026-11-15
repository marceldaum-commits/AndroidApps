package de.wissensgarten.core.data


/**
 * Merkt sich gelöschte Profile, Beete und Karten, damit der Geräte-Sync sie nicht vom anderen Gerät
 * zurückholt. Nur IDs, keine Inhalte.
 */
class Tombstones(private val prefs: KeyValueStore) {
    fun all(): Set<String> = prefs.getStringSet("ids", emptySet())!!
    fun add(ids: Collection<String>) { if (ids.isNotEmpty()) prefs.edit().putStringSet("ids", all() + ids).apply() }
    fun set(ids: Set<String>) { prefs.edit().putStringSet("ids", ids).apply() }
}
