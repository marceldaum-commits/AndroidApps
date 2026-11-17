package de.experimentopia.core.model

import org.junit.Test

/** Vorlese-Einstellungen bleiben beim Speichern erhalten */
class SpeechDumpTest {
    @Test fun einstellungenLesenSichWieder() {
        val a = AudioPrefs(music = false, musicVol = 0.3f, read = true, autoRead = true, voice = "Anna", rate = 1.2f, bestQuality = true)
        org.junit.Assert.assertEquals(a, AudioPrefs.decode(a.encode()))
        org.junit.Assert.assertEquals(AudioPrefs(), AudioPrefs.decode(null))
    }
}
