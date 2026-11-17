package de.wissensgarten.app.ui.fx

import de.wissensgarten.core.model.Composer
import de.wissensgarten.core.model.MusicMood
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.abs

class MusicSynthTest {
    @Test fun rendersQuicklyLoopsSeamlesslyAndIsNotTooLoud() {
        for (m in MusicMood.entries) {
            val t0 = System.nanoTime()
            val pcm = MusicSynth.render(Composer.compose(m))
            val ms = (System.nanoTime() - t0) / 1_000_000
            println("$m: ${pcm.size / MusicSynth.SR} s in $ms ms")
            assertTrue("$m zu langsam: $ms ms", ms < 8000)
            // Nahtstelle: Sprung zwischen letztem und erstem Wert nicht größer als sonst zwischen Nachbarn
            val jump = abs(pcm.last() - pcm.first())
            val typical = (1 until pcm.size).maxOf { abs(pcm[it] - pcm[it - 1]) }
            assertTrue("$m knackt an der Nahtstelle ($jump > $typical)", jump <= typical)
            assertTrue(pcm.maxOf { abs(it.toInt()) } < Short.MAX_VALUE)
            // zum Anhören
            File("build/musik").mkdirs()
            File("build/musik/${m.name.lowercase()}.wav").writeBytes(wav(pcm))
        }
    }

    private fun wav(s: ShortArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        fun str(x: String) = out.write(x.toByteArray())
        fun i32(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
        fun i16(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
        str("RIFF"); i32(36 + s.size * 2); str("WAVE"); str("fmt "); i32(16); i16(1); i16(1)
        i32(MusicSynth.SR); i32(MusicSynth.SR * 2); i16(2); i16(16); str("data"); i32(s.size * 2)
        s.forEach { i16(it.toInt()) }
        return out.toByteArray()
    }
}

class MusicSynthGentleTest {
    @Test fun gentleRenderIsIdenticalToNormalRender() = kotlinx.coroutines.runBlocking {
        val song = Composer.compose(MusicMood.SPRING)
        var pauses = 0
        val a = MusicSynth.render(song)
        val b = MusicSynth.renderGently(song) { pauses++ }
        assertArrayEquals(a, b)
        assertTrue("gibt die Kontrolle oft genug ab ($pauses)", pauses > 50)
    }
}
