package de.experimentopia.core.model

import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Vorlesen mit den eigenen Stimmen des Tonstudios auf unserem Server (Marcel 20.11.2026: „Die OpenAI-Verbindung kommt überall raus, wir haben unsere eigenen Stimmen“).
 * Deutsch: de-1 (Chatterbox). Die App selbst ruft keinen Sprachdienst auf: Feste Texte werden vorab vertont und als MP3 ausgeliefert.
 * Gekennzeichnet wird sie als „KI-generierte Stimme“ (synthetische Stimme).
 * Zurzeit ist das Vorlesen AUS ([ENABLED] = false), bis die vertonten Dateien da sind (Textliste ab 25.11.2026).
 */
object Tonstudio {
    /** Vorlesen an/aus für die ganze App: aus = keine Vorlese-Knöpfe, kein automatisches Vorlesen, keine Abrufe, keine Gerätestimme. */
    const val ENABLED = false

    const val LANG = "de"
    /** Länge eines Sprech-Stücks (höchstens 300 Zeichen) */
    const val MAX = 300
    /** Tempo-Stufen des Tonstudios: normal 1,0 und langsam 0,8 (langsam nur für Blubb- und Merksätze) */
    val SPEEDS = listOf(0.8f, 1.0f)
    /** Gespeicherte Wahl auf die nächste Stufe (alte Werte passen so auch) */
    fun speed(saved: Float): Float = SPEEDS.minBy { kotlin.math.abs(it - saved) }

    /** Jeder Leerraum zu einem Leerzeichen, vorne und hinten abgeschnitten (Unicode-NFC prüft der Export-Test) */
    fun normalize(t: String) = t.replace(Regex("""\s+"""), " ").trim()
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

    /** Textliste (Format 1) – leer, solange das Vorlesen aus ist */
    fun json(): String = buildJsonObject {
        put("format", 1); put("app", "experimentopia")
        putJsonArray("texte") { if (ENABLED) catalog().forEach { t -> addJsonObject { put("text", t); put("sprache", LANG) } } }
    }.toString()
}
