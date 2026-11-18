package de.raumlicht.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * „Eltern-Handy koppeln“: Wer möchte, teilt seinen Lernstand mit den Eltern – ohne Konto, Ende-zu-Ende verschlüsselt.
 * Die App erzeugt drei Zufallswerte: eine Kennung des Berichts ([Pairing.id]), ein Schreib-Geheimnis ([Pairing.write]) und den Schlüssel ([Pairing.key]).
 * Der Bericht wird auf dem Gerät mit dem Schlüssel verschlüsselt und so hochgeladen; der Server kann ihn nicht lesen. Der Schlüssel steckt nur im Link
 * hinter dem „#“ – dieser Teil geht nie an einen Server. Wer den Link hat, kann den Bericht lesen. „Kopplung aufheben“ löscht Bericht und Werte.
 * Raumlicht ist für die Oberstufe: Hier koppelt die Schülerin oder der Schüler selbst.
 */
object Eltern {
    /** Für alle sichtbar – seit 2.18.28: Die Eltern-Seite (/eltern-bericht/) ist seit dem 18.11.2026 auf der Website erreichbar. Auf false stellen blendet „Eltern-Handy koppeln“ außerhalb der Testseite aus. */
    const val ENABLED = true
    const val SERVER = "https://trainer.mathenachhilfe-online.com/eltern/v1/b/"
    const val TEST_SERVER = "https://trainer-test.mathenachhilfe-online.com/eltern/v1/b/"
    const val PAGE = "https://www.mathenachhilfe-online.com/eltern-bericht/"
    const val TEST_PAGE = "https://staging.mathenachhilfe-online.com/eltern-bericht/"
    /** frühestens alle 10 Minuten ein neuer Bericht */
    const val MIN_GAP_MS = 10 * 60 * 1000L
    /** Zeitraum des Berichts in Tagen */
    const val DAYS = 14
    const val MAX_NAME = 20
    const val NOTE = "Du entscheidest, ob deine Eltern mitlesen. Der Bericht wird auf deinem Gerät verschlüsselt; unser Server kann ihn nicht lesen. " +
        "Lesen kann ihn, wer den Link oder QR-Code hat – gib ihn nur an deine Eltern. „Kopplung aufheben“ löscht alles wieder. Ohne neuen Bericht löscht der Server ihn nach 90 Tagen."
    const val CONTENT = "Im Bericht stehen: Lernzeit und aktive Tage der letzten zwei Wochen, wie viele Aufgaben du gelöst hast und wie viele davon beim ersten Versuch, " +
        "dein Fortschritt je Gebiet, was gut sitzt und was noch hakt, die Summen der letzten Monate – und dein Klausurtermin, falls du einen eingetragen hast. Sonst nichts."

    private val B64 = Regex("[A-Za-z0-9_-]+")
    class Pairing(val id: String, val write: String, val key: String, val name: String = "") {
        fun encode() = "$id.$write.$key.$name"
        companion object {
            /** Aus dem gespeicherten Text – null, wenn nichts (Gültiges) gespeichert ist */
            fun decode(s: String): Pairing? { val p = s.split('.', limit = 4); if (p.size < 3) return null
                return Pairing(p[0], p[1], p[2], p.getOrElse(3) { "" }).takeIf { p[0].length == 22 && p[1].length == 22 && p[2].length == 43 && p.take(3).all(B64::matches) } }
            /** Aus frisch erzeugten Zufallswerten „id.schreib.key“ und dem Spitznamen */
            fun fresh(values: String, name: String) = decode(values + "." + name.trim().replace('.', ' ').take(MAX_NAME))
        }
    }
    /** Der Link für die Eltern: Kennung und Schlüssel stehen hinter dem „#“ */
    fun link(p: Pairing, page: String = PAGE) = "$page#${p.id}.${p.key}"
    fun uploadUrl(p: Pairing, server: String = SERVER) = server + p.id

