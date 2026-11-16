package de.experimentopia.web

import de.experimentopia.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val P = "experimentopia."
private val json = Json { ignoreUnknownKeys = true }

/** Fortschritt und Forscherbuch im Browser-Speicher (localStorage) – dieselben Regeln wie in der Android-App ([Progress]). */
class WebStore : LabStore {
    private fun <T> load(key: String, s: kotlinx.serialization.KSerializer<T>): List<T> =
        storeGet(P + key)?.let { runCatching { json.decodeFromString(ListSerializer(s), it) }.getOrNull() } ?: emptyList()
    private val _results = MutableStateFlow(load("ergebnisse", ResultRow.serializer()))
    private val _journal = MutableStateFlow(load("forscherbuch", NoteRow.serializer()))
    override val results: StateFlow<List<ResultRow>> = _results
    override val journal: StateFlow<List<NoteRow>> = _journal

    override suspend fun record(f: Finished) {
        val now = nowMs().toLong()
        val old = _results.value.firstOrNull { it.experimentId == f.id }
        _results.value = _results.value.filter { it.experimentId != f.id } + Progress.merge(old, f, now)
        _journal.value = listOf(Progress.note(f, now, (_journal.value.maxOfOrNull { it.id } ?: 0) + 1)) + _journal.value.take(499)
        storeSet(P + "ergebnisse", json.encodeToString(ListSerializer(ResultRow.serializer()), _results.value))
        storeSet(P + "forscherbuch", json.encodeToString(ListSerializer(NoteRow.serializer()), _journal.value))
    }

    override suspend fun reset() { _results.value = emptyList(); _journal.value = emptyList(); storeRemove(P + "ergebnisse"); storeRemove(P + "forscherbuch") }
}

/** Einstellungen im Browser-Speicher – dieselbe Schnittstelle wie unter Android. */
class WebPrefs : Prefs {
    private fun flag(key: String, default: Boolean) = MutableStateFlow(storeGet(P + key)?.let { it == "1" } ?: default)
    private fun put(key: String, v: Boolean) = storeSet(P + key, if (v) "1" else "0")
    private val _sound = flag("ton", true); private val _vibration = flag("vibration", true); private val _pro = flag("pro", false)
    override val sound: StateFlow<Boolean> = _sound; override fun setSound(v: Boolean) { put("ton", v); _sound.value = v }
    override val vibration: StateFlow<Boolean> = _vibration; override fun setVibration(v: Boolean) { put("vibration", v); _vibration.value = v }
    override val pro: StateFlow<Boolean> = _pro; override fun setPro(v: Boolean) { put("pro", v); _pro.value = v }
    private val _grade = MutableStateFlow(storeGet(P + "klasse")?.toIntOrNull() ?: 0)
    override val grade: StateFlow<Int> = _grade; override fun setGrade(g: Int) { storeSet(P + "klasse", g.toString()); _grade.value = g }
    override var welcomed: Boolean get() = storeGet(P + "begruesst") == "1"; set(v) = put("begruesst", v)
    private val _runs = MutableStateFlow(storeGet(P + "laborlaeufe")?.toIntOrNull() ?: 0)
    override val freeLabRuns: StateFlow<Int> = _runs
    override fun countFreeLab() { _runs.value += 1; storeSet(P + "laborlaeufe", _runs.value.toString()) }
    override var badgesSeen: Set<String>
        get() = storeGet(P + "abzeichen").orEmpty().split(',').filter { it.isNotBlank() }.toSet()
        set(v) = storeSet(P + "abzeichen", v.joinToString(","))
    override fun resetProgress() { storeRemove(P + "laborlaeufe"); storeRemove(P + "abzeichen"); _runs.value = 0 }
}
