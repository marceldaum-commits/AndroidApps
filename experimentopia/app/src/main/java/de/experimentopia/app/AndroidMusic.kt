package de.experimentopia.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import de.experimentopia.app.ui.fx.Composer
import de.experimentopia.app.ui.fx.MusicPlayer
import de.experimentopia.core.model.AudioPrefs
import de.experimentopia.core.model.World
import kotlin.random.Random

/**
 * Spielt Experimentopias Musik: komponiert im Hintergrund immer neue Phrasen der aktuellen Welt ([Composer]),
 * dazwischen ein paar Sekunden Stille. Folgt der Stummschaltung des Geräts und wird leiser, während vorgelesen wird.
 */
class AndroidMusic(context: Context) : MusicPlayer {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    @Volatile private var world: World? = null
    @Volatile private var generation = 0
    @Volatile private var enabled = true
    @Volatile private var volume = 0.5f
    @Volatile private var ducked = false
    /** App im Hintergrund: still */
    @Volatile private var paused = false
    @Volatile private var jingleWanted = false
    @Volatile private var inJingle = false
    private val lock = Object()
    private val track = AudioTrack.Builder()
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(Composer.SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
        .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(Composer.SR * 4 / 2).build()

    init { Thread(::loop, "musik").apply { isDaemon = true }.start() }

    /** Musik richtet sich nach der Medien-Lautstärke (nicht nach Klingelton/Vibration): stumm nur, wenn die Medien-Lautstärke auf 0 steht */
    private fun silent() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
    override val needsTouch = false
    private fun gain() = (if (ducked) 0.3f else 1f) * volume * 0.8f

    private fun loop() {
        var seed = Random.nextInt()
        while (true) {
            val gen = generation
            val w = world
            val pcm = when {
                jingleWanted -> { jingleWanted = false; inJingle = true; Composer.jingle() }
                !enabled || paused || w == null || silent() -> null
                else -> Composer.phrase(w, seed++)
            }
            if (pcm != null && enabled && !paused && !silent()) {
                runCatching { track.setVolume(gain()); track.play() }
                var i = 0
                while (i < pcm.size && gen == generation && enabled && !paused) {
                    val n = minOf(2048, pcm.size - i)
                    track.setVolume(gain())
                    track.write(pcm, i, n, AudioTrack.WRITE_BLOCKING); i += n
                }
                if (gen != generation || !enabled) runCatching { track.pause(); track.flush() } else runCatching { track.stop() }
            }
            inJingle = false
            // Stille zwischen den Phrasen – oder warten, bis sich etwas ändert
            synchronized(lock) { if (gen == generation) runCatching { lock.wait(if (pcm == null) 60_000L else (2_500L + Random.nextLong(2_500L))) } }
        }
    }

    private fun wake() = synchronized(lock) { generation++; lock.notifyAll() }
    // Die Erkennungsmelodie spielt zu Ende, auch wenn gleich danach die Welt gesetzt wird
    override fun world(w: World?) { if (w != world) { world = w; if (!inJingle && !jingleWanted) wake() } }
    override fun jingle() { jingleWanted = true; wake() }
    override fun configure(p: AudioPrefs) { val was = enabled; enabled = p.music; volume = p.musicVol; if (was != enabled) wake() }
    override fun duck(on: Boolean) { ducked = on }
    fun pause(on: Boolean) { if (paused != on) { paused = on; wake() } }
}
