package de.wissensgarten.core.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64

/**
 * „Eltern-Handy koppeln“: Eltern sehen auf dem eigenen Handy Fortschritt und Problemstellen ihres Kindes –
 * ohne Konto, Ende-zu-Ende verschlüsselt (AES-GCM-256). Der Schlüssel steht nur im Link nach dem „#“ und geht nie an einen Server.
 *
 * Gekoppelt wird nur hinter dem Eltern-Tor. Ohne Kopplung wird nichts gesendet. „Kopplung aufheben“ löscht den Bericht auf dem Server.
 * Der Bericht enthält keine Personendaten außer dem (optionalen) Spitznamen, den Eltern selbst eintragen.
 */
object ElternKopplung {
    const val FORMAT = 1
    /** Höchstens 24 KB beim Hochladen (Server-Grenze) */
    const val MAX_BYTES = 24 * 1024
    /** Höchstens alle 10 Minuten hochladen */
    const val ABSTAND_MS = 10 * 60 * 1000L

    data class Kopplung(val id: String, val schreib: String, val key: String)

    private val b64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    fun b64url(bytes: ByteArray): String = b64.encode(bytes)
    fun b64urlDecode(text: String): ByteArray = b64.decode(text)

    /** Neue Kopplung aus sicheren Zufallsbytes: id 16 B (22 Zeichen), schreib 16 B (22 Zeichen), key 32 B (43 Zeichen) */
    fun neu(zufall: (Int) -> ByteArray): Kopplung = Kopplung(b64url(zufall(16)), b64url(zufall(16)), b64url(zufall(32)))

    fun gueltig(k: Kopplung) = k.id.length == 22 && k.schreib.length == 22 && k.key.length == 43

    private fun server(test: Boolean) = if (test) "https://trainer-test.mathenachhilfe-online.com" else "https://trainer.mathenachhilfe-online.com"
    fun berichtUrl(test: Boolean, id: String) = "${server(test)}/eltern/v1/b/$id"
    /** Link für das Eltern-Handy – der Teil nach „#“ (id.key) verlässt den Browser der Eltern nie */
    fun elternLink(test: Boolean, k: Kopplung) =
        (if (test) "https://staging.mathenachhilfe-online.com" else "https://www.mathenachhilfe-online.com") + "/eltern-bericht/#${k.id}.${k.key}"

    // ---------------- Bericht ----------------

    data class Karte(val id: String, val bedId: String, val reps: Int, val stability: Double)
    data class Beet(val id: String, val titel: String, val thema: String?, val fachTitel: String)
    data class Antwort(val cardId: String, val zeit: Long, val richtig: Boolean)
    data class Arbeit(val tag: Long, val titel: String)

    private const val DAY = 24L * 60 * 60 * 1000

