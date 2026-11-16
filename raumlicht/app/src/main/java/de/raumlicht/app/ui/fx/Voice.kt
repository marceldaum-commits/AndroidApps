package de.raumlicht.app.ui.fx

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import de.raumlicht.core.model.Speech
import java.util.Locale

/**
 * Lux spricht: die Sprachausgabe des Geräts liest die Sprechblasen vor – höher und etwas flotter gestellt, damit es nach kleiner Drohne klingt.
 * Bevorzugt wird eine deutsche Stimme, die ohne Internet auskommt. Gibt es auf dem Gerät keine deutsche Stimme, bleibt Lux stumm ([available] = false).
 * [onSpeaking] meldet Anfang und Ende, damit die Musik so lange leiser wird.
 */
class LuxVoice(context: Context, private val onSpeaking: (Boolean) -> Unit = {}) : Speaker {
    @Volatile var enabled = true
    @Volatile var volume = 1f
    /** Gibt es eine deutsche Stimme? Steht erst kurz nach dem Start fest – deshalb beobachtbar. */
    var available by androidx.compose.runtime.mutableStateOf(false)
        private set
    /** Wird wahr, sobald die Sprachausgabe geantwortet hat (vorher weiß man nicht, ob es eine Stimme gibt) */
    var checked by androidx.compose.runtime.mutableStateOf(false)
        private set
    private var ready = false
    private var wanted: String? = null
    private var bright = false
    /** Deutsche Stimmen des Geräts, beste zuerst */
    var voices by androidx.compose.runtime.mutableStateOf(emptyList<VoiceInfo>())
        private set
    var current by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    private var pending: String? = null
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    // Manche Geräte melden sich schon, während die Sprachausgabe noch angelegt wird – deshalb erst im nächsten Schritt einrichten
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status -> main.post { if (status == TextToSpeech.SUCCESS) setUp(); checked = true } }

    private fun setUp() {
        val ok = runCatching { tts.setLanguage(Locale.GERMANY) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        available = ok >= TextToSpeech.LANG_AVAILABLE
        if (!available) return
        // Stimmen, die auf dem Gerät rechnen (dann geht kein Text ins Netz) und schon installiert sind – die hochwertigste zuerst
        voices = runCatching { tts.voices.orEmpty().filter { it.locale.language == "de" }.map { VoiceInfo(it.name, it.quality, it.isNetworkConnectionRequired, "notInstalled" in it.features.orEmpty()) } }.getOrDefault(emptyList()).let(::usable)
        apply()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = onSpeaking(true)
            override fun onDone(id: String?) = onSpeaking(false)
            @Deprecated("Deprecated in Java") override fun onError(id: String?) = onSpeaking(false)
            override fun onStop(id: String?, interrupted: Boolean) = onSpeaking(false)
        })
        ready = true
        pending?.let { say(it) }; pending = null
    }

    /** Stimme und Stimmlage einstellen: [wanted] = gemerkter Name (sonst die beste), [bright] = hellere „Drohnen“-Lage statt natürlicher */
    private fun apply() {
        val name = choose(voices, wanted) ?: return
        current = name
        runCatching { tts.voices?.firstOrNull { it.name == name }?.let { tts.voice = it } }
        // natürlich: fast unverändert und etwas ruhiger – stark verstellte Tonhöhe klingt blechern
        tts.setPitch(if (bright) 1.3f else 1.04f); tts.setSpeechRate(if (bright) 1.0f else 0.94f)
    }
    /** Nächste deutsche Stimme des Geräts wählen und gleich vorstellen. @return ihr Name */
    fun nextVoice(): String? {
        if (voices.size < 2) return current
        wanted = voices[(voices.indexOfFirst { it.name == current } + 1) % voices.size].name; apply(); say("Hallo, ich bin Lux. So klinge ich jetzt."); return current
    }
    fun setBright(on: Boolean) { bright = on; if (ready) { apply(); say("So klinge ich jetzt.") } }
    fun use(name: String, brightVoice: Boolean) { wanted = name.ifBlank { null }; bright = brightVoice; if (ready) apply() }
    /** Wievielte von wie vielen Stimmen gerade spricht, z. B. 2 to 5 */
    val position: Pair<Int, Int> get() = (voices.indexOfFirst { it.name == current } + 1) to voices.size

    /** Liest den Text vor; ein neuer Satz unterbricht den alten (Lux redet nie durcheinander). */
    override fun say(text: String) {
        if (!enabled || volume <= 0f) return
        if (!ready) { if (!checked) pending = text; return }
        val clean = Speech.clean(text); if (clean.isEmpty()) return
        runCatching { tts.speak(clean, TextToSpeech.QUEUE_FLUSH, Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }, "lux") }
    }
    override fun sayIfQuiet(text: String) { if (ready && runCatching { tts.isSpeaking }.getOrDefault(false)) return; say(text) }
    fun stop() { pending = null; runCatching { tts.stop() }; onSpeaking(false) }
    fun shutdown() { runCatching { tts.shutdown() } }
}

/** Was man über eine Stimme des Geräts wissen muss, um die angenehmste auszusuchen */
data class VoiceInfo(val name: String, val quality: Int, val network: Boolean, val missing: Boolean)
/** Brauchbar sind installierte Stimmen ohne Netz; sortiert nach Qualität (bei Gleichstand nach Namen, damit die Reihenfolge fest ist) */
fun usable(all: List<VoiceInfo>): List<VoiceInfo> = all.filter { !it.network && !it.missing }.sortedWith(compareByDescending<VoiceInfo> { it.quality }.thenBy { it.name })
/** Die gemerkte Stimme, wenn es sie noch gibt – sonst die hochwertigste */
fun choose(usable: List<VoiceInfo>, wanted: String?): String? = usable.firstOrNull { it.name == wanted }?.name ?: usable.firstOrNull()?.name
