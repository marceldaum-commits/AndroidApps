package de.raumlicht.core.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Die festen Vorlese-Texte der App – alles, was Lux wörtlich gleich vorliest (ohne Zufallszahlen und ohne Eingaben des Nutzers).
 * Daraus entsteht beim Bauen `webapp/vorlesen/texte.json`; unser Stimmen-Server holt die Liste und vertont jeden neuen Text einmal vorab.
 * Texte, die erst in der Aufgabe entstehen (Rückmeldungen auf Eingaben, endloses Üben, Tagesaufgabe), stehen hier nicht.
 */
object Vorlesen {
    const val FORMAT = 1
    const val APP = "raumlicht"
    const val LANGUAGE = "de"
    /** Obergrenze je Text, die der Stimmen-Server annimmt */
    const val MAX_CHARS = 1500

    /** Feste Sätze der Oberfläche, die Lux spricht (Einstellungen, Baumeister) */
    val phrases = listOf("Hallo, ich bin Lux!", "Hallo, ich bin Lux. So klinge ich jetzt.", "Geschafft!", "Der Schwung reicht nicht.", "Im Ziel, aber der Auftrag fehlt noch.")

    /** Die festen Texte, so wie sie in der App stehen (noch nicht vorlesefertig) */
    fun raw(): Set<String> {
        val fixed = LinkedHashSet<String>()
        fun collect(l: Level) {
            fixed += l.mission; fixed += l.tip
            runCatching { fixed += l.evaluate(l.solution).message; l.finale?.invoke(l.solution)?.acts?.forEach { fixed += it.say } }
        }
        Levels.chapters.forEach { c -> fixed += c.intro; c.levels.forEach { collect(it) } }
        Exams.all.forEach { e -> fixed += e.intro; e.parts.forEach { collect(it.level) } }
        fixed += phrases
        // Theorie-Karten: die Idee liest Lux vor, wenn man die Karte öffnet
        (Theory.cards + Theory.general).forEach { fixed += it.idea }
        SpotKind.entries.forEach { fixed += it.title }
        return fixed
    }

    /** Vorlesefertig (Vektoren und Formeln ausgeschrieben, siehe [Speech.clean]), ohne Leerraum-Reste, jeder Text nur einmal */
    fun texts(): List<String> = raw().map { normalize(Speech.clean(it)) }.filter { it.isNotEmpty() }.distinct()

    /** Jeder Leerraum wird zu einem Leerzeichen, vorne und hinten abgeschnitten – so rechnet auch der Stimmen-Server */
    fun normalize(text: String) = text.replace(Regex("\\s+"), " ").trim()

    // ---- Stimme „Nova“: vorab vertonte Dateien auf unserem Server
    /** So heißt die Stimme in der App – die Kennzeichnung „KI-generiert“ ist Pflicht */
    const val VOICE_LABEL = "Nova – KI-generierte Stimme"
    /** Was beim Vorlesen mit Daten passiert (steht in den Einstellungen von Android und WebApp) */
    const val PRIVACY = "Vorgelesen wird mit der Stimme Nova, einer mit KI (OpenAI) erzeugten Stimme. Feste Texte vertonen wir vorab. " +
        "Texte, die erst in der Aufgabe entstehen, schickt die App ohne Angaben zu dir an unseren Server stimmen.mathenachhilfe-online.com; " +
        "dieser lässt sie einmalig von OpenAI vertonen und speichert die Audiodatei. Es werden keine Namen, Konten oder Geräte-Kennungen übertragen."
    /** Unser Stimmen-Server. Dort liegen die MP3-Dateien; nur er spricht mit dem Dienst, der die Stimme erzeugt – die App nie. */
    const val SERVER = "https://stimmen.mathenachhilfe-online.com"
    const val VOICE = "nova"
    /** Längste Texte, die der Server zur Laufzeit noch vertont ([LIVE]) */
    const val LIVE_MAX_CHARS = 300
    /** Hier lässt die App einen Text vertonen, den es noch nicht als Datei gibt (entsteht erst in der Aufgabe): POST {"text","sprache"} → 200 {"url"} oder 204 */
    const val LIVE = "$SERVER/jetzt"

    /** Der Text, wie er gesprochen und für den Dateinamen gerechnet wird: vorlesefertig ([Speech.clean]) und ohne Leerraum-Reste. Leer = nichts zu sagen. */
    fun spoken(text: String) = normalize(Speech.clean(text))
    /**
     * Name der Datei zu einem vorlesefertigen Text: die ersten 32 Hex-Zeichen von SHA-256("nova|de|" + Text).
     * (Der Server normalisiert zusätzlich nach Unicode-NFC; unsere Texte sind es schon – das prüft ein Test für alle festen Texte.)
     */
    fun key(spoken: String, language: String = LANGUAGE) = Sha256.hex("$VOICE|$language|$spoken").take(32)
    /** Adresse der fertigen Datei zu einem vorlesefertigen Text */
    fun url(spoken: String) = key(spoken).let { "$SERVER/$VOICE/${it.take(2)}/$it.mp3" }
    /** Anfrage an [LIVE] für einen Text, der erst in der Aufgabe entstanden ist – enthält nur Text und Sprache, nichts über den Nutzer */
    fun liveBody(spoken: String): String = buildJsonObject { put("text", spoken); put("sprache", LANGUAGE) }.toString()
    /** Adresse aus der Antwort von [LIVE] ({"url": …}); null, wenn sie fehlt oder nicht auf unseren Server zeigt */
    fun liveUrl(answer: String): String? = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(answer) }.getOrNull()
        ?.let { (it as? kotlinx.serialization.json.JsonObject)?.get("url") as? JsonPrimitive }?.content
        ?.let { if (it.startsWith("/")) SERVER + it else it }?.takeIf { it.startsWith("$SERVER/") }

    /** Inhalt von `vorlesen/texte.json` */
    fun json(): String = buildJsonObject {
        put("format", FORMAT); put("app", APP)
        put("texte", JsonArray(texts().map { t -> buildJsonObject { put("text", JsonPrimitive(t)); put("sprache", LANGUAGE) } }))
    }.toString()
}
