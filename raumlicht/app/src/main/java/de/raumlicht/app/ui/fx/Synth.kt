package de.raumlicht.app.ui.fx

import java.io.ByteArrayOutputStream

/** Verpackt die im Code erzeugten Klänge ([de.raumlicht.core.model.Klang]) als WAV, damit Android sie abspielen kann. Die Klänge selbst entstehen im gemeinsamen Rechenkern – für Android und WebApp dieselben. */
object Synth {
    fun wav(samples: FloatArray, sr: Int): ByteArray {
        val data = ByteArray(samples.size * 2)
        samples.forEachIndexed { i, s ->
            val v = (s.coerceIn(-1f, 1f) * 32767).toInt()
            data[2 * i] = (v and 0xff).toByte(); data[2 * i + 1] = ((v shr 8) and 0xff).toByte()
        }
        val out = ByteArrayOutputStream()
        fun str(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun i32(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
        fun i16(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
        str("RIFF"); i32(36 + data.size); str("WAVE")
        str("fmt "); i32(16); i16(1); i16(1); i32(sr); i32(sr * 2); i16(2); i16(16)
        str("data"); i32(data.size); out.write(data)
        return out.toByteArray()
    }
}