    // ── Sicherung des Lernstands: derselbe Schlüssel, dieselbe Kennung – der Server kann auch sie nicht lesen ──
    /** höchstens so viele Zeichen darf die verschlüsselte Sicherung haben (512 KB) */
    const val MAX_BACKUP = 512 * 1024
    const val BACKUP_NOTE = "Solange gekoppelt ist, sichert Raumlicht deinen Lernstand zusätzlich verschlüsselt. Geht das Handy verloren oder werden die Browser-Daten gelöscht, " +
        "holst du ihn auf dem neuen Gerät mit dem Kopplungs-Link zurück. In der Sicherung stecken Sterne, Rekorde, Lernverlauf, Einstellungen und deine Freischalt-Codes."
    /** Was nicht in die Sicherung gehört: Geräte-Kennungen, die Kopplung selbst (kommt aus dem Link) und Merker dieses einen Browsers */
    private val NOT_SAVED = setOf("kiDevice", "elternKopplung", "elternGesendet", "elternSicherung", "sparsam", "modus", "mitschrift", "startfehler", "test")
    fun saved(key: String) = key !in NOT_SAVED
    /** Zuerst weglassen, wenn die Sicherung zu groß würde – das braucht niemand zum Weiterlernen */
    private val DROP_FIRST = listOf("diary", "show", "ride", "flight")
    private const val NAME_KEY = "elternName"
    /** Adresse der Sicherung: wie beim Bericht, nur „s“ statt „b“ */
    fun backupUrl(id: String, server: String = SERVER) = server.removeSuffix("b/") + "s/" + id
    /** So lang wird „<iv>.<verschlüsselt>“ für diesen Klartext (AES-GCM hängt 16 Byte an, base64url ohne Auffüllzeichen) */
    fun sealedLength(plain: String) = 16 + 1 + ((plain.encodeToByteArray().size + 16) * 4 + 2) / 3
    /** Prüfwert des Speicherstands – nur wenn er sich ändert, geht eine neue Sicherung raus */
    fun mark(data: Map<String, String>): String { var h = 1125899906842597L; data.entries.sortedBy { it.key }.forEach { (k, v) -> for (c in "$k=$v;") h = 31 * h + c.code }; return h.toString() }
    /**
     * Die Sicherung als JSON (Klartext vor dem Verschlüsseln). [data] ist der Speicherstand der App ohne Geräte-Kennungen. Das Schreib-Geheimnis steht mit drin,
     * damit das neue Gerät danach weiter schreiben kann. Zu groß? Erst Unwichtiges weglassen; passt es dann immer noch nicht: null.
     */
    fun backup(p: Pairing, version: String, nowMs: Long, data: Map<String, String>): String? {
        val keep = data.filterKeys(::saved).toMutableMap(); if (p.name.isNotBlank()) keep[NAME_KEY] = p.name
        val drop = DROP_FIRST.toMutableList()
        while (true) {
            val json = buildJsonObject { put("format", 1); put("app", Zaehler.APP); put("v", version); put("stand", iso(nowMs)); put("schreib", p.write)
                put("daten", JsonObject(keep.entries.sortedBy { it.key }.associate { it.key to JsonPrimitive(it.value) })) }.toString()
            if (sealedLength(json) <= MAX_BACKUP) return json
            keep.remove(drop.removeFirstOrNull() ?: return null)
        }
    }
    /** Kennung und Schlüssel aus dem eingefügten Kopplungs-Link (oder nur dem Teil hinter „#“) – null, wenn es keiner ist */
    fun fromLink(text: String): Pair<String, String>? { val p = text.trim().substringAfterLast('#').split('.')
        return if (p.size == 2 && p[0].length == 22 && p[1].length == 43 && p.all(B64::matches)) p[0] to p[1] else null }
    sealed class Restore {
        /** @param pairing die wiederhergestellte Kopplung – das Gerät schreibt damit weiter */
        class Ok(val pairing: Pairing, val data: Map<String, String>, val stand: String) : Restore()
        object OtherApp : Restore()
        object Broken : Restore()
    }
    /** Liest eine entschlüsselte Sicherung. [id] und [key] stammen aus dem Link. */
    fun restore(plain: String, id: String, key: String): Restore = try {
        val o = Json.parseToJsonElement(plain) as JsonObject; fun text(k: String) = (o[k] as? JsonPrimitive)?.content
        if (text("app") != Zaehler.APP) Restore.OtherApp else {
            val all = (o["daten"] as JsonObject).mapValues { (it.value as JsonPrimitive).content }
            val pairing = Pairing.fresh("$id.${text("schreib")}.$key", all[NAME_KEY].orEmpty())
            if (text("format") != "1" || pairing == null) Restore.Broken else Restore.Ok(pairing, all.filterKeys { it != NAME_KEY && saved(it) }, text("stand").orEmpty())
        }
    } catch (e: Exception) { Restore.Broken }
    /** „18.11.2026“ aus dem Stand der Sicherung („2026-11-18T…“), sonst leer */
    fun germanDay(stand: String) = Regex("^(\\d{4})-(\\d\\d)-(\\d\\d)").find(stand)?.destructured?.let { (y, m, d) -> "$d.$m.$y" }.orEmpty()

