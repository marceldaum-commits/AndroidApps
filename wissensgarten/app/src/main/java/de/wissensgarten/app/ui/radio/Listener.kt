package de.wissensgarten.app.ui.radio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Hört einmal zu und liefert, was gesagt wurde (mehrere Vorschläge). Ohne Dialog – für Brummi-Radio.
 * Bevorzugt die Erkennung auf dem Gerät, wenn vorhanden.
 */
class Listener(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    var onLevel: (Float) -> Unit = {}

    fun available() = SpeechRecognizer.isRecognitionAvailable(context)

    suspend fun listen(locale: Locale = Locale.GERMAN): List<String> = withContext(Dispatchers.Main) {
        val done = CompletableDeferred<List<String>>()
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(b: Bundle?) { done.complete(b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()) }
            override fun onError(error: Int) { done.complete(emptyList()) }
            override fun onRmsChanged(rmsdB: Float) = onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(p: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        r.startListening(intent)
        val result = withTimeoutOrNull(12_000) { done.await() } ?: run { r.cancel(); emptyList() }
        onLevel(0f)
        result
    }

    fun release() { recognizer?.destroy(); recognizer = null }
}
