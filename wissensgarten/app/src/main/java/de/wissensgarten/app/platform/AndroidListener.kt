package de.wissensgarten.app.platform

import de.wissensgarten.app.ui.fx.Heard
import de.wissensgarten.app.ui.fx.Listener
import de.wissensgarten.app.ui.fx.Locale

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

/**
 * Hört einmal zu und liefert, was gesagt wurde (mehrere Vorschläge). Ohne Dialog – für Brummi-Radio.
 * Bevorzugt die Erkennung auf dem Gerät, wenn vorhanden.
 */
class AndroidListener(private val context: Context) : Listener {
    private var recognizer: SpeechRecognizer? = null
    override var onLevel: (Float) -> Unit = {}

    override fun available() = SpeechRecognizer.isRecognitionAvailable(context)

    override suspend fun listen(locale: Locale): Heard = withContext(Dispatchers.Main) {
        val done = CompletableDeferred<Heard>()
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(b: Bundle?) {
                val l = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                done.complete(if (l.isEmpty()) Heard.Silence else Heard.Words(l))
            }
            override fun onError(error: Int) {
                done.complete(when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Heard.Silence
                    else -> Heard.Failed(error)   // Mikrofon belegt, kein Netz, Sprache fehlt, App im Hintergrund …
                })
            }
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
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.tag)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        r.startListening(intent)
        val result = withTimeoutOrNull(12_000) { done.await() } ?: run { r.cancel(); Heard.Silence }
        onLevel(0f)
        result
    }

    override fun cancel() { recognizer?.cancel() }
    override fun release() { recognizer?.destroy(); recognizer = null }
}
