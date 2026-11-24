package de.experimentopia.app.ui

import de.experimentopia.app.ui.fx.FamilyPlatform
import de.experimentopia.app.ui.settings.FamilyCoupling
import de.experimentopia.core.model.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/** Familien-Speicher: Anlegen, Sichern, Beitreten, Konflikt, Löschen – gegen einen nachgebauten Server (nur zum Testen) */
class FamilyCouplingTest {
    /** „Server“: eine Familie mit Index und Kind-Ständen, mit rev wie /familie/v1/ */
    private class Server {
        var alive = false; var index: JsonElement? = null; var indexRev = 0L
        val staende = mutableMapOf<String, Pair<JsonObject, Long>>(); var offline = false; var gesperrt = false; var konfliktBeiNaechstemSichern: (() -> Unit)? = null
        val aufrufe = mutableListOf<String>()
    }
    private class Geraet(val server: Server, val name: String) : FamilyPlatform {
        val speicher = mutableMapOf<String, String>(); var neuGeladen = 0; var uhr = 1_000_000L
        override val test = true
        override fun now() = uhr
        override fun read(key: String) = speicher[key]
        override fun write(key: String, value: String?) { if (value == null) speicher.remove(key) else speicher[key] = value }
        override fun reload() { neuGeladen++ }
        override fun call(op: String, args: String, done: (String) -> Unit) { server.aufrufe += "$name:$op"; done(antwort(op, Json.parseToJsonElement(args).jsonObject).toString()) }
        private fun fam(a: JsonObject) = a["fam"]!!.jsonObject
        private fun antwort(op: String, a: JsonObject): JsonObject = buildJsonObject {
            val s = server
            if (op != "neu" && op != "lesen" && op != "kindId" && op != "indexNeu" && op != "kindHinzu" && op != "blatt" && s.offline) { put("ok", false); put("grund", "netz"); return@buildJsonObject }
            when (op) {
                "neu" -> { put("ok", true); put("fam", buildJsonObject { put("famId", "F".repeat(22)); put("famKey", "K".repeat(43)); put("schreib", "S".repeat(22)) }); put("nummer", "ABCD-EFGH") }
                "lesen" -> { val t = a["text"]!!.jsonPrimitive.content; if (!t.contains("#familie=")) { put("ok", false); put("fam", JsonNull) } else { put("ok", true); put("fam", buildJsonObject { put("famId", "F".repeat(22)); put("famKey", "K".repeat(43)); put("schreib", "S".repeat(22)) }); put("nummer", "ABCD-EFGH") } }
                "anlegen" -> if (s.alive) { put("ok", false); put("grund", "gibt-es-schon") } else if (s.gesperrt) { put("ok", false); put("grund", "noch-nicht-live") } else { s.alive = true; put("ok", true) }
                "indexNeu" -> { put("ok", true); put("kindId", "KIND-$name"); put("index", buildJsonObject { put("v", 1); put("kinder", buildJsonArray { add(buildJsonObject { put("kindId", "KIND-$name"); put("name", a["name"]!!.jsonPrimitive.content) }) }); put("apps", buildJsonArray { add("experimentopia") }) }) }
                "kindHinzu" -> { val idx = a["index"]!!.jsonObject; val k = "KIND-NEU-$name"
                    put("ok", true); put("kindId", k); put("index", buildJsonObject { put("v", 1); put("kinder", JsonArray(idx["kinder"]!!.jsonArray + buildJsonObject { put("kindId", k); put("name", a["name"]!!.jsonPrimitive.content) })); put("apps", idx["apps"]!!) }) }
                "indexSichern" -> if (!s.alive) { put("ok", false); put("grund", "familie-weg") } else { s.index = a["index"]; s.indexRev++; put("ok", true); put("rev", s.indexRev) }
                "indexLaden" -> if (!s.alive) { put("ok", false); put("grund", "familie-weg") } else if (s.index == null) { put("ok", false); put("grund", "keine") } else { put("ok", true); put("rev", s.indexRev); put("daten", s.index!!) }
                "standLaden" -> if (!s.alive) { put("ok", false); put("grund", "familie-weg") } else { val st = s.staende[a["kind"]!!.jsonPrimitive.content]; if (st == null) { put("ok", false); put("grund", "keine") } else { put("ok", true); put("rev", st.second); put("daten", st.first) } }
                "standSichern" -> if (!s.alive) { put("ok", false); put("grund", "familie-weg") } else {
                    s.konfliktBeiNaechstemSichern?.let { s.konfliktBeiNaechstemSichern = null; it() }
                    val k = a["kind"]!!.jsonPrimitive.content; val cur = s.staende[k]?.second ?: 0L
                    if (a["rev"]!!.jsonPrimitive.long != cur) { put("ok", false); put("grund", "konflikt") } else { s.staende[k] = a["daten"]!!.jsonObject to cur + 1; put("ok", true); put("rev", cur + 1) } }
                "loeschen" -> { if (!s.alive) { put("ok", false); put("grund", "keine") } else { s.alive = false; s.staende.clear(); s.index = null; put("ok", true) } }
                "blatt" -> { put("ok", true); put("link", "https://lernen-test.mathenachhilfe-online.com/experimentopia/#familie=x"); put("nummer", "ABCD-EFGH"); put("blatt", "Notfall-Blatt …") }
            }
        }
    }

