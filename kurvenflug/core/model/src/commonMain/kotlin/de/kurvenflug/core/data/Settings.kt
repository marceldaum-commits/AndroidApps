package de.kurvenflug.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ein Spieler auf diesem Gerät (z. B. Geschwister). Nur Spitzname und Bild – keine echten Daten nötig. */
data class Profile(val id: Int, val name: String, val avatar: String)

/**
 * Einstellungen – nur auf dem Gerät, keine persönlichen Daten.
 *
 * Gemeinsam für alle Spieler: Ton, Musik, Vibration, Volt-Stimme, Updates, selbst gebaute Level.
 * Je Spieler (Profil): Klasse, Rekorde, Abzeichen, Story, Sticker, Aussehen, gesehene Erklärungen.
 * Profil 0 nutzt die alten Schlüssel ohne Vorsilbe – so bleibt ein bestehender Spielstand unverändert erhalten.
 */
class Settings(private val prefs: Prefs) {

    private fun flag(key: String, default: Boolean) = MutableStateFlow(prefs.getBoolean(key, default))
    private val _sound = flag("sound", true)
    private val _music = flag("music", true)
    private val _vibration = flag("vibration", true)
    private val _autoUpdate = flag("autoUpdate", true)
    private val _voltVoice = flag("voltVoice", true)
    val sound: StateFlow<Boolean> = _sound.asStateFlow()
    val music: StateFlow<Boolean> = _music.asStateFlow()
    val vibration: StateFlow<Boolean> = _vibration.asStateFlow()
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()
    val voltVoice: StateFlow<Boolean> = _voltVoice.asStateFlow()
    private val _musicVolume = MutableStateFlow(prefs.getInt("musicVolume", 70))
    private val _sfxVolume = MutableStateFlow(prefs.getInt("sfxVolume", 90))
    /** Lautstärke Musik bzw. Effekte in Prozent (0–100) */
    val musicVolume: StateFlow<Int> = _musicVolume.asStateFlow()
    val sfxVolume: StateFlow<Int> = _sfxVolume.asStateFlow()
    fun setMusicVolume(v: Int) { val c = v.coerceIn(0, 100); prefs.edit().putInt("musicVolume", c).apply(); _musicVolume.value = c }
    fun setSfxVolume(v: Int) { val c = v.coerceIn(0, 100); prefs.edit().putInt("sfxVolume", c).apply(); _sfxVolume.value = c }
    fun setSound(v: Boolean) { prefs.edit().putBoolean("sound", v).apply(); _sound.value = v }
    fun setMusic(v: Boolean) { prefs.edit().putBoolean("music", v).apply(); _music.value = v }
    fun setVibration(v: Boolean) { prefs.edit().putBoolean("vibration", v).apply(); _vibration.value = v }
    fun setAutoUpdate(v: Boolean) { prefs.edit().putBoolean("autoUpdate", v).apply(); _autoUpdate.value = v }
    fun setVoltVoice(v: Boolean) { prefs.edit().putBoolean("voltVoice", v).apply(); _voltVoice.value = v }

    // ------------------------------------------------------------ Elternbereich (für das ganze Gerät)
    private val _anonCount = flag("anonCount", false)
    /** Anonyme Nutzungszählung – nur wenn Eltern sie einschalten (aus = Standard, datensparsam) */
    val anonCount: StateFlow<Boolean> = _anonCount.asStateFlow()
    fun setAnonCount(v: Boolean) { prefs.edit().putBoolean("anonCount", v).apply(); _anonCount.value = v }
    private val _tutorHintOff = flag("tutorHintOff", false)
    /** Nachhilfe-Hinweis im Elternbereich ausgeblendet */
    val tutorHintOff: StateFlow<Boolean> = _tutorHintOff.asStateFlow()
    fun setTutorHintOff(v: Boolean) { prefs.edit().putBoolean("tutorHintOff", v).apply(); _tutorHintOff.value = v }
    /** Tag (epochDay), an dem der Nachhilfe-Hinweis zuletzt zu sehen war (Häufigkeitsgrenze) */
    var tutorHintShown: Long
        get() = prefs.getLong("tutorHintShown", 0)
        set(v) = prefs.edit().putLong("tutorHintShown", v).apply()
    private val _examUntil = MutableStateFlow(prefs.getLong("examUntil", 0))
    /** „Klassenarbeit bald“ gilt bis zu diesem Tag (epochDay), 0 = nicht gesetzt */
    val examUntil: StateFlow<Long> = _examUntil.asStateFlow()
    fun setExamUntil(v: Long) { prefs.edit().putLong("examUntil", v).apply(); _examUntil.value = v }

