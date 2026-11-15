package de.wissensgarten.app.platform

import de.wissensgarten.app.ui.fx.Sfx
import de.wissensgarten.app.ui.fx.Sound
import de.wissensgarten.app.ui.fx.Synth

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** Spielt die Garten-Geräusche. Erzeugt die WAV-Dateien beim ersten Start im Cache. */
class AndroidSfx(context: Context) : Sfx {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val ids = ConcurrentHashMap<Sound, Int>()
    private val ready = ConcurrentHashMap.newKeySet<Int>()

    @Volatile override var enabled = true

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready += id }
        val dir = context.cacheDir
        thread(name = "sfx-init", isDaemon = true) {
            Sound.entries.forEach { s ->
                runCatching {
                    val f = File(dir, "sfx_$VERSION_${s.name.lowercase()}.wav")
                    if (!f.exists()) f.writeBytes(Synth.wav(Synth.render(s)))
                    ids[s] = pool.load(f.path, 1)
                }
            }
        }
    }

    /** @param pitch 1 = normal, >1 höher (z. B. bei Serien) */
    override fun play(s: Sound, pitch: Float, volume: Float) {
        if (!enabled) return
        val id = ids[s] ?: return
        if (id !in ready) return
        pool.play(id, volume, volume, 1, 0, pitch.coerceIn(0.5f, 2f))
    }

    private companion object { const val VERSION_ = "v1" }
}

