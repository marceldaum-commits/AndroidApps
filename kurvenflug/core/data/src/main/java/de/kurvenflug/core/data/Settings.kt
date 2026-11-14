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

    /** Kapitel-Erklärung schon gesehen? */
    fun introSeen(chapter: Int) = prefs.getBoolean("intro_$chapter", false)
    fun markIntroSeen(chapter: Int) = prefs.edit().putBoolean("intro_$chapter", true).apply()

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
