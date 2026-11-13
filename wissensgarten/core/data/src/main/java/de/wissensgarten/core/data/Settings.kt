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

    fun setActiveProfile(id: String?) { prefs.edit().putString(K_PROFILE, id).apply(); _activeProfile.value = id }
    fun setTheme(t: ThemeMode) { prefs.edit().putString(K_THEME, t.name).apply(); _theme.value = t }
    fun setOnboarded(v: Boolean) { prefs.edit().putBoolean(K_ONBOARDED, v).apply(); _onboarded.value = v }

    private companion object {
        const val K_PROFILE = "aktives_profil"
        const val K_THEME = "design"
        const val K_ONBOARDED = "einrichtung_fertig"
    }
}