    // ── Monatssummen: bleiben dauerhaft auf dem Gerät (das Tages-Protokoll reicht nur 60 Tage zurück) ──
    data class Month(val month: String, val min: Int, val tasks: Int, val right: Int)
    fun months(s: String): List<Month> = s.split(';').mapNotNull { e -> val p = e.split(':'); val n = p.drop(1).mapNotNull { it.toIntOrNull() }
        if (p.size == 4 && n.size == 3 && Regex("\\d{4}-\\d\\d").matches(p[0])) Month(p[0], n[0], n[1], n[2]) else null }.sortedBy { it.month }
    private fun monthsText(l: Collection<Month>) = l.sortedBy { it.month }.joinToString(";") { "${it.month}:${it.min}:${it.tasks}:${it.right}" }
    fun addMonth(s: String, day: Long, min: Int = 0, tasks: Int = 0, right: Int = 0): String {
        val all = months(s).associateBy { it.month }.toMutableMap(); val m = isoDay(day).take(7); val old = all[m] ?: Month(m, 0, 0, 0)
        all[m] = Month(m, old.min + min, old.tasks + tasks, old.right + right); return monthsText(all.values)
    }
    /** Anfangsbestand aus dem Tages-Protokoll – für alle, die schon vor den Monatssummen gelernt haben */
    fun monthsFromLog(log: String) = monthsText(log(log).groupBy { isoDay(it.day).take(7) }.map { (m, d) -> Month(m, d.sumOf { it.min }, d.sumOf { it.tasks }, d.sumOf { it.right }) })

    // ── Lern-Protokoll: je Tag Minuten, gelöste Aufgaben, davon beim ersten Versuch – nur auf dem Gerät, höchstens 60 Tage ──
    data class Day(val day: Long, val min: Int, val tasks: Int, val right: Int)
    fun log(s: String): List<Day> = s.split(';').mapNotNull { e -> e.split(':').mapNotNull { it.toLongOrNull() }.takeIf { it.size == 4 }?.let { Day(it[0], it[1].toInt(), it[2].toInt(), it[3].toInt()) } }.sortedBy { it.day }
    fun add(s: String, day: Long, min: Int = 0, tasks: Int = 0, right: Int = 0): String {
        val all = log(s).associateBy { it.day }.toMutableMap(); val d = all[day] ?: Day(day, 0, 0, 0)
        all[day] = Day(day, (d.min + min).coerceAtMost(24 * 60), d.tasks + tasks, d.right + right)
        return all.values.filter { it.day > day - 60 }.sortedBy { it.day }.joinToString(";") { "${it.day}:${it.min}:${it.tasks}:${it.right}" }
    }

    private fun civil(epochDay: Long): Triple<Long, Long, Long> {
        val z = epochDay + 719468; val era = (if (z >= 0) z else z - 146096) / 146097; val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365; val doy = doe - (365 * yoe + yoe / 4 - yoe / 100); val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1; val m = if (mp < 10) mp + 3 else mp - 9; return Triple(yoe + era * 400 + (if (m <= 2) 1 else 0), m, d)
    }
    private fun two(n: Long) = n.toString().padStart(2, '0')
    /** „2026-11-18“ */
    fun isoDay(epochDay: Long) = civil(epochDay).let { (y, m, d) -> "$y-${two(m)}-${two(d)}" }
    /** „2026-11-18T15:40:00Z“ aus Millisekunden seit 1970 */
    fun iso(ms: Long): String { val s = ms / 1000; val day = if (s >= 0) s / 86400 else (s - 86399) / 86400; val r = s - day * 86400
        return isoDay(day) + "T${two(r / 3600)}:${two(r % 3600 / 60)}:${two(r % 60)}Z" }

