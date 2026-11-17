package de.wissensgarten.app.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import de.wissensgarten.app.ui.fx.NaturalVoicePack
import de.wissensgarten.app.ui.fx.VoicePackState
import de.wissensgarten.core.model.Lautschrift
import de.wissensgarten.core.model.PiperIds
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream

/**
 * Das Stimm-Paket „Thorsten“ (wie in Raumlicht): eine natürlich klingende deutsche Stimme, die ganz auf dem Gerät rechnet.
 * Die große Stimmdatei wird nur auf Wunsch einmal geladen (und per Prüfsumme kontrolliert). Der App liegen nur kleine Daten bei:
 * die Zeichenliste der Stimme und das vorab erzeugte Lautschrift-Wörterbuch – kein Sprach-Baustein wie espeak-ng.
 */
object ThorstenFiles {
    const val MODEL = "de_DE-thorsten-medium.onnx"
    const val SIZE = 63_201_421L
    const val SHA256 = "1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6"
    /**
     * Quelle: unser eigener Stimm-Server (gemeinsam für alle Apps). Dort liegt die reine Piper-Datei ([SOURCE_SIZE] Bytes).
     * sherpa-onnx braucht zusätzlich ein paar Angaben im Modell (Abtastrate, Sprecherzahl …). Diese 127 Bytes ([SHERPA_TAIL])
     * hängt die App nach dem Laden an – das Ergebnis ist Byte für Byte die geprüfte sherpa-Fassung ([SIZE], [SHA256]).
     */
    const val URL = "https://stimmen.mathenachhilfe-online.com/thorsten/$MODEL"
    const val SOURCE_SIZE = 63_201_294L
    const val SHERPA_TAIL_B64 = "chIKCm1vZGVsX3R5cGUSBHZpdHNyEAoHY29tbWVudBIFcGlwZXJyEgoIbGFuZ3VhZ2USBkdlcm1hbnILCgV2b2ljZRICZGVyDwoKaGFzX2VzcGVhaxIBMXIPCgpuX3NwZWFrZXJzEgExchQKC3NhbXBsZV9yYXRlEgUyMjA1MA=="
    val SHERPA_TAIL: ByteArray get() = java.util.Base64.getDecoder().decode(SHERPA_TAIL_B64)
    fun dir(ctx: Context) = File(ctx.filesDir, "stimme")
    fun model(ctx: Context) = File(dir(ctx), MODEL)
    fun tokens(ctx: Context) = File(dir(ctx), "tokens.txt")
    fun installed(ctx: Context, size: Long = SIZE) = model(ctx).let { it.isFile && it.length() == size }

    /** Prüft eine geladene Datei: richtige Größe und richtige Prüfsumme */
    fun matches(f: File, size: Long = SIZE, sha256: String = SHA256): Boolean {
        if (!f.isFile || f.length() != size) return false
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { i -> val b = ByteArray(1 shl 16); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) } == sha256
    }
    /** Zeichenliste der Stimme aus der App ins Stimm-Verzeichnis legen (nach jedem App-Update frisch) */
    fun unpackTokens(ctx: Context) {
        dir(ctx).mkdirs()
        ctx.assets.open("stimme/tokens.txt").use { i -> tokens(ctx).outputStream().use { i.copyTo(it) } }
    }
    /** Lautschrift-Wörterbuch aus der App (gzip) */
    fun lexicon(ctx: Context): Lautschrift = ctx.assets.open("stimme/woerter.tsv.gz").use { raw ->
        GZIPInputStream(raw).bufferedReader().useLines { Lautschrift(it.toList().asSequence()) } }
}

