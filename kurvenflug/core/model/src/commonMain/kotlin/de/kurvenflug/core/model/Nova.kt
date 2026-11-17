package de.kurvenflug.core.model

/**
 * KI-Stimme „Nova“ (Vorgabe Marcel 17.11.): Unser Stimmen-Werk vertont die festen Texte der App vorab (vorlesen/texte.json)
 * und legt sie als MP3 auf stimmen.mathenachhilfe-online.com ab. Die App schickt nie etwas an OpenAI – sie lädt nur fertige Dateien.
 * Dateiname: SHA-256 von "nova|" + Sprache + "|" + Text (normalisiert), Hex, erste 32 Zeichen; Ordner = die ersten 2 Zeichen.
 */
object Nova {
    const val HOST = "stimmen.mathenachhilfe-online.com"
    const val BASE = "https://$HOST/nova/"
    const val LABEL = "Nova – KI-generierte Stimme"

    /** Jeder Leerraum zu einem Leerzeichen, vorne/hinten abgeschnitten. (Die Texte der App sind bereits NFC – der Generator prüft das.) */
    fun normalize(text: String) = text.trim().replace(Regex("""\s+"""), " ")

    fun hash(text: String, language: String = "de"): String = Sha256.hex("nova|$language|${normalize(text)}".encodeToByteArray()).take(32)

    /** Adresse der fertigen Ansage zu einem vorlesefertigen Text ([Speech.clean]) */
    fun url(spoken: String, language: String = "de"): String { val h = hash(spoken, language); return "$BASE${h.take(2)}/$h.mp3" }
}
