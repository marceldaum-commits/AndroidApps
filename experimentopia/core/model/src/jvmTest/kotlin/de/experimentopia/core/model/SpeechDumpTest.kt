package de.experimentopia.core.model

import org.junit.Test

/** Vorlese-Einstellungen bleiben beim Speichern erhalten */
class SpeechDumpTest {
    @Test fun einstellungenLesenSichWieder() {
        val a = AudioPrefs(music = false, musicVol = 0.3f, read = true, autoRead = true, voice = "Anna", rate = 1.2f, bestQuality = true)
        org.junit.Assert.assertEquals(a, AudioPrefs.decode(a.encode()))
        org.junit.Assert.assertEquals(AudioPrefs(), AudioPrefs.decode(null))
    }

    /** Update von 2.1 (Thorsten, Tempo 0,8): nichts geht verloren, das Tempo rastet auf die nächste Stufe ein */
    @Test fun alteEinstellungenAus21() {
        val old = AudioPrefs.decode("musik=0;musikLaut=0.3;effektLaut=0.7;vorlesen=1;auto=1;tempo=0.8;stimme=thorsten")
        org.junit.Assert.assertEquals(false, old.music); org.junit.Assert.assertEquals(0.3f, old.musicVol); org.junit.Assert.assertEquals(true, old.autoRead)
        org.junit.Assert.assertEquals(0.75f, Nova.speed(old.rate)); org.junit.Assert.assertEquals(false, old.bestQuality)
    }
}
