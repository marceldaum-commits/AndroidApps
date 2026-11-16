package de.raumlicht.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Einstellungen – nur auf dem Gerät, keine persönlichen Daten. */
class Settings(ctx: Context) : de.raumlicht.core.model.Prefs {
    private val prefs = ctx.getSharedPreferences("raumlicht", Context.MODE_PRIVATE)

    private fun flag(key: String, default: Boolean) = MutableStateFlow(prefs.getBoolean(key, default))
    private val _sound = flag("sound", true)
    private val _music = flag("music", true)
    private val _vibration = flag("vibration", true)
    private val _autoUpdate = flag("autoUpdate", true)
    private val _voice = flag("voiceOn", false)   // Lux spricht nur, wenn man es einschaltet (seit 2.5.1; der alte Schlüssel „voice“ zählt nicht mehr)
    /** Lux liest ihre Sprechblasen laut vor */
    override val voice: StateFlow<Boolean> = _voice.asStateFlow()
    override fun setVoice(v: Boolean) { prefs.edit().putBoolean("voiceOn", v).apply(); _voice.value = v }
    override val sound: StateFlow<Boolean> = _sound.asStateFlow()
    override val music: StateFlow<Boolean> = _music.asStateFlow()
    override val vibration: StateFlow<Boolean> = _vibration.asStateFlow()
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()

    override fun setSound(v: Boolean) { prefs.edit().putBoolean("sound", v).apply(); _sound.value = v }
    override fun setMusic(v: Boolean) { prefs.edit().putBoolean("music", v).apply(); _music.value = v }
    override fun setVibration(v: Boolean) { prefs.edit().putBoolean("vibration", v).apply(); _vibration.value = v }
    fun setAutoUpdate(v: Boolean) { prefs.edit().putBoolean("autoUpdate", v).apply(); _autoUpdate.value = v }

    /** Lux' Stimme: gemerkter Name der Gerätestimme (leer = die beste) und ob sie heller klingen soll */
    var voiceName: String
        get() = prefs.getString("voiceName", "") ?: ""
        set(v) = prefs.edit().putString("voiceName", v).apply()
    /** Natürliche Stimme (geladenes Stimm-Paket) statt der Handy-Stimme benutzen */
    var voiceNatural: Boolean
        get() = prefs.getBoolean("voiceNatural", false)
        set(v) = prefs.edit().putBoolean("voiceNatural", v).apply()
    var voiceBright: Boolean
        get() = prefs.getBoolean("voiceBright", false)
        set(v) = prefs.edit().putBoolean("voiceBright", v).apply()

    /** Lautstärke von Musik und Geräuschen in Prozent (Standard: voll) */
    private val _volume = MutableStateFlow(prefs.getInt("volume", 100))
    override val volume: StateFlow<Int> = _volume.asStateFlow()
    override fun setVolume(v: Int) { val c = v.coerceIn(0, 100); prefs.edit().putInt("volume", c).apply(); _volume.value = c }

    /** Kapitel-Erklärung schon gesehen? */
    override fun introSeen(chapter: Int) = prefs.getBoolean("intro_$chapter", false)
    override fun markIntroSeen(chapter: Int) { prefs.edit().putBoolean("intro_$chapter", true).apply() }

    /** Endlos üben: längste Serie von Treffern beim ersten Versuch */
    private val _jamBest = MutableStateFlow(prefs.getInt("jamBest", 0))
    override val jamBest: StateFlow<Int> = _jamBest.asStateFlow()
    /** @return true bei neuem Rekord */
    override fun recordJam(streak: Int): Boolean {
        if (streak <= _jamBest.value) return false
        prefs.edit().putInt("jamBest", streak).apply(); _jamBest.value = streak
        return true
    }

    /** Ballerbude: bester Punktestand */
    private val _balloonBest = MutableStateFlow(prefs.getInt("balloonBest", 0))
    override val balloonBest: StateFlow<Int> = _balloonBest.asStateFlow()
    override fun recordBalloons(points: Int): Boolean {
        if (points <= _balloonBest.value) return false
        prefs.edit().putInt("balloonBest", points).apply(); _balloonBest.value = points
        return true
    }

