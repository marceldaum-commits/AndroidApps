package de.raumlicht.app.ui.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import de.raumlicht.core.model.Vorlesen
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Holt eine Adresse. GET, wenn [body] null ist, sonst POST mit JSON. Antwort: Statuscode und Inhalt (leer bei Fehlern). */
typealias NovaFetch = (url: String, body: String?) -> Pair<Int, ByteArray>

/**
 * Lux spricht mit „Nova“ – einer mit KI erzeugten Stimme. Die App rechnet nichts und spricht mit keinem fremden Dienst:
 * Zu jedem vorlesefertigen Text gibt es auf unserem Server eine MP3-Datei, deren Name sich aus dem Text ergibt ([Vorlesen.url]).
 *
 * 1. schon auf dem Gerät gespeichert (offline geladen oder schon einmal gehört) → sofort abspielen (geht auch ohne Internet)
 * 2. sonst die Datei von unserem Server holen (feste Texte sind vorab vertont)
 * 3. gibt es sie noch nicht (Text ist erst in der Aufgabe entstanden): unseren Server bitten, sie zu erzeugen ([Vorlesen.LIVE]) – nur Text und Sprache, nichts über den Nutzer
 * 4. klappt das alles nicht (offline, zu lang, Server sagt nein): diesen einen Text liest still die Stimme des Geräts ([fallback])
 *
 * Ein neuer Satz unterbricht den alten.
 */
class NovaVoice(private val ctx: Context, private val fallback: Speaker? = null, private val onSpeaking: (Boolean) -> Unit = {},
                private val fetch: NovaFetch = ::novaFetch,
                /** spielt eine Datei mit Lautstärke und Tempo ab und ruft am Ende [done] auf; gibt zurück, wie man sie anhält (Tests ersetzen das) */
                private val play: ((File, Float, Float, () -> Unit) -> (() -> Unit))? = null) : Speaker {
    @Volatile var enabled = true
    @Volatile var volume = 1f
    /** Sprechtempo: 1 = wie aufgenommen; die Datei wird schneller oder langsamer abgespielt, die Tonhöhe bleibt */
    @Volatile var rate = 1f
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "lux-nova").apply { isDaemon = true } }
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val turn = AtomicInteger(0)
    @Volatile private var speaking = false
    private var halt: (() -> Unit)? = null
    private val dir get() = File(ctx.cacheDir, "nova").apply { mkdirs() }

    override fun say(text: String) {
        if (!enabled || volume <= 0f) return
        val spoken = Vorlesen.spoken(text); if (spoken.isEmpty()) return
        val mine = turn.incrementAndGet()                                   // alles Ältere ist damit überholt
        silence(); fallbackStop()
        worker.execute {
            val file = runCatching { obtain(spoken) }.getOrNull()
            main.post {
                if (mine != turn.get() || !enabled) return@post
                if (file == null) { fallback?.say(text); return@post }       // stille Notlösung: die Stimme des Geräts
                start(file, mine)
            }
        }
    }
    override fun sayIfQuiet(text: String) { if (!speaking) say(text) }
    fun stop() { turn.incrementAndGet(); silence(); fallbackStop() }

    /** Die Datei zu einem vorlesefertigen Text: aus dem Speicher, sonst vom Server (Schritte 1–3). null = nicht zu bekommen. */
    internal fun obtain(spoken: String): File? {
        val key = Vorlesen.key(spoken)
        NovaOffline.find(ctx, key)?.let { return it }                        // offline geladen (Einstellungen)
        val file = File(dir, "$key.mp3")
        if (file.length() > 0) return file
        var (code, bytes) = fetch(Vorlesen.url(spoken), null)
        if (code == 404 && spoken.length <= Vorlesen.LIVE_MAX_CHARS) {
            val (c, answer) = fetch(Vorlesen.LIVE, Vorlesen.liveBody(spoken))
            val url = if (c == 200) Vorlesen.liveUrl(answer.decodeToString()) else null
            if (url != null) fetch(url, null).let { code = it.first; bytes = it.second } else return null
        }
        if (code != 200 || !looksLikeMp3(bytes)) return null
        val part = File(dir, file.name + ".teil"); part.writeBytes(bytes)
        return if (part.renameTo(file)) file else null
    }

    private fun start(file: File, mine: Int) {
        speaking = true; onSpeaking(true)
        val done = { main.post { if (mine == turn.get()) { halt = null; speaking = false; onSpeaking(false) } }; Unit }
        halt = runCatching { (play ?: ::playFile)(file, volume.coerceIn(0f, 1f), rate.coerceIn(0.5f, 2f), done) }.getOrElse { file.delete(); done(); null }
    }
    private fun silence() { runCatching { halt?.invoke() }; halt = null; if (speaking) { speaking = false; onSpeaking(false) } }
    private fun fallbackStop() { (fallback as? LuxVoice)?.stop() }

    private fun playFile(file: File, volume: Float, rate: Float, done: () -> Unit): () -> Unit {
        val p = MediaPlayer()
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        p.setDataSource(file.path); p.setVolume(volume, volume)
        p.setOnCompletionListener { runCatching { it.release() }; done() }
        p.setOnErrorListener { mp, _, _ -> runCatching { mp.release() }; file.delete(); done(); true }   // kaputte Datei: beim nächsten Mal neu holen
        p.prepare()
        // Tempo bei gleicher Tonhöhe; das Setzen startet die Wiedergabe schon – start() danach schadet nicht
        if (rate != 1f) runCatching { p.playbackParams = android.media.PlaybackParams().setSpeed(rate).setPitch(1f) }
        p.start()
        return { runCatching { p.stop() }; runCatching { p.release() } }
    }

    companion object {
        /** MP3 beginnt mit „ID3“ oder einem Rahmen-Kopf (0xFF 0xEx/0xFx) – so landet keine Fehlerseite im Speicher */
        fun looksLikeMp3(b: ByteArray) = b.size > 256 && ((b[0] == 'I'.code.toByte() && b[1] == 'D'.code.toByte() && b[2] == '3'.code.toByte()) || (b[0] == 0xFF.toByte() && (b[1].toInt() and 0xE0) == 0xE0))
        /** Alte gespeicherte Sätze wegräumen, wenn der Speicher zu groß wird (älteste zuerst) */
        fun trim(ctx: Context, maxBytes: Long = 60L * 1024 * 1024) {
            val files = File(ctx.cacheDir, "nova").listFiles()?.sortedBy { it.lastModified() } ?: return
            var total = files.sumOf { it.length() }
            for (f in files) { if (total <= maxBytes) break; total -= f.length(); f.delete() }
        }
    }
}

/** Einfacher Abruf ohne Kennungen: kein Cookie, kein Konto, nur die Adresse (und bei POST der Text) */
fun novaFetch(url: String, body: String?): Pair<Int, ByteArray> = runCatching {
    val c = URL(url).openConnection() as HttpURLConnection
    try {
        c.connectTimeout = 6000; c.readTimeout = 20000; c.instanceFollowRedirects = false
        if (body != null) { c.requestMethod = "POST"; c.doOutput = true; c.setRequestProperty("Content-Type", "application/json"); c.outputStream.use { it.write(body.encodeToByteArray()) } }
        val code = c.responseCode
        code to (if (code == 200) c.inputStream.use { it.readBytes() } else ByteArray(0))
    } finally { c.disconnect() }
}.getOrDefault(0 to ByteArray(0))
