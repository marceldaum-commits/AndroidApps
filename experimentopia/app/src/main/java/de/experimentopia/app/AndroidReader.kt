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
import de.experimentopia.app.ui.fx.OfflineVoice
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

    init {
        // Die alte Offline-Stimme (bis 2.1) wird nicht mehr gebraucht – ihre Stimmdatei (ca. 63 MB) nach dem Update wegräumen
        Thread({ runCatching { File(app.filesDir, "stimme").deleteRecursively() } }, "aufraeumen").apply { isDaemon = true }.start()
    }

    override var speaking by mutableStateOf(false); private set
    override var offline by mutableStateOf(OfflineVoice()); private set

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
        ttsReady = true
    }
    private fun sayDevice(text: String) {
        if (!ttsReady) { main.post { speaking = false }; return }
        tts.setSpeechRate(1.1f * Nova.speed(prefs.rate))
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

    private fun download(url: String, to: File): Boolean = get(url, to) == 200

    /** Datei holen: 200 = gespeichert, sonst HTTP-Status (404 = gibt es noch nicht) bzw. -1 ohne Verbindung */
    private fun get(url: String, to: File): Int = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 20_000
        val code = c.responseCode
        if (code != 200) return code
        if (c.contentType?.startsWith("audio/") != true) return -1
        val part = File(dir, to.name + ".teil")
        c.inputStream.use { i -> part.outputStream().use { i.copyTo(it) } }
        if (part.length() > 0 && part.renameTo(to)) 200 else -1
    }.getOrDefault(-1)

    private fun play(f: File, mine: Int): Boolean {
        val latch = CountDownLatch(1)
        val mp = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                setDataSource(f.path); prepare()
                playbackParams = android.media.PlaybackParams().setSpeed(Nova.speed(prefs.rate)).setPitch(1f)
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

    // ── Alle festen Sätze offline (nur auf ausdrücklichen Wunsch; /jetzt-Texte gehören nicht dazu)
    private val bulk = Executors.newSingleThreadExecutor { Thread(it, "nova-offline").apply { isDaemon = true } }
    @Volatile private var cancelBulk = false
    private fun saved() = dir.listFiles { f -> f.name.endsWith(".mp3") }?.sumOf { it.length() } ?: 0L
    private fun missing() = Nova.catalog().filterNot { file(it).isFile }

    override fun refreshOffline() {
        bulk.execute {
            val stand = runCatching { URL(Nova.standUrl(Nova.APP_HOST)).readText() }.getOrNull()
            val total = stand?.let { Nova.bytesOf(it) }; val best = stand?.let { Nova.bytesOriginalOf(it) }
            val s = saved(); val left = missing().size; val all = Nova.catalog().size
            main.post { offline = offline.copy(totalBytes = total ?: offline.totalBytes, totalBestBytes = best ?: offline.totalBestBytes, savedBytes = s, complete = left == 0, done = all - left, count = all) }
        }
    }

    override fun loadOffline(best: Boolean) {
        if (offline.loading) return
        cancelBulk = false
        offline = offline.copy(loading = true, note = null)
        bulk.execute {
            val all = Nova.catalog(); val todo = all.filterNot { file(it).isFile }
            val done = AtomicInteger(all.size - todo.size)
            main.post { offline = offline.copy(done = done.get(), count = all.size) }
            val pool = Executors.newFixedThreadPool(Nova.PARALLEL)
            var failed = 0
            val jobs = todo.map { p -> pool.submit<Boolean> {
                if (cancelBulk) return@submit true
                val code = if (best) get(Nova.originalUrl(p), file(p)).let { if (it == 404) get(Nova.url(p), file(p)) else it } else get(Nova.url(p), file(p))
                val n = done.incrementAndGet()
                if (n % 10 == 0 || n == all.size) main.post { offline = offline.copy(done = n) }
                code == 200 || code == 404   // 404: still überspringen
            } }
            jobs.forEach { if (runCatching { !it.get() }.getOrDefault(true)) failed++ }
            pool.shutdown()
            val s = saved(); val left = missing().size
            main.post {
                offline = offline.copy(loading = false, savedBytes = s, complete = left == 0, done = all.size - left,
                    note = when { cancelBulk -> "Angehalten – beim nächsten Tippen geht es dort weiter."; failed > 0 -> "Einige Sätze kamen nicht an (keine Verbindung?). Nochmal tippen lädt den Rest."; else -> null })
            }
        }
    }
    override fun cancelOffline() { cancelBulk = true }
    override fun deleteOffline() {
        cancelBulk = true
        bulk.execute {
            dir.listFiles()?.forEach { it.delete() }
            main.post { offline = offline.copy(savedBytes = 0, complete = false, done = 0, note = null) }
        }
    }

    override fun stop() {
        turn.incrementAndGet()
        runCatching { current?.stop() }; done?.countDown()
        if (ttsReady) runCatching { tts.stop() }
        speaking = false
    }
}
