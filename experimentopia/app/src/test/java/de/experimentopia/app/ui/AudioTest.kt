package de.experimentopia.app.ui

import de.experimentopia.app.ui.fx.Composer
import de.experimentopia.core.model.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class AudioTest {
    @Test fun musikIstSauberUndAbwechslungsreich() {
        World.entries.forEach { w ->
            val a = Composer.phrase(w, 1); val b = Composer.phrase(w, 2)
            assertTrue("$w ohne Fehlwerte", a.all { it.isFinite() })
            assertTrue("$w nicht zu laut", a.maxOf { abs(it) } <= 0.56f)
            assertTrue("$w etwa 8 Takte", a.size / Composer.SR.toDouble() in 10.0..22.0)
            assertTrue("$w jede Phrase anders", a.indices.take(a.size.coerceAtMost(b.size)).count { abs(a[it] - b[it]) > 1e-3 } > a.size / 10)
        }
        val j = Composer.jingle(); assertTrue(j.all { it.isFinite() } && j.size / Composer.SR.toDouble() < 4.0)
    }

    /** Hörproben zum Anhören (build/hoerproben): Erkennungsmelodie und je Welt zwei Phrasen mit Pause */
    @Test fun hoerproben() {
        val dir = File("build/hoerproben"); dir.mkdirs()
        fun save(name: String, pcm: FloatArray) = File(dir, "$name.wav").writeBytes(de.experimentopia.app.wav(pcm, Composer.SR))
        save("0_erkennungsmelodie", Composer.jingle())
        World.entries.forEach { w -> save("${w.ordinal + 1}_${w.name.lowercase()}", Composer.phrase(w, 1) + FloatArray(Composer.SR * 2) + Composer.phrase(w, 2)) }
    }
}
