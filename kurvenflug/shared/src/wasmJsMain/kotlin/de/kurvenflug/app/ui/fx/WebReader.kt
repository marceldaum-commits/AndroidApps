package de.kurvenflug.app.ui.fx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kurvenflug.app.*
import de.kurvenflug.core.model.Lautschrift
import de.kurvenflug.core.model.Speech

/**
 * Volt liest vor (Browser): Stimmen des Browsers (nur deutsche, die auf dem Gerät rechnen) oder „Thorsten“ (ONNX Runtime Web).
 * Die Laute rechnet die App aus der mitgelieferten Lautschrift (stimme/saetze.tsv, woerter.tsv, dieselben Dateien wie in der APK) – genau wie auf Android.
 * Kennt die Lautschrift ein Wort nicht, liest diesen Text die Browser-Stimme.
 */
object WebReader : Reader {
    override val canLoadPack = true
    override var voices by mutableStateOf(emptyList<VoiceChoice>()); private set
    override var pack by mutableStateOf<PackState>(PackState.Missing); private set
    override var speaking by mutableStateOf(false); private set

    private var wanted = ""
    private var rate = 1f
    private var started = false
    private var laute: Lautschrift? = null
    private var tokens: Map<Int, Int>? = null
    private var dataLoading = false
    /** Der Browser erlaubt Ton erst nach dem ersten Tippen – bis dahin merken wir uns den letzten Text und lesen ihn dann vor */
    private var unlocked = false
    private var packChecked = false
    /** Text, der auf Thorsten wartet (Paket wird gerade geöffnet) oder auf das erste Tippen */
    private var pending: String? = null

    private fun start() {
        if (started) return
        started = true
        runCatching {
            refreshVoices(); jsOnVoices { refreshVoices() }
            jsOnSpeaking { speaking = it }
            jsPackHas { has -> packChecked = true; if (has && pack == PackState.Missing) { pack = PackState.Ready; if (wanted == Reader.NATURAL) prepareNatural() }; flush() }
            jsOnUnlock { unlocked = true; flush() }
        }
    }
    private fun refreshVoices() {
        voices = jsVoices().lines().filter { it.isNotBlank() }.map { VoiceChoice(it, it.replace("Microsoft ", "").replace(" - German (Germany)", "").replace("Google ", "")) }
    }

    /** Lautschrift und Zeichenliste laden (aus dem Speicher „kf-stimme“, auch offline) und den Rechenkern vorwärmen */
    private fun prepareNatural() {
        if (laute != null || dataLoading) return
        dataLoading = true
        jsText("stimme/tokens.txt") { t -> jsText("stimme/saetze.tsv") { s -> jsText("stimme/woerter.tsv") { w ->
            dataLoading = false
            if (t.isNotBlank() && s.isNotBlank() && w.isNotBlank()) {
                tokens = Lautschrift.tokens(t.lineSequence()); laute = Lautschrift(s.lineSequence(), w.lineSequence())
            }
            flush()
        } } }
        jsNatWarm()
    }
    private fun flush() { val p = pending ?: return; if (!unlocked || waiting()) return; pending = null; say(p) }
    /** Thorsten ist gewählt und geladen, aber die Lautschrift wird noch geöffnet */
    private fun waiting() = wanted == Reader.NATURAL && (!packChecked || pack == PackState.Ready && laute == null && dataLoading)

    override fun configure(voice: String, rate: Float) {
        start(); wanted = voice; this.rate = rate.coerceIn(0.6f, 1.5f)
        if (voice == Reader.NATURAL && pack == PackState.Ready) prepareNatural()
    }

    override fun say(text: String) {
        start()
        val clean = Speech.clean(text); if (clean.isEmpty()) return
        if (!unlocked) { pending = text; return }
        if (wanted == Reader.NATURAL && pack == PackState.Ready && laute == null) prepareNatural()
        if (waiting()) { pending = text; return }
        if (wanted == Reader.NATURAL && pack == PackState.Ready) {
            val l = laute; val tk = tokens
            val sentences = l?.of(clean)
            if (l != null && tk != null && sentences != null) {
                val ids = sentences.joinToString("\n") { Lautschrift.ids(it, tk).joinToString(",") }
                runCatching { jsNatSay(ids, rate.toDouble()) { jsSay(clean, "", rate.toDouble()) } }
                return
            }
            prepareNatural()
        }
        runCatching { jsSay(clean, wanted, rate.toDouble()) }
    }

    override fun stop() { pending = null; if (started) runCatching { jsHush() }; speaking = false }

    override fun loadPack() {
        start()
        if (pack is PackState.Loading || pack == PackState.Ready) return
        pack = PackState.Loading(0)
        jsPackLoad({ p -> if (pack is PackState.Loading) pack = PackState.Loading(p) }) { err ->
            pack = when { err.isEmpty() -> PackState.Ready; err == "abgebrochen" -> PackState.Missing; else -> PackState.Failed(err) }
            if (err.isEmpty() && wanted == Reader.NATURAL) prepareNatural()
        }
    }
    override fun cancelPack() { jsPackCancel() }
    override fun removePack() { jsPackRemove(); laute = null; tokens = null; pack = PackState.Missing }
}