    private fun areaTitle(a: Area) = if (a == Area.GEOMETRIE) "Vektoren und Geometrie" else a.title

    /** Der Bericht als JSON (gemeinsames Format aller Apps). Enthält nur, was in [CONTENT] steht. */
    fun report(name: String, stars: Map<String, Int>, log: String, plan: Plan?, today: Long, nowMs: Long, months: String = ""): String = buildJsonObject {
        put("format", 1); put("app", Zaehler.APP); put("appName", "Raumlicht")
        if (name.isNotBlank()) put("name", name.trim().take(MAX_NAME))
        put("stand", iso(nowMs)); put("zeitraum", "letzte $DAYS Tage")
        val days = log(log).filter { it.day > today - DAYS && it.day <= today }
        put("lernzeitMin", days.sumOf { it.min }); put("tageAktiv", days.count { it.min > 0 || it.tasks > 0 })
        val tasks = days.sumOf { it.tasks }; put("aufgaben", tasks); if (tasks > 0) put("richtigProzent", days.sumOf { it.right } * 100 / tasks)
        // Stärken und Schwächen je Gebiet: Anteil der möglichen Sterne in den gespielten Aufgaben
        val rated = Levels.chapters.groupBy { it.area }.mapNotNull { (area, cs) -> val played = cs.flatMap { it.levels }.filter { (stars[it.id] ?: 0) > 0 }
            if (played.size < Nachhilfe.MIN_PLAYED) null else area to played.sumOf { stars[it.id] ?: 0 } * 100 / (played.size * 3) }
        put("staerken", JsonArray(rated.filter { it.second >= 80 }.map { (a, p) -> buildJsonObject { put("thema", Zaehler.topic(a)); put("titel", areaTitle(a)); put("prozent", p) } }))
        put("schwach", JsonArray(rated.filter { it.second < Nachhilfe.WEAK_PERCENT }.map { (a, p) -> buildJsonObject { put("thema", Zaehler.topic(a)); put("titel", areaTitle(a)); put("prozent", p)
            put("tipp", "Im Lernstand die wackeligen Kapitel noch einmal spielen – die Theorie-Karte zeigt dort die Formel und ein Beispiel.") } }))
        put("fortschritt", JsonArray(Levels.chapters.groupBy { it.area }.map { (a, cs) -> buildJsonObject { put("titel", areaTitle(a))
            put("geschafft", cs.sumOf { c -> c.levels.count { (stars[it.id] ?: 0) > 0 } }); put("von", cs.sumOf { it.levels.size }) } }))
        put("letzte", JsonArray(days.filter { it.min > 0 || it.tasks > 0 }.takeLast(5).map { d -> buildJsonObject { put("tag", isoDay(d.day)); put("min", d.min); put("aufgaben", d.tasks); put("richtig", d.right) } }))
        // Monatsverlauf: höchstens die letzten 12 Monate, älteste zuerst
        put("verlauf", JsonArray(months(months).takeLast(12).map { m -> buildJsonObject { put("monat", m.month); put("min", m.min); put("aufgaben", m.tasks); put("richtigProzent", if (m.tasks > 0) m.right * 100 / m.tasks else 0) } }))
        if (plan != null && plan.examDay >= today) put("klassenarbeit", buildJsonObject { put("tag", isoDay(plan.examDay))
            Levels.chapters.filter { it.number in plan.chapters }.map { it.area }.toSet().singleOrNull()?.let { put("thema", Zaehler.topic(it)) } })
        put("hinweis", "Raumlicht ist eine Mathe-App für die Oberstufe. Diesen Bericht teilt die Schülerin oder der Schüler selbst und kann das jederzeit beenden.")
    }.toString()
}
