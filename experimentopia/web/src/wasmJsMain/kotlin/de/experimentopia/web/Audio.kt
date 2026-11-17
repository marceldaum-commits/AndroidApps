package de.experimentopia.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.experimentopia.app.ui.fx.*
import de.experimentopia.core.model.AudioPrefs
import de.experimentopia.core.model.Nova
import de.experimentopia.core.model.World
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Dieselbe Musik wie unter Android ([Composer]): neue Phrasen je Welt, Pausen dazwischen, erst nach der ersten Berührung. */
class WebMusic(private val scope: CoroutineScope) : MusicPlayer {
    private var world: World? = null
    private var enabled = true
    private var volume = 0.5f
    private var ducked = false
    private var job: Job? = null
    private var inJingle = false
    private var seed = Random.nextInt()
    private fun gain() = (if (ducked) 0.3 else 1.0) * volume * 0.8

    private fun restart(withJingle: Boolean = false) {
        job?.cancel(); musicStop()
        if (!enabled) return
        job = scope.launch {
            while (!audioReady()) delay(400)
            if (withJingle) {
                inJingle = true
                storeBuffer("musik", newSamplesOf(Composer.jingle()), Composer.SR)
                val d = musicPlay("musik", gain()); delay((d * 1000).toLong() + 2500); inJingle = false
            }
            while (true) {
                val w = world
                if (w == null || pageHidden()) { delay(1000); continue }
                storeBuffer("musik", newSamplesOf(Composer.phrase(w, seed++)), Composer.SR)
                val d = musicPlay("musik", gain())
                delay((d * 1000).toLong() + 2500 + Random.nextLong(2500))
            }
        }
    }

    override fun world(w: World?) { if (w == world) return; world = w; if (!inJingle) restart() }
    override fun jingle() = restart(withJingle = true)
    override fun configure(p: AudioPrefs) {
        val was = enabled; enabled = p.music; volume = p.musicVol
        if (was != enabled) restart() else musicGain(gain())
    }
    override fun duck(on: Boolean) { ducked = on; musicGain(gain()) }
}

private fun newSamplesOf(pcm: FloatArray): JsAny { val b = newSamples(pcm.size); for (i in pcm.indices) putSample(b, i, pcm[i]); return b }

/** Vorlesen im Browser mit „Nova“ (nova.js) – dieselben Sätze und Dateien wie unter Android; ohne Internet liest die Stimme des Browsers. */
class WebReader : Reader {
    private var prefs = AudioPrefs()
    override var speaking by mutableStateOf(false); private set
    override var offline by mutableStateOf(OfflineVoice()); private set
    override var novaDown by mutableStateOf(false); private set
    private var turn = 0
    private val urls by lazy { Nova.catalog().joinToString("\n") { Nova.url(it) } }
    private val originals by lazy { Nova.catalog().joinToString("\n") { Nova.originalUrl(it) } }
    private val count by lazy { Nova.catalog().size }
    /** Zählstand der Live-Seite, wenn die App lokal oder anderswo läuft */
    private fun host() = pageHost().takeIf { it.endsWith("mathenachhilfe-online.com") } ?: Nova.APP_HOST

    init { dropOldVoice() }

    override fun configure(p: AudioPrefs) { prefs = p }

    override fun say(text: String) {
        if (!prefs.read) return
        val parts = Nova.parts(text); if (parts.isEmpty()) return
        stop()
        val mine = turn
        speaking = true
        fun fallback(from: Int) {
            if (mine != turn) return
            val rest = parts.drop(from).joinToString(" ")
            novaDown = true
            if (canSpeak()) browserSpeak(rest, "", 1.1 * Nova.speed(prefs.rate), 1.0, { }, { if (mine == turn) speaking = false }) else speaking = false
        }
        if (!userActivated()) { speaking = false; return }   // vor dem ersten Antippen darf der Browser gar keinen Ton abspielen
        novaScript { ok ->
            if (mine != turn) return@novaScript
            if (!ok) { fallback(0); return@novaScript }
            novaSpeak(parts.joinToString("\n"), parts.joinToString("\n") { Nova.url(it) }, Nova.speed(prefs.rate).toDouble(), { }, { if (mine == turn) speaking = false }, { i -> fallback(i) })
        }
    }

    override fun refreshOffline() {
        novaScript { ok -> if (ok) novaInfo(host(), urls) { total, best, saved, have ->
            offline = offline.copy(totalBytes = total.toLong().takeIf { it >= 0 } ?: offline.totalBytes, totalBestBytes = best.toLong().takeIf { it >= 0 } ?: offline.totalBestBytes, savedBytes = saved.toLong(), done = have, count = count, complete = have >= count)
        } }
    }
    override fun loadOffline(best: Boolean) {
        if (offline.loading) return
        offline = offline.copy(loading = true, note = null)
        novaScript { ok ->
            if (!ok) { offline = offline.copy(loading = false, note = "Keine Verbindung. Bitte später noch einmal versuchen."); return@novaScript }
            novaLoadAll(urls, if (best) originals else "", { n, all -> offline = offline.copy(done = n, count = all) }) { note ->
                offline = offline.copy(loading = false, note = note.ifBlank { null }); refreshOffline()
            }
        }
    }
    override fun cancelOffline() { novaCancelAll() }
    override fun deleteOffline() { novaScript { ok -> if (ok) novaClear { offline = offline.copy(savedBytes = 0, done = 0, complete = false, note = null) } } }

    override fun stop() { turn++; browserStop(); novaStop(); speaking = false }
}
