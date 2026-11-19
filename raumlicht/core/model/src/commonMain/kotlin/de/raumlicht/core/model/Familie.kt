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
    /** Einheitlicher Eltern-Hinweis aller Lern-Apps (FAMILIEN-SPEICHER.md v1.5) – Wortlaut nicht ändern */
    const val NOTE = "Eure Lernstände werden verschlüsselt auf unserem Server in Deutschland gespeichert. Der Schlüssel liegt auf euren Geräten. " +
        "Damit wir euch helfen können, falls ihr den Familien-Zugang verliert, hinterlegen wir zusätzlich eine verschlüsselte Notfall-Kopie. " +
        "Sie lässt sich nur mit einem getrennt und offline aufbewahrten Schlüssel von Mathenachhilfe Online öffnen. " +
        "Technisch könnten wir damit eure Lernstände lesen; wir tun das nicht und nutzen ihn nur, wenn ihr uns um Wiederherstellung bittet. " +
        "Jede Wiederherstellung seht ihr hier im Protokoll."
    const val DETAILS = "Wer den Familien-Link hat, kann die Lernstände öffnen – gut aufbewahren. Freischalt-Codes stehen nicht im Lernstand der Kinder. Ohne Konto, ohne E-Mail-Adresse."
    const val PRIVACY_URL = "https://www.mathenachhilfe-online.com/datenschutz/"
    /** Einheitliche Warnung vor dem Löschen der ganzen Familie – Wortlaut nicht ändern */
    const val DELETE_WARNING = "Das löscht die Familie in ALLEN Lern-Apps: alle Lernstände aller Kinder auf unserem Server, die Notfall-Kopie und das Protokoll. " +
        "Gekaufte Codes werden wieder frei und lassen sich in einer neuen Familie erneut einlösen. Die Lernstände auf diesem Gerät bleiben erhalten."
    const val GONE = "Diese Familie gibt es nicht mehr. Lernstand bleibt auf diesem Gerät."
    const val DELETED = "Die Familie wurde gelöscht. Der Lernstand auf diesem Gerät bleibt."
    /** Familien-Nummer so lesen wie der Server: ohne Bindestrich und Leerzeichen, groß, O→0, I/L→1 */
    fun plainNumber(t: String) = t.uppercase().replace(Regex("[\\s-]"), "").replace('O', '0').replace(Regex("[IL]"), "1")
    /** Löschen ist erst bestätigt, wenn die eingetippte Familien-Nummer stimmt */
    fun deleteConfirmed(typed: String, number: String) = number.isNotBlank() && plainNumber(typed) == plainNumber(number)
    /** Protokoll der Familie: je Zeile „<art>\t<Zeit>“ → lesbare Einträge, neueste zuerst */
    fun protocol(text: String): List<String> = text.split('\n').mapNotNull { l -> val p = l.split('\t'); if (p[0].isBlank()) null else {
        val what = when (p[0]) { "angelegt" -> "Familie angelegt"; "wiederhergestellt", "wiederherstellung" -> "Zugang durch Mathenachhilfe Online wiederhergestellt"; "eingeloest" -> "Code eingelöst"; else -> p[0] }
        (Eltern.germanDay(p.getOrElse(1) { "" }).ifEmpty { "–" }) + ": " + what } }.reversed()
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
        "weg" -> GONE
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
