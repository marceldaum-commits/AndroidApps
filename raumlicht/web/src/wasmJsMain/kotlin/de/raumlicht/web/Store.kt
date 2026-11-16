package de.raumlicht.web

import de.raumlicht.core.model.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val P = "raumlicht."

/** Sterne je Aufgabe – nur im Browser (localStorage). Das Beste bleibt, wie in der Android-App. */
class WebStars {
    private fun load() = storeGet(P + "sterne").orEmpty().split(';').mapNotNull { it.split('=').takeIf { p -> p.size == 2 }?.let { p -> p[0] to (p[1].toIntOrNull() ?: 0) } }.toMap()
    private val _all = MutableStateFlow(load())
    val all: StateFlow<Map<String, Int>> = _all
    fun record(levelId: String, stars: Int, attempts: Int) {
        if (stars <= (_all.value[levelId] ?: 0)) return
        _all.value = _all.value + (levelId to stars); storeSet(P + "sterne", _all.value.entries.joinToString(";") { "${it.key}=${it.value}" })
    }
    fun clear() { _all.value = emptyMap(); storeRemove(P + "sterne") }
}

/** Einstellungen und Rekorde im Browser-Speicher – dieselbe Schnittstelle wie die App-Einstellungen unter Android. */
class WebPrefs : Prefs {
    private fun flag(key: String, default: Boolean) = MutableStateFlow(storeGet(P + key)?.let { it == "1" } ?: default)
    private fun int(key: String, default: Int = 0) = MutableStateFlow(storeGet(P + key)?.toIntOrNull() ?: default)
    private fun text(key: String, default: String = "") = MutableStateFlow(storeGet(P + key) ?: default)
    private fun put(key: String, v: Boolean) = storeSet(P + key, if (v) "1" else "0")

    private val _sound = flag("ton", true); private val _music = flag("musik", true); private val _vibration = flag("vibration", true); private val _voice = flag("stimme", true)
    override val sound: StateFlow<Boolean> = _sound; override fun setSound(v: Boolean) { put("ton", v); _sound.value = v }
    override val music: StateFlow<Boolean> = _music; override fun setMusic(v: Boolean) { put("musik", v); _music.value = v }
    override val vibration: StateFlow<Boolean> = _vibration; override fun setVibration(v: Boolean) { put("vibration", v); _vibration.value = v }
    override val voice: StateFlow<Boolean> = _voice; override fun setVoice(v: Boolean) { put("stimme", v); _voice.value = v }
    private val _volume = int("lautstaerke", 100)
    override val volume: StateFlow<Int> = _volume; override fun setVolume(v: Int) { val c = v.coerceIn(0, 100); storeSet(P + "lautstaerke", c.toString()); _volume.value = c }

    override fun introSeen(chapter: Int) = storeGet(P + "intro_$chapter") == "1"
    override fun markIntroSeen(chapter: Int) = storeSet(P + "intro_$chapter", "1")

    private fun best(flow: MutableStateFlow<Int>, key: String, v: Int): Boolean { if (v <= flow.value) return false; storeSet(P + key, v.toString()); flow.value = v; return true }
    private val _jam = int("jamBest"); private val _lage = int("lageBest"); private val _balloon = int("balloonBest")
    override val jamBest: StateFlow<Int> = _jam; override fun recordJam(streak: Int) = best(_jam, "jamBest", streak)
    override val lageBest: StateFlow<Int> = _lage; override fun recordLage(score: Int) = best(_lage, "lageBest", score)
    override val balloonBest: StateFlow<Int> = _balloon; override fun recordBalloons(points: Int) = best(_balloon, "balloonBest", points)
    private val _exams = MutableStateFlow(storeKeys(P + "exam_").split('\n').filter { it.isNotBlank() }.associate { it.removePrefix(P + "exam_") to (storeGet(it)?.toIntOrNull() ?: 0) })
    override val exams: StateFlow<Map<String, Int>> = _exams
    override fun recordExam(id: String, points: Int) { if (points <= (_exams.value[id] ?: 0)) return; storeSet(P + "exam_$id", points.toString()); _exams.value = _exams.value + (id to points) }

    private val _skin = text("skin", "pink")
    override val skin: StateFlow<String> = _skin; override fun setSkin(c: String) { storeSet(P + "skin", c); _skin.value = c }
    private val _look = MutableStateFlow((storeGet(P + "trail") ?: "none") to (storeGet(P + "hat") ?: "none"))
    override val look: StateFlow<Pair<String, String>> = _look
    override fun setLook(trail: String, hat: String) { storeSet(P + "trail", trail); storeSet(P + "hat", hat); _look.value = trail to hat }
    private val _diary = text("diary"); private val _plan = text("plan")
    override val diary: StateFlow<String> = _diary; override fun setDiary(s: String) { storeSet(P + "diary", s); _diary.value = s }
    override val plan: StateFlow<String> = _plan; override fun setPlan(s: String) { storeSet(P + "plan", s); _plan.value = s }

    override var show: String get() = storeGet(P + "show") ?: ""; set(v) = storeSet(P + "show", v)
    override var ride: String get() = storeGet(P + "ride") ?: ""; set(v) = storeSet(P + "ride", v)
    override var flight: String get() = storeGet(P + "flight") ?: ""; set(v) = storeSet(P + "flight", v)

    override fun resetProgress() {
        (storeKeys(P + "intro_") + "\n" + storeKeys(P + "exam_")).split('\n').filter { it.isNotBlank() }.forEach { storeRemove(it) }
        listOf("jamBest", "lageBest", "balloonBest", "diary", "skin", "trail", "hat").forEach { storeRemove(P + it) }
        _jam.value = 0; _lage.value = 0; _balloon.value = 0; _exams.value = emptyMap(); _diary.value = ""; _skin.value = "pink"; _look.value = "none" to "none"
    }
}
