package de.kurvenflug.app.ui.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import de.kurvenflug.core.model.Lautschrift
import de.kurvenflug.core.model.Speech
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Volt liest vor (Android). Zwei Stimmen:
 *  - die Sprachausgabe des Geräts (nur deutsche Stimmen, die ohne Internet sprechen – es geht kein Text ins Netz),
 *  - „Thorsten“: natürliche Piper-Stimme, rechnet ganz auf dem Gerät (sherpa-onnx). Die große Stimmdatei wird nur auf Wunsch
 *    einmal geladen (von huggingface.co, per Prüfsumme kontrolliert). Die Laute kommen aus der mitgelieferten Lautschrift
 *    (assets/stimme) – kein espeak-ng (GPL) in der App. Kennt die Lautschrift ein Wort nicht, liest diesen Text die Gerätestimme.
 * Übernommen aus Raumlicht (gleicher Weg, gleiche Stimmdatei).
 */
class AndroidReader(context: Context, override val canLoadPack: Boolean) : Reader {
    private val ctx = context.applicationContext
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    override var voices by mutableStateOf(emptyList<VoiceChoice>()); private set
    override var pack by mutableStateOf<PackState>(if (Pack.installed(ctx)) PackState.Ready else PackState.Missing); private set
    override var speaking by mutableStateOf(false); private set

    private var wanted = ""
    private var rate = 1f
    private fun natural() = wanted == Reader.NATURAL && pack == PackState.Ready

    // ------------------------------------------------------------------ Stimme des Geräts
    private var ttsReady = false
    private var ttsChecked = false
    private var pending: String? = null
    // Erst beim ersten Gebrauch anlegen – kostet beim App-Start dann keine Zeit
    private val tts: TextToSpeech by lazy { TextToSpeech(ctx) { status -> main.post { if (status == TextToSpeech.SUCCESS) setUp(); ttsChecked = true; pending?.let { p -> pending = null; say(p) } } } }

