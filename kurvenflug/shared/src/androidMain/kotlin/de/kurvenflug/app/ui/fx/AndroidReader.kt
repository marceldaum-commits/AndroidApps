package de.kurvenflug.app.ui.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kurvenflug.core.model.Nova
import de.kurvenflug.core.model.Speech
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Volt liest vor (Android) – mit der KI-Stimme Nova (siehe [Reader]). Fertige Ansagen werden im Cache der App zwischengespeichert
 * (höchstens [CACHE_MB] MB, älteste fliegen zuerst raus). Notlösung ohne Netz: die Sprachausgabe des Geräts.
 */
class AndroidReader(context: Context) : Reader {
    private val ctx = context.applicationContext
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "volt-nova").apply { isDaemon = true } }
    private val turn = AtomicInteger(0)
    @Volatile private var private: Set<String> = emptySet()
    private var player: MediaPlayer? = null

    override var speaking by mutableStateOf(false); private set
    override var unreachable by mutableStateOf(false); private set

    init {
        // Reste der früheren Offline-Stimme (2.0–2.3) wegräumen
        worker.execute { runCatching { File(ctx.filesDir, "stimme").deleteRecursively() } }
    }

    override fun setPrivate(words: Set<String>) { private = words }

    @Volatile private var speed = 1f
    override fun setSpeed(speed: Float) { this.speed = speed.coerceIn(0.5f, 2f); if (ttsReady) tts.setSpeechRate(1.1f * this.speed) }

    override fun say(text: String) {
        val spoken = Speech.clean(text); if (spoken.isEmpty()) return
        val mine = turn.incrementAndGet()
        stopPlayback()
        worker.execute {
            if (mine != turn.get()) return@execute
            // Ganzer Text (fest vertont oder kurz genug für /jetzt) – sonst in ganzen Sätzen, nacheinander gespielt
            val whole = runCatching { fetch(spoken) }.getOrNull()
            val files = if (whole != null) listOf(whole) else if (spoken.length > Nova.MAX_LIVE)
                Nova.chunks(spoken).map { part -> if (mine != turn.get()) return@execute; runCatching { fetch(part) }.getOrNull() } else listOf(null)
            main.post {
                if (mine != turn.get()) return@post
                if (files.all { it != null } && playAll(files.filterNotNull(), 0, mine)) return@post
                unreachable = true   // Gerätestimme nur, wenn Nova wirklich nichts liefern konnte
                device(spoken)
            }
        }
    }

    override fun stop() { turn.incrementAndGet(); stopPlayback(); if (ttsUsed) runCatching { tts.stop() }; speaking = false }

    // ------------------------------------------------------------------ Nova laden
    private fun cacheDir() = File(ctx.cacheDir, "nova").also { it.mkdirs() }

    /** Fertige Ansage aus dem Zwischenspeicher, vom Server oder – für Laufzeit-Texte – frisch vertont; null = Notlösung */
    private fun fetch(spoken: String): File? {
        val h = Nova.hash(spoken)
        // gespeicherte Offline-Stimme: erst Bestqualität, dann Standard
        File(offlineDir(true), "$h.mp3").takeIf { it.isFile && it.length() > 0 }?.let { return it }
        File(offlineDir(false), "$h.mp3").takeIf { it.isFile && it.length() > 0 }?.let { return it }
        val f = File(cacheDir(), "$h.mp3")
        if (f.isFile && f.length() > 0) { f.setLastModified(System.currentTimeMillis()); return f }
        if (download(Nova.url(spoken), f)) return f
        if (!Nova.maySend(spoken, private)) return null
        val c = URL(Nova.LIVE).openConnection() as HttpURLConnection
        c.connectTimeout = 5_000; c.readTimeout = 20_000; c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write(Nova.liveBody(spoken).encodeToByteArray()) }
        if (c.responseCode != 200) return null
        val url = Nova.liveUrl(c.inputStream.use { it.readBytes().decodeToString() }) ?: return null
        return if (download(url, f)) f else null
    }

    private fun download(url: String, to: File, trimCache: Boolean = true): Boolean {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 5_000; c.readTimeout = 15_000
        if (c.responseCode != 200) return false
        val part = File(to.path + ".teil")
        c.inputStream.use { i -> part.outputStream().use { i.copyTo(it) } }
        if (part.length() == 0L || !part.renameTo(to)) { part.delete(); return false }
        if (trimCache) trim(); return true
    }

    private fun trim() {
        val files = cacheDir().listFiles().orEmpty().filter { it.extension == "mp3" }.sortedByDescending { it.lastModified() }
        var sum = 0L
        files.forEach { sum += it.length(); if (sum > CACHE_MB * 1_000_000L) it.delete() }
    }

    // ------------------------------------------------------------------ Abspielen
    /** Spielt die Ansagen nacheinander (lange Texte in ganzen Sätzen) */
    private fun playAll(files: List<File>, i: Int, mine: Int): Boolean = play(files[i], mine) { if (i + 1 < files.size) playAll(files, i + 1, mine) else speaking = false }

    private fun play(f: File, mine: Int, next: (() -> Unit)? = null): Boolean = runCatching {
        val p = MediaPlayer()
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        p.setDataSource(f.path)
        p.setOnCompletionListener { it.release(); if (player === it) player = null; if (mine == turn.get()) { if (next != null) next() else speaking = false } }
        p.setOnErrorListener { mp, _, _ -> if (mine == turn.get()) speaking = false; mp.release(); if (player === mp) player = null; true }
        p.prepare()
        // Sprechtempo bei gleicher Tonhöhe
        if (speed != 1f) p.playbackParams = p.playbackParams.setSpeed(speed).setPitch(1f)
        p.start()
        player = p; speaking = true
    }.isSuccess

    private fun stopPlayback() { runCatching { player?.stop(); player?.release() }; player = null }

    // ------------------------------------------------------------------ Notlösung: Stimme des Geräts
    private var ttsUsed = false
    private var ttsReady = false
    private var pending: String? = null
    private val tts: TextToSpeech by lazy {
        ttsUsed = true
        TextToSpeech(ctx) { status -> main.post { if (status == TextToSpeech.SUCCESS) setUp(); pending?.let { p -> pending = null; if (ttsReady) speak(p) } } }
    }
    private fun setUp() {
        if (runCatching { tts.setLanguage(Locale.GERMANY) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED) < TextToSpeech.LANG_AVAILABLE) return
        // nur Stimmen, die auf dem Gerät rechnen – es geht kein Text ins Netz
        runCatching { tts.voices.orEmpty().filter { it.locale.language == "de" && !it.isNetworkConnectionRequired && "notInstalled" !in it.features.orEmpty() }
            .maxByOrNull { it.quality }?.let { tts.voice = it } }
        tts.setPitch(1.08f); tts.setSpeechRate(1.1f * speed)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { main.post { speaking = true } }
            override fun onDone(id: String?) { main.post { speaking = false } }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { main.post { speaking = false } }
            override fun onStop(id: String?, interrupted: Boolean) { main.post { speaking = false } }
        })
        ttsReady = true
    }
    private fun device(spoken: String) { if (ttsReady) speak(spoken) else { pending = spoken; tts } }
    private fun speak(spoken: String) { runCatching { tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, Bundle(), "volt") } }

    // ------------------------------------------------------------------ „Alle Nova-Texte offline laden“
    /** Dauerhaft im App-Speicher (nicht im Cache, den das System leeren darf); keine Obergrenze beim manuellen Laden */
    private fun offlineDir(best: Boolean) = File(ctx.filesDir, if (best) "nova-best" else "nova").also { it.mkdirs() }
    override var offline by mutableStateOf(OfflineState()); private set
    private val loader = Executors.newFixedThreadPool(4) { Thread(it, "nova-offline").apply { isDaemon = true } }
    @Volatile private var cancelled = false
    private val names: List<String> by lazy {
        runCatching { ctx.assets.open("vorlesen/texte.json").use { it.readBytes().decodeToString() } }.map { Nova.listTexts(it).map(Nova::hash).distinct() }.getOrDefault(emptyList())
    }

    private fun scan(best: Boolean): Pair<Int, Long> {
        val have = offlineDir(best).listFiles().orEmpty().filter { it.extension == "mp3" }.associateBy { it.nameWithoutExtension }
        val mine = names.mapNotNull { have[it] }
        return mine.size to mine.sumOf { it.length() }
    }

    override fun refreshOffline() {
        worker.execute {
            val (n, bytes) = scan(false); val (nb, bytesB) = scan(true)
            val stand = runCatching {
                val c = URL(Nova.standUrl(Nova.APP_HOST)).openConnection() as HttpURLConnection
                c.connectTimeout = 5_000; c.readTimeout = 10_000
                if (c.responseCode == 200) c.inputStream.use { it.readBytes().decodeToString() } else null
            }.getOrNull()
            main.post { offline = offline.copy(total = names.size, have = n, haveBest = nb, haveBytes = bytes + bytesB,
                serverBytes = stand?.let(Nova::standBytes) ?: offline.serverBytes, serverBytesBest = stand?.let(Nova::standBytesBest) ?: offline.serverBytesBest) }
        }
    }

    override fun loadOffline(best: Boolean) {
        if (offline.loading) return
        cancelled = false
        val dir = offlineDir(best)
        val todo = names.filter { !File(dir, "$it.mp3").isFile }
        offline = offline.copy(loading = true, done = 0, todo = todo.size, problem = null)
        if (todo.isEmpty()) { offline = offline.copy(loading = false); refreshOffline(); return }
        val done = AtomicInteger(0); val failed = AtomicInteger(0); val left = AtomicInteger(todo.size)
        todo.forEach { h ->
            loader.execute {
                if (!cancelled) {
                    // 404 (noch nicht vertont) still überspringen; Netzfehler zählen
                    val target = File(dir, "$h.mp3"); val cached = File(cacheDir(), "$h.mp3")
                    val ok = runCatching {
                        // original/ fehlt noch → normale Datei, aber als Standard ablegen: beim nächsten Tippen wird die Bestqualität nachgeholt
                        if (best) { if (!download(Nova.fileUrl(h, true), target, trimCache = false)) File(offlineDir(false), "$h.mp3").let { std -> if (!std.isFile) download(Nova.fileUrl(h), std, trimCache = false) } }
                        else if (!(cached.isFile && cached.renameTo(target))) download(Nova.fileUrl(h), target, trimCache = false)
                    }
                    if (ok.isFailure) failed.incrementAndGet()
                    val d = done.incrementAndGet()
                    if (d % 10 == 0 || d == todo.size) main.post { if (offline.loading) offline = offline.copy(done = d) }
                }
                if (left.decrementAndGet() == 0) main.post {
                    offline = offline.copy(loading = false, problem = when {
                        cancelled -> null
                        failed.get() > 0 -> "${failed.get()} Ansagen ließen sich nicht laden – beim nächsten Tippen geht es dort weiter."
                        else -> null })
                    refreshOffline()
                }
            }
        }
    }
    override fun cancelOffline() { cancelled = true }
    override fun clearOffline() {
        cancelled = true
        worker.execute { offlineDir(false).deleteRecursively(); offlineDir(true).deleteRecursively(); main.post { offline = offline.copy(have = 0, haveBest = 0, haveBytes = 0, loading = false) }; refreshOffline() }
    }

    companion object { const val CACHE_MB = 40 }
}
