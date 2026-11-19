package de.raumlicht.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * KI-Tutor: Nach einem Fehlversuch erklärt Lux die Aufgabe auf Wunsch anders, und man kann Rückfragen stellen.
 * Die Antwort kommt von unserem Trainer-Server ([URL]); nur er spricht mit dem KI-Dienst – die App bekommt keinen Schlüssel.
 *
 * Bezahlt wird mit KI-Punkten: Ohne Code gibt es [TRIALS] Proben je Gerät, mit dem „KI-Abi-Pass“ [DAILY] Punkte am Tag bis zum 31. Juli des gewählten Abi-Jahres.
 * Gezählt und geprüft wird auf dem Server (Signatur, Datum, Tageszähler); die App prüft den Code zusätzlich selbst, um Laufzeit und Status anzuzeigen.
 * Gesendet werden die Aufgabe, die Eingabe, die Frage, der bisherige Wortwechsel, der Code und eine zufällige Geräte-Kennung – kein Name, kein Lernstand.
 */
object Ki {
    const val NAME = "Raumlicht KI-Abi-Pass"
    const val PRICE = "14,99 €"
    /** Produktseite des KI-Abi-Passes im Shop (dort wählt man das Abi-Jahr 2027, 2028 oder 2029). Leer = nicht erhältlich: dann gibt es nur die Proben und die Code-Eingabe. */
    const val CHECKOUT_URL = "https://www.mathenachhilfe-online.com/produkt/raumlicht-ki-abi-pass/"
    /**
     * Für alle sichtbar? Seit 2.18.15 ja: Die Server-Route läuft (trainer und trainer-test). Seit 2.18.20 ist auch der Shop eingetragen ([CHECKOUT_URL]).
     * Auf false zeigt die App den KI-Tutor nur auf der Testseite und bei allen, die schon einen gültigen KI-Code eingegeben haben.
     */
    const val ENABLED = true
    const val URL = "https://trainer.mathenachhilfe-online.com/api/raumlicht/ki"
    /** Für die Testseite (raumlicht-test ⇄ trainer-test) */
    const val TEST_URL = "https://trainer-test.mathenachhilfe-online.com/api/raumlicht/ki"
    const val DAILY = 10
    const val TRIALS = 3
    /** längste Rückfrage, die die App abschickt */
    const val MAX_QUESTION = 300
    /** so viele frühere Wortwechsel gehen als Zusammenhang mit */
    const val MAX_HISTORY = 6
    const val PRIVACY = "KI-Tutor (nur wenn du ihn benutzt): Die Aufgabe, deine Eingabe und deine Frage gehen an unseren Server trainer.mathenachhilfe-online.com; " +
        "er lässt sie von einer KI (Anthropic) beantworten. Mitgesendet werden dein KI-Code, falls du einen hast, und eine zufällige Geräte-Kennung zum Zählen der Proben – keine Namen, keine Lernstände."

    /** 1.1.2020 als Tag seit dem 1.1.1970 – darauf beziehen sich die Tage im Lizenz-Code */
    private const val DAY_2020 = 18262L

    /** Inhalt eines gültigen KI-Codes (Signatur, App, Produkt geprüft) – ohne Blick aufs Datum */
    fun info(code: String?, key: String = Pro.PUBLIC_KEY): License.Info? = License.check(code, key, License.PRODUCT_KI)
    /** Letzter gültiger Tag als Tag seit dem 1.1.1970 */
    fun lastDay(info: License.Info) = DAY_2020 + info.day
    /** Gilt dieser Code heute ([today] = Tag seit dem 1.1.1970) noch? */
    fun active(code: String?, today: Long, key: String = Pro.PUBLIC_KEY) = info(code, key)?.let { today <= lastDay(it) } ?: false
    /** „31.07.2027“ */
    fun date(epochDay: Long): String {
        // bürgerliches Datum aus der Tageszahl (Verfahren von Howard Hinnant)
        val z = epochDay + 719468; val era = (if (z >= 0) z else z - 146096) / 146097; val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365; val doy = doe - (365 * yoe + yoe / 4 - yoe / 100); val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1; val m = if (mp < 10) mp + 3 else mp - 9; val y = yoe + era * 400 + (if (m <= 2) 1 else 0)
        return "${d.toString().padStart(2, '0')}.${m.toString().padStart(2, '0')}.$y"
    }
    /** Der gültige Code aus einem Link „?ki=…“ oder aus einer eingefügten E-Mail – oder null (abgelaufene Codes gelten hier noch als Code, die Anzeige sagt dann „abgelaufen“) */
    fun fromText(raw: String, key: String = Pro.PUBLIC_KEY): String? =
        listOfNotNull(License.extract(raw), License.extract(License.PREFIX + raw), raw.trim().takeIf { it.isNotEmpty() }).firstOrNull { info(it, key) != null }

