package de.experimentopia.app

import de.experimentopia.app.ui.fx.*

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/** Spielt die Geräusche. Erzeugt die WAV-Dateien beim ersten Start im Cache. Töne mit fester Höhe werden live erzeugt. */
class AndroidSfx(context: Context) : SoundPlayer {
    private val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private val pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
    private val ids = ConcurrentHashMap<Sound, Int>()
    private val ready = ConcurrentHashMap.newKeySet<Int>()
    private val toneThread = Executors.newSingleThreadExecutor()
    private val toneCache = ConcurrentHashMap<String, FloatArray>()

    @Volatile var enabled = true
    /** Effekt-Lautstärke (Einstellungen) */
    @Volatile var volume = 0.9f
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    /** Stumm oder Vibration am Gerät: dann bleibt auch die App still */
    private fun silent() = audioManager.ringerMode != android.media.AudioManager.RINGER_MODE_NORMAL

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready += id }
        val dir = context.cacheDir
        thread(name = "sfx-init", isDaemon = true) {
            Sound.entries.forEach { s ->
                runCatching {
                    val f = File(dir, "sfx_${VERSION}_${s.name.lowercase()}.wav")
                    if (!f.exists()) f.writeBytes(wav(Synth.render(s)))
                    ids[s] = pool.load(f.path, 1)
                }
            }
        }
    }

    override fun play(s: Sound, pitch: Float, volume: Float) {
        if (!enabled || silent()) return
        val id = ids[s] ?: return
        if (id !in ready) return
        val v = (volume * this.volume).coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, pitch.coerceIn(0.5f, 2f))
    }

    /** Spielt einen Ton der Frequenz [hz] (Klangröhren, Gläser, Saite) */
    override fun tone(hz: Double, timbre: Timbre) {
        if (!enabled || silent()) return
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

    private companion object { const val VERSION = "v2" }
}


/** 16-Bit-WAV aus den im Code erzeugten Klängen (SoundPool braucht Dateien) */
fun wav(samples: FloatArray, rate: Int = Synth.SR): ByteArray {
    val data = ByteArray(samples.size * 2)
    samples.forEachIndexed { i, s ->
        val v = (s.coerceIn(-1f, 1f) * 32767).toInt()
        data[2 * i] = (v and 0xff).toByte(); data[2 * i + 1] = ((v shr 8) and 0xff).toByte()
    }
    val out = java.io.ByteArrayOutputStream()
    fun str(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
    fun i32(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
    fun i16(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
    str("RIFF"); i32(36 + data.size); str("WAVE")
    str("fmt "); i32(16); i16(1); i16(1); i32(rate); i32(rate * 2); i16(2); i16(16)
    str("data"); i32(data.size); out.write(data)
    return out.toByteArray()
}

/** Vibration über die Haptik des Bildschirms (keine Berechtigung nötig) */
class AndroidBuzzer(private val view: android.view.View) : Buzzer {
    override fun buzz(b: Buzz, enabled: Boolean) {
        if (!enabled) return
        view.performHapticFeedback(when (b) {
            Buzz.TICK -> android.view.HapticFeedbackConstants.CLOCK_TICK
            Buzz.HIT -> if (android.os.Build.VERSION.SDK_INT >= 30) android.view.HapticFeedbackConstants.CONFIRM else android.view.HapticFeedbackConstants.VIRTUAL_KEY
            Buzz.BIG -> android.view.HapticFeedbackConstants.LONG_PRESS
        })
    }
}
