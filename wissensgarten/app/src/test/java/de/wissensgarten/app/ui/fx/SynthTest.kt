package de.wissensgarten.app.ui.fx

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class SynthTest {
    @Test fun allSoundsRenderCleanly() {
        val out = File("build/sounds").apply { mkdirs() }
        for (s in Sound.entries) {
            val a = Synth.render(s)
            assertTrue("$s leer", a.size > 1000)
            assertTrue("$s ungültige Werte", a.none { it.isNaN() || abs(it) > 1f })
            assertTrue("$s zu leise", a.maxOf { abs(it) } > 0.5f)
            // kein Knacken am Ende
            assertTrue("$s endet nicht leise", abs(a.last()) < 0.05f)
            File(out, "${s.name.lowercase()}.wav").writeBytes(Synth.wav(a))
        }
    }
}
