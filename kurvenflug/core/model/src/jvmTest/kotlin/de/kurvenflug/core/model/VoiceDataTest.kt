package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Die mitgelieferte Lautschrift (voice/stimme) deckt alle Texte des Spiels ab – sonst `tools/stimme/lautschrift.py` neu laufen lassen. */
class VoiceDataTest {
    private val dir = File("../../voice/stimme")
    private val laute by lazy { Lautschrift(File(dir, "saetze.tsv").readLines().asSequence(), File(dir, "woerter.tsv").readLines().asSequence()) }
    private val tokens by lazy { Lautschrift.tokens(File(dir, "tokens.txt").readLines().asSequence()) }

    @Test fun everyGameTextIsSpeakable() {
        val missing = SpeechTexts.all().map { Speech.clean(it.second) }.filter { it.isNotBlank() }.flatMap { laute.missing(it) }.toSortedSet()
        assertTrue("Ohne Lautschrift (tools/stimme/lautschrift.py ausführen): ${missing.take(40)}", missing.isEmpty())
    }

    @Test fun everySoundHasANumber() {
        assertEquals(" ", tokens.entries.first { it.value == 3 }.key.let { Char(it).toString() })
        val unknown = File(dir, "saetze.tsv").readLines().flatMap { l -> Lautschrift.codepoints(l.substringAfter('\t').replace("|", "")).filter { it !in tokens }.toList() }.toSet()
        assertTrue("Laute ohne Nummer: ${unknown.map { Char(it) }}", unknown.isEmpty())
    }

    @Test fun idsLikePiper() {
        // aus piper/phoneme_ids.py: ^ _ (Laut _)* $
        assertArrayEquals(intArrayOf(1, 0, 20, 0, 120, 0, 2), Lautschrift.ids("hˈ", tokens))
        val s = laute.of(Speech.clean("Flieg zur Station bei (6 | −4)."))!!
        assertEquals(1, s.size); assertTrue(s[0].startsWith("flˈiːk"))
        assertNull(laute.of("Quaxelbrumm"))
    }
}
