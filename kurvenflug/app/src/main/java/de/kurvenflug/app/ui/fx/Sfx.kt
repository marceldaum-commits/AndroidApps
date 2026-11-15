package de.kurvenflug.app.ui.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** Spielt die Spiel-Geräusche. Erzeugt die WAV-Dateien beim ersten Start im Cache. */
class Sfx(context: Context) {
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

    @Volatile var enabled = true

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
    fun play(s: Sound, pitch: Float = 1f, volume: Float = 0.9f) {
        if (!enabled) return
        val id = ids[s] ?: return
        if (id !in ready) return
        pool.play(id, volume, volume, 1, 0, pitch.coerceIn(0.5f, 2f))
    }

    /**
     * Volts Plapper-Stimme: eine kurze Silbe je Wortteil, die Tonhöhe folgt den Vokalen im Text.
     * Höchstens 14 Silben, damit es niedlich bleibt und nicht nervt.
     */
    suspend fun babble(text: String) {
        if (!enabled) return
        val vowels = text.lowercase().filter { it in "aeiouäöü" }.take(14)
        for (c in vowels) {
            val pitch = when (c) { 'i' -> 1.45f; 'e' -> 1.25f; 'a' -> 1.0f; 'ä' -> 1.15f; 'o' -> 0.85f; 'ö' -> 0.95f; else -> 0.75f }
            play(Sound.VOICE, pitch, 0.35f)
            kotlinx.coroutines.delay(75)
        }
    }

    private companion object { const val VERSION_ = "v1" }
}

val LocalSfx = staticCompositionLocalOf<Sfx?> { null }
