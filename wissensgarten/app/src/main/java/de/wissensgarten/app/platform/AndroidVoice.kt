package de.wissensgarten.app.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import de.wissensgarten.app.ui.fx.Locale
import de.wissensgarten.app.ui.fx.Voice
import de.wissensgarten.app.ui.fx.VoiceOption
import de.wissensgarten.app.ui.fx.VoicePackState
import de.wissensgarten.app.ui.fx.VoiceRoute
import de.wissensgarten.core.data.VOICE_THORSTEN
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Brummis Stimme: Android-Sprachausgabe (offline, kostenlos), etwas höher gestimmt, damit sie nach Hummel klingt –
 * oder auf Wunsch die natürliche Stimme „Thorsten“ ([ThorstenEngine]), die ganz auf dem Gerät rechnet.
 * Fremdsprachige Antworten werden immer mit einer Gerätestimme der passenden Sprache gesprochen – gut für die Aussprache.
 */
class AndroidVoice(context: Context) : Voice, TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false
    @Volatile private var voiceName = ""
    @Volatile private var rate = 1f

    /** Wird aufgerufen, wenn Brummi anfängt / aufhört zu sprechen (Musik leiser stellen). */
    override var onSpeaking: (Boolean) -> Unit = {}
    override val voices = MutableStateFlow<List<VoiceOption>>(emptyList())

    private val pack = ThorstenPack(context.applicationContext)
    private val thorsten = ThorstenEngine(context.applicationContext, pack) { onSpeaking(it) }
    override val natural get() = pack

    init { pack.onChanged = { syncThorsten() } }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.setPitch(1.25f); tts.setSpeechRate(0.95f * rate)
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(id: String?) = onSpeaking(true)
                override fun onDone(id: String?) { onSpeaking(false); waiting.remove(id)?.complete(Unit) }
                @Deprecated("Deprecated in Java") override fun onError(id: String?) { onSpeaking(false); waiting.remove(id)?.complete(Unit) }
                override fun onStop(id: String?, interrupted: Boolean) { onSpeaking(false); waiting.remove(id)?.complete(Unit) }
            })
            voices.value = runCatching { germanVoices() }.getOrDefault(emptyList())
        }
    }

    /** Deutsche Stimmen, die ohne Internet sprechen und schon auf dem Gerät sind */
    private fun germanVoices(): List<VoiceOption> {
        val list = tts.voices.orEmpty().filter { v ->
            v.locale.language == "de" && !v.isNetworkConnectionRequired && v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }.sortedWith(compareBy({ it.locale.country != "DE" }, { it.name }))
        return list.mapIndexed { i, v ->
            val land = v.locale.getDisplayCountry(java.util.Locale.GERMAN).ifBlank { "Deutsch" }
            VoiceOption(v.name, "Stimme ${i + 1} · $land")
        }
    }

    override fun setVoice(name: String) { voiceName = name; syncThorsten() }
    override fun setRate(percent: Int) { rate = VoiceRoute.rate(percent); if (ready) tts.setSpeechRate(0.95f * rate) }

    /** Thorsten nur im Speicher halten, wenn er gewählt und geladen ist */
    private fun syncThorsten() {
        if (voiceName == VOICE_THORSTEN && pack.state.value == VoicePackState.Ready) thorsten.load() else thorsten.unload()
    }

    /** Laute für Thorsten – oder null, dann spricht die Gerätestimme */
    private fun natural(text: String, locale: Locale): List<String>? =
        if (thorsten.ready) VoiceRoute.natural(text, locale, voiceName, thorsten.lexicon) else null

    override fun speak(text: String, locale: Locale) {
        natural(text, locale)?.let { s -> if (ready) tts.stop(); thorsten.say(s, rate); return }
        thorsten.stop()
        if (!ready) return
        val clean = Voice.stripEmoji(text).trim()
        if (clean.isEmpty()) return
        choose(locale)
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "brummi")
    }

    override fun stop() {
        thorsten.stop()
        if (ready) tts.stop(); waiting.values.forEach { it.complete(Unit) }; waiting.clear()
    }

    override val isReady get() = ready || thorsten.ready
    private val waiting = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    /** Sprechen und warten, bis Brummi fertig ist (für Brummi-Radio und Antwort + Erklärung). */
    override suspend fun speakAndWait(text: String, locale: Locale) {
        natural(text, locale)?.let { s ->
            if (ready) tts.stop()
            val done = CompletableDeferred<Unit>()
            thorsten.say(s, rate) { done.complete(Unit) }
            withTimeoutOrNull(60_000) { done.await() }
            return
        }
        if (!ready) return
        val clean = Voice.stripEmoji(text).trim()
        if (clean.isEmpty()) return
        val id = "radio-" + System.nanoTime()
        val done = CompletableDeferred<Unit>()
        waiting[id] = done
        choose(locale)
        tts.speak(clean, TextToSpeech.QUEUE_ADD, null, id)
        // Sicherheitsnetz, falls die Sprachausgabe hängt
        withTimeoutOrNull(30_000) { done.await() }
        waiting.remove(id)
    }

    /** Deutsch mit der gewählten Gerätestimme, Fremdsprachen mit der Standardstimme der Sprache */
    private fun choose(locale: Locale) {
        if (locale.language == "de" && voiceName.isNotEmpty() && voiceName != VOICE_THORSTEN) {
            val v = runCatching { tts.voices?.firstOrNull { it.name == voiceName } }.getOrNull()
            if (v != null && tts.setVoice(v) == TextToSpeech.SUCCESS) return
        }
        tts.language = jl(locale)
    }

    private fun jl(l: Locale): java.util.Locale {
        val j = java.util.Locale.forLanguageTag(l.tag)
        return if (tts.isLanguageAvailable(j) >= TextToSpeech.LANG_AVAILABLE) j else java.util.Locale.GERMAN
    }
}
