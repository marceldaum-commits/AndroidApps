package de.wissensgarten.core.data

import android.content.Context

/**
 * Merkt sich gelöschte Profile, Beete und Karten, damit der Geräte-Sync sie nicht vom anderen Gerät
 * zurückholt. Nur IDs, keine Inhalte.
 */
class Tombstones(context: Context) {
    private val prefs = context.getSharedPreferences("sync_geloescht", Context.MODE_PRIVATE)
    fun all(): Set<String> = prefs.getStringSet("ids", emptySet())!!
    fun add(ids: Collection<String>) { if (ids.isNotEmpty()) prefs.edit().putStringSet("ids", all() + ids).apply() }
    fun set(ids: Set<String>) { prefs.edit().putStringSet("ids", ids).apply() }
}
