package de.wissensgarten.core.model

import kotlin.io.encoding.Base64

/** Kleine Plattform-Brücke: Uhr, Zeitzone, Komprimierung – je eine Umsetzung für Android (jvm) und Web (wasmJs). */
expect object Platform {
    /** Jetzt in Millisekunden seit 1970 (UTC). */
    fun nowMs(): Long
    /** Abstand der Ortszeit zu UTC in Millisekunden zum Zeitpunkt [ms] (Sommerzeit inklusive). */
    fun tzOffsetMs(ms: Long): Int
    /** Rohes Deflate (ohne Kopf) – für Klassen-Codes. */
    fun deflateRaw(bytes: ByteArray): ByteArray
    fun inflateRaw(bytes: ByteArray): ByteArray
    fun gzip(bytes: ByteArray): ByteArray
    fun gunzip(bytes: ByteArray): ByteArray
}

/** URL-sicheres Base64 ohne Auffüll-Zeichen (wie bisher für Duell- und Klassen-Codes). */
object Codes {
    private val b64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
    fun encode(bytes: ByteArray): String = b64.encode(bytes)
    fun decode(s: String): ByteArray = b64.decode(s)

    private val table = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
        c
    }
    /** CRC-32 wie java.util.zip.CRC32 (als positive Zahl). */
    fun crc32(bytes: ByteArray): Long {
        var c = -1
        for (b in bytes) c = table[(c xor b.toInt()) and 0xFF] xor (c ushr 8)
        return (c.inv().toLong()) and 0xFFFFFFFFL
    }
}
