package de.wissensgarten.core.data

import de.wissensgarten.core.model.Duel
import de.wissensgarten.core.model.Duels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Letzte Duelle auf diesem Gerät (nur lokal gespeichert). */
class DuelHistory(private val prefs: KeyValueStore) {
    private val _duels = MutableStateFlow(load())
    val duels: StateFlow<List<Duel>> = _duels.asStateFlow()

    private fun load(): List<Duel> =
        prefs.getString(KEY, "").orEmpty().split('\n').filter { it.isNotBlank() }.mapNotNull { Duels.decode(it) }

    /** Speichert den neuesten Stand eines Duells (gleiche Zufallszahl + Paket = gleiches Duell). */
    fun save(d: Duel) {
        val list = listOf(d) + _duels.value.filterNot { it.seed == d.seed && it.packId == d.packId }
        val trimmed = list.take(20)
        prefs.edit().putString(KEY, trimmed.joinToString("\n") { Duels.encode(it) }).apply()
        _duels.value = trimmed
    }

    /** Tautropfen gibt es pro Profil nur beim ersten Spielen eines Duells. */
    fun claimReward(profileId: String, d: Duel): Boolean {
        val key = "belohnt_${profileId}_${d.packId}_${d.seed}"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    private companion object { const val KEY = "liste" }
}