    private fun setUp() {
        if (runCatching { tts.setLanguage(Locale.GERMANY) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED) < TextToSpeech.LANG_AVAILABLE) return
        voices = runCatching {
            tts.voices.orEmpty().filter { it.locale.language == "de" && !it.isNetworkConnectionRequired && "notInstalled" !in it.features.orEmpty() }
                .sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }.thenBy { it.name })
                .mapIndexed { i, v -> VoiceChoice(v.name, "Gerät ${i + 1}" + if (v.locale.country.isNotEmpty() && v.locale.country != "DE") " (${v.locale.country})" else "") }
        }.getOrDefault(emptyList())
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { main.post { speaking = true } }
            override fun onDone(id: String?) { main.post { speaking = false } }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { main.post { speaking = false } }
            override fun onStop(id: String?, interrupted: Boolean) { main.post { speaking = false } }
        })
        ttsReady = true
        applyDevice()
    }
    private fun applyDevice() {
        if (!ttsReady) return
        val name = voices.firstOrNull { it.id == wanted }?.id ?: voices.firstOrNull()?.id
        runCatching { tts.voices?.firstOrNull { it.name == name }?.let { tts.voice = it } }
        tts.setPitch(1.08f); tts.setSpeechRate(0.95f * rate)
    }
    private fun sayDevice(clean: String) {
        if (!ttsReady) { if (!ttsChecked) pending = clean; return }
        runCatching { tts.speak(clean, TextToSpeech.QUEUE_FLUSH, Bundle(), "volt") }
    }

    override fun configure(voice: String, rate: Float) {
        used = true; tts   // Sprachausgabe anwerfen (Stimmen-Liste kommt kurz danach)
        this.wanted = voice; this.rate = rate.coerceIn(0.6f, 1.5f); applyDevice()
        if (voice == Reader.NATURAL) loadEngine()
    }

    override fun say(text: String) {
        val clean = Speech.clean(text); if (clean.isEmpty()) return
        if (!used) { used = true; tts }
        if (natural()) {
            val laute = sounds?.of(clean)
            if (engine != null && laute != null) { runCatching { tts.stop() }; sayNatural(clean, laute); return }
            if (engine == null) loadEngine()
        }
        stopNatural(); sayDevice(clean)
    }

    private var used = false
    override fun stop() { pending = null; if (used) runCatching { tts.stop() }; stopNatural(); speaking = false }

    // ------------------------------------------------------------------ Thorsten
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "volt-stimme").apply { isDaemon = true } }
    private val turn = AtomicInteger(0)
    @Volatile private var engine: OfflineTts? = null
    @Volatile private var sounds: Lautschrift? = null
    @Volatile private var loading = false
    private var track: AudioTrack? = null

    private fun loadEngine() {
        if (engine != null || loading || !Pack.installed(ctx)) return
        loading = true
        worker.execute {
            runCatching {
                val dir = Pack.dir(ctx); Pack.unpackHelpers(ctx)
                sounds = Lautschrift(File(dir, "saetze.tsv").readLines().asSequence(), File(dir, "woerter.tsv").readLines().asSequence())
                val e = OfflineTts(null, OfflineTtsConfig(model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(model = Pack.model(ctx).path, tokens = File(dir, "tokens.txt").path), numThreads = 2, provider = "cpu")))
                val sr = e.sampleRate()
                val min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
                track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(min, sr * 2)).build()
                engine = e
            }.onFailure { main.post { pack = PackState.Failed("Die Stimme ließ sich auf diesem Gerät nicht starten (${it.javaClass.simpleName}).") } }
            loading = false
        }
    }

    private fun sayNatural(clean: String, laute: List<String>) {
        val mine = turn.incrementAndGet()
        runCatching { track?.pause(); track?.flush() }
        worker.execute {
            val e = engine ?: return@execute; val out = track ?: return@execute
            if (mine != turn.get()) return@execute
            main.post { speaking = true }
            runCatching {
                out.play()
                e.generateWithConfigAndCallback(clean, GenerationConfig(speed = rate, phonemeCodepoints = laute.map { Lautschrift.codepoints(it) }.toTypedArray())) { samples ->
                    if (mine != turn.get()) 0 else { out.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING); 1 }
                }
                if (mine == turn.get()) out.stop()
            }
            if (mine == turn.get()) main.post { speaking = false }
        }
    }
    private fun stopNatural() { turn.incrementAndGet(); runCatching { track?.pause(); track?.flush() } }

    // ------------------------------------------------------------------ Stimm-Paket laden
    @Volatile private var cancelled = false
    private val loader = Executors.newSingleThreadExecutor { Thread(it, "stimme-laden").apply { isDaemon = true } }

    override fun loadPack() {
        if (!canLoadPack) return
        if (pack is PackState.Loading || pack == PackState.Ready) return
        cancelled = false; pack = PackState.Loading(0)
        loader.execute {
            val part = File(Pack.dir(ctx), Pack.MODEL + ".teil")
            val result = runCatching {
                Pack.unpackHelpers(ctx)
                val c = URL(Pack.URL).openConnection() as HttpURLConnection
                c.connectTimeout = 20_000; c.readTimeout = 30_000; c.instanceFollowRedirects = true
                if (c.responseCode !in 200..299) error("Der Server antwortet mit ${c.responseCode}.")
                c.inputStream.use { i -> part.outputStream().use { o -> val b = ByteArray(1 shl 16); var done = 0L; var shown = -1
                    while (true) { if (cancelled) error("abgebrochen"); val n = i.read(b); if (n < 0) break; o.write(b, 0, n); done += n
                        val p = (done * 100 / Pack.SIZE).toInt().coerceIn(0, 99); if (p != shown) { shown = p; main.post { if (pack is PackState.Loading) pack = PackState.Loading(p) } } } } }
                if (!Pack.matches(part)) error("Die Datei ist unvollständig oder beschädigt angekommen.")
                if (!part.renameTo(Pack.model(ctx))) error("Die Datei ließ sich nicht speichern.")
            }
            part.delete()
            main.post {
                pack = when { result.isSuccess -> PackState.Ready; cancelled -> PackState.Missing
                    else -> PackState.Failed(result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() && it != "abgebrochen" } ?: "Keine Verbindung. Bitte später noch einmal versuchen.") }
                if (result.isSuccess && wanted == Reader.NATURAL) loadEngine()
            }
        }
    }
    override fun cancelPack() { cancelled = true }
    override fun removePack() {
        cancelled = true; stopNatural()
        worker.execute { runCatching { track?.release() }; runCatching { engine?.release() }; track = null; engine = null; sounds = null; Pack.dir(ctx).deleteRecursively() }
        pack = PackState.Missing
    }

    fun shutdown() { stop(); if (used) runCatching { tts.shutdown() } }

    /** Das Stimm-Paket „Thorsten“ (dieselbe Datei wie in Raumlicht) */
    object Pack {
        const val MODEL = "de_DE-thorsten-medium.onnx"
        const val SIZE = 63_201_421L
        const val SHA256 = "1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6"
        const val URL = "https://huggingface.co/csukuangfj/vits-piper-de_DE-thorsten-medium/resolve/main/$MODEL"
        fun dir(ctx: Context) = File(ctx.filesDir, "stimme").also { it.mkdirs() }
        fun model(ctx: Context) = File(dir(ctx), MODEL)
        fun installed(ctx: Context) = model(ctx).let { it.isFile && it.length() == SIZE }
        fun matches(f: File): Boolean {
            if (!f.isFile || f.length() != SIZE) return false
            val md = MessageDigest.getInstance("SHA-256"); f.inputStream().use { i -> val b = ByteArray(1 shl 16); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) } }
            return md.digest().joinToString("") { "%02x".format(it) } == SHA256
        }
        /** Zeichenliste und Lautschrift aus der App ins Stimm-Verzeichnis (nach jedem Update neu, damit neue Texte drin sind) */
        fun unpackHelpers(ctx: Context) {
            ctx.assets.list("stimme").orEmpty().forEach { n -> ctx.assets.open("stimme/$n").use { i -> File(dir(ctx), n).outputStream().use { i.copyTo(it) } } }
        }
    }
}
