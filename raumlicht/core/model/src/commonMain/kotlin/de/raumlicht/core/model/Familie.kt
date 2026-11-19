package de.raumlicht.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Gemeinsamer Familien-Speicher der Lern-Apps (`/familie/v1/`): Eltern legen eine Familie an, jedes Kind hat darin seinen Lernstand –
 * verschlüsselt im Browser (Baustein `familie.js` vom Mathe-Trainer), der Server kann nichts lesen.
 * Läuft NEBEN „Eltern-Handy koppeln“; die Kopplung bleibt, wie sie ist. Live noch aus: Der Server ist bis zur Freigabe gesperrt.
 */
object Familie {
    /** Auf der Live-Seite sichtbar? Erst einschalten, wenn der Server freigegeben ist – auf der Teststufe ist die Seite immer da */
    const val ENABLED = false
    const val APP = "raumlicht"
    const val SERVER = "https://lernen.mathenachhilfe-online.com"
    const val TEST_SERVER = "https://lernen-test.mathenachhilfe-online.com"
    const val MAX_NAME = 20
    /** Rechte gehören der Familie, nicht dem einzelnen Kind – sie stehen nie im Lernstand eines Kindes */
    val RIGHTS = setOf("proCode", "kiCode")

    const val INTRO = "Eine Familie, alle Lern-Apps: Der Lernstand liegt verschlüsselt im Familien-Speicher. Geht ein Gerät verloren, holt ihr ihn mit dem Familien-Link zurück – " +
        "und Geschwister lernen auf demselben Tablet, jedes mit eigenem Stand."
    const val NOTE = "Verschlüsselt wird hier im Browser; der Server kann nichts lesen. Wer den Familien-Link hat, kann die Lernstände öffnen – gut aufbewahren. " +
        "Freischalt-Codes stehen nicht im Lernstand der Kinder. Ohne Konto, ohne E-Mail-Adresse.\n" +
        "Für den Notfall liegt eine verschlüsselte Kopie des Familien-Schlüssels bei uns: Nur Mathenachhilfe Online kann sie mit einem getrennt verwahrten Schlüssel öffnen, um euch den Zugang zurückzugeben, wenn der Link verloren ist."
    const val TEST_NOTE = "Teststufe: Der Familien-Speicher wird gerade erprobt. Die bisherige Kopplung mit dem Eltern-Handy bleibt unverändert."

    /** Der Lernstand eines Kindes: alles, was auch die Sicherung enthält, aber ohne Rechte */
    fun child(data: Map<String, String>): Map<String, String> = data.filterKeys { Eltern.saved(it) && it !in RIGHTS }
    /** Die Rechte, getrennt abgelegt; leere Einträge zählen nicht */
    fun rights(data: Map<String, String>): Map<String, String> = data.filter { it.key in RIGHTS && it.value.isNotBlank() }

    private val FAM = Regex("[\\w-]{22}\\.[\\w-]{43}\\.[\\w-]{22}")
    private val KID = Regex("[\\w-]{22}")
    /** Liest „…#familie=<famId>.<famKey>.<schreib>“ aus einem Link (oder die drei Teile allein) – sonst null */
    fun fromLink(text: String): String? { val t = text.trim().substringAfterLast("familie=").substringBefore('&'); return t.takeIf { FAM.matches(it) } }
    fun link(fam: String, base: String) = "$base/$APP/#familie=$fam"

    /** Ein Kind im Familien-Index (Spitzname, vom Server nicht lesbar) */
    data class Kid(val id: String, val name: String) { val label get() = name.ifBlank { "Kind ohne Namen" } }
    /** Antwort der Brücke: je Zeile „<kindId>\t<Spitzname>“ */
    fun kids(text: String): List<Kid> = text.split('\n').mapNotNull { l -> val p = l.split('\t', limit = 2); p[0].takeIf { KID.matches(it) }?.let { Kid(it, p.getOrElse(1) { "" }.trim().take(MAX_NAME)) } }
    fun cleanName(name: String) = name.replace(Regex("[\\t\\n|]"), " ").trim().take(MAX_NAME)

    /**
     * Was dieses Gerät über seine Familie weiß.
     * @param fam „famId.famKey.schreib“  @param rev Stand-Nummer des Servers (0 = noch nichts gesichert)  @param mark Prüfwert des zuletzt gesicherten Lernstands
     */
    data class State(val fam: String, val kid: String, val number: String, val rev: Int = 0, val mark: String = "", val name: String = "") {
        fun encode() = "$fam|$kid|$number|$rev|$mark|$name"
        companion object {
            fun decode(s: String): State? { val p = s.split('|', limit = 6); if (p.size < 6 || !FAM.matches(p[0]) || !KID.matches(p[1])) return null
                return State(p[0], p[1], p[2], p[3].toIntOrNull() ?: 0, p[4], p[5]) }
        }
    }

    /** Lernstand als JSON für den Baustein (Schlüssel sortiert, damit gleiche Stände gleich aussehen) und zurück */
    fun json(data: Map<String, String>): String = JsonObject(child(data).entries.sortedBy { it.key }.associate { it.key to JsonPrimitive(it.value) }).toString()
    fun data(json: String): Map<String, String>? = try { child((Json.parseToJsonElement(json) as JsonObject).mapValues { (it.value as JsonPrimitive).content }) } catch (e: Exception) { null }

    /** Gibt es auf diesem Gerät schon etwas, das beim Übernehmen verloren ginge? */
    fun hasProgress(data: Map<String, String>) = child(data).any { (k, v) -> k in PROGRESS && v.isNotBlank() }
    private val PROGRESS = setOf("sterne", "stars", "lernProtokoll", "diary")

    /** Verständliche Rückmeldung zu den Fehler-Kennungen des Bausteins */
    fun problem(reason: String) = when (reason) {
        "netz" -> "Keine Verbindung. Bitte später noch einmal versuchen."
        "keine" -> "Diese Familie gibt es nicht (mehr). Stimmt der Link?"
        "schluessel" -> "Der Familien-Link passt nicht zu den gespeicherten Daten."
        "schreibrecht" -> "Mit diesem Link darf nicht gespeichert werden."
        "noch-nicht-live" -> "Der Familien-Speicher ist noch nicht freigeschaltet."
        "zu-gross" -> "Der Familien-Speicher ist voll."
        "zu-oft" -> "Zu viele Versuche. Bitte einen Moment warten."
        "link" -> "Das ist kein Familien-Link. Er sieht so aus: …/#familie=…"
        "baustein" -> "Der Familien-Speicher ließ sich nicht laden. Bitte Seite neu laden."
        else -> "Das hat gerade nicht geklappt ($reason)."
    }
}
