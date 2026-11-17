package de.wissensgarten.app.ui.fx

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow
import de.wissensgarten.app.AppHost
import de.wissensgarten.core.model.MusicMood

/** Sprache für Vorlesen und Spracherkennung (BCP-47, z. B. „de-DE“). */
data class Locale(val tag: String) {
    constructor(language: String, country: String) : this("$language-$country")
    val language: String get() = tag.substringBefore('-')
    companion object {
        val GERMAN = Locale("de-DE")
        val ENGLISH = Locale("en-GB")
        val FRENCH = Locale("fr-FR")
    }
}

/** Kurze Geräusche (im Code erzeugt, siehe [Synth]). */
interface Sfx {
    var enabled: Boolean
    /** @param pitch 1 = normal, >1 höher (z. B. bei Serien) */
    fun play(s: Sound, pitch: Float = 1f, volume: Float = 0.9f)
}

/** Hintergrundmusik (im Code komponiert, siehe MusicSynth). */
interface Music {
    fun setEnabled(on: Boolean)
    fun setMood(m: MusicMood)
    fun onForeground(on: Boolean)
    fun duck(on: Boolean)
    fun prepare(vararg moods: MusicMood)
}

/**
 * Brummis Stimme: Sprachausgabe des Geräts bzw. des Browsers – oder die natürliche Stimme „Thorsten“ ([natural]),
 * die auf dem Gerät rechnet. Thorsten liest nur Deutsch; Englisch, Französisch und Spanisch liest immer eine Gerätestimme der Sprache.
 */
interface Voice {
    /** Wird aufgerufen, wenn Brummi anfängt / aufhört zu sprechen (Musik leiser stellen). */
    var onSpeaking: (Boolean) -> Unit
    val isReady: Boolean
    fun speak(text: String, locale: Locale = Locale.GERMAN)
    fun stop()
    /** Sprechen und warten, bis Brummi fertig ist (für Brummi-Radio). */
    suspend fun speakAndWait(text: String, locale: Locale = Locale.GERMAN)

    /** Deutsche Stimmen des Geräts bzw. Browsers zur Auswahl (nur solche, die ohne Internet sprechen) */
    val voices: StateFlow<List<VoiceOption>> get() = NO_VOICES
    /** Gewählte Stimme: "" = Standard, [de.wissensgarten.core.data.VOICE_THORSTEN] = Thorsten, sonst [VoiceOption.id] */
    fun setVoice(name: String) {}
    /** Sprechtempo in Prozent (50…150) */
    fun setRate(percent: Int) {}
    /** Stimm-Paket „Thorsten“; null = gibt es auf dieser Plattform nicht */
    val natural: NaturalVoicePack? get() = null

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

/** Ergebnis des Zuhörens: etwas gehört, nichts gesagt, oder technischer Fehler (dann nichts bewerten!). */
sealed interface Heard {
    data class Words(val list: List<String>) : Heard
    data object Silence : Heard
    data class Failed(val code: Int) : Heard
}

/** Spracherkennung: hört einmal zu und liefert, was gesagt wurde. */
interface Listener {
    var onLevel: (Float) -> Unit
    fun available(): Boolean
    suspend fun listen(locale: Locale = Locale.GERMAN): Heard
    fun cancel()
    fun release()
}

val LocalSfx = staticCompositionLocalOf<Sfx?> { null }
val LocalVoice = staticCompositionLocalOf<Voice?> { null }
/** Liest Brummi automatisch vor? (aus Einstellung + Klassenstufe) */
val LocalReadAloud = staticCompositionLocalOf { false }
/** Fach der aktuellen Karte – bestimmt die Vorlese-Sprache. */
val LocalSubject = staticCompositionLocalOf<String?> { null }
/** Plattform-Brücke für tiefer liegende Bildschirme (Teilen, Spracherkennung …). */
val LocalHost = staticCompositionLocalOf<AppHost?> { null }
