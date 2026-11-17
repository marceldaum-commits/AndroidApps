package de.experimentopia.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import de.experimentopia.app.ui.fx.NaturalState
import de.experimentopia.app.ui.fx.Reader
import de.experimentopia.app.ui.fx.VoiceOption
import de.experimentopia.core.model.AudioPrefs
import de.experimentopia.core.model.Lautschrift
import de.experimentopia.core.model.Speech
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Die Stimmdatei „Thorsten“ (Piper, Aufnahmen CC0): wird nur auf Wunsch einmal geladen und per Prüfsumme kontrolliert – wie bei Raumlicht.
 * Der App liegen nur kleine Daten bei: Zeichenliste und vorab erzeugte Lautschrift (tools/stimme), kein espeak-ng.
 */
object VoicePack {
    const val MODEL = "de_DE-thorsten-medium.onnx"
    const val SIZE = 63_201_421L
    const val SHA256 = "1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6"
    const val URL = "https://huggingface.co/csukuangfj/vits-piper-de_DE-thorsten-medium/resolve/main/$MODEL"
    fun dir(ctx: Context) = File(ctx.filesDir, "stimme")
    fun model(ctx: Context) = File(dir(ctx), MODEL)
    fun installed(ctx: Context, size: Long = SIZE) = model(ctx).let { it.isFile && it.length() == size }
    fun matches(f: File, size: Long = SIZE, sha256: String = SHA256): Boolean {
        if (!f.isFile || f.length() != size) return false
        val md = MessageDigest.getInstance("SHA-256"); f.inputStream().use { i -> val b = ByteArray(1 shl 16); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) } == sha256
    }
}

/** Vorlesen unter Android: Stimmen des Geräts (TextToSpeech) oder „Thorsten“ (sherpa-onnx, rechnet auf dem Gerät). */
class AndroidReader(ctx: Context) : Reader {
    private val app = ctx.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var prefs = AudioPrefs()

    override var voices by mutableStateOf(emptyList<VoiceOption>()); private set
    override var natural by mutableStateOf<NaturalState>(if (VoicePack.installed(app)) NaturalState.Ready else NaturalState.Missing); private set
    override var speaking by mutableStateOf(false); private set

