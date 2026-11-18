package de.experimentopia.core.model

import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Vorlesen mit der Stimme „Nova“ (KI-Stimme von OpenAI, vertont auf unserem Stimmen-Server – die App selbst spricht nie mit OpenAI).
 * Feste Texte stehen in /vorlesen/texte.json der WebApp; der Server vertont sie vorab und legt sie unter /nova/<h[0:2]>/<h>.mp3 ab.
 * Texte, die erst zur Laufzeit entstehen, vertont der Server auf Anfrage (/jetzt) – nur der Text, ohne Angaben zum Kind.
 */
object Nova {
    /**
     * Vorlesen an/aus für die ganze App (Vorgabe 18.11.2026: vorerst aus – Musik und Klänge reichen).
     * Aus heißt: keine Vorlese-Knöpfe, kein automatisches Vorlesen, keine Abrufe beim Stimmen-Server, keine Gerätestimme,
     * leere /vorlesen/texte.json. Der Code bleibt vollständig erhalten – zum Wiedereinschalten nur hier auf true setzen.
     */
    const val ENABLED = false

    const val SERVER = "https://stimmen.mathenachhilfe-online.com"
    const val NOW = "$SERVER/jetzt"
    /** Herkunft, unter der die Android-App beim Server anfragt (der Server nimmt nur bekannte Herkünfte an) */
    const val ORIGIN = "https://experimentopia.mathenachhilfe-online.com"
    const val LANG = "de"
    /** Länge eines Sprech-Stücks: so passt jedes Stück auch zu /jetzt (höchstens 300 Zeichen) */
    const val MAX = 300

    /** Jeder Leerraum zu einem Leerzeichen, vorne und hinten abgeschnitten (Unicode-NFC prüft der Export-Test) */
    fun normalize(t: String) = t.replace(Regex("""\s+"""), " ").trim()

    /** Zählstand des Servers für eine Adresse (Feld "bytes" = Größe aller fertigen Sätze) */
    fun standUrl(host: String) = "$SERVER/nova/stand-$host.json"
    const val APP_HOST = "experimentopia.mathenachhilfe-online.com"
    fun bytesOf(standJson: String): Long? = Regex(""""bytes"\s*:\s*(\d+)""").find(standJson)?.groupValues?.get(1)?.toLongOrNull()
    fun bytesOriginalOf(standJson: String): Long? = Regex(""""bytes_original"\s*:\s*(\d+)""").find(standJson)?.groupValues?.get(1)?.toLongOrNull()
    /** „ca. 73 MB“ – auf ganze MB gerundet, mindestens 1 */
    fun mb(bytes: Long) = maxOf(1L, (bytes + 524_288) / 1_048_576)
    /** Gleichzeitige Downloads beim Offline-Laden */
    const val PARALLEL = 4

    /** Stufen des Reglers „Sprechtempo“ (1,0 = Server-Tempo); abgespielt wird schneller/langsamer bei gleicher Tonhöhe */
    val SPEEDS = listOf(0.75f, 0.9f, 1.0f, 1.15f, 1.3f, 1.5f)
    /** Gespeicherte Wahl auf die nächste Stufe (alte Werte aus Versionen vor 2.2 passen so auch) */
    fun speed(saved: Float): Float = SPEEDS.minBy { kotlin.math.abs(it - saved) }

    /** Hinweis für den Datenschutz-Text (Wortlaut abgestimmt mit allen Lern-Apps) */
    const val PRIVACY = "Vorgelesen wird mit der Stimme Nova, einer mit KI (OpenAI) erzeugten Stimme. Feste Texte vertonen wir vorab. Texte, die erst im Versuch " +
        "entstehen (z. B. Messwerte), schickt die App ohne Angaben zu dir an unseren Server stimmen.mathenachhilfe-online.com; dieser lässt sie einmalig von OpenAI " +
        "vertonen und speichert die Audiodatei. Es werden keine Namen, Konten oder Geräte-Kennungen übertragen, und unser Server speichert keine Zugriffsprotokolle."

    /** Dateiname: die ersten 32 Hex-Zeichen von SHA-256("nova|de|" + Text) */
    fun hash(text: String, lang: String = LANG): String =
        Sha256.digest("nova|$lang|${normalize(text)}".encodeToByteArray()).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }.take(32)

    fun url(text: String) = hash(text).let { "$SERVER/nova/${it.take(2)}/$it.mp3" }
    /** Bestqualität (128 kbit/s) nur fürs Offline-Laden; fehlt sie (404), nimmt die App die normale Datei */
    fun originalUrl(text: String) = hash(text).let { "$SERVER/nova/original/${it.take(2)}/$it.mp3" }

    /** Ein App-Text → sprechbare Stücke (Satzgrenzen, je höchstens [MAX] Zeichen) */
    fun parts(text: String): List<String> = chunks(Speech.clean(text))

    fun chunks(clean: String, max: Int = MAX): List<String> {
        val sentences = Regex("""(?<=[.!?:;])\s+""").split(normalize(clean)).filter { it.isNotBlank() }.flatMap { hardSplit(it, max) }
        val out = mutableListOf<String>(); var cur = ""
        for (s in sentences) {
            cur = if (cur.isEmpty()) s else if (cur.length + 1 + s.length <= max) "$cur $s" else { out += cur; s }
        }
        if (cur.isNotEmpty()) out += cur
        return out
    }

    /** Sehr lange Sätze an Kommas, notfalls an Leerzeichen teilen */
    private fun hardSplit(s: String, max: Int): List<String> {
        if (s.length <= max) return listOf(s)
        val out = mutableListOf<String>(); var rest = s
        while (rest.length > max) {
            val cut = rest.lastIndexOf(", ", max).takeIf { it > max / 3 }?.plus(1) ?: rest.lastIndexOf(' ', max).takeIf { it > 0 } ?: max
            out += rest.substring(0, cut).trim(); rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) out += rest
        return out
    }

    /** Alle festen Stücke der App, ohne Wiederholungen */
    fun catalog(): List<String> = ReadTexts.all().flatMap { parts(it) }.distinct()

    /** Inhalt von /vorlesen/texte.json */
    fun json(): String = buildJsonObject {
        put("format", 1); put("app", "experimentopia")
        putJsonArray("texte") { if (ENABLED) catalog().forEach { t -> addJsonObject { put("text", t); put("sprache", LANG) } } }
    }.toString()
}
