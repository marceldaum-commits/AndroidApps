package de.kurvenflug.core.model

import de.kurvenflug.core.data.Prefs
import de.kurvenflug.core.data.Settings
import org.junit.Assert.*
import org.junit.Test

/** Abschlussprüfung 17.11.: Einstellungen überleben einen Neustart; Werte aus Vorversionen stören nicht und gehen nicht verloren. */
class SettingsPersistenceTest {
    private class MemPrefs : Prefs {
        val m = HashMap<String, Any?>()
        override fun getBoolean(key: String, default: Boolean) = m[key] as? Boolean ?: default
        override fun getInt(key: String, default: Int) = m[key] as? Int ?: default
        override fun getLong(key: String, default: Long) = m[key] as? Long ?: default
        override fun getString(key: String, default: String?) = m[key] as? String ?: default
        @Suppress("UNCHECKED_CAST") override fun getStringSet(key: String, default: Set<String>) = m[key] as? Set<String> ?: default
        override fun keys() = m.keys.toSet()
        override fun edit() = object : Prefs.Editor {
            val ops = ArrayList<() -> Unit>()
            override fun putBoolean(key: String, v: Boolean) = apply { ops += { m[key] = v } }
            override fun putInt(key: String, v: Int) = apply { ops += { m[key] = v } }
            override fun putLong(key: String, v: Long) = apply { ops += { m[key] = v } }
            override fun putString(key: String, v: String?) = apply { ops += { m[key] = v } }
            override fun putStringSet(key: String, v: Set<String>) = apply { ops += { m[key] = v } }
            override fun remove(key: String) = apply { ops += { m.remove(key) } }
            override fun apply() { ops.forEach { it() } }
        }
    }

    @Test fun everythingSurvivesRestart() {
        val p = MemPrefs()
        Settings(p).apply {
            setProCode("KFTESTCODE"); setReadAloud(false); setReadSpeed(130); setOfflineBest(true)
            setMusic(false); setMusicVolume(40); setSfxVolume(55); setSound(false); setVibration(false); setAutoUpdate(false); setVoltVoice(false)
        }
        Settings(p).apply {
            assertEquals("KFTESTCODE", proCode.value); assertFalse(readAloud.value); assertEquals(130, readSpeed.value); assertTrue(offlineBest.value)
            assertFalse(music.value); assertEquals(40, musicVolume.value); assertEquals(55, sfxVolume.value)
            assertFalse(sound.value); assertFalse(vibration.value); assertFalse(autoUpdate.value); assertFalse(voltVoice.value)
        }
    }

    @Test fun defaultsAndOldVersionKeys() {
        // Schlüssel aus 2.0–2.3 (Thorsten: Stimme, Tempo in %) liegen noch herum – sie stören nicht
        val p = MemPrefs().apply { m["readVoice"] = "thorsten"; m["readRate"] = 80; m["readAloud"] = true; m["proCode"] = "KFALT" }
        Settings(p).apply {
            assertTrue(readAloud.value); assertEquals(100, readSpeed.value); assertFalse(offlineBest.value)
            assertEquals("KFALT", proCode.value)      // Pro-Code aus der Vorversion bleibt
        }
        // Unbekannte Tempo-Werte werden auf die nächste Stufe gesetzt
        Settings(p).apply { setReadSpeed(104) }; assertEquals(100, Settings(p).readSpeed.value)
    }
}
