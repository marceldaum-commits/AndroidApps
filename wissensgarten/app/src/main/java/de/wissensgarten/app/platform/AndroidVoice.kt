package de.wissensgarten.app.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.speech.tts.TextToSpeech
import de.wissensgarten.app.ui.fx.Locale
import de.wissensgarten.app.ui.fx.NovaGeraet
import de.wissensgarten.core.model.Nova
import de.wissensgarten.core.model.NovaOfflineSpeicher
import de.wissensgarten.core.model.NovaQualitaet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Android-Teil von Brummis Stimme „Nova“: MP3-Dateien vom Stimm-Server im App-Speicher (`filesDir/nova`), Wiedergabe mit
 * MediaPlayer (Tempo ohne Tonhöhen-Änderung) und als stille Notlösung die Android-Sprachausgabe (etwas höher gestimmt).
 * Anfragen gehen ohne Cookies, ohne Referrer und mit neutraler Kennung („Wissensgarten“) – nicht mit dem Gerätenamen.
 * „Alle Nova-Texte offline laden“ legt die Dateien dauerhaft in `nova-offline` (Standard) bzw. `nova-original` (Bestqualität) –
 * die Größengrenze des Zwischenspeichers (`nova`) räumt dort nie auf.
 */
class AndroidNova(context: Context) : NovaGeraet, NovaOfflineSpeicher, TextToSpeech.OnInitListener {
    private val ctx = context.applicationContext
    private val dir = File(ctx.filesDir, "nova")
    private val offlineDir = File(ctx.filesDir, "nova-offline")
    private val originalDir = File(ctx.filesDir, "nova-original")
    private fun dirOf(q: NovaQualitaet) = if (q == NovaQualitaet.BEST) originalDir else offlineDir
    private val tts = TextToSpeech(ctx, this)
    @Volatile private var ready = false
    private val waiting = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    private var player: MediaPlayer? = null
    private var finishPlay: ((Boolean) -> Unit)? = null

