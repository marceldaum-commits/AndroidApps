package de.wissensgarten.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Klassen-Code: Eine Lehrkraft (oder ein Elternteil) teilt ein eigenes Beet als QR-Code oder Link.
 * Alles steckt im Code selbst – kein Server, kein Konto, keine Schülerdaten.
 */
@Serializable
data class ClassSet(val title: String, val emoji: String, val cards: List<ClassCard>)

@Serializable
data class ClassCard(val q: String, val a: String, val img: String? = null, val info: String? = null)

object ClassCodes {
    const val MAX_CARDS = 60
    /** Ein QR-Code fasst gut scanbar etwa 1800 Zeichen – darüber wird er zu fein zum Scannen. */
    const val MAX_LENGTH = 1800
    private const val PREFIX = "WGK1-"
    const val LINK_BASE = "https://www.mathenachhilfe-online.com/klasse/#"
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(set: ClassSet): String {
        val trimmed = set.copy(cards = set.cards.take(MAX_CARDS))
        val raw = json.encodeToString(ClassSet.serializer(), trimmed).toByteArray()
        val crc = CRC32().apply { update(raw) }.value.toString(36)
        val d = Deflater(Deflater.BEST_COMPRESSION, true).apply { setInput(raw); finish() }
        val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray()) + "." + crc
    }

    fun link(set: ClassSet) = LINK_BASE + encode(set)

    /** Passt der Code noch in einen gut scanbaren QR-Code? */
    fun fitsQr(set: ClassSet) = link(set).length <= MAX_LENGTH

    /** Findet einen Klassen-Code auch mitten in einem Text oder Link. Null, wenn kaputt. */
    fun decode(text: String): ClassSet? {
        val m = Regex("""WGK1-([A-Za-z0-9_-]+)\.([0-9a-z]+)""").find(text) ?: return null
        return runCatching {
            val packed = Base64.getUrlDecoder().decode(m.groupValues[1])
            val inf = Inflater(true).apply { setInput(packed) }
            val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
            while (!inf.finished()) {
                val n = inf.inflate(buf)
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
                out.write(buf, 0, n)
                if (out.size() > 200_000) return null
            }
            inf.end()
            val raw = out.toByteArray()
            if (CRC32().apply { update(raw) }.value.toString(36) != m.groupValues[2]) return null
            json.decodeFromString(ClassSet.serializer(), String(raw)).let { s ->
                s.copy(title = s.title.take(60), cards = s.cards.take(MAX_CARDS).filter { it.q.isNotBlank() && it.a.isNotBlank() })
            }
        }.getOrNull()
    }
}

/**
 * Aus erkanntem Text (Foto vom Arbeitsblatt / Vokabelliste) Karten machen.
 * Erkennt „Frage – Antwort“, „Wort = Wort“, „Wort: Erklärung“, „Wort → Wort“ und Tabulator-Spalten.
 */
object Worksheet {
    private val separators = listOf("\t", " → ", "->", " = ", " – ", " — ", " - ", ": ", " : ", "=", "–")

    fun parse(text: String): List<Pair<String, String>> =
        text.lines().map { it.trim().trimStart('•', '*', '·').trim() }
            .map { it.replace(Regex("""^\(?\d{1,2}[.)]\s+"""), "") } // „1. “ oder „3) “ am Anfang weg
            .filter { it.length in 3..160 }
            .mapNotNull { line -> split(line) }
            .distinct()

    private fun split(line: String): Pair<String, String>? {
        for (sep in separators) {
            val i = line.indexOf(sep)
            if (i <= 0) continue
            val a = line.substring(0, i).trim().trimEnd(':', '-', '=')
            val b = line.substring(i + sep.length).trim().trimStart(':', '-', '=').trim()
            if (a.isNotEmpty() && b.isNotEmpty() && a.length <= 80 && b.length <= 120) return a to b
        }
        return null
    }
}

/** Sucht zu einem Begriff ein passendes Emoji als Merkbild (Wörterbuch kommt aus der App). */
class EmojiLexicon(private val words: Map<String, String>) {
    fun guess(vararg texts: String): String? {
        for (t in texts) {
            val tokens = t.lowercase().split(Regex("""[^\p{L}]+""")).filter { it.length >= 3 }
            for (tok in tokens) {
                words[tok]?.let { return it }
                // einfache Grundform: Hunde → hund, Katzen → katze
                listOf("en", "n", "e", "s", "er").firstNotNullOfOrNull { suf ->
                    if (tok.endsWith(suf) && tok.length - suf.length >= 3) words[tok.removeSuffix(suf)] else null
                }?.let { return it }
            }
        }
        return null
    }

    companion object {
        fun parse(tsv: String) = EmojiLexicon(tsv.lines().mapNotNull { l ->
            val p = l.split('\t'); if (p.size == 2) p[0] to p[1] else null
        }.toMap())
    }
}