/** Lädt das Stimm-Paket, zeigt den Fortschritt, lässt sich abbrechen und wieder entfernen. */
class ThorstenPack(
    private val ctx: Context,
    private val fetch: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection },
    /** Erwartete Größe und Prüfsumme – im Test mit kleinen Daten austauschbar */
    private val size: Long = ThorstenFiles.SIZE, private val sha256: String = ThorstenFiles.SHA256,
    /** wird nach dem Laden angehängt (sherpa-Angaben), siehe [ThorstenFiles.URL] */
    private val tail: ByteArray = ThorstenFiles.SHERPA_TAIL,
) : NaturalVoicePack {
    override val state = MutableStateFlow<VoicePackState>(if (ThorstenFiles.installed(ctx, size)) VoicePackState.Ready else VoicePackState.Missing)
    override val problem = MutableStateFlow<String?>(null)
    /** nach erfolgreichem Laden bzw. Entfernen */
    var onChanged: () -> Unit = {}
    @Volatile private var cancelled = false
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "stimme-laden").apply { isDaemon = true } }

    override fun download() {
        if (state.value is VoicePackState.Loading || state.value == VoicePackState.Ready) return
        cancelled = false; state.value = VoicePackState.Loading(0)
        worker.execute {
            val part = File(ThorstenFiles.dir(ctx), ThorstenFiles.MODEL + ".teil")
            val result = runCatching {
                ThorstenFiles.unpackTokens(ctx)
                val c = fetch(ThorstenFiles.URL); c.connectTimeout = 20_000; c.readTimeout = 30_000; c.instanceFollowRedirects = true
                if (c.responseCode !in 200..299) error("Der Server antwortet mit ${c.responseCode}.")
                c.inputStream.use { i -> part.outputStream().use { o -> val b = ByteArray(1 shl 16); var done = 0L; var shown = -1
                    while (true) { if (cancelled) error("abgebrochen"); val n = i.read(b); if (n < 0) break; o.write(b, 0, n); done += n
                        val p = (done * 100 / size).toInt().coerceIn(0, 99); if (p != shown) { shown = p; state.value = VoicePackState.Loading(p) } }
                    if (done + tail.size == size) o.write(tail) } }
                if (!ThorstenFiles.matches(part, size, sha256)) error("Die Datei ist unvollständig oder beschädigt angekommen.")
                if (!part.renameTo(ThorstenFiles.model(ctx))) error("Die Datei ließ sich nicht speichern.")
            }
            part.delete()
            state.value = when {
                result.isSuccess -> VoicePackState.Ready
                cancelled -> VoicePackState.Missing
                else -> VoicePackState.Failed(result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() && it != "abgebrochen" } ?: "Keine Verbindung. Bitte später noch einmal versuchen.")
            }
            if (result.isSuccess) onChanged()
        }
    }
    override fun cancel() { cancelled = true }
    override fun remove() { cancelled = true; state.value = VoicePackState.Missing; problem.value = null; onChanged(); ThorstenFiles.dir(ctx).deleteRecursively() }
}

/**
 * Thorsten spricht: rechnet die Sprache auf dem Gerät (sherpa-onnx mit der Piper-Stimme) und spielt sie ab, während sie entsteht.
 * Ein neuer Satz unterbricht den alten. Lässt sich die Stimme nicht starten, bleibt [ready] falsch – dann spricht die Gerätestimme.
 */
class ThorstenEngine(private val ctx: Context, private val pack: ThorstenPack, private val onSpeaking: (Boolean) -> Unit) {
    @Volatile var ready = false
        private set
    @Volatile var lexicon: Lautschrift? = null
        private set
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "thorsten").apply { isDaemon = true } }
    private val turn = AtomicInteger(0)
    @Volatile private var speaking = false
    private var tts: OfflineTts? = null
    private var track: AudioTrack? = null
    @Volatile private var loading = false

    /** Stimme im Hintergrund starten (dauert einen Moment) – nur wenn das Paket vollständig da ist */
    fun load() {
        if (ready || loading || !ThorstenFiles.installed(ctx)) return
        loading = true
        worker.execute {
            runCatching {
                ThorstenFiles.unpackTokens(ctx)
                val lex = ThorstenFiles.lexicon(ctx)
                val engine = OfflineTts(null, OfflineTtsConfig(model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(model = ThorstenFiles.model(ctx).path, tokens = ThorstenFiles.tokens(ctx).path),
                    numThreads = 2, provider = "cpu")))
                val rate = engine.sampleRate()
                val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
                track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(min, rate * 4 / 2)).build()
                tts = engine; lexicon = lex
            }.onSuccess { pack.problem.value = null; ready = true }
             .onFailure { pack.problem.value = "Thorsten ließ sich auf diesem Gerät nicht starten (${it.javaClass.simpleName}) – Brummi liest mit der Gerätestimme."; ready = false }
            loading = false
        }
    }
    fun unload() { stop(); ready = false; lexicon = null; worker.execute { runCatching { track?.release() }; runCatching { tts?.release() }; track = null; tts = null } }

    /** Sätze (Lautschrift) sprechen; [done] kommt, wenn fertig oder unterbrochen */
    fun say(sentences: List<String>, speed: Float, done: (() -> Unit)? = null) {
        val mine = turn.incrementAndGet()                                   // alles Ältere ist damit überholt
        runCatching { track?.pause(); track?.flush() }
        worker.execute {
            val engine = tts; val out = track
            if (engine == null || out == null || mine != turn.get()) { done?.invoke(); return@execute }
            runCatching {
                speaking = true; onSpeaking(true)
                out.play()
                engine.generateWithConfigAndCallback(sentences.joinToString(" "), GenerationConfig(speed = speed,
                    phonemeCodepoints = sentences.map { PiperIds.codepoints(it) }.toTypedArray())) { samples ->
                    if (mine != turn.get()) 0 else { out.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING); 1 }
                }
                if (mine == turn.get()) out.stop()                           // spielt den Rest aus dem Puffer noch zu Ende
            }
            if (mine == turn.get()) { speaking = false; onSpeaking(false) }
            done?.invoke()
        }
    }
    fun stop() { turn.incrementAndGet(); runCatching { track?.pause(); track?.flush() }; if (speaking) { speaking = false; onSpeaking(false) } }
}
