package de.wissensgarten.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
