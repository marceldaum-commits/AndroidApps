package de.wissensgarten.core.model

/**
 * Plus-Codes von der Website (gemeinsames Format aller Lern-Apps): ohne Konto, ohne Server prüfbar.
 *
 * Inhalt (9 Byte): Version 1 · App (3 = Wissensgarten) · Produkt (1 = Plus Jahr, 3 = Plus für immer) · laufende Nummer (4 Byte)
 * · Tag (2 Byte, Tage seit 1.1.2020 – beim Jahres-Plus der Kauftag; gültig 365 Tage).
 * Dahinter 64 Byte Ed25519-Signatur des Lizenz-Dienstes. Alles in Crockford-Base32, z. B. „WG-040G6-…“.
 * Den geheimen Schlüssel hat nur der Lizenz-Dienst; die App kennt den öffentlichen.
 */
object License {
    const val PREFIX = "WG"
    const val APP_ID = 3
    const val PRODUCT_YEAR = 1
    const val PRODUCT_FOREVER = 3
    const val YEAR_DAYS = 365
    /** 1.1.2020 als Tageszahl seit 1.1.1970 */
    const val DAY0 = 18262L
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    /** Länge eines Codes ohne Vorsilbe und Bindestriche: 73 Byte in Base32 */
    private const val CHARS = 117

    data class Info(val app: Int, val product: Int, val serial: Long, val day: Long) {
        /** Letzter gültiger Tag (Tage seit 1970); null = für immer */
        val until: Long? get() = if (product == PRODUCT_YEAR) DAY0 + day + YEAR_DAYS - 1 else null
    }

    /** Den Code aus einem Text holen (auch mitten in einer E-Mail oder aus dem Link „?pro=…“), ohne Prüfung. */
    fun extract(text: String): String? {
        for (m in Regex("""(?i)WG""").findAll(text)) {
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
            if (sb.length == CHARS) return "$PREFIX-" + sb.chunked(5).joinToString("-")
        }
        return null
    }

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

    private fun hex(s: String): ByteArray? = s.trim().takeIf { it.length == 64 && it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' } }
        ?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()

    /** Echter Code für den Wissensgarten (Signatur stimmt), unabhängig vom Datum? Sonst null. */
    fun parse(code: String?, publicKeyHex: String): Info? {
        if (code.isNullOrBlank()) return null
        val pk = hex(publicKeyHex) ?: return null
        val raw = decode(code)?.takeIf { it.size == 73 } ?: return null
        val payload = raw.copyOfRange(0, 9)
        if (payload[0].toInt() != 1) return null
        if (!Ed25519.verify(payload, raw.copyOfRange(9, 73), pk)) return null
        fun u(i: Int) = payload[i].toLong() and 0xff
        val info = Info(u(1).toInt(), u(2).toInt(), (u(3) shl 24) or (u(4) shl 16) or (u(5) shl 8) or u(6), (u(7) shl 8) or u(8))
        if (info.app != APP_ID || info.product !in setOf(PRODUCT_YEAR, PRODUCT_FOREVER)) return null
        return info
    }

    /** Gültiger und (beim Jahres-Plus) noch nicht abgelaufener Code? [today] = Tage seit 1970. */
    fun check(code: String?, publicKeyHex: String, today: Long): Info? =
        parse(code, publicKeyHex)?.takeIf { info -> info.until?.let { today <= it } ?: true }
}
