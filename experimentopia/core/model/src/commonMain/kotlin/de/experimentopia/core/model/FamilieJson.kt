package de.experimentopia.core.model

import kotlinx.serialization.json.*

/**
 * Sprache zwischen der App und familie.js (über familie-bruecke.js): Aufrufe und Antworten sind JSON-Texte.
 * Alles Kryptografische (HKDF, AES-GCM, gzip, X25519-Notfall-Kopie) macht familie.js unverändert – hier wird nur gepackt und gelesen.
 */
object FamilieJson {
    data class Fam(val famId: String, val famKey: String, val schreib: String) {
        fun encode() = buildJsonObject { put("famId", famId); put("famKey", famKey); put("schreib", schreib) }.toString()
    }
    data class Kind(val kindId: String, val name: String)
    data class Antwort(
        val ok: Boolean, val grund: String = "", val rev: Long = 0,
        val fam: Fam? = null, val nummer: String = "", val kindId: String = "", val link: String = "", val blatt: String = "",
        /** Kind-Stand: Teil → Text */
        val daten: Map<String, String> = emptyMap(),
        /** Familien-Index als JSON-Text (Kinder, Apps) */
        val index: String? = null, val kinder: List<Kind> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true }
    fun famOrNull(s: String?): Fam? = s?.let { runCatching { fam(json.parseToJsonElement(it)) }.getOrNull() }
    private fun fam(e: JsonElement?): Fam? = (e as? JsonObject)?.let { o ->
        val a = o["famId"]?.jsonPrimitive?.content; val b = o["famKey"]?.jsonPrimitive?.content; val c = o["schreib"]?.jsonPrimitive?.content
        if (a != null && b != null && c != null) Fam(a, b, c) else null
    }

    fun parse(text: String?): Antwort = runCatching {
        val o = json.parseToJsonElement(text ?: "").jsonObject
        val daten = o["daten"]
        val isIndex = daten is JsonObject && daten["kinder"] != null
        Antwort(
            ok = o["ok"]?.jsonPrimitive?.booleanOrNull == true, grund = o["grund"]?.jsonPrimitive?.content ?: "",
            rev = o["rev"]?.jsonPrimitive?.longOrNull ?: 0, fam = fam(o["fam"]), nummer = o["nummer"]?.jsonPrimitive?.content ?: "",
            kindId = o["kindId"]?.jsonPrimitive?.content ?: "", link = o["link"]?.jsonPrimitive?.content ?: "", blatt = o["blatt"]?.jsonPrimitive?.content ?: "",
            daten = if (daten is JsonObject && !isIndex) daten.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.takeIf { it.isString }?.let { k to it.content } }.toMap() else emptyMap(),
            index = (if (isIndex) daten else o["index"])?.toString(),
            kinder = kinderOf((if (isIndex) daten else o["index"]) as? JsonObject),
        )
    }.getOrDefault(Antwort(false, "fehler"))

    private fun kinderOf(index: JsonObject?): List<Kind> = (index?.get("kinder") as? JsonArray).orEmpty().mapNotNull { k ->
        (k as? JsonObject)?.let { Kind(it["kindId"]?.jsonPrimitive?.content ?: return@let null, it["name"]?.jsonPrimitive?.content ?: "") }
    }

    // ── Aufrufe
    fun neu() = "{}"
    fun lesen(text: String) = buildJsonObject { put("text", text) }.toString()
    fun kindId() = "{}"
    fun blatt(f: Fam, seite: String) = buildJsonObject { put("fam", json.parseToJsonElement(f.encode())); put("seite", seite) }.toString()
    private fun mit(f: Fam, basis: String, extra: JsonObjectBuilder.() -> Unit = {}) = buildJsonObject { put("fam", json.parseToJsonElement(f.encode())); put("basis", basis); extra() }.toString()
    fun anlegen(f: Fam, basis: String) = mit(f, basis)
    fun indexNeu(name: String) = buildJsonObject { put("name", name) }.toString()
    fun indexLaden(f: Fam, basis: String) = mit(f, basis)
    fun indexSichern(f: Fam, basis: String, index: String, rev: Long) = mit(f, basis) { put("index", json.parseToJsonElement(index)); put("rev", rev) }
    fun kindHinzu(index: String, name: String) = buildJsonObject { put("index", json.parseToJsonElement(index)); put("name", name) }.toString()
    fun standLaden(f: Fam, basis: String, kind: String) = mit(f, basis) { put("kind", kind) }
    fun standSichern(f: Fam, basis: String, kind: String, daten: Map<String, String>, rev: Long) =
        mit(f, basis) { put("kind", kind); put("rev", rev); put("daten", buildJsonObject { daten.entries.sortedBy { it.key }.forEach { put(it.key, it.value) } }) }
    fun loeschen(f: Fam, basis: String) = mit(f, basis)

    /** Server je Stufe: Test-Stufe → lernen-test, sonst lernen (Familien-Speicher liegt unter /familie/v1/) */
    fun basis(test: Boolean) = if (test) "https://lernen-test.mathenachhilfe-online.com" else "https://lernen.mathenachhilfe-online.com"

    /** Fehlergrund von familie.js in Worte für Eltern */
    fun grundText(grund: String) = when (grund) {
        "netz" -> "Gerade kein Internet – es wird später noch einmal versucht."
        "noch-nicht-live" -> "Der Familien-Speicher ist noch nicht freigeschaltet."
        "gibt-es-schon" -> "Diese Familie gibt es schon."
        "familie-weg" -> Familie.FAMILIE_WEG
        "schreibrecht" -> "Dieses Gerät darf in der Familie nichts ändern (Link unvollständig?)."
        "zu-gross" -> "Der Lernstand ist zu groß für den Familien-Speicher – bitte melden."
        "zu-oft" -> "Zu viele Anfragen – bitte gleich noch einmal versuchen."
        "schluessel" -> "Der Familien-Schlüssel passt nicht zu den gespeicherten Daten."
        "keine" -> "Dazu gibt es noch nichts."
        else -> "Das hat leider nicht geklappt ($grund)."
    }
}
