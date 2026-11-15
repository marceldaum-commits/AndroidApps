package de.kurvenflug.core.model

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

actual fun zipDeflate(data: ByteArray): ByteArray {
    val d = Deflater(Deflater.BEST_COMPRESSION, true)
    d.setInput(data); d.finish()
    val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
    while (!d.finished()) out.write(buf, 0, d.deflate(buf))
    d.end()
    return out.toByteArray()
}

actual fun zipInflate(data: ByteArray, limit: Int): ByteArray {
    val inf = Inflater(true); inf.setInput(data)
    val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
    var guard = 0
    while (!inf.finished() && guard++ < 200) { val n = inf.inflate(buf); if (n == 0 && inf.needsInput()) break; out.write(buf, 0, n) }
    inf.end()
    require(out.size() < limit)
    return out.toByteArray()
}
