package de.kurvenflug.app.ui.fx

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Volt liest vor – mit einer KI-generierten (synthetischen) Stimme aus unserem Tonstudio (Vorlesen ist derzeit ausgeschaltet, siehe [de.kurvenflug.core.model.Nova.ENABLED]).
 * Ablauf (gleich auf Android und im Browser):
 *  1. Text vorlesefertig machen ([de.kurvenflug.core.model.Speech.clean]) → fertige Ansage unter [de.kurvenflug.core.model.Nova.url]
 *     laden (feste Texte vertont unser Stimmen-Werk vorab) und zwischenspeichern.
 *  2. Fehlt sie (Text mit Zahlen zur Laufzeit), den Text ohne Angaben zum Kind an unseren Server schicken ([de.kurvenflug.core.model.Nova.LIVE]),
 *     nur wenn er kurz genug ist und keinen Spielernamen enthält ([de.kurvenflug.core.model.Nova.maySend]).
 *  3. Klappt beides nicht (offline, Server sagt nein): still die Stimme des Geräts als Notlösung.
 * Die App spricht nie mit einem KI-Anbieter, sie lädt nur fertige Dateien von unserem Server.
 */
interface Reader {
    /** Spricht gerade (dann wird die Musik leiser) – beobachtbar */
    val speaking: Boolean
    /** Nova war in dieser Sitzung schon einmal nicht erreichbar (dann einmal dezent Bescheid geben) – beobachtbar */
    val unreachable: Boolean get() = false
    /** Liest den Text vor; ein neuer Text unterbricht den alten. */
    fun say(text: String)
    fun stop()
    /** Spielernamen – Texte, die einen davon enthalten, gehen nie an den Server */
    fun setPrivate(words: Set<String>) {}
    /** Sprechtempo (1 = Server-Tempo): Abspielgeschwindigkeit der Nova-Ansage bei gleicher Tonhöhe; auch für die Notlösung */
    fun setSpeed(speed: Float) {}

    /** „Alle Nova-Texte offline laden“ – beobachtbar */
    val offline: OfflineState get() = OfflineState()
    /** Stand neu ermitteln: was liegt schon auf dem Gerät, wie groß ist alles laut Server */
    fun refreshOffline() {}
    /** [best] = Bestqualität (unkomprimiert, 128 kbit/s) statt Standard */
    fun loadOffline(best: Boolean) {}
    fun cancelOffline() {}
    fun clearOffline() {}

    companion object {
        const val PROBE = de.kurvenflug.core.model.Speech.PROBE
    }
}

/**
 * Offline-Paket der Nova-Stimme: [have] von [total] festen Texten liegen auf dem Gerät ([haveBytes]); [serverBytes] = Größe aller
 * Ansagen laut Server (live gelesen). Während des Ladens: [done] von [todo].
 */
data class OfflineState(
    val total: Int = 0, val have: Int = 0, val haveBytes: Long = 0, val serverBytes: Long? = null,
    /** Bestqualität: gespeicherte Texte und Größe laut Server */
    val haveBest: Int = 0, val serverBytesBest: Long? = null,
    val loading: Boolean = false, val done: Int = 0, val todo: Int = 0, val problem: String? = null,
) {
    /** Noch zu laden, geschätzt: alles laut Server minus das, was schon da ist */
    fun missingBytes(best: Boolean): Long? = (if (best) serverBytesBest else serverBytes)?.let { all -> (all * (total - count(best)).coerceAtLeast(0) / total.coerceAtLeast(1)) }
    fun count(best: Boolean) = if (best) haveBest else have
    fun complete(best: Boolean) = total > 0 && count(best) >= total
    val stored get() = have + haveBest
}

/** Megabyte zum Anzeigen (mindestens 1, wenn überhaupt etwas da ist) */
fun mb(bytes: Long): String = if (bytes <= 0) "0" else ((bytes + 500_000) / 1_000_000).coerceAtLeast(1).toString()

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
