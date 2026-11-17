package de.raumlicht.core.model

/**
 * Freischalt-Codes für Raumlicht Pro: ohne Konto und ohne Server prüfbar (gleiches Verfahren wie bei KurvenFlug).
 *
 * Inhalt (9 Byte): Version 1 · App (4 = Raumlicht) · Produkt (1 = Raumlicht Pro) · laufende Nummer (4 Byte) · Tag der Ausstellung (2 Byte, Tage seit 1.1.2020).
 * Dahinter 64 Byte Ed25519-Signatur des Lizenz-Dienstes. Alles zusammen in Crockford-Base32, z. B. „RL-1AB2C-…“.
 * Den geheimen Schlüssel hat nur der Lizenz-Dienst; die App kennt nur den öffentlichen ([Pro.PUBLIC_KEY]).
 */
object License {
    const val PREFIX = "RL"
    const val APP_ID = 4
    const val PRODUCT_PASS = 1
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    /** Länge eines Codes ohne Vorsilbe und Bindestriche: 73 Byte in Base32 */
    private const val CHARS = 117

    data class Info(val app: Int, val product: Int, val serial: Long, val day: Long)

    /** Der Code aus einem Text (auch mitten in einer E-Mail oder einem Link „?pro=…“), ohne Prüfung */
    fun extract(text: String): String? {
        // genau 117 Zeichen einsammeln – Bindestriche und Zeilenumbrüche (E-Mail) dazwischen überspringen
        for (m in Regex("(?i)$PREFIX").findAll(text)) {
            val sb = StringBuilder(); var i = m.range.last + 1
            while (i < text.length && sb.length < CHARS) {
                val c = text[i]
                if (c == '-' || c.isWhitespace()) { i++; continue }
                if (c.isLetterOrDigit() && c.code < 128) sb.append(c.uppercaseChar()) else break
                i++
            }
            if (sb.length == CHARS) return PREFIX + "-" + sb.chunked(5).joinToString("-")
        }
        return null
    }

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder(); var buf = 0; var bits = 0
        for (b in bytes) {
            buf = (buf shl 8) or (b.toInt() and 0xff); bits += 8
            while (bits >= 5) { sb.append(ALPHABET[(buf shr (bits - 5)) and 31]); bits -= 5 }
            buf = buf and ((1 shl bits) - 1)
        }
        if (bits > 0) sb.append(ALPHABET[(buf shl (5 - bits)) and 31])
        return PREFIX + "-" + sb.chunked(5).joinToString("-")
    }

    fun decode(code: String): ByteArray? {
        // Verwechsler beim Abtippen: O/0, I/1, L/1 (die Vorsilbe „RL“ vorher abtrennen)
        val body = code.trim().uppercase().removePrefix(PREFIX).filter { it.isLetterOrDigit() }.replace('O', '0').replace('I', '1').replace('L', '1')
        val out = ArrayList<Byte>(); var buf = 0; var bits = 0
        for (c in body) {
            val v = ALPHABET.indexOf(c).takeIf { it >= 0 } ?: return null
            buf = (buf shl 5) or v; bits += 5
            if (bits >= 8) { out += (buf shr (bits - 8)).toByte(); bits -= 8; buf = buf and ((1 shl bits) - 1) }
        }
        return out.toByteArray()
    }

    private fun hex(s: String): ByteArray? = s.trim().takeIf { it.length % 2 == 0 && it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' } }
        ?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()

    /** Gültiger Pro-Code für Raumlicht? Sonst null. */
    fun check(code: String?, publicKeyHex: String): Info? {
        if (code.isNullOrBlank()) return null
        val pk = hex(publicKeyHex)?.takeIf { it.size == 32 } ?: return null
        val raw = decode(code)?.takeIf { it.size == 73 } ?: return null
        val payload = raw.copyOfRange(0, 9)
        if (payload[0].toInt() != 1) return null
        if (!Ed25519.verify(payload, raw.copyOfRange(9, 73), pk)) return null
        fun u(i: Int) = payload[i].toLong() and 0xff
        val info = Info(u(1).toInt(), u(2).toInt(), (u(3) shl 24) or (u(4) shl 16) or (u(5) shl 8) or u(6), (u(7) shl 8) or u(8))
        return info.takeIf { it.app == APP_ID && it.product == PRODUCT_PASS }
    }
}