    /**
     * Baut den Bericht (JSON, Format 1). [jetzt] in ms, [heute] als lokaler Epoch-Tag, [offset] für lokale Tage.
     * Zeitraum: Kennzahlen der letzten 7 Tage, Stärken und Problemstellen aus 14 Tagen.
     */
    fun bericht(
        name: String?, klasse: Int?, jetzt: Long, heute: Long,
        beete: List<Beet>, karten: List<Karte>, antworten: List<Antwort>, arbeiten: List<Arbeit>,
        tipp: String?, offset: (Long) -> Int = Platform::tzOffsetMs,
    ): JsonObject {
        val beetVon = karten.associate { it.id to it.bedId }
        val beetById = beete.associateBy { it.id }
        fun tag(ms: Long) = Scheduler.localDay(ms, offset)
        val woche = antworten.filter { heute - tag(it.zeit) in 0..6 }
        val zwei = antworten.filter { heute - tag(it.zeit) in 0..13 }
        fun pct(l: List<Antwort>) = if (l.isEmpty()) 0 else l.count { it.richtig } * 100 / l.size

        // Stärken und Problemstellen je Thema (bzw. Fach, wenn es kein Nachhilfe-Thema gibt)
        data class Gruppe(val key: String, val titel: String, val thema: String?)
        val gruppen = zwei.mapNotNull { a -> beetVon[a.cardId]?.let(beetById::get)?.let { b ->
            val g = b.thema?.let { Gruppe(it, Angebote.themaName(it).replaceFirstChar { c -> c.uppercase() }, it) } ?: Gruppe("fach:" + b.fachTitel, b.fachTitel, null)
            g to a
        } }.groupBy({ it.first }, { it.second }).filterValues { it.size >= 5 }
        val staerken = gruppen.filter { pct(it.value) >= 85 }.entries.sortedByDescending { it.value.size }.take(5)
        val schwach = gruppen.filter { pct(it.value) < 60 }.entries.sortedBy { pct(it.value) }.take(5)

        val naechste = arbeiten.filter { it.tag >= heute }.minByOrNull { it.tag }
        return buildJsonObject {
            put("format", FORMAT)
            put("app", "wissensgarten")
            put("appName", "Wissensgarten")
            name?.trim()?.take(20)?.takeIf { it.isNotEmpty() }?.let { put("name", it) }
            if (klasse != null && klasse in 1..13) put("klasse", klasse)
            put("stand", isoZeit(jetzt))
            put("zeitraum", "letzte 7 Tage")
            put("lernzeitMin", (woche.size * WeekReport.SECONDS_PER_CARD + 59) / 60)
            put("tageAktiv", woche.map { tag(it.zeit) }.toSet().size)
            put("aufgaben", woche.size)
            put("richtigProzent", pct(woche))
            putJsonArray("staerken") { staerken.forEach { (g, l) -> addJsonObject {
                put("thema", g.thema ?: g.key.removePrefix("fach:")); put("titel", g.titel); put("prozent", pct(l))
            } } }
            putJsonArray("schwach") { schwach.forEach { (g, l) -> addJsonObject {
                put("thema", g.thema ?: g.key.removePrefix("fach:")); put("titel", g.titel); put("prozent", pct(l))
                put("tipp", "Gemeinsam kurz wiederholen: lieber 5 Minuten an mehreren Tagen als einmal lange. Lasst euch eine Karte erklären – Erklären festigt Wissen.")
            } } }
            putJsonArray("fortschritt") {
                val proBeet = karten.groupBy { it.bedId }
                beete.mapNotNull { b -> proBeet[b.id]?.let { b to it } }.sortedByDescending { it.second.size }.take(15).forEach { (b, l) ->
                    addJsonObject { put("titel", b.titel.take(40)); put("geschafft", l.count { it.reps > 0 && it.stability >= 7.0 }); put("von", l.size) }
                }
            }
            putJsonArray("letzte") {
                woche.groupBy { tag(it.zeit) }.entries.sortedBy { it.key }.forEach { (d, l) -> addJsonObject {
                    put("tag", isoTag(d)); put("min", (l.size * WeekReport.SECONDS_PER_CARD + 59) / 60); put("aufgaben", l.size); put("richtig", l.count { it.richtig })
                } }
            }
            if (naechste != null) putJsonObject("klassenarbeit") { put("tag", isoTag(naechste.tag)); put("thema", naechste.titel.take(60)) }
            tipp?.let { put("hinweis", it) }
        }
    }

    /** Bericht als Text, notfalls gekürzt, damit er unter 24 KB bleibt (nach Verschlüsselung und Base64 kommen ~35 % dazu) */
    fun berichtText(b: JsonObject): String {
        var o = b
        var t = o.toString()
        val grenze = MAX_BYTES * 70 / 100
        if (t.encodeToByteArray().size > grenze) {
            o = JsonObject(o + ("fortschritt" to JsonArray((o["fortschritt"] as? JsonArray).orEmpty().take(5))))
            t = o.toString()
        }
        return t
    }

    /** Lokaler Epoch-Tag → „2026-11-18“ */
    fun isoTag(epochDay: Long): String = kotlinx.datetime.LocalDate.fromEpochDays(epochDay.toInt()).toString()
    /** ms → ISO-Zeit in UTC, z. B. „2026-11-18T15:40:00Z“ */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun isoZeit(ms: Long): String = kotlin.time.Instant.fromEpochMilliseconds(ms / 1000 * 1000).toString()
}
