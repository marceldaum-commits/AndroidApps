package de.wissensgarten.app.platform

import de.wissensgarten.app.ui.fx.Music
import de.wissensgarten.app.ui.fx.MusicSynth

import android.content.Context
import android.media.*
import de.wissensgarten.core.model.Composer
import de.wissensgarten.core.model.MusicMood
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Leise Hintergrundmusik im Code erzeugt (siehe [Composer], [MusicSynth]) – keine Audiodateien, keine Lizenzen.
 * Nahtlos wiederholt, sanfte Überblendungen, Pause im Hintergrund, leiser während Brummi spricht.
 * Läuft schon andere Musik auf dem Handy (z. B. Hörspiel), bleibt der Garten still.
 */
class AndroidMusic(private val context: Context) : Music {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val audio = context.getSystemService(AudioManager::class.java)
    private var track: AudioTrack? = null
    private var current: MusicMood? = null
    private var wanted: MusicMood? = null
    private var enabled = true
    private var foreground = false
    private var ducked = false
    private var level = 0f
    private val cache = HashMap<MusicMood, ShortArray>()
    private var fadeJob: Job? = null
    private var interrupted = false
    private var volume = 0.45f
    /** Erst nach der ersten Berührung spielen – nie ungefragt beim Start */
    private var unlocked = false

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes())
        .setOnAudioFocusChangeListener { change ->
            scope.launch {
                when (change) {
                    // dauerhaft verloren (andere Musik-App): aufhören
                    AudioManager.AUDIOFOCUS_LOSS -> stopNow()
                    // kurz unterbrochen (Assistent, Navi-Ansage, Wecker): nur anhalten, Fokus behalten → danach weiter
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { fadeTo(0f, 200); runCatching { track?.pause() }; interrupted = true }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> fadeTo(0.3f * BASE)
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (interrupted) { interrupted = false; runCatching { track?.play() } }
                        refresh()
                    }
                }
            }
        }.build()

    init {
        // Stellt jemand das Handy auf lautlos, verstummt die Musik sofort (und kommt danach wieder)
        runCatching {
            androidx.core.content.ContextCompat.registerReceiver(context.applicationContext, object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context?, i: android.content.Intent?) { scope.launch { refresh() } }
            }, android.content.IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }

    override fun setEnabled(on: Boolean) { scope.launch { enabled = on; refresh() } }
    override fun setMood(m: MusicMood) { scope.launch { wanted = m; refresh() } }
    override fun onForeground(on: Boolean) { scope.launch { foreground = on; refresh() } }
    override fun setVolume(v: Float) { scope.launch { volume = v.coerceIn(0f, 1f); if (track != null) fadeTo(target(), 250) else refresh() } }
    override fun unlock() { scope.launch { if (!unlocked) { unlocked = true; refresh() } } }
    override fun duck(on: Boolean) { scope.launch { ducked = on; if (track != null) fadeTo(target()) } }

    /** Musik schon vorab erzeugen, damit sie sofort da ist. */
    override fun prepare(vararg moods: MusicMood) { scope.launch { moods.forEach { pcm(it) } } }

    private fun target() = BASE * volume * 2f * (if (ducked) 0.35f else 1f)

    /** Handy lautlos/Vibration: dann auch keine Musik */
    private fun silentMode() = runCatching { audio.ringerMode != AudioManager.RINGER_MODE_NORMAL }.getOrDefault(false)

    private suspend fun refresh() {
        val m = wanted
        val shouldPlay = enabled && foreground && unlocked && volume > 0f && !silentMode() && m != null
        if (!shouldPlay) { fadeOutAndStop(); return }
        if (current == m && track != null) { fadeTo(target()); return }
        // Andere App spielt Musik (Hörbuch, Spotify …)? Dann nicht dazwischenfunken.
        if (track == null && audio.isMusicActive) return
        fadeOutAndStop()
        if (audio.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        val data = pcm(m!!)
        val t = AudioTrack.Builder()
            .setAudioAttributes(attributes())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(MusicSynth.SR)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
        t.write(data, 0, data.size)
        t.setLoopPoints(0, data.size, -1)
        level = 0f; t.setVolume(0f)
        t.play()
        track = t; current = m
        fadeTo(target(), 1500)
    }

    private suspend fun fadeTo(to: Float, ms: Long = 600) {
        fadeJob?.cancelAndJoin()
        val t = track ?: return
        val from = level
        fadeJob = scope.launch {
            val steps = (ms / 30).coerceAtLeast(1)
            for (i in 1..steps) {
                level = from + (to - from) * i / steps
                runCatching { t.setVolume(level) }
                delay(30)
            }
        }
        fadeJob?.join()
    }

    private suspend fun fadeOutAndStop() {
        if (track == null) return
        fadeTo(0f, 500)
        stopNow()
    }

    private fun stopNow() {
        track?.runCatching { stop(); release() }
        track = null; current = null; level = 0f
        audio.abandonAudioFocusRequest(focusRequest)
    }

    /** Erzeugt das Stück einmal und legt es im Cache ab (nächster Start: sofort da). */
    private fun pcm(m: MusicMood): ShortArray = cache.getOrPut(m) {
        val f = File(context.cacheDir, "musik/${m.name.lowercase()}-v$VERSION.pcm")
        if (f.exists()) runCatching {
            val bb = ByteBuffer.wrap(f.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            return@getOrPut ShortArray(bb.remaining()).also { bb.get(it) }
        }
        val data = MusicSynth.render(Composer.compose(m))
        runCatching {
            f.parentFile?.mkdirs()
            val bb = ByteBuffer.allocate(data.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            bb.asShortBuffer().put(data)
            f.writeBytes(bb.array())
        }
        data
    }

    private fun attributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

    companion object {
        /** Musik bewusst leise – Geräusche und Brummi sollen vorne bleiben (bei Lautstärke 50 %). */
        const val BASE = 0.32f
        /** Erhöhen, wenn sich Komponist oder Klang ändern (alter Cache wird dann ignoriert). */
        const val VERSION = 2
    }
}
