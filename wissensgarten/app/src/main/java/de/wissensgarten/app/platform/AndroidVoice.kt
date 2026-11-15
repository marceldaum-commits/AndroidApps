package de.wissensgarten.app.platform

import de.wissensgarten.app.ui.fx.Locale
import de.wissensgarten.app.ui.fx.Voice

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Brummis Stimme: Android-Sprachausgabe (offline, kostenlos). Etwas höher gestimmt, damit sie nach Hummel klingt.
 * Fremdsprachige Antworten werden in der passenden Sprache gesprochen – gut für die Aussprache.
 */
class AndroidVoice(context: Context) : Voice, TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    /** Wird aufgerufen, wenn Brummi anfängt / aufhört zu sprechen (Musik leiser stellen). */
    override var onSpeaking: (Boolean) -> Unit = {}

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.setPitch(1.25f); tts.setSpeechRate(0.95f)
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(id: String?) = onSpeaking(true)
                override fun onDone(id: String?) { onSpeaking(false); waiting.remove(id)?.complete(Unit) }
                @Deprecated("Deprecated in Java") override fun onError(id: String?) { onSpeaking(false); waiting.remove(id)?.complete(Unit) }
                override fun onStop(id: String?, interrupted: Boolean) { onSpeaking(false); waiting.remove(id)?.complete(Unit) }
            })
        }
    }

    override fun speak(text: String, locale: Locale) {
        if (!ready) return
        val clean = Voice.stripEmoji(text).trim()
        if (clean.isEmpty()) return
        tts.language = jl(locale)
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "brummi")
    }

    override fun stop() { if (ready) tts.stop(); waiting.values.forEach { it.complete(Unit) }; waiting.clear() }

    override val isReady get() = ready
    private val waiting = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.CompletableDeferred<Unit>>()

    /** Sprechen und warten, bis Brummi fertig ist (für Brummi-Radio). */
    override suspend fun speakAndWait(text: String, locale: Locale) {
        if (!ready) return
        val clean = Voice.stripEmoji(text).trim()
        if (clean.isEmpty()) return
        val id = "radio-" + System.nanoTime()
        val done = kotlinx.coroutines.CompletableDeferred<Unit>()
        waiting[id] = done
        tts.language = jl(locale)
        tts.speak(clean, TextToSpeech.QUEUE_ADD, null, id)
        // Sicherheitsnetz, falls die Sprachausgabe hängt
        kotlinx.coroutines.withTimeoutOrNull(30_000) { done.await() }
        waiting.remove(id)
    }

    private fun jl(l: Locale): java.util.Locale {
        val j = java.util.Locale.forLanguageTag(l.tag)
        return if (tts.isLanguageAvailable(j) >= TextToSpeech.LANG_AVAILABLE) j else java.util.Locale.GERMAN
    }
}
