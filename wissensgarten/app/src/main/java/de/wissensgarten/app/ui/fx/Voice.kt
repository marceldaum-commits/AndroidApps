package de.wissensgarten.app.ui.fx

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/**
 * Brummis Stimme: Android-Sprachausgabe (offline, kostenlos). Etwas höher gestimmt, damit sie nach Hummel klingt.
 * Fremdsprachige Antworten werden in der passenden Sprache gesprochen – gut für die Aussprache.
 */
class Voice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    /** Wird aufgerufen, wenn Brummi anfängt / aufhört zu sprechen (Musik leiser stellen). */
    var onSpeaking: (Boolean) -> Unit = {}

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

    fun speak(text: String, locale: Locale = Locale.GERMAN) {
        if (!ready) return
        val clean = stripEmoji(text).trim()
        if (clean.isEmpty()) return
        val l = if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) locale else Locale.GERMAN
        tts.language = l
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "brummi")
    }

    fun stop() { if (ready) tts.stop(); waiting.values.forEach { it.complete(Unit) }; waiting.clear() }

    val isReady get() = ready
    private val waiting = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.CompletableDeferred<Unit>>()

    /** Sprechen und warten, bis Brummi fertig ist (für Brummi-Radio). */
    suspend fun speakAndWait(text: String, locale: Locale = Locale.GERMAN) {
        if (!ready) return
        val clean = stripEmoji(text).trim()
        if (clean.isEmpty()) return
        val id = "radio-" + System.nanoTime()
        val done = kotlinx.coroutines.CompletableDeferred<Unit>()
        waiting[id] = done
        tts.language = if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) locale else Locale.GERMAN
        tts.speak(clean, TextToSpeech.QUEUE_ADD, null, id)
        // Sicherheitsnetz, falls die Sprachausgabe hängt
        kotlinx.coroutines.withTimeoutOrNull(30_000) { done.await() }
        waiting.remove(id)
    }

    companion object {
        private val EMOJI = Regex("[\\p{So}\\p{Cs}\\uFE0F\\u200D]")
        fun stripEmoji(s: String) = s.replace(EMOJI, "")

        private val GERMAN_HINT = Regex("\\b(der|die|das|ein|eine|ist|was|wie|welche[rs]?|heißt|und|auf|von|mit|nicht)\\b|[äöüß]", RegexOption.IGNORE_CASE)

        /** Sprache für einen Text: bei Sprach-Fächern Fremdsprache, außer der Text ist erkennbar deutsch. */
        fun localeFor(subject: String?, text: String): Locale {
            val foreign = when (subject) {
                "englisch" -> Locale.ENGLISH
                "franzoesisch" -> Locale.FRENCH
                "spanisch" -> Locale("es", "ES")
                else -> return Locale.GERMAN
            }
            return if (GERMAN_HINT.containsMatchIn(text)) Locale.GERMAN else foreign
        }
    }
}

val LocalVoice = staticCompositionLocalOf<Voice?> { null }
/** Liest Brummi automatisch vor? (aus Einstellung + Klassenstufe) */
val LocalReadAloud = staticCompositionLocalOf { false }

/** Fach der aktuellen Karte – bestimmt die Vorlese-Sprache. */
val LocalSubject = staticCompositionLocalOf<String?> { null }
