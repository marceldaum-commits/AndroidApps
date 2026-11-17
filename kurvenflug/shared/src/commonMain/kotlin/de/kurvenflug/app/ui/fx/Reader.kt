package de.kurvenflug.app.ui.fx

import androidx.compose.runtime.staticCompositionLocalOf

/** Eine wählbare Stimme (Gerät/Browser oder die natürliche Stimme „Thorsten“). */
data class VoiceChoice(val id: String, val label: String)

/** Zustand des Stimm-Pakets „Thorsten“ (wird nur auf Wunsch geladen). */
sealed interface PackState {
    data object Missing : PackState
    data class Loading(val percent: Int) : PackState
    data class Failed(val why: String) : PackState
    data object Ready : PackState
}

/**
 * Volt liest vor: Sprechblasen, Aufträge, Rechen-Stern-Fragen und Erklärungen.
 * Android: Sprachausgabe des Geräts bzw. „Thorsten“ (sherpa-onnx). Browser: Stimmen des Browsers bzw. „Thorsten“ (ONNX Runtime Web).
 * Beide Wege nutzen dieselbe Aufbereitung ([de.kurvenflug.core.model.Speech]) und dieselbe Lautschrift.
 */
interface Reader {
    /** Deutsche Stimmen des Geräts/Browsers, die ohne Internet sprechen (beste zuerst) – beobachtbar */
    val voices: List<VoiceChoice>
    /** Das Stimm-Paket „Thorsten“ kann geladen werden (nicht in der Play-Fassung: die kommt ohne Internet aus) */
    val canLoadPack: Boolean
    /** Stimm-Paket „Thorsten“ – beobachtbar */
    val pack: PackState
    /** Spricht gerade (dann wird die Musik leiser) – beobachtbar */
    val speaking: Boolean
    /** Gewählte Stimme ("" = beste Stimme des Geräts, [NATURAL] = Thorsten) und Tempo (1 = normal) */
    fun configure(voice: String, rate: Float)
    /** Liest den Text vor; ein neuer Text unterbricht den alten. */
    fun say(text: String)
    fun stop()
    fun loadPack()
    fun cancelPack()
    fun removePack()

    companion object {
        const val NATURAL = "thorsten"
        const val NATURAL_LABEL = "Thorsten (natürlich)"
        /** Größe des Stimm-Pakets für den Hinweis vor dem Laden */
        const val PACK_MB = 63
        /** Woher das Stimm-Paket kommt (für den Datenschutz-Hinweis) */
        const val PACK_HOST = "huggingface.co"
        const val PROBE = de.kurvenflug.core.model.Speech.PROBE
    }
}

/** null = kein Vorlesen möglich (z. B. Bildschirmfoto-Tests) */
val LocalReader = staticCompositionLocalOf<Reader?> { null }
/** Vorlesen eingeschaltet */
val LocalReadAloud = staticCompositionLocalOf { false }

/**
 * Vorlesen aus der Oberfläche: liest [text] vor und merkt sich, wer gerade spricht ([owner]).
 * Verschwindet der Sprecher (z. B. eine Seite wird weggewischt), hört er nur auf, wenn nicht schon ein anderer spricht.
 */
object Voice {
    private var owner: Any? = null
    fun say(reader: Reader?, text: String, from: Any) { if (reader == null) return; owner = from; reader.say(text) }
    fun leave(reader: Reader?, from: Any) { if (owner === from) { owner = null; reader?.stop() } }
}

/** Liest [text] vor, sobald er erscheint oder sich ändert (wenn Vorlesen an ist), und hört auf, wenn er verschwindet. */
@androidx.compose.runtime.Composable
fun ReadAloud(text: String?) {
    val reader = LocalReader.current; val on = LocalReadAloud.current
    val me = androidx.compose.runtime.remember { Any() }
    androidx.compose.runtime.LaunchedEffect(text, on) { if (on && !text.isNullOrBlank()) Voice.say(reader, text, me) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Voice.leave(reader, me) } }
}
