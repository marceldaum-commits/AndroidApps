package de.kurvenflug.app.ui.fx

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Volt liest vor – mit der Stimme **Nova** (KI-generiert, OpenAI; Vorgabe Marcel 17.11.: „zukünftig nur Nova“).
 * Ablauf (gleich auf Android und im Browser):
 *  1. Text vorlesefertig machen ([de.kurvenflug.core.model.Speech.clean]) → fertige Ansage unter [de.kurvenflug.core.model.Nova.url]
 *     laden (feste Texte vertont unser Stimmen-Werk vorab) und zwischenspeichern.
 *  2. Fehlt sie (Text mit Zahlen zur Laufzeit), den Text ohne Angaben zum Kind an unseren Server schicken ([de.kurvenflug.core.model.Nova.LIVE]),
 *     nur wenn er kurz genug ist und keinen Spielernamen enthält ([de.kurvenflug.core.model.Nova.maySend]).
 *  3. Klappt beides nicht (offline, Server sagt nein): still die Stimme des Geräts als Notlösung.
 * Die App schickt nie etwas an OpenAI.
 */
interface Reader {
    /** Spricht gerade (dann wird die Musik leiser) – beobachtbar */
    val speaking: Boolean
    /** Liest den Text vor; ein neuer Text unterbricht den alten. */
    fun say(text: String)
    fun stop()
    /** Spielernamen – Texte, die einen davon enthalten, gehen nie an den Server */
    fun setPrivate(words: Set<String>) {}
    /** Sprechtempo (1 = Server-Tempo): Abspielgeschwindigkeit der Nova-Ansage bei gleicher Tonhöhe; auch für die Notlösung */
    fun setSpeed(speed: Float) {}

    companion object {
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
