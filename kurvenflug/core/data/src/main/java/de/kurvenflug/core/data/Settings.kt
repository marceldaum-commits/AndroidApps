package de.kurvenflug.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Einstellungen – nur auf dem Gerät, keine persönlichen Daten. */
class Settings(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("kurvenflug", Context.MODE_PRIVATE)

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

    /** Klassenstufe 5–10 (null = alle Kapitel zeigen); gefragt wird nur einmal, danach über 🎓 änderbar */
    private val _grade = MutableStateFlow(prefs.getInt("grade", 0).takeIf { it in 5..10 })
    private val _gradeAsked = flag("gradeAsked", false)
    val grade: StateFlow<Int?> = _grade.asStateFlow()
    val gradeAsked: StateFlow<Boolean> = _gradeAsked.asStateFlow()
    fun setGrade(g: Int?) {
        prefs.edit().putInt("grade", g ?: 0).putBoolean("gradeAsked", true).apply()
        _grade.value = g; _gradeAsked.value = true
    }

    /** Kapitel-Erklärung schon gesehen? */
    fun introSeen(chapter: Int) = prefs.getBoolean("intro_$chapter", false)
    fun markIntroSeen(chapter: Int) = prefs.edit().putBoolean("intro_$chapter", true).apply()
    /** Wie oft der Rechenweg für eine Bahnart schon geöffnet wurde (beim ersten Mal rechnet Volt einen Schritt vor) */
    fun walkthroughsSeen(kind: String) = prefs.getInt("walk_$kind", 0)
    fun markWalkthrough(kind: String) = prefs.edit().putInt("walk_$kind", walkthroughsSeen(kind) + 1).apply()

    /** Werkstatt: Form, Spur, Volts Hut (Indizes in Workshop.*) */
    private val _look = MutableStateFlow(Triple(prefs.getInt("shape", 0), prefs.getInt("trail", 0), prefs.getInt("hat", 0)))
    val look: StateFlow<Triple<Int, Int, Int>> = _look.asStateFlow()
    fun setLook(shape: Int = _look.value.first, trail: Int = _look.value.second, hat: Int = _look.value.third) {
        _look.value = Triple(shape, trail, hat)
        prefs.edit().putInt("shape", shape).putInt("trail", trail).putInt("hat", hat).apply()
    }

    /** Gewählte Sonden-Farbe */
    var skin: Int
        get() = prefs.getInt("skin", 0)
        set(v) = prefs.edit().putInt("skin", v).apply()

    private val _stormBest = MutableStateFlow(prefs.getInt("stormBest", 0))
    private val _duelWins = MutableStateFlow(prefs.getInt("duelWins", 0))
    private val _duelsPlayed = MutableStateFlow(prefs.getInt("duelsPlayed", 0))
    /** Rekord im Asteroidensturm (Wellen) */
    val stormBest: StateFlow<Int> = _stormBest.asStateFlow()
    val duelWins: StateFlow<Int> = _duelWins.asStateFlow()
    val duelsPlayed: StateFlow<Int> = _duelsPlayed.asStateFlow()

    /** @return true bei neuem Rekord */
    fun recordStorm(waves: Int): Boolean {
        if (waves <= _stormBest.value) return false
        prefs.edit().putInt("stormBest", waves).apply(); _stormBest.value = waves
        return true
    }
    fun recordDuel(won: Boolean) {
        _duelsPlayed.value += 1; if (won) _duelWins.value += 1
        prefs.edit().putInt("duelsPlayed", _duelsPlayed.value).putInt("duelWins", _duelWins.value).apply()
    }

    /** Schon gefeierte Abzeichen (damit „Neu!“ nur einmal erscheint) */
    var badgesSeen: Set<String>
        get() = prefs.getStringSet("badgesSeen", emptySet()).orEmpty()
        set(v) = prefs.edit().putStringSet("badgesSeen", v).apply()

    /** Eigene Level aus dem Baukasten (als Teil-Codes, neueste zuerst) */
    private val _myLevels = MutableStateFlow(prefs.getString("myLevels", "").orEmpty().lines().filter { it.isNotBlank() })
    val myLevels: StateFlow<List<String>> = _myLevels.asStateFlow()
    fun saveMyLevel(code: String, replace: String? = null) {
        val l = _myLevels.value.filter { it != code && it != replace }
        setMyLevels(listOf(code) + l)
    }
    fun deleteMyLevel(code: String) = setMyLevels(_myLevels.value - code)
    private fun setMyLevels(l: List<String>) { _myLevels.value = l.take(50); prefs.edit().putString("myLevels", _myLevels.value.joinToString("\n")).apply() }

    /** Rekord im Formel-Detektiv (richtige Fälle von 8) */
    var detectiveBest: Int
        get() = prefs.getInt("detectiveBest", 0)
        set(v) = prefs.edit().putInt("detectiveBest", v).apply()

    var lastUpdateCheck: Long
        get() = prefs.getLong("lastUpdateCheck", 0)
        set(v) = prefs.edit().putLong("lastUpdateCheck", v).apply()
}

/** Alles, was die App braucht, an einer Stelle. */
class AppContainer(ctx: Context) {
    val db = FlightDatabase.create(ctx)
    val progress = ProgressRepository(db.progress())
    val settings = Settings(ctx)
}
