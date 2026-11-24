package de.experimentopia.web

import de.experimentopia.app.ui.fx.ParentSync
import de.experimentopia.core.model.ParentLink

/** Eltern-Kopplung im Browser: Kopplung im localStorage, Zufall aus crypto.getRandomValues, Hochladen per fetch */
class WebParentSync : ParentSync {
    override fun random(n: Int): ByteArray { val h = randomHex(n); return ByteArray(n) { h.substring(2 * it, 2 * it + 2).toInt(16).toByte() } }
    override fun send(method: String, url: String, write: String, body: String?, done: (Int) -> Unit) = httpSend(method, url, write, body ?: "", done)
    override fun share(title: String, url: String, done: (String) -> Unit) = shareLink(title, url, done)
    override var stored: String?
        get() = storeGet(KEY) ?: storeGet(OLD)?.also { storeSet(KEY, it); storeRemove(OLD) }   // alter Name ohne App-Vorsilbe (bis 2.12.0)
        set(v) { storeRemove(OLD); if (v == null) storeRemove(KEY) else storeSet(KEY, v) }
    override val test: Boolean = ParentLink.isTestHost(pageHost())
    override fun now(): Long = nowMs().toLong()
    override val offsetMs: Long get() = tzOffsetMin() * 60_000L
    override fun get(url: String, done: (Int, String) -> Unit) = httpGet(url, done)
    /** Alles, was mit „experimentopia.“ beginnt: Ergebnisse, Forscherbuch, Einstellungen, Monatssummen, Pass-Code */
    /** Kopplung (gehört zum Gerät) und Technik (Ladeprotokoll, Fassung, Umzug) gehören nicht in die Sicherung */
    private fun learning(k: String) = k.startsWith(PREFIX) && k != KEY && k != PREFIX + "schritte" && k != PREFIX + "umzug" && !k.startsWith(PREFIX + "modus") &&
        !k.startsWith(PREFIX + "hinweis.") && k !in FAMILIE.map { PREFIX + it }
    private fun keys() = storeKeys(PREFIX).split('\n').filter { it.isNotEmpty() && learning(it) }
    override fun snapshot(): Map<String, String> = keys().mapNotNull { k -> storeGet(k)?.let { k to it } }.toMap()
    override fun restore(data: Map<String, String>) {
        keys().forEach { storeRemove(it) }
        data.filterKeys { learning(it) }.forEach { (k, v) -> storeSet(k, v) }
        reloadPage()
    }
    override var months: String?
        get() = storeGet(PREFIX + "verlauf")
        set(v) { if (v == null) storeRemove(PREFIX + "verlauf") else storeSet(PREFIX + "verlauf", v) }
    override val version: String get() = Version.NAME
    override val journalKey: String = PREFIX + "forscherbuch"
    private companion object { const val KEY = "experimentopia.eltern"; const val OLD = "exEltern"; const val PREFIX = "experimentopia."
        /** Schlüssel und Merkzettel des Familien-Speichers gehören zu diesem Gerät, nicht in eine Sicherung */
        val FAMILIE = listOf("familie", "familienummer", "kind", "familierev", "familiezeit", "familielink") }
}

/** Anonyme Zählung im Browser – Standard an, Schalter im Elternbereich (Speicher „experimentopia.zaehlung“) */
class WebCounter : de.experimentopia.app.ui.fx.Counter {
    private val test = ParentLink.isTestHost(pageHost())
    private val _on = kotlinx.coroutines.flow.MutableStateFlow(storeGet(KEY) != "0")
    override val enabled: kotlinx.coroutines.flow.StateFlow<Boolean> = _on
    override fun setEnabled(on: Boolean) { storeSet(KEY, if (on) "1" else "0"); _on.value = on }
    override fun count(ev: String, ort: String?, thema: String?) {
        if (!_on.value) return
        val body = de.experimentopia.core.model.Zaehlung.inhalt(Version.NAME, ev, ort, thema) ?: return
        zaehlenSenden(de.experimentopia.core.model.Zaehlung.url(test), body)
    }
    private companion object { const val KEY = "experimentopia.zaehlung" }
}

/** Familien-Speicher im Browser: Aufrufe an familie.js (unverändert) über familie-bruecke.js; Merkzettel im Speicher „experimentopia.<name>“ */
class WebFamily : de.experimentopia.app.ui.fx.FamilyPlatform {
    override fun call(op: String, args: String, done: (String) -> Unit) = familieCall(op, args, done)
    override val test: Boolean = ParentLink.isTestHost(pageHost())
    override fun now(): Long = nowMs().toLong()
    override fun read(key: String): String? = storeGet("experimentopia.$key")
    override fun write(key: String, value: String?) { if (value == null) storeRemove("experimentopia.$key") else storeSet("experimentopia.$key", value) }
    override fun reload() = reloadPage()
}
