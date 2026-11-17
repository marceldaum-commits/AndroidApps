package de.experimentopia.app.ui

import de.experimentopia.core.model.Nova
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nova spricht nur ganze, vorab vertonte Sätze. Darum darf kein Bildschirm einen Vorlese-Text selbst zusammenbauen:
 * Alles, was BlubbSays/ReadButton/say bekommt, muss aus ReadTexts oder Say kommen (beide stehen in texte.json).
 */
class NovaCoverageTest {
    @Test fun keineEigenenVorleseTexteInDerOberflaeche() {
        val src = File("../shared/src/commonMain/kotlin").walkTopDown().filter { it.extension == "kt" }
        val bad = src.flatMap { f -> f.readLines().mapIndexedNotNull { i, l ->
            val t = l.trim()
            val spoken = Regex("""(BlubbSays|ReadButton|\.say)\(\s*"""").containsMatchIn(t) && !t.contains("Sicherheit zuerst: \$text")
            if (spoken) "${f.name}:${i + 1}" else null
        } }.toList()
        assertTrue("Vorlese-Text direkt im Bildschirm (bitte in Say oder ReadTexts): $bad", bad.isEmpty())
    }

    @Test fun jederSatzHatSatzendeOderIstEinName() {
        val pieces = Nova.catalog()
        assertTrue(pieces.size > 2000)
        assertTrue("Satz zerstückelt: ${pieces.filter { it.length > Nova.MAX }}", pieces.none { it.length > Nova.MAX })
    }
}
