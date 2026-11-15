package de.wissensgarten.core.model

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater

actual object Platform {
    actual fun nowMs(): Long = System.currentTimeMillis()
    actual fun tzOffsetMs(ms: Long): Int = java.util.TimeZone.getDefault().getOffset(ms)
    actual fun deflateRaw(bytes: ByteArray): ByteArray {
        val d = Deflater(Deflater.BEST_COMPRESSION, true).apply { setInput(bytes); finish() }
        val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end(); return out.toByteArray()
    }
    actual fun inflateRaw(bytes: ByteArray): ByteArray {
        val inf = Inflater(true).apply { setInput(bytes) }
        val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
        while (!inf.finished()) {
            val n = inf.inflate(buf)
            if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
            out.write(buf, 0, n)
            if (out.size() > 2_000_000) error("zu groß")
        }
        inf.end(); return out.toByteArray()
    }
    actual fun gzip(bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { o -> GZIPOutputStream(o).use { it.write(bytes) } }.toByteArray()
    actual fun gunzip(bytes: ByteArray): ByteArray = GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
}

/** Für Tests und Android: Kalendertag in einer bestimmten Zeitzone. */
fun Scheduler.localDay(ms: Long, zone: java.util.TimeZone): Long = localDay(ms) { zone.getOffset(it) }
fun WeekReport.build(entries: List<WeekReport.Entry>, cards: Map<String, WeekReport.CardInfo>, now: Long, zone: java.util.TimeZone) =
    build(entries, cards, now) { zone.getOffset(it) }
