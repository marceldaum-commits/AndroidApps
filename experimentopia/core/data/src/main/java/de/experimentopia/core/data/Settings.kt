package de.experimentopia.core.data

import android.content.Context
import de.experimentopia.core.model.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Einstellungen – nur auf dem Gerät, keine persönlichen Daten. */
class Settings(ctx: Context) : Prefs {
    private val prefs = ctx.getSharedPreferences("experimentopia", Context.MODE_PRIVATE)

    private fun flag(key: String, default: Boolean) = MutableStateFlow(prefs.getBoolean(key, default))
    private val _sound = flag("sound", true)
    private val _vibration = flag("vibration", true)
    private val _autoUpdate = flag("autoUpdate", true)
    private val _pro = flag("pro", false)
    override val sound: StateFlow<Boolean> = _sound.asStateFlow()
    override val vibration: StateFlow<Boolean> = _vibration.asStateFlow()
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()
    /** Forscher-Pass gekauft? Wirkt erst, wenn Pro eingeschaltet ist (Pro.ENABLED). */
    override val pro: StateFlow<Boolean> = _pro.asStateFlow()

    override fun setSound(v: Boolean) { prefs.edit().putBoolean("sound", v).apply(); _sound.value = v }
    override fun setVibration(v: Boolean) { prefs.edit().putBoolean("vibration", v).apply(); _vibration.value = v }

    /** Musik, Effekt-Lautstärke und Vorlesen – als eine Zeile gespeichert (gleiches Format wie im Web) */
    private val _audio = MutableStateFlow(de.experimentopia.core.model.AudioPrefs.decode(prefs.getString("audio", null)))
    override val audio: StateFlow<de.experimentopia.core.model.AudioPrefs> = _audio.asStateFlow()
    override fun setAudio(a: de.experimentopia.core.model.AudioPrefs) { prefs.edit().putString("audio", a.encode()).apply(); _audio.value = a }
    fun setAutoUpdate(v: Boolean) { prefs.edit().putBoolean("autoUpdate", v).apply(); _autoUpdate.value = v }
    override fun setPro(v: Boolean) { prefs.edit().putBoolean("pro", v).apply(); _pro.value = v }
    private val _proCode = MutableStateFlow(prefs.getString("proCode", null))
    override val proCode: StateFlow<String?> = _proCode.asStateFlow()
    override fun setProCode(c: String?) { prefs.edit().putString("proCode", c).apply(); _proCode.value = c }

    /** Klassenstufe für den Filter (0 = alle) */
    private val _grade = MutableStateFlow(prefs.getInt("grade", 0))
    override val grade: StateFlow<Int> = _grade.asStateFlow()
    override fun setGrade(g: Int) { prefs.edit().putInt("grade", g).apply(); _grade.value = g }

    /** Blubbs Begrüßung schon gesehen? */
    override var welcomed: Boolean
        get() = prefs.getBoolean("welcomed", false)
        set(v) = prefs.edit().putBoolean("welcomed", v).apply()

    private val _freeLabRuns = MutableStateFlow(prefs.getInt("freeLabRuns", 0))
    override val freeLabRuns: StateFlow<Int> = _freeLabRuns.asStateFlow()
    override fun countFreeLab() { _freeLabRuns.value += 1; prefs.edit().putInt("freeLabRuns", _freeLabRuns.value).apply() }

    override var badgesSeen: Set<String>
        get() = prefs.getStringSet("badgesSeen", emptySet()).orEmpty()
        set(v) = prefs.edit().putStringSet("badgesSeen", v).apply()

    /** Zuletzt gefundenes Update (Version, APK-Adresse) – der Hinweis bleibt, bis es installiert ist */
    var foundUpdate: Pair<String, String>?
        get() = prefs.getString("foundUpdate", null)?.split('\t')?.takeIf { it.size == 2 }?.let { it[0] to it[1] }
        set(v) = prefs.edit().putString("foundUpdate", v?.let { "${it.first}\t${it.second}" }).apply()

    var lastUpdateCheck: Long
        get() = prefs.getLong("lastUpdateCheck", 0)
        set(v) = prefs.edit().putLong("lastUpdateCheck", v).apply()

    override fun resetProgress() { prefs.edit().remove("freeLabRuns").remove("badgesSeen").apply(); _freeLabRuns.value = 0 }
}

/** Alles, was die App braucht, an einer Stelle. */
class AppContainer(ctx: Context) {
    val db = LabDatabase.create(ctx)
    val progress = ProgressRepository(db.results())
    val settings = Settings(ctx)
}
