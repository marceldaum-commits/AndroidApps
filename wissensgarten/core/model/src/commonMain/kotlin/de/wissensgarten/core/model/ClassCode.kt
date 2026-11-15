package de.wissensgarten.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
        val raw = json.encodeToString(ClassSet.serializer(), trimmed).encodeToByteArray()
        val crc = Codes.crc32(raw).toString(36)
        return PREFIX + Codes.encode(Platform.deflateRaw(raw)) + "." + crc
    }

    fun link(set: ClassSet) = LINK_BASE + encode(set)

    /** Passt der Code noch in einen gut scanbaren QR-Code? */
    fun fitsQr(set: ClassSet) = link(set).length <= MAX_LENGTH

    /** Findet einen Klassen-Code auch mitten in einem Text oder Link. Null, wenn kaputt. */
    fun decode(text: String): ClassSet? {
        val m = Regex("""WGK1-([A-Za-z0-9_-]+)\.([0-9a-z]+)""").find(text) ?: return null
        return runCatching {
            val raw = Platform.inflateRaw(Codes.decode(m.groupValues[1]))
            if (raw.size > 200_000) return null
            if (Codes.crc32(raw).toString(36) != m.groupValues[2]) return null
            json.decodeFromString(ClassSet.serializer(), raw.decodeToString()).let { s ->
                s.copy(title = s.title.take(60), cards = s.cards.take(MAX_CARDS).filter { it.q.isNotBlank() && it.a.isNotBlank() })
            }
        }.getOrNull()
    }
}