    /** Lage-Check: beste Runde (richtige von 8) */
    private val _lageBest = MutableStateFlow(prefs.getInt("lageBest", 0))
    override val lageBest: StateFlow<Int> = _lageBest.asStateFlow()
    override fun recordLage(score: Int): Boolean {
        if (score <= _lageBest.value) return false
        prefs.edit().putInt("lageBest", score).apply(); _lageBest.value = score
        return true
    }

    /** Abi-Simulation: beste Punktzahl je Aufgabe */
    private val _exams = MutableStateFlow(prefs.all.filterKeys { it.startsWith("exam_") }.mapKeys { it.key.removePrefix("exam_") }.mapValues { (it.value as? Int) ?: 0 })
    override val exams: StateFlow<Map<String, Int>> = _exams.asStateFlow()
    override fun recordExam(id: String, points: Int) {
        if (points <= (_exams.value[id] ?: 0)) return
        prefs.edit().putInt("exam_$id", points).apply(); _exams.value = _exams.value + (id to points)
    }

    /** Werkstatt: Farbe von Lux */
    private val _skin = MutableStateFlow(prefs.getString("skin", "pink") ?: "pink")
    override val skin: StateFlow<String> = _skin.asStateFlow()
    override fun setSkin(c: String) { prefs.edit().putString("skin", c).apply(); _skin.value = c }

    /** Werkstatt: Leuchtspur und Aufsatz */
    private val _look = MutableStateFlow((prefs.getString("trail", "none") ?: "none") to (prefs.getString("hat", "none") ?: "none"))
    override val look: StateFlow<Pair<String, String>> = _look.asStateFlow()
    override fun setLook(trail: String, hat: String) { prefs.edit().putString("trail", trail).putString("hat", hat).apply(); _look.value = trail to hat }

    /** Fehler-Tagebuch: Zeilen „Anzahl⇥Aufgabe⇥Meldung“ */
    private val _diary = MutableStateFlow(prefs.getString("diary", "") ?: "")
    override val diary: StateFlow<String> = _diary.asStateFlow()
    override fun setDiary(s: String) { prefs.edit().putString("diary", s).apply(); _diary.value = s }

    /**
     * Neu anfangen: Rekorde, Abi-Punkte, Fehler-Tagebuch, gesehene Erklärungen und das freigespielte Aussehen von Lux zurücksetzen.
     * Ton-, Update-Einstellungen, Klausur-Plan und die gebaute Show bleiben.
     */
    override fun resetProgress() {
        val e = prefs.edit()
        prefs.all.keys.filter { it.startsWith("intro_") || it.startsWith("exam_") }.forEach { e.remove(it) }
        listOf("jamBest", "lageBest", "balloonBest", "diary", "skin", "trail", "hat").forEach { e.remove(it) }
        e.apply()
        _jamBest.value = 0; _lageBest.value = 0; _balloonBest.value = 0; _exams.value = emptyMap(); _diary.value = ""; _skin.value = "pink"; _look.value = "none" to "none"
    }

    /** Klausur-Planer: Termin und Kapitel (leer = kein Plan) */
    private val _plan = MutableStateFlow(prefs.getString("plan", "") ?: "")
    override val plan: StateFlow<String> = _plan.asStateFlow()
    override fun setPlan(s: String) { prefs.edit().putString("plan", s).apply(); _plan.value = s }

    /** Baukasten: die zuletzt gebaute Show als Code */
    override var show: String
        get() = prefs.getString("show", "") ?: ""
        set(v) = prefs.edit().putString("show", v).apply()

    /** Kurve abfahren: die zuletzt gebaute eigene Funktion („Term|von|bis“) */
    override var ride: String
        get() = prefs.getString("ride", "") ?: ""
        set(v) = prefs.edit().putString("ride", v).apply()

    /** Flug durch den Raum: der zuletzt gebaute eigene Flug */
    override var flight: String
        get() = prefs.getString("flight", "") ?: ""
        set(v) = prefs.edit().putString("flight", v).apply()

    /** Endlos üben: die zuletzt gewählte Schwierigkeit (leer = alle Stufen) */
    override var jamLevel: String
        get() = prefs.getString("jamLevel", "") ?: ""
        set(v) = prefs.edit().putString("jamLevel", v).apply()

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
