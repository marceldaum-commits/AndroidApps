package de.kurvenflug.app

import de.kurvenflug.core.data.LevelProgress
import de.kurvenflug.core.data.Play
import de.kurvenflug.core.data.Prefs
import de.kurvenflug.core.data.ProgressStore
import de.kurvenflug.core.model.nowMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/** Einstellungen im lokalen Speicher des Browsers (Vorsilbe „kf.“). Nichts verlässt das Gerät. */
class WebPrefs(private val prefix: String = "kf.") : Prefs {
    private fun raw(key: String) = jsStoreGet(prefix + key)
    override fun getBoolean(key: String, default: Boolean) = raw(key)?.toBooleanStrictOrNull() ?: default
    override fun getInt(key: String, default: Int) = raw(key)?.toIntOrNull() ?: default
    override fun getLong(key: String, default: Long) = raw(key)?.toLongOrNull() ?: default
    override fun getString(key: String, default: String?) = raw(key) ?: default
    override fun getStringSet(key: String, default: Set<String>) = raw(key)?.let { s -> if (s.isEmpty()) emptySet() else s.split('\n').toSet() } ?: default
    override fun keys() = jsStoreKeys().split('\n').filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }.toSet()
    override fun edit(): Prefs.Editor = object : Prefs.Editor {
        val ops = mutableListOf<() -> Unit>()
        fun put(k: String, v: String) = apply { ops += { jsStoreSet(prefix + k, v) } }
        override fun putBoolean(key: String, v: Boolean) = put(key, v.toString())
        override fun putInt(key: String, v: Int) = put(key, v.toString())
        override fun putLong(key: String, v: Long) = put(key, v.toString())
        override fun putString(key: String, v: String?) = if (v == null) remove(key) else put(key, v)
        override fun putStringSet(key: String, v: Set<String>) = put(key, v.joinToString("\n"))
        override fun remove(key: String) = apply { ops += { jsStoreRemove(prefix + key) } }
        override fun apply() = ops.forEach { it() }
    }
}

/**
 * Spielstand im Browser – gleiche Regeln wie die Android-Datenbank: das Beste bleibt, Zellen bleiben gesammelt.
 * Je Spieler eine Zeile pro Level; das Eltern-Protokoll behält die letzten 60 Tage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebProgressStore(private val profile: StateFlow<Int>, private val prefix: String = "kf.") : ProgressStore {
    private val progress = HashMap<Int, MutableStateFlow<List<LevelProgress>>>()
    private val playLog = HashMap<Int, MutableStateFlow<List<Play>>>()

    private fun progressOf(p: Int) = progress.getOrPut(p) {
        MutableStateFlow(jsStoreGet("${prefix}progress.$p").orEmpty().lines().mapNotNull { l ->
            val f = l.split('|'); if (f.size < 5) null
            else LevelProgress(p, f[0], f[1].toIntOrNull() ?: 0, f[2].toIntOrNull() ?: 0, f[3].toIntOrNull() ?: 0, f[4].toLongOrNull() ?: 0)
        })
    }
    private fun playsOf(p: Int) = playLog.getOrPut(p) {
        MutableStateFlow(jsStoreGet("${prefix}plays.$p").orEmpty().lines().mapNotNull { l ->
            val f = l.split('|'); if (f.size < 5) null
            else Play(0, p, f[0], f[1].toIntOrNull() ?: 0, f[2].toIntOrNull() ?: 0, f[3].toIntOrNull() ?: 0, f[4].toLongOrNull() ?: 0)
        })
    }
    private fun save(p: Int) {
        jsStoreSet("${prefix}progress.$p", progressOf(p).value.joinToString("\n") { "${it.levelId}|${it.stars}|${it.cellMask}|${it.attempts}|${it.solvedAt}" })
        jsStoreSet("${prefix}plays.$p", playsOf(p).value.joinToString("\n") { "${it.levelId}|${it.stars}|${it.attempts}|${it.seconds}|${it.at}" })
    }

    override val all: Flow<List<LevelProgress>> = profile.flatMapLatest { progressOf(it) }
    override fun plays(since: Long): Flow<List<Play>> = profile.flatMapLatest { p -> playsOf(p).map { l -> l.filter { it.at >= since }.sortedBy { it.at } } }

    private fun log(p: Int, play: Play) {
        val keep = nowMillis() - 60L * 86_400_000L
        playsOf(p).value = (playsOf(p).value + play).filter { it.at >= keep }
    }

    override suspend fun record(levelId: String, stars: Int, cells: Set<Int>, attempts: Int, seconds: Int) {
        val p = profile.value; val now = nowMillis()
        val list = progressOf(p).value
        val old = list.firstOrNull { it.levelId == levelId }
        val mask = cells.fold(old?.cellMask ?: 0) { m, i -> m or (1 shl i) }
        val new = LevelProgress(p, levelId, maxOf(stars, old?.stars ?: 0), mask, (old?.attempts ?: 0) + attempts, old?.solvedAt?.takeIf { it > 0 } ?: now)
        progressOf(p).value = list.filter { it.levelId != levelId } + new
        log(p, Play(0, p, levelId, stars, attempts, seconds, now))
        save(p)
    }

    override suspend fun upgrade(levelId: String, stars: Int) {
        val p = profile.value
        val list = progressOf(p).value
        val old = list.firstOrNull { it.levelId == levelId } ?: return
        if (stars <= old.stars) return
        progressOf(p).value = list.map { if (it.levelId == levelId) it.copy(stars = stars) else it }
        save(p)
    }

    override suspend fun stuck(levelId: String, attempts: Int, seconds: Int) {
        val p = profile.value
        log(p, Play(0, p, levelId, 0, attempts, seconds, nowMillis())); save(p)
    }

    override suspend fun clear() = clear(profile.value)
    override suspend fun clear(profileId: Int) {
        progressOf(profileId).value = emptyList(); playsOf(profileId).value = emptyList()
        jsStoreRemove("${prefix}progress.$profileId"); jsStoreRemove("${prefix}plays.$profileId")
    }
}
