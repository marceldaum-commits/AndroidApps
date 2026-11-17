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
    private var started = false
    private var unlocked = false
    private var pending: String? = null
    private var private: Set<String> = emptySet()

    private fun start() {
        if (started) return
        started = true
        runCatching {
            jsOnSpeaking { speaking = it }
            jsOnUnlock { unlocked = true; pending?.let { p -> pending = null; say(p) } }
        }
    }

    override fun setPrivate(words: Set<String>) { private = words }
    override fun setSpeed(speed: Float) { runCatching { jsNovaSpeed(speed.toDouble()) } }

    override fun say(text: String) {
        start()
        val spoken = Speech.clean(text); if (spoken.isEmpty()) return
        if (!unlocked) { pending = text; return }
        runCatching { jsNova(Nova.url(spoken), spoken, Nova.maySend(spoken, private)) }
    }

    override fun stop() { pending = null; if (started) runCatching { jsHush() }; speaking = false }
}
