package de.wissensgarten.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Update von 2.0.x (alte Stimmenwahl, freies Tempo) auf den aktuellen Stand:
 * Nur der alte Schlüssel „stimme“ verschwindet, alles andere (Profil, Plus-Code, Lautstärken, Fortschritt) bleibt.
 */
class SettingsUpgradeTest {
    @Test fun alteStimmenEinstellungWirdAufgeraeumtRestBleibt() {
        val old = mapOf<String, Any>(
            "stimme" to "alte-stimme", "stimme_tempo" to 137, "aktives_profil" to "p1", "einrichtung_fertig" to true,
            "musik" to false, "musik_laut" to 30, "ton_laut" to 80, "vorlesen" to "ON", "plus_code" to "WG-TEST",
            "truhen_p1" to 4, "kapitel_p1" to setOf("1", "2"), "design" to "DARK",
        )
        var saved: Map<String, Any> = old
        val store = MemoryKeyValueStore(old) { saved = it }
        val s = Settings(store)

        assertFalse(store.contains("stimme"))
        assertFalse(saved.containsKey("stimme"))
        assertEquals(130, s.voiceRate.value)                 // freier Wert 137 → nächste Stufe
        assertEquals("p1", s.activeProfileId.value)
        assertTrue(s.onboarded.value)
        assertFalse(s.music.value)
        assertEquals(30, s.musicVolume.value)
        assertEquals(80, s.sfxVolume.value)
        assertEquals(ReadAloud.ON, s.readAloud.value)
        assertEquals(ThemeMode.DARK, s.theme.value)
        assertEquals("WG-TEST", s.plusCode)
        assertEquals(4, s.chestsOpened("p1"))
        assertEquals(setOf(1, 2), s.chaptersRead("p1"))
        // Nichts außer „stimme“ wurde entfernt
        assertEquals(old.keys - "stimme", saved.keys)
    }

    @Test fun ohneAltlastWirdNichtsGeschrieben() {
        var writes = 0
        val s = Settings(MemoryKeyValueStore(mapOf("stimme_tempo" to 115)) { writes++ })
        assertEquals(115, s.voiceRate.value)
        assertEquals(0, writes)
    }
}
