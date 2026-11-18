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
    private companion object { const val KEY = "exEltern" }
}
