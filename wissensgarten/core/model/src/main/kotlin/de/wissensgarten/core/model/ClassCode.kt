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