    init {
        // Aufräumen im Hintergrund: alte Offline-Stimme (bis 2.0.2, ca. 63 MB) löschen, Nova-Speicher klein halten
        Thread {
            runCatching { File(ctx.filesDir, "stimme").takeIf { it.exists() }?.deleteRecursively() }
            runCatching { dir.mkdirs(); trim(dir, MAX_BYTES) }   // nur der Zwischenspeicher, nie nova-offline / nova-original
        }.apply { isDaemon = true; start() }
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        tts.setPitch(1.25f)
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { waiting.remove(id)?.complete(Unit) }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { waiting.remove(id)?.complete(Unit) }
            override fun onStop(id: String?, interrupted: Boolean) { waiting.remove(id)?.complete(Unit) }
        })
    }

    // ---------------------------------------------------------------- Speicher und Netz

    private fun file(h: String) = File(dir, "$h.mp3")
    /** Zum Abspielen: Bestqualität vor Standard (offline) vor Zwischenspeicher */
    private fun found(h: String): File? = listOf(File(originalDir, "$h.mp3"), File(offlineDir, "$h.mp3"), file(h)).firstOrNull { it.isFile && it.length() > 0 }

    override suspend fun imSpeicher(h: String): Boolean = withContext(Dispatchers.IO) {
        val f = found(h)
        (f != null).also { if (f != null && f.parentFile == dir) f.setLastModified(System.currentTimeMillis()) }
    }

    override suspend fun laden(url: String, h: String): Int = withContext(Dispatchers.IO) { runCatching { get(url, h, dir) }.getOrDefault(-1) }

    // ---------------------------------------------------------------- Offline laden

    override suspend fun offlineHashes(q: NovaQualitaet): Set<String> = withContext(Dispatchers.IO) {
        dirOf(q).list()?.filter { it.endsWith(".mp3") }?.map { it.removeSuffix(".mp3") }?.toSet() ?: emptySet()
    }

    override suspend fun sichern(url: String, h: String, q: NovaQualitaet): Int = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(dirOf(q), "$h.mp3")
            if (target.isFile && target.length() > 0) return@runCatching 200
            // Standard schon im Zwischenspeicher: nur umziehen, nicht neu laden
            if (q == NovaQualitaet.STANDARD && file(h).isFile) { dirOf(q).mkdirs(); if (file(h).renameTo(target)) return@runCatching 200 }
            get(url, h, dirOf(q))
        }.getOrDefault(-1)
    }

    override suspend fun text(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val c = open(url).apply { setRequestProperty("Accept", "application/json") }
            try { if (c.responseCode == 200) c.inputStream.use { it.readBytes().decodeToString() } else null } finally { c.disconnect() }
        }.getOrNull()
    }

    override suspend fun belegt(): Long = withContext(Dispatchers.IO) {
        listOf(dir, offlineDir, originalDir).sumOf { d -> d.listFiles()?.sumOf { it.length() } ?: 0L }
    }

    override suspend fun loeschen() = withContext(Dispatchers.IO) {
        listOf(dir, offlineDir, originalDir).forEach { d -> d.listFiles()?.forEach { it.delete() } }
    }

    override suspend fun jetzt(text: String, sprache: String, h: String): Int = withContext(Dispatchers.IO) {
        runCatching {
            val c = open("${Nova.BASE}/jetzt").apply {
                requestMethod = "POST"; doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                val body = buildJsonObject { put("text", text); put("sprache", sprache) }.toString()
                c.outputStream.use { it.write(body.encodeToByteArray()) }
                val code = c.responseCode
                if (code != 200) return@runCatching code
                val url = Json.parseToJsonElement(c.inputStream.use { it.readBytes().decodeToString() }).jsonObject["url"]?.jsonPrimitive?.content
                // Nur Dateien von unserem eigenen Stimm-Server
                if (url == null || !url.startsWith(Nova.BASE + "/")) return@runCatching -1
                if (get(url, h, dir) == 200) 200 else -1
            } finally { c.disconnect() }
        }.getOrDefault(-1)
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 4_000; readTimeout = 10_000; useCaches = false; instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Wissensgarten")
    }

    private fun get(url: String, h: String, into: File): Int {
        val c = open(url).apply { setRequestProperty("Accept", "audio/mpeg") }
        try {
            val code = c.responseCode
            if (code == 200) {
                val bytes = c.inputStream.use { it.readNBytesCompat(MAX_FILE) } ?: return -1
                if (bytes.isEmpty()) return -1
                into.mkdirs()
                val tmp = File(into, "$h.teil")
                tmp.writeBytes(bytes)
                if (!tmp.renameTo(File(into, "$h.mp3"))) { tmp.delete(); return -1 }
            }
            return code
        } finally { c.disconnect() }
    }

    // ---------------------------------------------------------------- Abspielen

    override suspend fun abspielen(h: String, tempo: Float): Boolean {
        val f = withContext(Dispatchers.IO) { found(h) } ?: return false
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                audioStopp()
                val mp = MediaPlayer()
                var done = false
                fun finish(ok: Boolean) {
                    if (done) return; done = true
                    if (player === mp) { player = null; finishPlay = null }
                    runCatching { mp.release() }
                    if (cont.isActive) cont.resume(ok)
                }
                try {
                    mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    mp.setDataSource(f.path)
                    mp.setOnCompletionListener { finish(true) }
                    mp.setOnErrorListener { _, _, _ -> finish(false); true }
                    mp.prepare()
                    player = mp; finishPlay = ::finish
                    // Tempo bei gleicher Tonhöhe (setzen startet die Wiedergabe)
                    mp.playbackParams = PlaybackParams().setSpeed(tempo).setPitch(1f)
                    if (!mp.isPlaying) mp.start()
                } catch (e: Exception) {
                    f.delete()   // kaputte Datei – nächstes Mal neu laden
                    finish(false)
                }
                cont.invokeOnCancellation { android.os.Handler(android.os.Looper.getMainLooper()).post { finish(true) } }
            }
        }
    }

    /** Wiedergabe beenden – ein wartendes [abspielen] kehrt zurück (als „gespielt“, damit keine Gerätestimme einspringt) */
    override fun audioStopp() {
        val p = player ?: return
        runCatching { if (p.isPlaying) p.stop() }
        finishPlay?.invoke(true)
    }

    // ---------------------------------------------------------------- Gerätestimme (Notlösung)

    override val geraetBereit get() = ready

    override suspend fun geraetSprechen(text: String, locale: Locale, tempo: Float) {
        if (!ready) return
        val id = "brummi-" + System.nanoTime()
        val done = CompletableDeferred<Unit>()
        waiting[id] = done
        tts.setSpeechRate(0.95f * tempo)
        tts.language = jl(locale)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        try { withTimeoutOrNull(60_000) { done.await() } } finally { waiting.remove(id) }
    }

    override fun geraetStopp() {
        if (ready) tts.stop()
        waiting.values.forEach { it.complete(Unit) }; waiting.clear()
    }

    private fun jl(l: Locale): java.util.Locale {
        val j = java.util.Locale.forLanguageTag(l.tag)
        return if (tts.isLanguageAvailable(j) >= TextToSpeech.LANG_AVAILABLE) j else java.util.Locale.GERMAN
    }

    companion object {
        /** Höchstens so viel automatischer Zwischenspeicher; ältere (lange nicht gehörte) Dateien gehen zuerst */
        const val MAX_BYTES = 150L * 1024 * 1024
        const val MAX_FILE = 5 * 1024 * 1024

        fun trim(dir: File, max: Long) {
            val files = dir.listFiles()?.filter { it.isFile } ?: return
            files.filter { it.name.endsWith(".teil") }.forEach { it.delete() }
            var total = files.filter { it.exists() }.sumOf { it.length() }
            if (total <= max) return
            for (f in files.filter { it.exists() }.sortedBy { it.lastModified() }) {
                if (total <= max * 9 / 10) break
                total -= f.length(); f.delete()
            }
        }

        private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray? {
            val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(16 * 1024)
            while (true) {
                val n = read(buf); if (n < 0) break
                out.write(buf, 0, n); if (out.size() > limit) return null
            }
            return out.toByteArray()
        }
    }
}
