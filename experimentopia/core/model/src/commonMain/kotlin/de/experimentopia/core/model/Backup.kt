package de.experimentopia.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/** Monatssummen – bleiben dauerhaft, auch wenn das Forscherbuch alte Einträge abgibt (höchstens 500). */
@Serializable
data class MonthSum(val min: Int = 0, val aufgaben: Int = 0, val richtig: Int = 0)

object Monthly {
    private val json = Json { ignoreUnknownKeys = true }
    fun decode(s: String?): Map<String, MonthSum> = s?.let { runCatching { json.decodeFromString<Map<String, MonthSum>>(it) }.getOrNull() } ?: emptyMap()
    fun encode(m: Map<String, MonthSum>) = json.encodeToString(m)

    /** Monat (JJJJ-MM) eines Zeitpunkts in Ortszeit */
    fun monthOf(ms: Long, offsetMs: Long) = ParentReport.dayText((ms + offsetMs).floorDiv(86_400_000L)).take(7)

    /**
     * Neue Summen aus dem Forscherbuch einrechnen. Je Monat zählt das Maximum aus gespeicherter und neu berechneter Summe –
     * so bleiben Monate vollständig, deren älteste Einträge das Forscherbuch schon abgegeben hat.
     */
    fun merge(stored: Map<String, MonthSum>, journal: List<NoteRow>, offsetMs: Long): Map<String, MonthSum> {
        val fresh = journal.groupBy { monthOf(it.at, offsetMs) }.mapValues { (_, l) ->
            val days = l.groupBy { (it.at + offsetMs).floorDiv(86_400_000L) }
            MonthSum(days.values.sumOf { d -> ParentReport.minutes(d.map { it.at }) }, l.size, l.count { it.right })
        }
        return (stored.keys + fresh.keys).associateWith { k ->
            val a = stored[k] ?: MonthSum(); val b = fresh[k] ?: MonthSum()
            if (b.aufgaben > a.aufgaben) b else if (a.aufgaben > b.aufgaben) a else MonthSum(maxOf(a.min, b.min), a.aufgaben, maxOf(a.richtig, b.richtig))
        }.entries.sortedBy { it.key }.associate { it.key to it.value }
    }

    /** Für den Bericht: höchstens 12 Monate, älteste zuerst */
    fun verlauf(m: Map<String, MonthSum>) = m.entries.sortedBy { it.key }.takeLast(12)
}

/** Ergebnis beim Einlesen eines Kopplungs-Links */
data class PastedLink(val id: String, val key: String, val test: Boolean?)

/**
 * Ende-zu-Ende-verschlüsselte Sicherung des Lernstands (Leitstand-Spezifikation „Sicherung des Lernstands“, 18.11.2026).
 * Klartext: {"format":1,"app":"experimentopia","v","stand","schreib","daten":{…}}; [daten] = der Browser-Speicher der App (ohne Geräte-IDs).
 */
object Backup {
    const val MAX_BYTES = 512 * 1024
    const val APP = "experimentopia"
    private const val DAY = 86_400_000L

    /** ISO-8601 mit Zeitzone, z. B. 2026-11-18T16:30:00+01:00 */
    fun isoLocal(ms: Long, offsetMs: Long): String {
        val base = ParentReport.iso(ms + offsetMs).dropLast(1)
        val m = offsetMs / 60_000; val sign = if (m < 0) "-" else "+"; val a = kotlin.math.abs(m)
        return "$base$sign${(a / 60).toString().padStart(2, '0')}:${(a % 60).toString().padStart(2, '0')}"
    }

    fun build(version: String, now: Long, offsetMs: Long, write: String, nickname: String, data: Map<String, String>): String = buildJsonObject {
        put("format", 1); put("app", APP); put("v", version); put("stand", isoLocal(now, offsetMs)); put("schreib", write)
        putJsonObject("daten") {
            if (nickname.isNotBlank()) put("spitzname", nickname)
            putJsonObject("speicher") { data.entries.sortedBy { it.key }.forEach { (k, v) -> put(k, v) } }
        }
    }.toString()

    /** Zu groß? Erst das Forscherbuch auf die letzten 90 Tage kürzen (Monatssummen bleiben vollständig). */
    fun shrink(data: Map<String, String>, journalKey: String, now: Long): Map<String, String> {
        val raw = data[journalKey] ?: return data
        val arr = runCatching { Json.parseToJsonElement(raw).jsonArray }.getOrNull() ?: return data
        val kept = JsonArray(arr.filter { (it.jsonObject["at"]?.jsonPrimitive?.longOrNull ?: 0L) >= now - 90 * DAY })
        return data + (journalKey to kept.toString())
    }

    sealed interface Read {
        data class Ok(val write: String, val nickname: String, val data: Map<String, String>, val stand: String) : Read
        data object OtherApp : Read
        data object Broken : Read
    }
    fun read(plain: String): Read {
        val o = runCatching { Json.parseToJsonElement(plain).jsonObject }.getOrNull() ?: return Read.Broken
        if (o["app"]?.jsonPrimitive?.contentOrNull != APP) return Read.OtherApp
        val d = o["daten"]?.jsonObject ?: return Read.Broken
        val w = o["schreib"]?.jsonPrimitive?.contentOrNull ?: return Read.Broken
        val mem = d["speicher"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: return Read.Broken
        return Read.Ok(w, d["spitzname"]?.jsonPrimitive?.contentOrNull.orEmpty(), mem, o["stand"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }

    /** „…/eltern-bericht/#<id>.<key>“ oder nur „<id>.<key>“ – Leerzeichen und Zeilenumbrüche aus dem Einfügen werden ignoriert */
    fun parseLink(text: String): PastedLink? {
        val t = text.trim().filterNot { it.isWhitespace() }
        val frag = t.substringAfter('#', t)
        val parts = frag.split('.')
        if (parts.size != 2) return null
        val (id, key) = parts
        if (id.length != 22 || key.length != 43 || B64u.decode(id)?.size != 16 || B64u.decode(key)?.size != 32) return null
        val test = when { t.contains("staging.") -> true; t.contains("www.mathenachhilfe-online.com") -> false; else -> null }
        return PastedLink(id, key, test)
    }

    /** Prüfsumme des Speicherstands (ohne Zeitstempel) – zeigt, ob sich seit der letzten Sicherung etwas geändert hat */
    fun hash(nickname: String, data: Map<String, String>) =
        B64u.encode(Sha256.digest((nickname + "\u0000" + data.entries.sortedBy { it.key }.joinToString("\u0000") { it.key + "=" + it.value }).encodeToByteArray())).take(22)

    fun server(test: Boolean) = if (test) "https://trainer-test.mathenachhilfe-online.com" else "https://trainer.mathenachhilfe-online.com"
    fun url(test: Boolean, id: String) = "${server(test)}/eltern/v1/s/$id"

    /** Antwort des Servers { "d": "<iv>.<ct>", "t": … } entschlüsseln; null = Schlüssel falsch oder Daten kaputt */
    fun open(response: String, keyB64: String): String? {
        val d = runCatching { Json.parseToJsonElement(response).jsonObject["d"]?.jsonPrimitive?.content }.getOrNull() ?: response.trim()
        val (iv, ct) = d.split('.').takeIf { it.size == 2 } ?: return null
        val key = B64u.decode(keyB64) ?: return null
        return AesGcm.decrypt(key, B64u.decode(iv) ?: return null, B64u.decode(ct) ?: return null)?.decodeToString()
    }
}