    // ------------------------------------------------------------ Vorlesen (für das ganze Gerät)
    private val _readAloud = flag("readAloud", true)
    /** Volt liest Sprechblasen, Aufträge, Rechen-Stern-Fragen und Erklärungen vor */
    val readAloud: StateFlow<Boolean> = _readAloud.asStateFlow()
    fun setReadAloud(v: Boolean) { prefs.edit().putBoolean("readAloud", v).apply(); _readAloud.value = v }
    private val _offlineBest = flag("offlineBest", false)
    /** Offline-Stimme in Bestqualität laden (gemerkte Wahl) */
    val offlineBest: StateFlow<Boolean> = _offlineBest.asStateFlow()
    fun setOfflineBest(v: Boolean) { prefs.edit().putBoolean("offlineBest", v).apply(); _offlineBest.value = v }
    private val _readSpeed = MutableStateFlow(prefs.getInt("readSpeed", 100))
    /** Sprechtempo in Prozent, eine der Stufen aus [de.kurvenflug.core.model.Nova.SPEEDS] (100 = Server-Tempo) */
    val readSpeed: StateFlow<Int> = _readSpeed.asStateFlow()
    fun setReadSpeed(v: Int) { val c = de.kurvenflug.core.model.Nova.nearestSpeed(v); prefs.edit().putInt("readSpeed", c).apply(); _readSpeed.value = c }

    var lastUpdateCheck: Long
        get() = prefs.getLong("lastUpdateCheck", 0)
        set(v) = prefs.edit().putLong("lastUpdateCheck", v).apply()

    /** Tage seit dem letzten Öffnen (für Volts „Da bist du ja wieder!“) – merkt sich gleich das heutige Öffnen. */
    fun daysSinceLastOpen(now: Long = de.kurvenflug.core.model.nowMillis()): Int {
        val last = prefs.getLong(k("lastOpen"), 0L)
        prefs.edit().putLong(k("lastOpen"), now).apply()
        return if (last == 0L) 0 else ((now - last) / 86_400_000L).toInt()
    }

    /** Eigene Level aus dem Baukasten (als Teil-Codes, neueste zuerst) – für alle Spieler gemeinsam */
    // ------------------------------------------------------------ Kurvenflug Pro (für das ganze Gerät, nicht je Spieler)
    private val _proCode = MutableStateFlow(prefs.getString("proCode", "").orEmpty())
    /** Freischalt-Code von der Website (leer = keiner) */
    val proCode: StateFlow<String> = _proCode.asStateFlow()
    fun setProCode(code: String) { prefs.edit().putString("proCode", code).apply(); _proCode.value = code }
    private val _proOffered = flag("proOffered", false)
    /** Ein Kaufweg wurde schon einmal angeboten (Play: Produkt vorhanden) – dann gelten die Pro-Sperren auch offline */
    val proOffered: StateFlow<Boolean> = _proOffered.asStateFlow()
    fun markProOffered() { if (!_proOffered.value) { prefs.edit().putBoolean("proOffered", true).apply(); _proOffered.value = true } }

