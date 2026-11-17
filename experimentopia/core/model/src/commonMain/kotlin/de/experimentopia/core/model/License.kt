package de.experimentopia.core.model

/**
 * Pro-Codes von der Website: ohne Konto, ohne Server prüfbar.
 *
 * Inhalt (9 Byte): Version 1 · App (6 = Experimentopia) · Produkt (1 = Pro, 2 = Schullizenz) · laufende Nummer (4 Byte)
 * · Tag (2 Byte, Tage seit 1.1.2020; bei der Schullizenz: gültig bis einschließlich diesem Tag).
 * Dahinter 64 Byte Ed25519-Signatur des Lizenz-Dienstes. Alles zusammen in Crockford-Base32, z. B. „EX-1AB2C-…“.
 * Den geheimen Schlüssel hat nur der Lizenz-Dienst; die App kennt den öffentlichen ([publicKeyHex]).
 */
object License {
    const val PREFIX = "EX"
    const val PRODUCT_PRO = 1
    const val PRODUCT_SCHOOL = 2
    /** 1.1.2020 als Tageszahl seit 1.1.1970 */
    const val DAY0 = 18262L
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    data class Info(val app: Int, val product: Int, val serial: Long, val day: Long) {
        /** Schullizenz: letzter gültiger Tag (Tage seit 1970) */
        val until: Long? get() = if (product == PRODUCT_SCHOOL) DAY0 + day else null
    }

    /** Der Code aus einem Text (auch mitten in einer E-Mail oder einem Link „?pro=…“), ohne Prüfung. */
    fun extract(text: String): String? {
        // genau 117 Zeichen (73 Byte) einsammeln – Bindestriche und Zeilenumbrüche (E-Mail) dazwischen überspringen
        for (m in Regex("""(?i)EX""").findAll(text)) {
            val sb = StringBuilder()
            var i = m.range.last + 1
            while (i < text.length && sb.length < CHARS) {
                val c = text[i]
                when {
                    c == '-' || c.isWhitespace() -> {}
                    c.isLetterOrDigit() && c.code < 128 -> sb.append(c.uppercaseChar())
                    else -> break
                }
                i++
            }
            if (sb.length == CHARS) return "$PREFIX-$sb"
        }
        return null
    }

    /** Länge eines Codes ohne Vorsilbe und Bindestriche: 73 Byte in Base32 */
    private const val CHARS = 117

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        var buf = 0; var bits = 0
        for (b in bytes) {
            buf = (buf shl 8) or (b.toInt() and 0xff); bits += 8
            while (bits >= 5) { sb.append(ALPHABET[(buf shr (bits - 5)) and 31]); bits -= 5 }
            buf = buf and ((1 shl bits) - 1)
        }
        if (bits > 0) sb.append(ALPHABET[(buf shl (5 - bits)) and 31])
        return PREFIX + "-" + sb.chunked(5).joinToString("-")
    }

    fun decode(code: String): ByteArray? {
        val body = code.trim().uppercase().removePrefix(PREFIX).filter { it.isLetterOrDigit() }
            .replace('O', '0').replace('I', '1').replace('L', '1')
        val out = ArrayList<Byte>()
        var buf = 0; var bits = 0
        for (c in body) {
            val v = ALPHABET.indexOf(c).takeIf { it >= 0 } ?: return null
            buf = (buf shl 5) or v; bits += 5
            if (bits >= 8) { out += (buf shr (bits - 8)).toByte(); bits -= 8; buf = buf and ((1 shl bits) - 1) }
        }
        return out.toByteArray()
    }

    private fun hex(s: String): ByteArray? = s.trim().takeIf { it.length % 2 == 0 && it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' } }
        ?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()

    /** Gültiger Code für diese App (und bei Schullizenzen noch nicht abgelaufen)? Sonst null. */
    fun check(code: String?, publicKeyHex: String, today: Long, app: Int = Pro.APP_ID): Info? {
        if (code.isNullOrBlank()) return null
        val pk = hex(publicKeyHex)?.takeIf { it.size == 32 } ?: return null
        val raw = decode(code)?.takeIf { it.size == 73 } ?: return null
        val payload = raw.copyOfRange(0, 9)
        if (payload[0].toInt() != 1) return null
        if (!Ed25519.verify(payload, raw.copyOfRange(9, 73), pk)) return null
        fun u(i: Int) = payload[i].toLong() and 0xff
        val info = Info(u(1).toInt(), u(2).toInt(), (u(3) shl 24) or (u(4) shl 16) or (u(5) shl 8) or u(6), (u(7) shl 8) or u(8))
        if (info.app != app || info.product !in setOf(PRODUCT_PRO, PRODUCT_SCHOOL)) return null
        if (info.until?.let { today > it } == true) return null
        return info
    }
}
