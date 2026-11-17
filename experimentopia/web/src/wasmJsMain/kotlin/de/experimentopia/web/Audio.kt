package de.experimentopia.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.experimentopia.app.ui.fx.*
import de.experimentopia.core.model.AudioPrefs
import de.experimentopia.core.model.Lautschrift
import de.experimentopia.core.model.Speech
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

/** Vorlesen im Browser: Stimmen des Browsers oder „Thorsten“ (stimme.js mit onnxruntime-web) – dieselben Texte und Regeln wie unter Android. */
class WebReader : Reader {
    private var prefs = AudioPrefs()
    override var voices by mutableStateOf(emptyList<VoiceOption>()); private set
    override var natural by mutableStateOf<NaturalState>(NaturalState.Missing); private set
    override var speaking by mutableStateOf(false); private set
    private var sounds: Lautschrift? = null
    private var starting = false

    init {
        if (canSpeak()) browserVoices { list ->
            voices = list.lines().filter { it.contains('\t') }.mapIndexed { i, l -> val (id, name) = l.split('\t', limit = 2); VoiceOption(id, name.ifBlank { "Browserstimme ${i + 1}" }) }
        }
        // War Thorsten schon einmal geladen? Dann gleich als bereit zeigen (gestartet wird erst beim ersten Vorlesen)
        if (stimmeKnown()) stimmeScript { ok -> if (ok) stimmeInstalled { has -> if (has) natural = NaturalState.Ready } }
    }

    private fun loadLautschrift(done: () -> Unit) {
        if (sounds != null) { done(); return }
        loadFile("stimme/saetze.tsv") { s -> loadFile("stimme/woerter.tsv") { w ->
            if (s != null && w != null) sounds = Lautschrift(s.decodeToString().lineSequence(), w.decodeToString().lineSequence())
            done()
        } }
    }
    /** Thorsten im Hintergrund starten (Stimmdatei aus dem Browser-Speicher) */
    private fun ensureEngine(then: () -> Unit) {
        if (stimmeReady() && sounds != null) { then(); return }
        if (starting) return
        starting = true
        stimmeScript { ok -> if (!ok) { starting = false; return@stimmeScript }
            loadLautschrift { stimmeLoad({ }, { err -> starting = false; if (err == null) then() else natural = NaturalState.Failed(err) }) } }
    }

    override fun configure(p: AudioPrefs) { prefs = p; if (p.voice == AudioPrefs.NATURAL && natural == NaturalState.Ready) ensureEngine { } }

    override fun say(text: String) {
        if (!prefs.read) return
        val clean = Speech.clean(text); if (clean.isEmpty()) return
        stop()
        if (prefs.voice == AudioPrefs.NATURAL && natural == NaturalState.Ready && audioReady()) {
            val laute = sounds?.of(clean)
            if (laute != null && stimmeReady()) {
                stimmeSpeak(laute.joinToString("\n"), prefs.rate.toDouble(), 1.0, { speaking = true }, { speaking = false }); return
            }
            if (laute == null && sounds == null) { ensureEngine { say(text) }; return }
        }
        // Stimme des Browsers (auch, wenn Thorsten ein Wort nicht kennt)
        if (canSpeak()) browserSpeak(clean, prefs.voice.takeIf { it != AudioPrefs.NATURAL } ?: "", (prefs.rate * 0.95).toDouble(), 1.0, { speaking = true }, { speaking = false })
    }

    override fun stop() { browserStop(); stimmeStop(); speaking = false }

    override fun loadNatural() {
        if (natural is NaturalState.Loading || natural == NaturalState.Ready) return
        natural = NaturalState.Loading(0)
        stimmeScript { ok ->
            if (!ok) { natural = NaturalState.Failed("Die Sprach-Bausteine ließen sich nicht laden."); return@stimmeScript }
            loadLautschrift {
                stimmeLoad({ p -> natural = NaturalState.Loading(p) }) { err ->
                    natural = when (err) { null -> { storeSet("experimentopia.thorsten", "1"); NaturalState.Ready }; "abgebrochen" -> NaturalState.Missing; else -> NaturalState.Failed(err) }
                }
            }
        }
    }
    override fun cancelNatural() { stimmeCancel() }
    override fun removeNatural() { stop(); storeRemove("experimentopia.thorsten"); stimmeScript { ok -> if (ok) stimmeRemove { } }; natural = NaturalState.Missing }
}
