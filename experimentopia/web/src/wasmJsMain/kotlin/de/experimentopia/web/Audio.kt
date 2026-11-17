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
                delay((d * 1000).toLong() + 4000 + Random.nextLong(5000))
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
    private var turn = 0

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
            if (canSpeak()) browserSpeak(rest, "", 1.1 * Nova.speed(prefs.rate), 1.0, { }, { if (mine == turn) speaking = false }) else speaking = false
        }
        if (!audioReady()) { fallback(0); return }
        novaScript { ok ->
            if (mine != turn) return@novaScript
            if (!ok) { fallback(0); return@novaScript }
            novaSpeak(parts.joinToString("\n"), parts.joinToString("\n") { Nova.url(it) }, Nova.speed(prefs.rate).toDouble(), { }, { if (mine == turn) speaking = false }, { i -> fallback(i) })
        }
    }

    override fun stop() { turn++; browserStop(); novaStop(); speaking = false }
}
