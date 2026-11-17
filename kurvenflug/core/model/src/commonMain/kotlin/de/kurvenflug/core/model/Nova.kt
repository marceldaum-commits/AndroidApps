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
    /** Laufzeit-Texte vertonen lassen: POST {"text", "sprache"} → 200 {"url"} oder 204 (dann Gerätestimme) */
    const val LIVE = "https://$HOST/jetzt"
    const val MAX_LIVE = 300
    /** Stufen für „Sprechtempo“ in Prozent (Vorgabe Leitstand); 100 = das Server-Tempo, das Marcel gewählt hat */
    val SPEEDS = listOf(75, 90, 100, 115, 130, 150)
    fun nearestSpeed(percent: Int) = SPEEDS.minBy { kotlin.math.abs(it - percent) }
    /** „0,75“ … „1,5“ für die Anzeige */
    fun speedLabel(percent: Int): String { val p = nearestSpeed(percent); val s = "${p / 100},${(p % 100).toString().padStart(2, '0')}".removeSuffix("0"); return if (s.endsWith(",")) s + "0" else s }

    /**
     * Darf dieser (vorlesefertige) Text zum Vertonen an unseren Server? Nur kurz genug und ohne Spielernamen –
     * es werden nie Angaben über das Kind gesendet.
     */
    fun maySend(spoken: String, private: Collection<String>): Boolean =
        spoken.length <= MAX_LIVE && private.none { containsWord(spoken, it.trim()) }

    /** Kommt [word] als eigenes Wort (Groß/klein egal) in [text] vor? */
    private fun containsWord(text: String, word: String): Boolean {
        if (word.isEmpty()) return false
        var i = text.indexOf(word, ignoreCase = true)
        while (i >= 0) {
            val before = text.getOrNull(i - 1); val after = text.getOrNull(i + word.length)
            if ((before == null || !before.isLetter()) && (after == null || !after.isLetter())) return true
            i = text.indexOf(word, i + 1, ignoreCase = true)
        }
        return false
    }

    /** Anfrage an [LIVE] als JSON */
    fun liveBody(spoken: String, language: String = "de"): String =
        "{\"text\": \"" + spoken.replace("\\", "\\\\").replace("\"", "\\\"") + "\", \"sprache\": \"$language\"}"

    /** Die Adresse aus der Antwort von [LIVE] ({"url": "…"}); nur Adressen auf unserem Server */
    fun liveUrl(json: String): String? = Regex(""""url"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)?.takeIf { it.startsWith(BASE) }

    /** Größe aller Ansagen laut Server (Feld "bytes" in stand-<host>.json); Android nimmt den Live-Host */
    fun standUrl(host: String) = "${BASE}stand-$host.json"
    const val APP_HOST = "kurvenflug.mathenachhilfe-online.com"
    fun standBytes(json: String): Long? = Regex(""""bytes"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull()
    /** Größe in Bestqualität (128 kbit/s, Ordner original/) – Feld "bytes_original" */
    fun standBytesBest(json: String): Long? = Regex(""""bytes_original"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull()
    /** Adresse einer Ansage nach Dateinamen; [best] = Bestqualität (bei 404 die normale nehmen) */
    fun fileUrl(h: String, best: Boolean = false) = BASE + (if (best) "original/" else "") + "${h.take(2)}/$h.mp3"
    /** Die Texte aus vorlesen/texte.json (Format 1) */
    fun listTexts(json: String): List<String> = Regex(""""text"\s*:\s*"((?:[^"\\]|\\.)*)"""").findAll(json).map { m ->
        val s = m.groupValues[1]; val out = StringBuilder(); var i = 0
        while (i < s.length) { val c = s[i]
            if (c == '\\' && i + 1 < s.length) { when (val n = s[i + 1]) {
                'n' -> out.append('\n'); 't' -> out.append('\t'); 'u' -> { out.append(s.substring(i + 2, i + 6).toInt(16).toChar()); i += 4 }
                else -> out.append(n) }; i += 2 } else { out.append(c); i++ } }
        out.toString()
    }.toList()

    /** Jeder Leerraum zu einem Leerzeichen, vorne/hinten abgeschnitten. (Die Texte der App sind bereits NFC – der Generator prüft das.) */
    fun normalize(text: String) = text.trim().replace(Regex("""\s+"""), " ")

    fun hash(text: String, language: String = "de"): String = Sha256.hex("nova|$language|${normalize(text)}".encodeToByteArray()).take(32)

    /** Adresse der fertigen Ansage zu einem vorlesefertigen Text ([Speech.clean]) */
    fun url(spoken: String, language: String = "de"): String { val h = hash(spoken, language); return "$BASE${h.take(2)}/$h.mp3" }
}
