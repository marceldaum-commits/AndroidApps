package de.experimentopia.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * „Eltern-Handy koppeln“: Eltern sehen auf dem eigenen Handy Fortschritt und Problemstellen – ohne Konto, Ende-zu-Ende verschlüsselt.
 * Der Server sieht nur [id] und einen verschlüsselten Text; [key] steht nur im Link hinter „#“ (wird nie an einen Server geschickt).
 * Spezifikation vom Leitstand (18.11.2026): Format 1, PUT/DELETE mit X-Schreib, AES-GCM-256 mit 12-Byte-IV, höchstens 24 KB.
 */
@Serializable
data class ParentLink(
    /** 16 Zufallsbytes, base64url (22 Zeichen) – Adresse des Berichts */
    val id: String,
    /** 16 Zufallsbytes, base64url (22 Zeichen) – Schreibrecht, bleibt auf dem Gerät */
    val write: String,
    /** 32 Zufallsbytes, base64url (43 Zeichen) – Schlüssel, nur im Eltern-Link */
    val key: String,
    /** Test- oder Live-Stufe (beim Koppeln festgelegt) */
    val test: Boolean,
    val nickname: String = "",
    /** Zeitpunkt des letzten erfolgreichen Hochladens (ms), 0 = noch nie */
    val sentAt: Long = 0,
    /** Prüfsumme der zuletzt hochgeladenen Sicherung – nur bei Änderung neu senden */
    val backupHash: String = "",
    val backupAt: Long = 0,
) {
    val backupUrl get() = Backup.url(test, id)
    val server get() = if (test) "https://trainer-test.mathenachhilfe-online.com" else "https://trainer.mathenachhilfe-online.com"
    val uploadUrl get() = "$server/eltern/v1/b/$id"
    fun encode() = Json.encodeToString(serializer(), this)
    val parentUrl get() = (if (test) "https://staging.mathenachhilfe-online.com" else "https://www.mathenachhilfe-online.com") + "/eltern-bericht/#$id.$key"

    companion object {
        /** Neue Kopplung aus Zufallsbytes ([random] liefert kryptografisch sichere Bytes der Plattform) */
        fun create(random: (Int) -> ByteArray, test: Boolean, nickname: String = "") =
            ParentLink(B64u.encode(random(16)), B64u.encode(random(16)), B64u.encode(random(32)), test, nickname.trim().take(20))
        /** Test-Stufe, wenn die App selbst auf der Test-Seite (ab 2.13.0 lernen-test.…/experimentopia/) oder lokal läuft */
        fun isTestHost(host: String) = host.startsWith("lernen-test.") || host.contains("-test") || host.startsWith("localhost") || host.startsWith("127.") || host.contains("staging")
        fun parse(s: String?): ParentLink? = s?.let { runCatching { Json.decodeFromString(serializer(), it) }.getOrNull() }

    }
}

object ParentReport {
    const val MAX_BYTES = 24 * 1024
    /** höchstens alle 10 Minuten hochladen */
    const val MIN_GAP_MS = 10 * 60 * 1000L
    private const val DAY = 86_400_000L
    private const val WINDOW_DAYS = 30

    /** Thema für die Eltern-Seite (Fach-Bereich) */
    fun slug(t: Topic) = when (t) {
        Topic.AUFTRIEB, Topic.HEBEL, Topic.MASCHINEN, Topic.NEWTON, Topic.FALL, Topic.SCHWINGUNG, Topic.ENERGIE -> "physik-mechanik"
        Topic.STROM, Topic.MAGNET, Topic.ELEKTROMAGNETISMUS -> "physik-elektrizitaet"
        Topic.WAERME -> "physik-waerme"
        Topic.LICHT, Topic.OPTIK -> "physik-optik"
        Topic.SCHALL -> "physik-akustik"
        Topic.TRENNEN -> "chemie-stoffe"
        Topic.SAEURE -> "chemie-saeuren-laugen"
        Topic.REAKTION -> "chemie-reaktionen"
        Topic.VERERBUNG -> "biologie-genetik"
        Topic.ZELLEN -> "biologie-zellen"
        Topic.ERNAEHRUNG -> "biologie-ernaehrung"
        Topic.OEKOLOGIE -> "biologie-oekologie"
        Topic.KOERPER -> "biologie-koerper"
    }

    /** ISO-8601 in UTC, z. B. 2026-11-18T15:44:47Z */
    fun iso(ms: Long): String {
        val d = dayText(ms / DAY)
        val s = ((ms % DAY) + DAY) % DAY / 1000
        return "${d}T${(s / 3600).toString().padStart(2, '0')}:${(s / 60 % 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}Z"
    }
    /** Tag (seit 1.1.1970) als JJJJ-MM-TT */
    fun dayText(day: Long): String {
        val z = day + 719_468
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val dd = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val y = yoe + era * 400 + if (m <= 2) 1 else 0
        return "$y-${m.toString().padStart(2, '0')}-${dd.toString().padStart(2, '0')}"
    }