    private val _myLevels = MutableStateFlow(prefs.getString("myLevels", "").orEmpty().lines().filter { it.isNotBlank() })
    val myLevels: StateFlow<List<String>> = _myLevels.asStateFlow()
    fun saveMyLevel(code: String, replace: String? = null) {
        val l = _myLevels.value.filter { it != code && it != replace }
        setMyLevels(listOf(code) + l)
    }
    fun deleteMyLevel(code: String) = setMyLevels(_myLevels.value - code)
    private fun setMyLevels(l: List<String>) { _myLevels.value = l.take(50); prefs.edit().putString("myLevels", _myLevels.value.joinToString("\n")).apply() }

    // ------------------------------------------------------------------ Profile

    companion object {
        const val MAX_PROFILES = 6
        val AVATARS = listOf("🚀", "🦁", "🦊", "🐱", "🐼", "🦖", "🐙", "🤖")
    }

    private fun readProfiles(): List<Profile> = prefs.getString("profiles", null)?.lines()?.mapNotNull { line ->
        val p = line.split('|', limit = 3)
        p.getOrNull(0)?.toIntOrNull()?.let { Profile(it, p.getOrElse(2) { "Spieler" }, p.getOrElse(1) { AVATARS[0] }) }
    }?.takeIf { it.isNotEmpty() } ?: listOf(Profile(0, "Spieler 1", AVATARS[0]))

    private val _profiles = MutableStateFlow(readProfiles())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()
    private val _profile = MutableStateFlow(prefs.getInt("profile", 0).takeIf { id -> _profiles.value.any { it.id == id } } ?: _profiles.value.first().id)
    /** Aktuelles Profil (Id) */
    val profile: StateFlow<Int> = _profile.asStateFlow()
    val current: Profile get() = _profiles.value.first { it.id == _profile.value }

    private fun saveProfiles(l: List<Profile>) {
        _profiles.value = l
        prefs.edit().putString("profiles", l.joinToString("\n") { "${it.id}|${it.avatar}|${it.name.replace("|", "").replace("\n", " ")}" }).apply()
    }

    /** Neues Profil anlegen (höchstens [MAX_PROFILES]); gibt es zurück, oder null wenn voll. */
    fun addProfile(name: String, avatar: String): Profile? {
        if (_profiles.value.size >= MAX_PROFILES) return null
        val p = Profile((_profiles.value.maxOf { it.id }) + 1, name.trim().take(16).ifBlank { "Spieler ${_profiles.value.size + 1}" }, avatar)
        saveProfiles(_profiles.value + p)
        return p
    }

    fun renameProfile(id: Int, name: String, avatar: String) =
        saveProfiles(_profiles.value.map { if (it.id == id) it.copy(name = name.trim().take(16).ifBlank { it.name }, avatar = avatar) else it })

    fun switchProfile(id: Int) {
        if (_profiles.value.none { it.id == id }) return
        prefs.edit().putInt("profile", id).apply()
        _profile.value = id
        load()
    }

    /** Profil löschen (nicht das letzte). Seine Einstellungen werden entfernt; den Spielstand löscht das Repository. */
    fun deleteProfile(id: Int): Boolean {
        if (_profiles.value.size <= 1 || _profiles.value.none { it.id == id }) return false
        val wasCurrent = _profile.value == id
        val old = _profile.value
        _profile.value = id; clearGameKeys(); _profile.value = old
        saveProfiles(_profiles.value.filter { it.id != id })
        if (wasCurrent) switchProfile(_profiles.value.first().id)
        return true
    }

    /** Schlüssel des aktuellen Profils: Profil 0 ohne Vorsilbe (alter Spielstand), sonst „p3_…“ */
    private fun k(key: String) = if (_profile.value == 0) key else "p${_profile.value}_$key"

    // ------------------------------------------------------------------ je Profil

    private val _grade = MutableStateFlow<Int?>(null)
    private val _gradeAsked = MutableStateFlow(false)
    private val _look = MutableStateFlow(Triple(0, 0, 0))
    private val _stormBest = MutableStateFlow(0)
    private val _duelWins = MutableStateFlow(0)
    private val _duelsPlayed = MutableStateFlow(0)
    private val _storySeen = MutableStateFlow(false)
    private val _stickers = MutableStateFlow(emptySet<String>())
    private val _rooms = MutableStateFlow(emptySet<Int>())
    private val _decor = MutableStateFlow(emptyMap<String, String>())

