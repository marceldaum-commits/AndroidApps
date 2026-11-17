package de.experimentopia.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.experimentopia.app.ui.fx.Reader
import de.experimentopia.core.model.AudioPrefs
import de.experimentopia.core.model.Nova
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Vorlesen unter Android mit der Stimme „Nova“ (KI-Stimme, vorab auf unserem Stimmen-Server vertont).
 * Jeder Satz wird einmal geladen und bleibt auf dem Gerät. Fehlt eine Datei oder gibt es kein Internet, liest leise die Stimme des Geräts.
 * Die App schickt nie etwas an OpenAI – nur Texte, die erst im Versuch entstehen, gehen ohne Angaben zum Kind an unseren Server.
 */
class AndroidReader(ctx: Context) : Reader {
    private val app = ctx.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var prefs = AudioPrefs()
    private val dir = File(app.filesDir, "nova").apply { mkdirs() }

    override var speaking by mutableStateOf(false); private set

    private val turn = AtomicInteger(0)
    private val fetcher = Executors.newFixedThreadPool(2) { Thread(it, "nova-laden").apply { isDaemon = true } }
    private val player = Executors.newSingleThreadExecutor { Thread(it, "nova").apply { isDaemon = true } }
    @Volatile private var current: MediaPlayer? = null
    @Volatile private var done: CountDownLatch? = null

    // ── Notlösung: Stimme des Geräts
    private var ttsReady = false
    private val tts: TextToSpeech = TextToSpeech(app) { status -> main.post { if (status == TextToSpeech.SUCCESS) setUpDevice() } }
    private fun setUpDevice() {
        if (runCatching { tts.setLanguage(Locale.GERMANY) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED) < TextToSpeech.LANG_AVAILABLE) return
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { main.post { speaking = true } }
            override fun onDone(id: String?) { main.post { speaking = false } }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { main.post { speaking = false } }
            override fun onStop(id: String?, interrupted: Boolean) { main.post { speaking = false } }
        })
        tts.setSpeechRate(1.1f)
        ttsReady = true
    }
    private fun sayDevice(text: String) {
        if (!ttsReady) { main.post { speaking = false }; return }
        runCatching { tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "blubb") }
    }

    // ── Nova
    private fun file(part: String) = File(dir, Nova.hash(part) + ".mp3")

    /** Satz als MP3 holen: erst vom Gerät, dann die vorab vertonte Datei, sonst (neuer Text) beim Server vertonen lassen */
    private fun fetch(part: String): File? {
        val f = file(part)
        if (f.isFile && f.length() > 0) return f
        if (download(Nova.url(part), f)) return f
        if (part.length > Nova.MAX) return null
        val url = runCatching {
            val c = URL(Nova.NOW).openConnection() as HttpURLConnection
            c.connectTimeout = 10_000; c.readTimeout = 30_000; c.requestMethod = "POST"; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("Origin", Nova.ORIGIN)
            c.outputStream.use { it.write(JSONObject().put("text", part).put("sprache", Nova.LANG).toString().toByteArray()) }
            if (c.responseCode != 200) null else JSONObject(c.inputStream.bufferedReader().readText()).optString("url").takeIf { it.startsWith(Nova.SERVER + "/") }
        }.getOrNull() ?: return null
        return if (download(url, f)) f else null
    }

    private fun download(url: String, to: File): Boolean = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 20_000
        if (c.responseCode != 200 || c.contentType?.startsWith("audio/") != true) return false
        val part = File(dir, to.name + ".teil")
        c.inputStream.use { i -> part.outputStream().use { i.copyTo(it) } }
        part.length() > 0 && part.renameTo(to)
    }.getOrDefault(false)

    private fun play(f: File, mine: Int): Boolean {
        val latch = CountDownLatch(1)
        val mp = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                setDataSource(f.path); prepare()
                setOnCompletionListener { latch.countDown() }
                setOnErrorListener { _, _, _ -> latch.countDown(); true }
            }
        }.getOrElse { f.delete(); return false }
        if (mine != turn.get()) { mp.release(); return true }
        current = mp; done = latch
        mp.start()
        latch.await(2, TimeUnit.MINUTES)
        current = null; runCatching { mp.release() }
        return true
    }

    override fun configure(p: AudioPrefs) { prefs = p }

    override fun say(text: String) {
        if (!prefs.read) return
        val parts = Nova.parts(text); if (parts.isEmpty()) return
        stop()
        val mine = turn.get()
        val loads: List<Future<File?>> = parts.map { p -> fetcher.submit<File?> { if (mine == turn.get()) fetch(p) else null } }
        speaking = true
        player.execute {
            for ((i, job) in loads.withIndex()) {
                if (mine != turn.get()) return@execute
                val f = runCatching { job.get(40, TimeUnit.SECONDS) }.getOrNull()
                if (mine != turn.get()) return@execute
                if (f == null || !play(f, mine)) {          // offline oder Datei fehlt: Rest mit der Stimme des Geräts
                    val rest = parts.drop(i).joinToString(" ")
                    main.post { if (mine == turn.get()) sayDevice(rest) }
                    return@execute
                }
            }
            main.post { if (mine == turn.get()) speaking = false }
        }
    }

    override fun stop() {
        turn.incrementAndGet()
        runCatching { current?.stop() }; done?.countDown()
        if (ttsReady) runCatching { tts.stop() }
        speaking = false
    }
}