    /**
     * Lernzeit wird geschätzt (die App misst keine Bildschirmzeit): Versuche mit weniger als 20 Minuten Abstand gehören zu einer Sitzung;
     * eine Sitzung dauert vom ersten bis zum letzten Versuch plus 3 Minuten für den ersten.
     */
    fun minutes(times: List<Long>): Int {
        if (times.isEmpty()) return 0
        val t = times.sorted(); var total = 0L; var start = t[0]; var last = t[0]
        for (x in t.drop(1)) { if (x - last > 20 * 60_000L) { total += last - start + 3 * 60_000L; start = x }; last = x }
        total += last - start + 3 * 60_000L
        return ((total + 30_000) / 60_000).toInt()
    }

    private fun pct(right: Int, all: Int) = if (all == 0) 0 else (right * 100 + all / 2) / all

    /** Der Bericht als JSON (Format 1). [offsetMs] = Zeitzone des Geräts (für die Tage). */
    fun build(results: List<ResultRow>, journal: List<NoteRow>, grade: Int, nickname: String, now: Long, offsetMs: Long = 0, months: Map<String, MonthSum> = emptyMap()): String {
        val local = { ms: Long -> (ms + offsetMs).floorDiv(DAY) }
        val today = local(now)
        val recent = journal.filter { Catalog.byId(it.experimentId) != null && today - local(it.at) < WINDOW_DAYS && it.at <= now + 60_000 }
        val days = recent.groupBy { local(it.at) }
        // Stärken und Problemstellen je Thema über alle Vermutungen (mindestens 3 Versuche im Thema)
        val byTopic = journal.filter { Catalog.byId(it.experimentId) != null }.groupBy { Catalog.topicOf(it.experimentId) }
            .mapValues { (_, l) -> pct(l.count { it.right }, l.size) to l.size }.filter { it.value.second >= 3 }
        val strong = byTopic.filter { it.value.first >= 70 }.entries.sortedByDescending { it.value.first }.take(3)
        val weak = byTopic.filter { it.value.first < 50 }.entries.sortedBy { it.value.first }.take(3)
        val done = results.filter { Catalog.byId(it.experimentId) != null }.map { it.experimentId }.toSet()
        val o = buildJsonObject {
            put("format", 1); put("app", "experimentopia"); put("appName", "Experimentopia")
            nickname.trim().take(20).takeIf { it.isNotEmpty() }?.let { put("name", it) }
            if (grade in 1..13) put("klasse", grade)
            put("stand", iso(now))
            put("zeitraum", "letzte $WINDOW_DAYS Tage")
            put("lernzeitMin", days.values.sumOf { l -> minutes(l.map { it.at }) })
            put("tageAktiv", days.size)
            put("aufgaben", recent.size)
            put("richtigProzent", pct(recent.count { it.right }, recent.size))
            putJsonArray("staerken") { strong.forEach { (t, v) -> addJsonObject { put("thema", slug(t)); put("titel", t.title); put("prozent", v.first) } } }
            putJsonArray("schwach") { weak.forEach { (t, v) -> addJsonObject {
                put("thema", slug(t)); put("titel", t.title); put("prozent", v.first)
                put("tipp", "Gemeinsam die Theorie-Karte „${t.title}“ (📖) anschauen und einen Versuch im Freien Labor wiederholen – falsche Vermutungen sind beim Forschen normal.")
            } } }
            putJsonArray("fortschritt") { Subject.entries.forEach { s ->
                val all = Catalog.all.filter { it.topic.subject == s }
                addJsonObject { put("titel", s.title); put("geschafft", all.count { it.id in done }); put("von", all.size) }
            } }
            putJsonArray("letzte") { days.entries.sortedByDescending { it.key }.take(7).forEach { (d, l) -> addJsonObject {
                put("tag", dayText(d)); put("min", minutes(l.map { it.at })); put("aufgaben", l.size); put("richtig", l.count { it.right })
            } } }
            putJsonArray("verlauf") { Monthly.verlauf(months).forEach { (m, v) -> addJsonObject {
                put("monat", m); put("min", v.min); put("aufgaben", v.aufgaben); put("richtigProzent", pct(v.richtig, v.aufgaben))
            } } }
            put("hinweis", "„Richtig“ heißt: Die Vermutung vor dem Versuch stimmte. Falsche Vermutungen werden in Experimentopia als Aha-Moment gefeiert. Die Lernzeit ist geschätzt.")
        }
        return o.toString()
    }

    /** Verschlüsselt für den Server: „<iv b64url>.<geheimtext+prüfwert b64url>“ */
    fun seal(json: String, keyB64: String, iv: ByteArray): String {
        val key = B64u.decode(keyB64) ?: error("Schlüssel ungültig")
        return B64u.encode(iv) + "." + B64u.encode(AesGcm.encrypt(key, iv, json.encodeToByteArray()))
    }
}
