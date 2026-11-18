package de.kurvenflug.app.ui.fx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kurvenflug.app.*
import de.kurvenflug.core.model.Nova
import de.kurvenflug.core.model.Speech

/**
 * Volt liest vor (Browser) – mit der KI-Stimme Nova, genau wie auf Android (siehe [Reader]). Laden, Zwischenspeichern und
 * Abspielen erledigt kf.js; Text, Dateiname und die Entscheidung „darf zum Server“ kommen von hier (gemeinsamer Code).
 * Der Browser erlaubt Ton erst nach dem ersten Tippen – bis dahin merken wir uns den letzten Text und lesen ihn dann vor.
 */
object WebReader : Reader {
    override var speaking by mutableStateOf(false); private set
    override var unreachable by mutableStateOf(false); private set
    private var started = false
    private var unlocked = false
    private var pending: String? = null
    private var private: Set<String> = emptySet()

    private fun start() {
        if (started) return
        started = true
        runCatching {
            jsOnSpeaking { speaking = it }
            jsOnUnreach { unreachable = true }
            jsOnUnlock { unlocked = true; pending?.let { p -> pending = null; say(p) } }
        }
    }

    override fun setPrivate(words: Set<String>) { private = words }
    override fun setSpeed(speed: Float) { runCatching { jsNovaSpeed(speed.toDouble()) } }

    override fun say(text: String) {
        start()
        val spoken = Speech.clean(text); if (spoken.isEmpty()) return
        if (!unlocked) { pending = text; return }
        // ganzer Text zuerst; lange Texte zusätzlich in ganzen Sätzen (falls der ganze nicht vorab vertont ist)
        val items = listOf(spoken) + (if (spoken.length > Nova.MAX_LIVE) Nova.chunks(spoken) else emptyList())
        runCatching { jsNova(items.joinToString("\n") { "${Nova.url(it)}\t${it.replace('\t', ' ').replace('\n', ' ')}\t${if (Nova.maySend(it, private)) "1" else "0"}" }) }
    }

    // ------------------------------------------------------------------ „Alle Nova-Texte offline laden“
    override var offline by mutableStateOf(OfflineState()); private set
    private var hashes: String? = null
    private fun withList(then: (String) -> Unit) {
        hashes?.let { return then(it) }
        jsText("vorlesen/texte.json") { json -> val hs = Nova.listTexts(json).map(Nova::hash).distinct(); hashes = hs.joinToString("\n"); offline = offline.copy(total = hs.size); then(hashes!!) }
    }
    override fun refreshOffline() {
        withList { hs -> jsOffStat(hs) { n, nb, bytes, server, serverBest -> offline = offline.copy(have = n, haveBest = nb, haveBytes = bytes.toLong(),
            serverBytes = if (server > 0) server.toLong() else offline.serverBytes, serverBytesBest = if (serverBest > 0) serverBest.toLong() else offline.serverBytesBest) } }
    }
    override fun loadOffline(best: Boolean) {
        if (offline.loading) return
        offline = offline.copy(loading = true, done = 0, todo = 0, problem = null)
        withList { hs ->
            jsOffLoad(hs, best, { d, n -> offline = offline.copy(done = d, todo = n) }) { failed, aborted ->
                offline = offline.copy(loading = false, problem = when {
                    failed < 0 -> "Dieser Browser kann die Ansagen nicht speichern."
                    failed > 0 && !aborted -> "$failed Ansagen ließen sich nicht laden – beim nächsten Tippen geht es dort weiter."
                    else -> null })
                refreshOffline()
            }
        }
    }
    override fun cancelOffline() { jsOffCancel() }
    override fun clearOffline() { jsOffCancel(); jsOffClear { offline = offline.copy(have = 0, haveBest = 0, haveBytes = 0, loading = false); refreshOffline() } }

    override fun stop() { pending = null; if (started) runCatching { jsHush() }; speaking = false }
}
