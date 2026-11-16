package de.experimentopia.app.ui.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/** Spielt die Geräusche. Erzeugt die WAV-Dateien beim ersten Start im Cache. Töne mit fester Höhe werden live erzeugt. */
class Sfx(context: Context) {
    private val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private val pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
    private val ids = ConcurrentHashMap<Sound, Int>()
    private val ready = ConcurrentHashMap.newKeySet<Int>()
    private val toneThread = Executors.newSingleThreadExecutor()
    private val toneCache = ConcurrentHashMap<String, FloatArray>()

    @Volatile var enabled = true

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready += id }
        val dir = context.cacheDir
        thread(name = "sfx-init", isDaemon = true) {
            Sound.entries.forEach { s ->
                runCatching {
                    val f = File(dir, "sfx_${VERSION}_${s.name.lowercase()}.wav")
                    if (!f.exists()) f.writeBytes(Synth.wav(Synth.render(s)))
                    ids[s] = pool.load(f.path, 1)
                }
            }
        }
    }

    fun play(s: Sound, pitch: Float = 1f, volume: Float = 0.9f) {
        if (!enabled) return
        val id = ids[s] ?: return
        if (id !in ready) return
        pool.play(id, volume, volume, 1, 0, pitch.coerceIn(0.5f, 2f))
    }

    /** Spielt einen Ton der Frequenz [hz] (Klangröhren, Gläser, Saite) */
    fun tone(hz: Double, timbre: Timbre) {
        if (!enabled) return
        toneThread.execute {
            runCatching {
                val key = "${timbre.name}_${hz.toInt()}"
                val f = toneCache.getOrPut(key) { Synth.tone(hz, timbre) }
                val pcm = ShortArray(f.size) { (f[it] * 32767).toInt().toShort() }
                val track = AudioTrack.Builder().setAudioAttributes(attrs)
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Synth.SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                track.write(pcm, 0, pcm.size); track.play()
                Thread.sleep((pcm.size * 1000L / Synth.SR) + 50)
                track.release()
            }
        }
    }

    private companion object { const val VERSION = "v1" }
}

val LocalSfx = staticCompositionLocalOf<Sfx?> { null }
