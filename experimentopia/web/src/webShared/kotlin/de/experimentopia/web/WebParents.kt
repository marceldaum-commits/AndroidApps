package de.experimentopia.web

import de.experimentopia.app.ui.fx.ParentSync
import de.experimentopia.core.model.ParentLink

/** Eltern-Kopplung im Browser: Kopplung im localStorage, Zufall aus crypto.getRandomValues, Hochladen per fetch */
class WebParentSync : ParentSync {
    override fun random(n: Int): ByteArray { val h = randomHex(n); return ByteArray(n) { h.substring(2 * it, 2 * it + 2).toInt(16).toByte() } }
    override fun send(method: String, url: String, write: String, body: String?, done: (Int) -> Unit) = httpSend(method, url, write, body ?: "", done)
    override fun share(title: String, url: String, done: (String) -> Unit) = shareLink(title, url, done)
    override var stored: String?
        get() = storeGet(KEY)
        set(v) { if (v == null) storeRemove(KEY) else storeSet(KEY, v) }
    override val test: Boolean = ParentLink.isTestHost(pageHost())
    override fun now(): Long = nowMs().toLong()
    override val offsetMs: Long get() = tzOffsetMin() * 60_000L
    override fun get(url: String, done: (Int, String) -> Unit) = httpGet(url, done)
    /** Alles, was mit „experimentopia.“ beginnt: Ergebnisse, Forscherbuch, Einstellungen, Monatssummen, Pass-Code */
    override fun snapshot(): Map<String, String> = storeKeys(PREFIX).split('\n').filter { it.isNotEmpty() }.mapNotNull { k -> storeGet(k)?.let { k to it } }.toMap()
    override fun restore(data: Map<String, String>) {
        storeKeys(PREFIX).split('\n').filter { it.isNotEmpty() }.forEach { storeRemove(it) }
        data.filterKeys { it.startsWith(PREFIX) }.forEach { (k, v) -> storeSet(k, v) }
        reloadPage()
    }
    override var months: String?
        get() = storeGet(PREFIX + "verlauf")
        set(v) { if (v == null) storeRemove(PREFIX + "verlauf") else storeSet(PREFIX + "verlauf", v) }
    override val version: String get() = Version.NAME
    override val journalKey: String = PREFIX + "forscherbuch"
    private companion object { const val KEY = "exEltern"; const val PREFIX = "experimentopia." }
}