    /** Werte des aktuellen Profils in die Flows laden */
    private fun load() {
        _grade.value = prefs.getInt(k("grade"), 0).takeIf { it in 5..13 }
        _gradeAsked.value = prefs.getBoolean(k("gradeAsked"), false)
        _look.value = Triple(prefs.getInt(k("shape"), 0), prefs.getInt(k("trail"), 0), prefs.getInt(k("hat"), 0))
        _stormBest.value = prefs.getInt(k("stormBest"), 0)
        _duelWins.value = prefs.getInt(k("duelWins"), 0)
        _duelsPlayed.value = prefs.getInt(k("duelsPlayed"), 0)
        _storySeen.value = prefs.getBoolean(k("storySeen"), false)
        _stickers.value = prefs.getStringSet(k("stickers"), emptySet()).orEmpty().toSet()
        _rooms.value = prefs.getStringSet(k("rooms"), emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        _decor.value = prefs.getStringSet(k("decor"), emptySet()).orEmpty().mapNotNull { e -> e.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
    }
    init { load() }

    /** Klassenstufe 5–13 (null = alle Kapitel zeigen); gefragt wird nur einmal, danach über 🎓 änderbar */
    val grade: StateFlow<Int?> = _grade.asStateFlow()
    val gradeAsked: StateFlow<Boolean> = _gradeAsked.asStateFlow()
    fun setGrade(g: Int?) {
        prefs.edit().putInt(k("grade"), g ?: 0).putBoolean(k("gradeAsked"), true).apply()
        _grade.value = g; _gradeAsked.value = true
    }

    /** Kapitel-Erklärung schon gesehen? */
    fun introSeen(chapter: Int) = prefs.getBoolean(k("intro_$chapter"), false)
    fun markIntroSeen(chapter: Int) = prefs.edit().putBoolean(k("intro_$chapter"), true).apply()
    /** Wie oft der Rechenweg für eine Bahnart schon geöffnet wurde (beim ersten Mal rechnet Volt einen Schritt vor) */
    fun walkthroughsSeen(kind: String) = prefs.getInt(k("walk_$kind"), 0)
    fun markWalkthrough(kind: String) = prefs.edit().putInt(k("walk_$kind"), walkthroughsSeen(kind) + 1).apply()

    /** Werkstatt: Form, Spur, Volts Hut (Indizes in Workshop.*) */
    val look: StateFlow<Triple<Int, Int, Int>> = _look.asStateFlow()
    fun setLook(shape: Int = _look.value.first, trail: Int = _look.value.second, hat: Int = _look.value.third) {
        _look.value = Triple(shape, trail, hat)
        prefs.edit().putInt(k("shape"), shape).putInt(k("trail"), trail).putInt(k("hat"), hat).apply()
    }

    /** Gewählte Sonden-Farbe */
    var skin: Int
        get() = prefs.getInt(k("skin"), 0)
        set(v) = prefs.edit().putInt(k("skin"), v).apply()

    /** Rekord im Asteroidensturm (Wellen) */
    val stormBest: StateFlow<Int> = _stormBest.asStateFlow()
    val duelWins: StateFlow<Int> = _duelWins.asStateFlow()
    val duelsPlayed: StateFlow<Int> = _duelsPlayed.asStateFlow()

    /** @return true bei neuem Rekord */
    fun recordStorm(waves: Int): Boolean {
        if (waves <= _stormBest.value) return false
        prefs.edit().putInt(k("stormBest"), waves).apply(); _stormBest.value = waves
        return true
    }
    fun recordDuel(won: Boolean) {
        _duelsPlayed.value += 1; if (won) _duelWins.value += 1
        prefs.edit().putInt(k("duelsPlayed"), _duelsPlayed.value).putInt(k("duelWins"), _duelWins.value).apply()
    }

    /** Schon gefeierte Abzeichen (damit „Neu!“ nur einmal erscheint) */
    var badgesSeen: Set<String>
        get() = prefs.getStringSet(k("badgesSeen"), emptySet()).orEmpty()
        set(v) = prefs.edit().putStringSet(k("badgesSeen"), v).apply()

    /** Schon gefeierte Bauteile der Aurora (Kapitelnummern) und ob die Geschichte erzählt wurde */
    var partsSeen: Set<String>
        get() = prefs.getStringSet(k("partsSeen"), emptySet()).orEmpty()
        set(v) = prefs.edit().putStringSet(k("partsSeen"), v).apply()
    /** Schon gefeierte Gold-Teile */
    var goldSeen: Set<String>
        get() = prefs.getStringSet(k("goldSeen"), emptySet()).orEmpty()
        set(v) = prefs.edit().putStringSet(k("goldSeen"), v).apply()
    val storySeen: StateFlow<Boolean> = _storySeen.asStateFlow()
    fun markStorySeen() { prefs.edit().putBoolean(k("storySeen"), true).apply(); _storySeen.value = true }

    /** Sticker-Album: Schlüssel „rocket“ bzw. „rocket*“ (Glitzer) */
    val stickers: StateFlow<Set<String>> = _stickers.asStateFlow()
    fun addSticker(key: String) { _stickers.value = _stickers.value + key; prefs.edit().putStringSet(k("stickers"), _stickers.value).apply() }

    /** Raumstation: freigeschaltete Räume und Sticker-Deko („Raum:Platz“ → Sticker-Schlüssel) */
    val rooms: StateFlow<Set<Int>> = _rooms.asStateFlow()
    val decor: StateFlow<Map<String, String>> = _decor.asStateFlow()
    fun unlockRoom(id: Int) { _rooms.value = _rooms.value + id; prefs.edit().putStringSet(k("rooms"), _rooms.value.map { it.toString() }.toSet()).apply() }
    fun setDecor(room: Int, slot: Int, sticker: String?) {
        val key = "$room:$slot"
        _decor.value = if (sticker == null) _decor.value - key else _decor.value + (key to sticker)
        prefs.edit().putStringSet(k("decor"), _decor.value.map { "${it.key}=${it.value}" }.toSet()).apply()
    }

    /** Rekord im Formel-Detektiv (richtige Fälle von 8) */
    var detectiveBest: Int
        get() = prefs.getInt(k("detectiveBest"), 0)
        set(v) = prefs.edit().putInt(k("detectiveBest"), v).apply()

    private val gameKeys = listOf("grade", "gradeAsked", "shape", "trail", "hat", "skin", "stormBest", "duelWins", "duelsPlayed",
        "badgesSeen", "detectiveBest", "partsSeen", "storySeen", "stickers", "lastOpen", "goldSeen", "rooms", "decor")

    private fun clearGameKeys() {
        val e = prefs.edit()
        val prefix = k("")
        prefs.keys().filter { key ->
            val rest = if (prefix.isEmpty()) key else key.removePrefix(prefix).takeIf { key.startsWith(prefix) } ?: return@filter false
            // Profil 0: nur Schlüssel ohne „pN_“-Vorsilbe
            (prefix.isNotEmpty() || !Regex("^p\\d+_").containsMatchIn(key)) && (rest.startsWith("intro_") || rest.startsWith("walk_") || rest in gameKeys)
        }.forEach { e.remove(it) }
        e.apply()
    }

    /**
     * Neu anfangen (nur das aktuelle Profil): Rekorde, Klasse, Abzeichen, Erklärungen, Aussehen, Story, Sticker auf Anfang.
     * Bleiben: Ton/Musik/Vibration, Update-Einstellung, die selbst gebauten Level und die anderen Profile.
     */
    fun resetGame() { clearGameKeys(); load() }
}
