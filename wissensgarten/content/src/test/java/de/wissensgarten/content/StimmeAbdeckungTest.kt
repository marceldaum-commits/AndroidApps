package de.wissensgarten.content

import de.wissensgarten.core.model.Lautschrift
import de.wissensgarten.core.model.PackJson
import de.wissensgarten.core.model.PiperIds
import de.wissensgarten.core.model.SprechText
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Brummis natürliche Stimme „Thorsten“ kennt nur Wörter aus dem mitgelieferten Wörterbuch (`assets/stimme/woerter.tsv.gz`).
 * Fehlt ein Wort, liest die Gerätestimme den ganzen Text – das ist kein Fehler, soll aber selten bleiben.
 * Schlägt dieser Test an: `python3 tools/stimme/lautschrift.py` neu laufen lassen (siehe tools/stimme/README.md).
 */
class StimmeAbdeckungTest {
    private val dir = File("src/main/assets/stimme")
    private val lex by lazy { Lautschrift(GZIPInputStream(File(dir, "woerter.tsv.gz").inputStream()).bufferedReader().readLines().asSequence()) }
    /** wie Voice.localeFor: in Sprach-Fächern liest Thorsten nur erkennbar deutsche Texte */
    private val germanHint = Regex("\\b(der|die|das|ein|eine|ist|was|wie|welche[rs]?|heißt|und|auf|von|mit|nicht)\\b|[äöüß]", RegexOption.IGNORE_CASE)

    private fun texts(): List<String> = File("src/main/assets/packs").listFiles { f -> f.extension == "json" }!!.sorted().flatMap { f ->
        val p = PackJson.parse(f.readText())
        (listOf(p.title, p.description) + p.intro.map { it.text } + p.cards.flatMap { listOfNotNull(it.q, it.a, it.info) })
            .filter { it.isNotBlank() && (p.subject !in setOf("englisch", "franzoesisch", "spanisch") || germanHint.containsMatchIn(it)) }
    }

    @Test fun fastAlleTexteDerPaketeSindSprechbar() {
        val all = texts(); val gaps = HashMap<String, Int>(); var ok = 0
        for (t in all) { val clean = SprechText.clean(t); if (clean.isEmpty()) { ok++; continue }
            val miss = lex.missing(clean); if (miss.isEmpty()) ok++ else miss.forEach { gaps[it] = (gaps[it] ?: 0) + 1 } }
        val share = ok * 100.0 / all.size
        val top = gaps.entries.sortedByDescending { it.value }.take(40).joinToString { "${it.key} (${it.value}×)" }
        println("Thorsten-Abdeckung: $ok von ${all.size} Texten (${"%.1f".format(share)} %), ${gaps.size} fehlende Wörter. Häufigste: $top")
        assertTrue("Nur ${"%.1f".format(share)} % der Pakettexte sind mit Thorsten sprechbar – tools/stimme/lautschrift.py neu laufen lassen. Fehlend: $top", share >= 97.0)
    }

    @Test fun woerterbuchBleibtKlein() {
        assertTrue("Wörterbuch zu groß", File(dir, "woerter.tsv.gz").length() < 400_000)
        assertTrue("zu wenige Wörter", lex.size > 5000)
    }

    /** Schreibt Laut-Nummern einiger Beispieltexte nach build/hoerprobe.txt – damit lässt sich die Stimme im Browser-Test prüfen. */
    @Test fun hoerprobe() {
        val map = PiperIds.tokens(File(dir, "tokens.txt").readLines().asSequence())
        val texts = listOf("Hallo, ich bin Brummi! So klinge ich, wenn ich dir vorlese.", "Die Hauptstadt von Frankreich ist Paris.",
            "Im Jahr 1989 fiel die Mauer. Das sind 25 Prozent.") + texts().take(3)
        File("build").mkdirs()
        File("build/hoerprobe.txt").writeText(texts.joinToString("\n") { t ->
            val s = lex.of(SprechText.clean(t)) ?: error("ohne Lautschrift: ${lex.missing(SprechText.clean(t))}")
            t + "\t" + s.joinToString(";") { PiperIds.encode(it, map).joinToString(",") }
        })
    }

    @Test fun alleLauteKenntDieStimme() {
        val map = PiperIds.tokens(File(dir, "tokens.txt").readLines().asSequence())
        val unknown = GZIPInputStream(File(dir, "woerter.tsv.gz").inputStream()).bufferedReader().readLines()
            .flatMap { l -> PiperIds.codepoints(l.substringAfter('\t')).filter { it !in map }.toList() }.toSet()
        assertTrue("Laute ohne Nummer in tokens.txt: ${unknown.joinToString { Character.toChars(it).concatToString() }}", unknown.isEmpty())
    }
}
