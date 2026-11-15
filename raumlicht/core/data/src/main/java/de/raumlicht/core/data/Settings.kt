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

    /** Jam-Session: längste Serie von Treffern beim ersten Versuch */
    private val _jamBest = MutableStateFlow(prefs.getInt("jamBest", 0))
    val jamBest: StateFlow<Int> = _jamBest.asStateFlow()
    /** @return true bei neuem Rekord */
    fun recordJam(streak: Int): Boolean {
        if (streak <= _jamBest.value) return false
        prefs.edit().putInt("jamBest", streak).apply(); _jamBest.value = streak
        return true
    }

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
