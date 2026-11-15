package de.raumlicht.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Einstellungen – nur auf dem Gerät, keine persönlichen Daten. */
class Settings(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("raumlicht", Context.MODE_PRIVATE)

    private fun flag(key: String, default: Boolean) = MutableStateFlow(prefs.getBoolean(key, default))
    private val _sound = flag("sound", true)
    private val _music = flag("music", true)
    private val _vibration = flag("vibration", true)
    private val _autoUpdate = flag("autoUpdate", true)
    val sound: StateFlow<Boolean> = _sound.asStateFlow()
    val music: StateFlow<Boolean> = _music.asStateFlow()
    val vibration: StateFlow<Boolean> = _vibration.asStateFlow()
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()

    fun setSound(v: Boolean) { prefs.edit().putBoolean("sound", v).apply(); _sound.value = v }
    fun setMusic(v: Boolean) { prefs.edit().putBoolean("music", v).apply(); _music.value = v }
    fun setVibration(v: Boolean) { prefs.edit().putBoolean("vibration", v).apply(); _vibration.value = v }
    fun setAutoUpdate(v: Boolean) { prefs.edit().putBoolean("autoUpdate", v).apply(); _autoUpdate.value = v }

    /** Kapitel-Erklärung schon gesehen? */
    fun introSeen(chapter: Int) = prefs.getBoolean("intro_$chapter", false)
    fun markIntroSeen(chapter: Int) = prefs.edit().putBoolean("intro_$chapter", true).apply()

    /** Endlos üben: längste Serie von Treffern beim ersten Versuch */
    private val _jamBest = MutableStateFlow(prefs.getInt("jamBest", 0))
    val jamBest: StateFlow<Int> = _jamBest.asStateFlow()
    /** @return true bei neuem Rekord */
    fun recordJam(streak: Int): Boolean {
        if (streak <= _jamBest.value) return false
        prefs.edit().putInt("jamBest", streak).apply(); _jamBest.value = streak
        return true
    }

    /** Lage-Check: beste Runde (richtige von 8) */
    private val _lageBest = MutableStateFlow(prefs.getInt("lageBest", 0))
    val lageBest: StateFlow<Int> = _lageBest.asStateFlow()
    fun recordLage(score: Int): Boolean {
        if (score <= _lageBest.value) return false
        prefs.edit().putInt("lageBest", score).apply(); _lageBest.value = score
        return true
    }

    /** Abi-Simulation: beste Punktzahl je Aufgabe */
    private val _exams = MutableStateFlow(prefs.all.filterKeys { it.startsWith("exam_") }.mapKeys { it.key.removePrefix("exam_") }.mapValues { (it.value as? Int) ?: 0 })
    val exams: StateFlow<Map<String, Int>> = _exams.asStateFlow()
    fun recordExam(id: String, points: Int) {
        if (points <= (_exams.value[id] ?: 0)) return
        prefs.edit().putInt("exam_$id", points).apply(); _exams.value = _exams.value + (id to points)
    }

    /** Werkstatt: Farbe von Lux */
    private val _skin = MutableStateFlow(prefs.getString("skin", "pink") ?: "pink")
    val skin: StateFlow<String> = _skin.asStateFlow()
    fun setSkin(c: String) { prefs.edit().putString("skin", c).apply(); _skin.value = c }

    /** Werkstatt: Leuchtspur und Aufsatz */
    private val _look = MutableStateFlow((prefs.getString("trail", "none") ?: "none") to (prefs.getString("hat", "none") ?: "none"))
    val look: StateFlow<Pair<String, String>> = _look.asStateFlow()
    fun setLook(trail: String = _look.value.first, hat: String = _look.value.second) { prefs.edit().putString("trail", trail).putString("hat", hat).apply(); _look.value = trail to hat }

    /** Fehler-Tagebuch: Zeilen „Anzahl⇥Aufgabe⇥Meldung“ */
    private val _diary = MutableStateFlow(prefs.getString("diary", "") ?: "")
    val diary: StateFlow<String> = _diary.asStateFlow()
    fun setDiary(s: String) { prefs.edit().putString("diary", s).apply(); _diary.value = s }

    /** Klausur-Planer: Termin und Kapitel (leer = kein Plan) */
    private val _plan = MutableStateFlow(prefs.getString("plan", "") ?: "")
    val plan: StateFlow<String> = _plan.asStateFlow()
    fun setPlan(s: String) { prefs.edit().putString("plan", s).apply(); _plan.value = s }

    /** Baukasten: die zuletzt gebaute Show als Code */
    var show: String
        get() = prefs.getString("show", "") ?: ""
        set(v) = prefs.edit().putString("show", v).apply()

    var lastUpdateCheck: Long
        get() = prefs.getLong("lastUpdateCheck", 0)
        set(v) = prefs.edit().putLong("lastUpdateCheck", v).apply()
}

/** Alles, was die App braucht, an einer Stelle. */
class AppContainer(ctx: Context) {
    val db = StageDatabase.create(ctx)
    val progress = ProgressRepository(db.progress())
    val settings = Settings(ctx)
}
