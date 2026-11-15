package de.raumlicht.app.ui.fx

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.staticCompositionLocalOf
import de.raumlicht.core.model.Speech
import java.util.Locale

/** Wer Lux' Sprechblasen laut vorliest. */
fun interface Speaker { fun say(text: String) }
val LocalSpeaker = staticCompositionLocalOf<Speaker?> { null }

/**
 * Lux spricht: die Sprachausgabe des Geräts liest die Sprechblasen vor – höher und etwas flotter gestellt, damit es nach kleiner Drohne klingt.
 * Bevorzugt wird eine deutsche Stimme, die ohne Internet auskommt. Gibt es auf dem Gerät keine deutsche Stimme, bleibt Lux stumm ([available] = false).
 * [onSpeaking] meldet Anfang und Ende, damit die Musik so lange leiser wird.
 */
class LuxVoice(context: Context, private val onSpeaking: (Boolean) -> Unit = {}) : Speaker {
    @Volatile var enabled = true
    @Volatile var volume = 1f
    @Volatile var available = false
        private set
    private var ready = false
    private var pending: String? = null
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) setUp()
    }

    private fun setUp() {
        val ok = runCatching { tts.setLanguage(Locale.GERMANY) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        available = ok >= TextToSpeech.LANG_AVAILABLE
        if (!available) return
        // lieber eine Stimme, die auf dem Gerät rechnet – dann geht kein Text ins Netz
        runCatching { tts.voices?.filter { it.locale.language == "de" && !it.isNetworkConnectionRequired }?.minByOrNull { it.quality * -1 }?.let { tts.voice = it } }
        tts.setPitch(1.45f); tts.setSpeechRate(1.08f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = onSpeaking(true)
            override fun onDone(id: String?) = onSpeaking(false)
            @Deprecated("Deprecated in Java") override fun onError(id: String?) = onSpeaking(false)
            override fun onStop(id: String?, interrupted: Boolean) = onSpeaking(false)
        })
        ready = true
        pending?.let { say(it) }; pending = null
    }

    /** Liest den Text vor; ein neuer Satz unterbricht den alten (Lux redet nie durcheinander). */
    override fun say(text: String) {
        if (!enabled || volume <= 0f) return
        if (!ready) { pending = text; return }
        val clean = Speech.clean(text); if (clean.isEmpty()) return
        runCatching { tts.speak(clean, TextToSpeech.QUEUE_FLUSH, Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }, "lux") }
    }
    fun stop() { pending = null; runCatching { tts.stop() }; onSpeaking(false) }
    fun shutdown() { runCatching { tts.shutdown() } }
}