    enum class Kind(val wire: String, val points: Int) { EXPLAIN("erklaerung", 1), QUESTION("rueckfrage", 1) }
    /** Ein Wortwechsel: wer („schueler“ / „lux“) hat was gesagt */
    data class Turn(val role: String, val text: String)

    /** Die Eingabe in Worten: „n⃗ = (0|0|1), c = 3“ */
    fun input(level: Level, values: Values): String = level.controls.joinToString(", ") { c ->
        val v = values[c.key] ?: c.start
        c.name + " = " + if (v.size == 1) Geo.num(v[0] * c.step) + c.suffix else "(" + v.joinToString("|") { Geo.num(it) } + ")"
    }
    /** Formelzeile als Text */
    fun formula(level: Level, values: Values): String = runCatching { level.formula(values).joinToString("\n") { row -> row.joinToString("") { p -> when (p) {
        is Part.Txt -> p.s; is Part.Num -> p.s; is Part.Col -> "(" + p.cells.joinToString("|") + ")"; else -> "" } } } }.getOrDefault("")

    /**
     * Die Anfrage an den Server. [feedback] ist die Rückmeldung der App auf die Eingabe; der Rechenweg geht mit, damit die KI nichts erfindet –
     * der Server gibt ihn der KI als Hintergrund, mit der Anweisung, die Lösung nicht zu verraten.
     */
    fun request(level: Level, values: Values, feedback: String, kind: Kind, question: String, history: List<Turn>, code: String, device: String, version: String): String = buildJsonObject {
        put("app", "raumlicht"); put("version", version); put("art", kind.wire)
        put("aufgabe", buildJsonObject {
            put("id", level.id); put("kapitel", level.chapter); put("titel", level.title); put("auftrag", level.mission); put("tipp", level.tip)
            put("formel", formula(level, values)); put("rechenweg", JsonArray(level.way.map { JsonPrimitive(it) }))
        })
        put("eingabe", input(level, values)); put("richtig", false); put("meldung", feedback)
        put("frage", question.trim().take(MAX_QUESTION))
        put("verlauf", JsonArray(history.takeLast(MAX_HISTORY).map { t -> buildJsonObject { put("rolle", t.role); put("text", t.text.take(1200)) } }))
        put("geraet", device); put("code", code)
    }.toString()

    /** Antwort des Servers: [text] (bei Erfolg), [rest] = KI-Punkte, die heute bzw. als Probe noch übrig sind (null = unbekannt) */
    class Answer(val status: Int, val text: String, val rest: Int?)
    fun answer(status: Int, body: String): Answer {
        val o = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()
        return Answer(status, ((o?.get("text") as? JsonPrimitive)?.content ?: "").trim(), (o?.get("rest") as? JsonPrimitive)?.content?.toIntOrNull())
    }
    /** Was Lux sagt, wenn es keine Antwort gibt */
    fun problem(status: Int, hasCode: Boolean): String = when (status) {
        402 -> if (hasCode) "Dein KI-Code gilt nicht mehr." else "Deine $TRIALS Proben sind aufgebraucht."
        429 -> "Für heute sind deine KI-Punkte aufgebraucht. Morgen gibt es neue – bis dahin helfen Tipp und Theorie."
        0 -> "Ich erreiche den KI-Tutor gerade nicht. Prüf deine Internetverbindung und versuch es noch einmal."
        else -> "Der KI-Tutor ist gerade nicht erreichbar. Versuch es später noch einmal – Tipp und Theorie helfen auch ohne ihn."
    }
}
