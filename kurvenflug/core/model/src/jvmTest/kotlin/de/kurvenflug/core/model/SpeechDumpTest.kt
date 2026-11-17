package de.kurvenflug.core.model

import org.junit.Test
import java.io.File

/**
 * Schreibt alle vorlesbaren Texte nach build/sprechtexte.txt (je Zeile: Art, Tab, vorlesefertiger Text).
 * Daraus erzeugt `tools/stimme/lautschrift.py` die Lautschrift für die natürliche Stimme „Thorsten“.
 */
class SpeechDumpTest {
    @Test fun dump() {
        val texts = SpeechTexts.all().map { (k, t) -> k to Speech.clean(t) }.filter { it.second.isNotBlank() }.distinct()
        File("build").mkdirs()
        File("build/sprechtexte.txt").writeText(texts.joinToString("") { "${it.first}\t${it.second}\n" })
        // Zum Prüfen: Original → gesprochen
        File("build/sprechtexte-vergleich.txt").writeText(SpeechTexts.all().joinToString("") { "${it.second}\n  → ${Speech.clean(it.second)}\n" })
        check(texts.count { it.first == "fest" } > 300) { "zu wenige feste Texte" }
    }
}
