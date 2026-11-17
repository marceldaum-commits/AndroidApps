package de.wissensgarten.app.ui.fx

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow
import de.wissensgarten.app.AppHost
import de.wissensgarten.core.model.LaufzeitText
import de.wissensgarten.core.model.MusicMood
import de.wissensgarten.core.model.Sprechtempo

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
    /** Lautstärke aller Geräusche 0..1 (Einstellung „Geräusche“), wird mit dem `volume` jedes Aufrufs verrechnet */
    var level: Float
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
    /** Lautstärke 0..1 (Einstellung „Musik“) */
    fun setVolume(v: Float)
    /** Erste Berührung: erst ab jetzt darf Musik erklingen (nie ungefragt beim Start) */
    fun unlock()
}

/**
 * Brummis Stimme: „Nova“, eine KI-Stimme (OpenAI), vertont von unserem Stimm-Server – siehe [NovaVoice].
 * Ohne Internet oder wenn ein Text (noch) nicht vertont ist, liest still die Stimme des Geräts.
 * Fremdsprachige Texte (Englisch, Französisch, Spanisch) spricht Nova in der passenden Sprache.
 */
interface Voice {
    /** Wird aufgerufen, wenn Brummi anfängt / aufhört zu sprechen (Musik leiser stellen). */
    var onSpeaking: (Boolean) -> Unit
    val isReady: Boolean
    /** Fester Text (Pakete, Brummis Sätze): kommt vorab vertont vom Server, sonst Gerätestimme – nie an `/jetzt` */
    fun speak(text: String, locale: Locale = Locale.GERMAN)
    /** Laufzeit-Text: darf, wenn nicht vorab vertont, einmalig an `/jetzt` (siehe [LaufzeitText]) */
    fun speak(text: LaufzeitText, locale: Locale = Locale.GERMAN) = speak(text.text, locale)
    fun stop()
    /** Sprechen und warten, bis Brummi fertig ist (für Brummi-Radio). */
    suspend fun speakAndWait(text: String, locale: Locale = Locale.GERMAN)
    suspend fun speakAndWait(text: LaufzeitText, locale: Locale = Locale.GERMAN) = speakAndWait(text.text, locale)
    /** Sprechtempo als Stufe in Prozent ([Sprechtempo.STUFEN], 100 = Tempo des Servers) */
    fun setRate(percent: Int) {}

    companion object {
        private val EMOJI = Regex("[\\p{So}\\p{Cs}\\uFE0F\\u200D]")
        fun stripEmoji(s: String) = s.replace(EMOJI, "")

        /** Sprache für einen Text: bei Sprach-Fächern Fremdsprache, außer der Text ist erkennbar deutsch (dieselbe Regel wie die Nova-Liste). */
        fun localeFor(subject: String?, text: String): Locale = when (de.wissensgarten.core.model.Nova.sprache(subject, text)) {
            "en" -> Locale.ENGLISH
            "fr" -> Locale.FRENCH
            "es" -> Locale("es", "ES")
            else -> Locale.GERMAN
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
