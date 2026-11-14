package de.wissensgarten.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Vorlesen: AUTO = für Grundschulkinder an, sonst aus. */
enum class ReadAloud(val label: String) { AUTO("Automatisch"), ON("Immer"), OFF("Nie") }

enum class ThemeMode(val label: String) { LIGHT("Hell"), DARK("Dunkel"), SYSTEM("Wie das Handy") }

/** Kleine Geräte-Einstellungen. Lernstand liegt in der Datenbank. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("einstellungen", Context.MODE_PRIVATE)

    private val _activeProfile = MutableStateFlow(prefs.getString(K_PROFILE, null))
    val activeProfileId: StateFlow<String?> = _activeProfile.asStateFlow()

    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(K_THEME, ThemeMode.LIGHT.name)!!) }.getOrDefault(ThemeMode.LIGHT)
    )
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _onboarded = MutableStateFlow(prefs.getBoolean(K_ONBOARDED, false))
    val onboarded: StateFlow<Boolean> = _onboarded.asStateFlow()

    private val _sound = MutableStateFlow(prefs.getBoolean(K_SOUND, true))
    val sound: StateFlow<Boolean> = _sound.asStateFlow()
    fun setSound(v: Boolean) { prefs.edit().putBoolean(K_SOUND, v).apply(); _sound.value = v }

    private val _music = MutableStateFlow(prefs.getBoolean("musik", true))
    val music: StateFlow<Boolean> = _music.asStateFlow()
    fun setMusic(v: Boolean) { prefs.edit().putBoolean("musik", v).apply(); _music.value = v }

    /** Beim Start (höchstens einmal am Tag) bei GitHub nach einer neuen App-Version schauen. */
    private val _autoUpdate = MutableStateFlow(prefs.getBoolean("auto_update", true))
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()
    fun setAutoUpdate(v: Boolean) { prefs.edit().putBoolean("auto_update", v).apply(); _autoUpdate.value = v }
    var lastUpdateCheck: Long
        get() = prefs.getLong("update_check", 0)
        set(v) { prefs.edit().putLong("update_check", v).apply() }

    private val _readAloud = MutableStateFlow(runCatching { ReadAloud.valueOf(prefs.getString("vorlesen", "AUTO")!!) }.getOrDefault(ReadAloud.AUTO))
    val readAloud: StateFlow<ReadAloud> = _readAloud.asStateFlow()
    /** Silben farbig (Silbenmethode). AUTO = Klasse 1–2 */
    private val _syllables = MutableStateFlow(runCatching { ReadAloud.valueOf(prefs.getString("silben", "AUTO")!!) }.getOrDefault(ReadAloud.AUTO))
    val syllables: StateFlow<ReadAloud> = _syllables.asStateFlow()
    fun setSyllables(r: ReadAloud) { prefs.edit().putString("silben", r.name).apply(); _syllables.value = r }

    fun setReadAloud(r: ReadAloud) { prefs.edit().putString("vorlesen", r.name).apply(); _readAloud.value = r }

    // Gieß-Erinnerung
    private val _reminderOn = MutableStateFlow(prefs.getBoolean(K_REMINDER, false))
    val reminderOn: StateFlow<Boolean> = _reminderOn.asStateFlow()
    private val _reminderHour = MutableStateFlow(prefs.getInt(K_REMINDER_HOUR, 17))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()
    private val _reminderAsked = MutableStateFlow(prefs.getBoolean(K_REMINDER_ASKED, false))
    /** Wurde die Erinnerungs-Frage im Garten schon beantwortet? */
    val reminderAsked: StateFlow<Boolean> = _reminderAsked.asStateFlow()

    fun setReminder(on: Boolean, hour: Int = _reminderHour.value) {
        prefs.edit().putBoolean(K_REMINDER, on).putInt(K_REMINDER_HOUR, hour).putBoolean(K_REMINDER_ASKED, true).apply()
        _reminderOn.value = on; _reminderHour.value = hour; _reminderAsked.value = true
    }

    // Abenteuer: gesehene Erklärungen, geöffnete Truhen, Freunde im Garten (pro Profil)
    private val _rev = MutableStateFlow(0)
    /** Zählt bei jeder Änderung hoch – damit die Oberfläche neu liest. */
    val adventureRev: StateFlow<Int> = _rev.asStateFlow()
    private fun bump() { _rev.value++ }

    /** Alle Pakete, deren Erklär-Geschichte dieses Profil schon gesehen hat (für den Geräte-Sync). */
    fun introsOf(profileId: String): Set<String> = prefs.all.keys.filter { it.startsWith("intro_${profileId}_") && prefs.getBoolean(it, false) }
        .map { it.removePrefix("intro_${profileId}_") }.toSet()
    fun setIntros(profileId: String, packs: Set<String>) { prefs.edit().apply { packs.forEach { putBoolean("intro_${profileId}_$it", true) } }.apply() }
    fun setChaptersRead(profileId: String, ns: Set<Int>) { prefs.edit().putStringSet("kapitel_$profileId", ns.map { "$it" }.toSet()).apply() }
    fun setHybrids(profileId: String, ids: Set<String>) { prefs.edit().putStringSet("kreuzungen_$profileId", ids).apply() }
    fun setGoodNightDay(profileId: String, day: Long) { prefs.edit().putLong("gutenacht_$profileId", day).apply() }
    fun clearGardenFriends(profileId: String) { prefs.edit().remove("freunde_$profileId").apply() }
    /** Nach einem Sync: alle Ansichten neu laden */
    fun syncChanged() = bump()

    // ---- Klassenarbeiten (Prüfungsmodus)
    private val examJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val examList = kotlinx.serialization.builtins.ListSerializer(de.wissensgarten.core.model.Exam.serializer())
    fun exams(profileId: String): List<de.wissensgarten.core.model.Exam> =
        prefs.getString("pruefungen_$profileId", null)?.let { runCatching { examJson.decodeFromString(examList, it) }.getOrNull() }.orEmpty()
    fun setExams(profileId: String, list: List<de.wissensgarten.core.model.Exam>) {
        prefs.edit().putString("pruefungen_$profileId", examJson.encodeToString(examList, list.sortedBy { it.day })).apply(); bump()
    }

    fun introSeen(profileId: String, packId: String) = prefs.getBoolean("intro_${profileId}_$packId", false)
    fun markIntroSeen(profileId: String, packId: String) { prefs.edit().putBoolean("intro_${profileId}_$packId", true).apply(); bump() }

    fun chestsOpened(profileId: String) = prefs.getInt("truhen_$profileId", 0)
    fun setChestsOpened(profileId: String, n: Int) { prefs.edit().putInt("truhen_$profileId", n).apply(); bump() }

    /** Ausgewählte Freunde für den Garten; null = automatisch die neuesten. */
    fun gardenFriends(profileId: String): Set<String>? = prefs.getStringSet("freunde_$profileId", null)
    fun setGardenFriends(profileId: String, ids: Set<String>) { prefs.edit().putStringSet("freunde_$profileId", ids).apply(); bump() }

    fun chaptersRead(profileId: String): Set<Int> = prefs.getStringSet("kapitel_$profileId", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
    fun markChapterRead(profileId: String, n: Int) { prefs.edit().putStringSet("kapitel_$profileId", (chaptersRead(profileId) + n).map { "$it" }.toSet()).apply(); bump() }

    fun hybrids(profileId: String): Set<String> = prefs.getStringSet("kreuzungen_$profileId", emptySet())!!
    fun addHybrid(profileId: String, id: String) { prefs.edit().putStringSet("kreuzungen_$profileId", hybrids(profileId) + id).apply(); bump() }
    fun familyClaimedWeek() = prefs.getLong("familienbaum_woche", -1)
    fun setFamilyClaimedWeek(w: Long) { prefs.edit().putLong("familienbaum_woche", w).apply(); bump() }

    fun surpriseFound(profileId: String, day: Long, i: Int) = prefs.getBoolean("fest_${profileId}_${day}_$i", false)
    fun markSurprise(profileId: String, day: Long, i: Int) { prefs.edit().putBoolean("fest_${profileId}_${day}_$i", true).apply(); bump() }

    fun goodNightDay(profileId: String) = prefs.getLong("gutenacht_$profileId", -1)
    fun markGoodNight(profileId: String, day: Long) { prefs.edit().putLong("gutenacht_$profileId", day).apply(); bump() }

    fun setActiveProfile(id: String?) { prefs.edit().putString(K_PROFILE, id).apply(); _activeProfile.value = id }
    fun setTheme(t: ThemeMode) { prefs.edit().putString(K_THEME, t.name).apply(); _theme.value = t }
    fun setOnboarded(v: Boolean) { prefs.edit().putBoolean(K_ONBOARDED, v).apply(); _onboarded.value = v }

    private companion object {
        const val K_PROFILE = "aktives_profil"
        const val K_THEME = "design"
        const val K_ONBOARDED = "einrichtung_fertig"
        const val K_SOUND = "ton"
        const val K_REMINDER = "erinnerung"
        const val K_REMINDER_HOUR = "erinnerung_stunde"
        const val K_REMINDER_ASKED = "erinnerung_gefragt"
    }
}
