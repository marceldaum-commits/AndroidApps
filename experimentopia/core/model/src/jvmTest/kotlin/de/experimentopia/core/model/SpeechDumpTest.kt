package de.experimentopia.core.model

import org.junit.Test
import java.io.File

/** Sammelt alle Vorlese-Texte für das Lautschrift-Werkzeug (tools/stimme/lautschrift.py) nach build/sprechtexte.txt */
class SpeechDumpTest {
    @Test fun sammeln() {
        val out = File("build/sprechtexte.txt"); out.parentFile.mkdirs()
        out.writeText(ReadTexts.all().map { Speech.clean(it) }.filter { it.isNotBlank() }.distinct().joinToString("\n") { "fest\t$it" } + "\n")
    }
    @Test fun einstellungenLesenSichWieder() {
        val a = AudioPrefs(music = false, musicVol = 0.3f, read = true, autoRead = true, voice = "Anna", rate = 1.2f)
        org.junit.Assert.assertEquals(a, AudioPrefs.decode(a.encode()))
        org.junit.Assert.assertEquals(AudioPrefs(), AudioPrefs.decode(null))
    }
}
