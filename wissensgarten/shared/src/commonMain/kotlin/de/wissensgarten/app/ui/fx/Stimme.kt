package de.wissensgarten.app.ui.fx

import de.wissensgarten.core.data.VOICE_THORSTEN
import de.wissensgarten.core.model.Lautschrift
import de.wissensgarten.core.model.SprechText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Eine Stimme des Geräts bzw. Browsers (nur solche, die ohne Internet sprechen). */
data class VoiceOption(val id: String, val label: String)

/** Zustand des Stimm-Pakets „Thorsten“ (wird erst auf Wunsch geladen). */
sealed interface VoicePackState {
    data object Missing : VoicePackState
    data class Loading(val percent: Int) : VoicePackState
    data class Failed(val why: String) : VoicePackState
    data object Ready : VoicePackState
}

/**
 * Das Stimm-Paket „Thorsten“: eine natürlich klingende deutsche Stimme (Piper, Aufnahmen CC0), die ganz auf dem Gerät rechnet.
 * Die große Stimmdatei kommt nur auf Wunsch von unserem Stimm-Server (stimmen.mathenachhilfe-online.com) – danach verlässt kein Text das Gerät.
 */
interface NaturalVoicePack {
    val state: StateFlow<VoicePackState>
    /** Warum die Stimme auf diesem Gerät nicht startet (null = alles gut) */
    val problem: StateFlow<String?>
    val sizeMb: Int get() = THORSTEN_MB
    fun download()
    fun cancel()
    fun remove()
}

const val THORSTEN_MB = 63

internal val NO_VOICES: StateFlow<List<VoiceOption>> = MutableStateFlow(emptyList())

/** Wer liest einen Text vor: Thorsten (dann Laute je Satz) oder die Stimme des Geräts (null). */
object VoiceRoute {
    /** Kurzer Satz für die Probe-Taste */
    const val PROBE = "Hallo, ich bin Brummi! So klinge ich, wenn ich dir vorlese."

    /**
     * Laute je Satz für Thorsten – oder null: Thorsten nicht gewählt oder nicht bereit, keine deutsche Sprache,
     * oder ein Wort fehlt im Wörterbuch (dann liest die Gerätestimme diesen einen Text).
     */
    fun natural(text: String, locale: Locale, voiceName: String, lexicon: Lautschrift?): List<String>? {
        if (voiceName != VOICE_THORSTEN || locale.language != "de" || lexicon == null) return null
        val clean = SprechText.clean(Voice.stripEmoji(text))
        if (clean.isEmpty()) return null
        return lexicon.of(clean)
    }

    /** Sprechtempo 50…150 % → Faktor */
    fun rate(percent: Int) = percent.coerceIn(50, 150) / 100f
}
