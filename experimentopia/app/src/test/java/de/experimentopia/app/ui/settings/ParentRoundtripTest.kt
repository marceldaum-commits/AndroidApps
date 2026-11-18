package de.experimentopia.app.ui.settings

import de.experimentopia.app.ui.fx.ParentSync
import de.experimentopia.core.model.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom

/**
 * Rundlauf gegen den echten Test-Server (trainer-test) – nur mit RUNDLAUF_NETZ=1:
 * Gerät A koppelt und sendet Bericht + Sicherung → Gerät B (leer) stellt mit dem Eltern-Link wieder her → gleicher Lernstand →
 * B schreibt weiter (204) → Gerät mit falschem Schreibrecht bekommt 403 → Aufheben löscht Bericht und Sicherung.
 */
class ParentRoundtripTest {
    private fun call(method: String, url: String, write: String?, body: String?): Pair<Int, String> = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method; c.connectTimeout = 15_000; c.readTimeout = 15_000
        write?.let { c.setRequestProperty("X-Schreib", it) }
        if (body != null) { c.doOutput = true; c.setRequestProperty("Content-Type", "text/plain"); c.outputStream.use { it.write(body.toByteArray()) } }
        val st = c.responseCode
        st to ((if (st < 400) c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty())
    }.getOrDefault(0 to "")
    private val rnd = SecureRandom()

    /** Ein „Gerät“: Speicher als Map, echtes HTTP */
    private inner class Device(val mem: MutableMap<String, String>) : ParentSync {
        val log = mutableListOf<String>()
        override fun random(n: Int) = ByteArray(n).also { rnd.nextBytes(it) }
        override fun send(method: String, url: String, write: String, body: String?, done: (Int) -> Unit) {
            val st = call(method, url, write, body).first
            log += "$method ${url.substringAfter("/eltern/v1/")} → $st"; done(st)
        }
        override fun get(url: String, done: (Int, String) -> Unit) {
            val (st, text) = call("GET", url, null, null)
            log += "GET ${url.substringAfter("/eltern/v1/")} → $st"; done(st, text)
        }
        override fun share(title: String, url: String, done: (String) -> Unit) = done("")
        override var stored: String? = null
        override val test = true
        override fun now() = System.currentTimeMillis()
        override val offsetMs = 3_600_000L
        override fun snapshot() = mem.filterKeys { it.startsWith("experimentopia.") }
        var restored = false
        override fun restore(data: Map<String, String>) { mem.clear(); mem.putAll(data); restored = true }
        override var months: String? get() = mem["experimentopia.verlauf"]; set(v) { if (v == null) mem.remove("experimentopia.verlauf") else mem["experimentopia.verlauf"] = v }
        override val version = "test"
        override val journalKey = "experimentopia.forscherbuch"
    }

    @Test fun rundlauf() {
        assumeTrue("nur mit RUNDLAUF_NETZ=1", System.getenv("RUNDLAUF_NETZ") == "1")
        val now = System.currentTimeMillis()
        val journal = Catalog.all.take(8).mapIndexed { i, e -> NoteRow(i + 1L, e.id, now - i * 240_000L, "g", "r", i % 3 != 0, 1, true) }
        val stars = journal.associate { it.experimentId to 2 }
        val a = Device(mutableMapOf(
            "experimentopia.ergebnisse" to journal.joinToString(",", "[", "]") { """{"experimentId":"${it.experimentId}","stars":2,"firstGuessRight":${it.right}}""" },
            "experimentopia.forscherbuch" to kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(NoteRow.serializer()), journal),
            "experimentopia.klasse" to "6", "experimentopia.procode" to "EX-TESTCODE", "experimentopia.abzeichen" to "erster,aha1"))
        val ca = ParentCoupling(a)
        val link = ca.couple("Rundlauf")
        var ok = false; ca.upload(stars, journal, 6) { ok = it }
        assertTrue("A: Bericht + Sicherung ${a.log} ${ca.status.value}", ok)
        assertTrue(a.log.any { it.startsWith("PUT s/") && it.endsWith("204") })
        assertNotNull("Monatssummen gespeichert", a.mem["experimentopia.verlauf"])
        // zweites Senden ohne Änderung: keine neue Sicherung
        val before = a.log.size; ca.upload(stars, journal, 6)
        assertFalse("nur bei Änderung", a.log.drop(before).any { it.startsWith("PUT s/") })

        // Gerät B: leer, stellt mit dem Eltern-Link wieder her
        val b = Device(mutableMapOf()); val cb = ParentCoupling(b)
        var found: ParentCoupling.Found? = null
        cb.fetchBackup(link.parentUrl) { found = it }
        val f = found as? ParentCoupling.Found.Ok ?: error("B: ${(found as? ParentCoupling.Found.Problem)?.message} ${b.log}")
        cb.applyRestore(f)
        assertTrue(b.restored); assertEquals("gleicher Lernstand", a.snapshot(), b.snapshot())
        assertEquals(link.id, cb.link.value!!.id); assertEquals(link.write, cb.link.value!!.write); assertEquals("Rundlauf", cb.link.value!!.nickname)
        // B schreibt weiter (neuer Versuch → Bericht + Sicherung)
        val j2 = journal + NoteRow(99, Catalog.all[20].id, now, "g", "r", true, 1, true)
        b.mem["experimentopia.klasse"] = "7"
        var okB = false; cb.upload(stars + (Catalog.all[20].id to 3), j2, 7) { okB = it }
        assertTrue("B schreibt weiter ${b.log}", okB && b.log.count { it.startsWith("PUT") && it.endsWith("204") } == 2)

        // falsches Schreibrecht → 403
        val c = Device(mutableMapOf()); var st = -1
        c.send("PUT", link.backupUrl, ParentLink.create(c::random, true).write, "AAAAAAAAAAAAAAAA.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") { st = it }
        assertEquals(403, st)
        // Link einer falschen Sicherung: nicht vorhanden
        var nf: ParentCoupling.Found? = null; ParentCoupling(c).fetchBackup(ParentLink.create(c::random, true).parentUrl) { nf = it }
        assertTrue((nf as ParentCoupling.Found.Problem).message.startsWith("Keine Sicherung"))

        // Aufheben (von B) löscht Bericht und Sicherung
        cb.unlink()
        var after = 0; c.get(link.backupUrl) { s, _ -> after = s }
        assertEquals(404, after)
        println("Rundlauf A: " + a.log); println("Rundlauf B: " + b.log)
    }
}
