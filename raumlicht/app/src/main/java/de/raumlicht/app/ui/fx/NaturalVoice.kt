package de.raumlicht.app.ui.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import de.raumlicht.core.model.Lautschrift
import de.raumlicht.core.model.Speech
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Das Stimm-Paket „Thorsten“: eine natürlich klingende deutsche Stimme, die ganz auf dem Gerät rechnet.
 * Die große Stimmdatei wird nur auf Wunsch einmal geladen (und per Prüfsumme kontrolliert). Der App liegen nur kleine Daten bei:
 * die Zeichenliste der Stimme und die vorab erzeugte Lautschrift ([Lautschrift]) – kein Sprach-Baustein wie espeak-ng (seit 2.6.0).
 */
object VoicePack {
    const val MODEL = "de_DE-thorsten-medium.onnx"
    const val SIZE = 63_201_421L
    const val SHA256 = "1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6"
    const val URL = "https://huggingface.co/csukuangfj/vits-piper-de_DE-thorsten-medium/resolve/main/$MODEL"
    const val MB = 63
    fun dir(ctx: Context) = File(ctx.filesDir, "stimme")
    fun model(ctx: Context) = File(dir(ctx), MODEL)
    fun installed(ctx: Context, size: Long = SIZE) = model(ctx).let { it.isFile && it.length() == size } && File(dir(ctx), "tokens.txt").isFile

    /** Prüft eine geladene Datei: richtige Größe und richtige Prüfsumme */
    fun matches(f: File, size: Long = SIZE, sha256: String = SHA256): Boolean {
        if (!f.isFile || f.length() != size) return false
        val md = MessageDigest.getInstance("SHA-256"); f.inputStream().use { i -> val b = ByteArray(1 shl 16); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) } == sha256
    }
    /** Hilfsdaten (Zeichenliste, Lautschrift) aus der App ins Stimm-Verzeichnis legen; Reste früherer Versionen (espeak-ng-Daten) wegräumen */
    fun unpackHelpers(ctx: Context) {
        File(dir(ctx), "espeak-ng-data").deleteRecursively()
        fun copy(path: String, to: File) { val kids = ctx.assets.list(path).orEmpty()
            if (kids.isEmpty()) { to.parentFile?.mkdirs(); ctx.assets.open(path).use { i -> to.outputStream().use { i.copyTo(it) } } } else kids.forEach { copy("$path/$it", File(to, it)) } }
        copy("stimme", dir(ctx))
    }
}

sealed interface PackState {
    data object Missing : PackState
    data class Loading(val percent: Int) : PackState
    data class Failed(val why: String) : PackState
    data object Ready : PackState
}

/** Lädt das Stimm-Paket, zeigt den Fortschritt, lässt sich abbrechen und wieder entfernen. */
class VoicePackLoader(private val ctx: Context, private val fetch: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection }, private val onReady: () -> Unit = {},
                      /** Erwartete Größe und Prüfsumme – im Test mit kleinen Daten austauschbar */
                      private val size: Long = VoicePack.SIZE, private val sha256: String = VoicePack.SHA256) {
    var state by mutableStateOf<PackState>(if (VoicePack.installed(ctx, size)) PackState.Ready else PackState.Missing)
        private set
    @Volatile private var cancelled = false
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "stimme-laden").apply { isDaemon = true } }

    fun start() {
        if (state is PackState.Loading || state == PackState.Ready) return
        cancelled = false; state = PackState.Loading(0)
        worker.execute {
            val part = File(VoicePack.dir(ctx), VoicePack.MODEL + ".teil")
            val result = runCatching {
                VoicePack.unpackHelpers(ctx)
                val c = fetch(VoicePack.URL); c.connectTimeout = 20_000; c.readTimeout = 30_000; c.instanceFollowRedirects = true
                if (c.responseCode !in 200..299) error("Der Server antwortet mit ${c.responseCode}.")
                c.inputStream.use { i -> part.outputStream().use { o -> val b = ByteArray(1 shl 16); var done = 0L; var shown = -1
                    while (true) { if (cancelled) error("abgebrochen"); val n = i.read(b); if (n < 0) break; o.write(b, 0, n); done += n
                        val p = (done * 100 / size).toInt().coerceIn(0, 99); if (p != shown) { shown = p; state = PackState.Loading(p) } } } }
                if (!VoicePack.matches(part, size, sha256)) error("Die Datei ist unvollständig oder beschädigt angekommen.")
                if (!part.renameTo(VoicePack.model(ctx))) error("Die Datei ließ sich nicht speichern.")
            }
            part.delete()
            state = when { result.isSuccess -> PackState.Ready; cancelled -> PackState.Missing
                else -> PackState.Failed(result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() } ?: "Keine Verbindung. Bitte später noch einmal versuchen.") }
            if (result.isSuccess) onReady()
        }
    }
    fun cancel() { cancelled = true }
    fun remove() { cancelled = true; VoicePack.dir(ctx).deleteRecursively(); state = PackState.Missing }
}