    private val ser = ListSerializer(ResultRow.serializer())
    private fun ergebnisse(vararg r: ResultRow) = Json.encodeToString(ser, r.toList())

    @Test fun anlegenUndSichern() {
        val s = Server(); val g = Geraet(s, "A"); g.write("ergebnisse", ergebnisse(ResultRow("x1", 3, true, 1, 10, 10)))
        val c = FamilyCoupling(g); var ok = false
        c.create("Lea") { ok = it }
        assertTrue(c.status.value, ok); assertNotNull(c.fam.value); assertEquals("ABCD-EFGH", c.nummer); assertEquals("KIND-A", g.read("kind"))
        assertTrue("Familie liegt auf dem Server", s.alive); assertEquals(1, s.indexRev)
        val st = s.staende["KIND-A"]!!.first
        assertEquals(setOf("ergebnisse"), st.keys); assertEquals(ergebnisse(ResultRow("x1", 3, true, 1, 10, 10)), st["ergebnisse"]!!.jsonPrimitive.content)
        assertTrue(c.status.value!!.startsWith("✓")); assertTrue(g.read("familiezeit") != null)
        assertFalse("keine Rechte, keine Einstellungen im Kind-Stand", st.keys.any { it == "procode" || it == "klang" })
    }
    @Test fun zweitesGeraetBeitretenUndAbgleichen() {
        val s = Server(); val a = Geraet(s, "A"); a.write("ergebnisse", ergebnisse(ResultRow("x1", 3, true, 1, 10, 10)))
        FamilyCoupling(a).create("Lea")
        val b = Geraet(s, "B"); b.write("ergebnisse", ergebnisse(ResultRow("x2", 2, false, 1, 20, 20)))
        val cb = FamilyCoupling(b); cb.join("https://x/#familie=A.B.C")
        assertEquals(listOf(FamilieJson.Kind("KIND-A", "Lea")), cb.kinder.value); assertNull("vor der Wahl nichts gespeichert", b.read("familie"))
        cb.choose("KIND-A")
        assertEquals("KIND-A", b.read("kind")); assertEquals(1, b.neuGeladen)                // neuer Stand von A → Neustart
        val lokal = Json.decodeFromString(ser, b.read("ergebnisse")!!).map { it.experimentId }.toSet(); assertEquals(setOf("x1", "x2"), lokal)   // nichts verloren
        val remote = Json.decodeFromString(ser, s.staende["KIND-A"]!!.first["ergebnisse"]!!.jsonPrimitive.content).map { it.experimentId }.toSet(); assertEquals(setOf("x1", "x2"), remote)
    }
    @Test fun beitrittMitNeuemKind() {
        val s = Server(); FamilyCoupling(Geraet(s, "A")).create("Lea")
        val b = Geraet(s, "B"); val c = FamilyCoupling(b); c.join("#familie=A.B.C"); c.chooseNew("Tim")
        assertEquals("KIND-NEU-B", b.read("kind")); assertEquals(2, s.index!!.jsonObject["kinder"]!!.jsonArray.size)
    }
    @Test fun falscherLinkOhneNetz() {
        val s = Server(); val b = Geraet(s, "B"); val c = FamilyCoupling(b)
        c.join("das ist kein link"); assertTrue(c.status.value!!.contains("kein Familien-Link")); assertNull(c.kinder.value)
        s.alive = false; c.join("#familie=A.B.C"); assertNull(b.read("familie")); assertTrue(c.status.value!!.contains("nicht"))
    }
    @Test fun konfliktWirdZusammengefuehrt_nieUeberschrieben() {
        val s = Server(); val a = Geraet(s, "A"); a.write("ergebnisse", ergebnisse(ResultRow("x1", 1, true, 1, 10, 10)))
        val c = FamilyCoupling(a); c.create("Lea")
        a.uhr += 120_000
        // anderes Gerät schreibt dazwischen einen neuen Stand
        a.write("ergebnisse", ergebnisse(ResultRow("x1", 1, true, 1, 10, 10), ResultRow("x3", 2, true, 1, 30, 30)))
        s.konfliktBeiNaechstemSichern = { s.staende["KIND-A"] = buildJsonObject { put("ergebnisse", ergebnisse(ResultRow("x9", 3, true, 1, 99, 99))) } to 5L }
        var ok = false; c.backup { ok = it }
        assertTrue(c.status.value, ok)
        val ids = Json.decodeFromString(ser, s.staende["KIND-A"]!!.first["ergebnisse"]!!.jsonPrimitive.content).map { it.experimentId }.toSet()
        assertEquals("beide Seiten bleiben erhalten", setOf("x1", "x3", "x9"), ids); assertEquals(6L, s.staende["KIND-A"]!!.second)
    }
    @Test fun ohneNetzEsGehtSpaeterWeiter() {
        val s = Server(); val a = Geraet(s, "A"); a.write("abzeichen", "a")
        val c = FamilyCoupling(a); c.create(""); s.offline = true; a.uhr += 120_000
        var ok = true; c.backup { ok = it }; assertFalse(ok); assertTrue(c.status.value!!.contains("kein Internet")); assertNotNull("Familie bleibt verbunden", c.fam.value)
        s.offline = false; c.backup { ok = it }; assertTrue(ok)
    }
    @Test fun nichtFreigeschaltetUndDoppelterStart() {
        val s = Server(); s.gesperrt = true; val a = Geraet(s, "A"); val c = FamilyCoupling(a); var ok = true
        c.create("") { ok = it }; assertFalse(ok); assertTrue(c.status.value!!.contains("noch nicht freigeschaltet")); assertNull("nichts gespeichert, nichts hochgeladen", a.read("familie")); assertTrue(s.staende.isEmpty())
    }
    @Test fun loeschenBehaeltDenLernstand() {
        val s = Server(); val a = Geraet(s, "A"); a.write("abzeichen", "a,b"); val c = FamilyCoupling(a); c.create("Lea")
        var ok = false; c.delete { ok = it }
        assertTrue(ok); assertFalse(s.alive); assertNull(c.fam.value); listOf("familie", "familienummer", "kind", "familierev").forEach { assertNull(it, a.read(it)) }
        assertEquals("Lernstand bleibt auf diesem Gerät", "a,b", a.read("abzeichen"))
    }
    @Test fun familieAufAnderemGeraetGeloescht() {
        val s = Server(); val a = Geraet(s, "A"); a.write("abzeichen", "a"); val c = FamilyCoupling(a); c.create("")
        s.alive = false; a.uhr += 120_000; var ok = true; c.backup { ok = it }
        assertFalse(ok); assertEquals(Familie.FAMILIE_WEG, c.status.value); assertNull(c.fam.value); assertEquals("a", a.read("abzeichen"))
    }
    @Test fun automatischNurAlleSechzigSekunden() {
        val s = Server(); val a = Geraet(s, "A"); val c = FamilyCoupling(a); c.create("")
        val n = s.aufrufe.size; var ok = true; c.backup(manual = false) { ok = it }; assertFalse(ok); assertEquals("zu früh: kein Netzaufruf", n, s.aufrufe.size)
        a.uhr += 61_000; c.backup(manual = false) { ok = it }; assertTrue(ok)
    }
}