    // ── Stimme des Geräts
    private var ttsReady = false
    private val tts: TextToSpeech = TextToSpeech(app) { status -> main.post { if (status == TextToSpeech.SUCCESS) setUpDevice() } }
    private fun setUpDevice() {
        if (runCatching { tts.setLanguage(Locale.GERMANY) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED) < TextToSpeech.LANG_AVAILABLE) return
        val list = runCatching { tts.voices.orEmpty().filter { it.locale.language == "de" && !it.isNetworkConnectionRequired && "notInstalled" !in it.features.orEmpty() }
            .sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }.thenBy { it.name }) }.getOrDefault(emptyList())
        voices = list.mapIndexed { i, v -> VoiceOption(v.name, "Gerätestimme ${i + 1}" + if (v.quality >= 400) " (sehr gut)" else "") }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { main.post { speaking = true } }
            override fun onDone(id: String?) { main.post { speaking = false } }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { main.post { speaking = false } }
            override fun onStop(id: String?, interrupted: Boolean) { main.post { speaking = false } }
        })
        ttsReady = true; applyDevice()
    }
    private fun applyDevice() {
        if (!ttsReady) return
        runCatching { tts.voices?.firstOrNull { it.name == prefs.voice }?.let { tts.voice = it } }
        tts.setSpeechRate(prefs.rate * 0.95f)
    }
    private fun sayDevice(clean: String) {
        if (!ttsReady) return
        runCatching { tts.speak(clean, TextToSpeech.QUEUE_FLUSH, Bundle(), "blubb") }
    }

    // ── Thorsten
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "thorsten").apply { isDaemon = true } }
    private val turn = AtomicInteger(0)
    @Volatile private var engine: OfflineTts? = null
    @Volatile private var track: AudioTrack? = null
    @Volatile private var sounds: Lautschrift? = null
    @Volatile private var cancelled = false
    private val loader = Executors.newSingleThreadExecutor { Thread(it, "thorsten-laden").apply { isDaemon = true } }

    private fun helpers(dir: File) {
        dir.mkdirs()
        listOf("tokens.txt", "saetze.tsv", "woerter.tsv").forEach { n -> app.assets.open("stimme/$n").use { i -> File(dir, n).outputStream().use { i.copyTo(it) } } }
    }
    private fun startEngine() {
        if (engine != null || !VoicePack.installed(app)) return
        worker.execute {
            runCatching {
                val dir = VoicePack.dir(app); helpers(dir)          // nach einem App-Update die neue Lautschrift übernehmen
                sounds = Lautschrift(File(dir, "saetze.tsv").readLines().asSequence(), File(dir, "woerter.tsv").readLines().asSequence())
                val e = OfflineTts(null, OfflineTtsConfig(model = OfflineTtsModelConfig(vits = OfflineTtsVitsModelConfig(model = VoicePack.model(app).path, tokens = File(dir, "tokens.txt").path), numThreads = 2, provider = "cpu")))
                val rate = e.sampleRate()
                val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
                track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(min, rate * 2)).build()
                engine = e
            }.onFailure { main.post { natural = NaturalState.Failed("Die Stimme ließ sich auf diesem Gerät nicht starten (${it.javaClass.simpleName}).") } }
        }
    }
    private fun sayNatural(clean: String): Boolean {
        val laute = sounds?.of(clean) ?: return false
        val mine = turn.incrementAndGet()
        runCatching { track?.pause(); track?.flush() }
        worker.execute {
            val e = engine ?: return@execute; val out = track ?: return@execute
            if (mine != turn.get()) return@execute
            main.post { speaking = true }
            runCatching {
                out.play()
                e.generateWithConfigAndCallback(clean, GenerationConfig(speed = prefs.rate, phonemeCodepoints = laute.map { Lautschrift.codepoints(it) }.toTypedArray())) { samples ->
                    if (mine != turn.get()) 0 else { out.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING); 1 }
                }
                if (mine == turn.get()) out.stop()
            }
            if (mine == turn.get()) main.post { speaking = false }
        }
        return true
    }

    override fun configure(p: AudioPrefs) {
        prefs = p; applyDevice()
        if (p.voice == AudioPrefs.NATURAL && natural == NaturalState.Ready) startEngine()
    }

    override fun say(text: String) {
        if (!prefs.read) return
        val clean = Speech.clean(text); if (clean.isEmpty()) return
        stop()
        val useNatural = prefs.voice == AudioPrefs.NATURAL && engine != null
        if (!(useNatural && sayNatural(clean))) sayDevice(clean)   // unbekanntes Wort: diesen Text liest die Gerätestimme
    }

    override fun stop() {
        turn.incrementAndGet(); runCatching { track?.pause(); track?.flush() }
        if (ttsReady) runCatching { tts.stop() }
        speaking = false
    }

    override fun loadNatural() {
        if (natural is NaturalState.Loading || natural == NaturalState.Ready) return
        cancelled = false; natural = NaturalState.Loading(0)
        loader.execute {
            val dir = VoicePack.dir(app); val part = File(dir, VoicePack.MODEL + ".teil")
            val result = runCatching {
                helpers(dir)
                val c = URL(VoicePack.URL).openConnection() as HttpURLConnection; c.connectTimeout = 20_000; c.readTimeout = 30_000; c.instanceFollowRedirects = true
                if (c.responseCode !in 200..299) error("Der Server antwortet mit ${c.responseCode}.")
                c.inputStream.use { i -> part.outputStream().use { o -> val b = ByteArray(1 shl 16); var done = 0L; var shown = -1
                    while (true) { if (cancelled) error("abgebrochen"); val n = i.read(b); if (n < 0) break; o.write(b, 0, n); done += n
                        val p = (done * 100 / VoicePack.SIZE).toInt().coerceIn(0, 99); if (p != shown) { shown = p; main.post { natural = NaturalState.Loading(p) } } } } }
                if (!VoicePack.matches(part)) error("Die Datei ist unvollständig oder beschädigt angekommen.")
                if (!part.renameTo(VoicePack.model(app))) error("Die Datei ließ sich nicht speichern.")
            }
            part.delete()
            main.post {
                natural = when { result.isSuccess -> NaturalState.Ready; cancelled -> NaturalState.Missing
                    else -> NaturalState.Failed(result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() } ?: "Keine Verbindung. Bitte später noch einmal versuchen.") }
                if (result.isSuccess) startEngine()
            }
        }
    }
    override fun cancelNatural() { cancelled = true }
    override fun removeNatural() {
        cancelled = true; stop()
        worker.execute { runCatching { track?.release() }; runCatching { engine?.release() }; track = null; engine = null; VoicePack.dir(app).deleteRecursively() }
        natural = NaturalState.Missing
    }
}