/**
 * Lux mit natürlicher Stimme: rechnet die Sprache auf dem Gerät (sherpa-onnx mit der Piper-Stimme „Thorsten“) und spielt sie ab, während sie entsteht.
 * Ein neuer Satz unterbricht den alten. Ist das Stimm-Paket nicht da oder lässt es sich nicht starten, bleibt [ready] falsch – dann spricht die Handy-Stimme.
 * Die Laute kommen aus der mitgelieferten [Lautschrift]; kennt sie ein Wort nicht, liest diesen einen Text die Handy-Stimme ([fallback]).
 */
class NaturalVoice(private val ctx: Context, private val fallback: Speaker? = null, private val onSpeaking: (Boolean) -> Unit = {}) : Speaker {
    @Volatile var enabled = true
    @Volatile var volume = 1f
    var ready by mutableStateOf(false)
        private set
    /** Warum die Stimme nicht startet (für die Einstellungen) */
    var problem by mutableStateOf<String?>(null)
        private set
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "lux-stimme").apply { isDaemon = true } }
    private val turn = AtomicInteger(0)
    @Volatile private var speaking = false
    private var tts: OfflineTts? = null
    @Volatile private var sounds: Lautschrift? = null
    private var track: AudioTrack? = null

    /** Stimme im Hintergrund starten (dauert einen Moment) – nur wenn das Paket vollständig da ist */
    fun load() {
        if (ready || !VoicePack.installed(ctx)) return
        worker.execute {
            runCatching {
                val dir = VoicePack.dir(ctx)
                VoicePack.unpackHelpers(ctx)                              // nach einem App-Update: neue Lautschrift übernehmen, Altes wegräumen
                sounds = Lautschrift(File(dir, "saetze.tsv").readLines().asSequence(), File(dir, "woerter.tsv").readLines().asSequence())
                val engine = OfflineTts(null, OfflineTtsConfig(model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(model = VoicePack.model(ctx).path, tokens = File(dir, "tokens.txt").path),
                    numThreads = 2, provider = "cpu")))
                val rate = engine.sampleRate()
                val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
                track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(min, rate * 4 / 2)).build()
                tts = engine
            }.onSuccess { problem = null; ready = true }.onFailure { problem = "Die Stimme ließ sich auf diesem Gerät nicht starten (${it.javaClass.simpleName})."; ready = false }
        }
    }
    fun unload() { stop(); ready = false; worker.execute { runCatching { track?.release() }; runCatching { tts?.release() }; track = null; tts = null } }

    override fun say(text: String) {
        if (!enabled || volume <= 0f || !ready) return
        val clean = Speech.clean(text); if (clean.isEmpty()) return
        val laute = sounds?.of(clean)
        if (laute == null) { stop(); fallback?.say(text); return }           // unbekanntes Wort: diesen Text liest die Handy-Stimme
        val mine = turn.incrementAndGet()                                   // alles Ältere ist damit überholt
        runCatching { track?.pause(); track?.flush() }
        worker.execute {
            val engine = tts ?: return@execute; val out = track ?: return@execute
            if (mine != turn.get()) return@execute
            runCatching {
                speaking = true; onSpeaking(true)
                out.setVolume(volume.coerceIn(0f, 1f)); out.play()
                engine.generateWithConfigAndCallback(clean, GenerationConfig(speed = 1.0f, phonemeCodepoints = laute.map { Lautschrift.codepoints(it) }.toTypedArray())) { samples ->
                    if (mine != turn.get()) 0 else { out.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING); 1 }
                }
                if (mine == turn.get()) out.stop()                           // spielt den Rest aus dem Puffer noch zu Ende
            }
            if (mine == turn.get()) { speaking = false; onSpeaking(false) }
        }
    }
    override fun sayIfQuiet(text: String) { if (!speaking) say(text) }
    fun stop() { turn.incrementAndGet(); runCatching { track?.pause(); track?.flush() }; if (speaking) { speaking = false; onSpeaking(false) } }
}

/** Schickt jede Sprechblase an die natürliche Stimme, wenn sie geladen und gewünscht ist – sonst an die Stimme des Handys. */
class VoiceRouter(private val device: Speaker, private val natural: Speaker, private val useNatural: () -> Boolean) : Speaker {
    override fun say(text: String) = (if (useNatural()) natural else device).say(text)
    override fun sayIfQuiet(text: String) = (if (useNatural()) natural else device).sayIfQuiet(text)
}
