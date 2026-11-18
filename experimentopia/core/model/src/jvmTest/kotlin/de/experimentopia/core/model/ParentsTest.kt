package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import kotlinx.serialization.json.*

/** Eltern-Kopplung: Verschlüsselung wie WebCrypto, QR-Code lesbar, Bericht nach Spezifikation. */
class ParentsTest {
    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(2 * it, 2 * it + 2).toInt(16).toByte() }
    private fun ByteArray.hex() = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    @Test fun aesGcmNormwerte() {
        // GCM-Spezifikation, Testfälle 13 und 14 (Schlüssel und IV null)
        assertEquals("530f8afbc74536b9a963b4f1c4cb738b", AesGcm.encrypt(ByteArray(32), ByteArray(12), ByteArray(0)).hex())
        assertEquals("cea7403d4d606b6e074ec5d3baf39d18d0d1c8a799996bf0265b98b5d48ab919", AesGcm.encrypt(ByteArray(32), ByteArray(12), ByteArray(16)).hex())
    }

    @Test fun aesGcmWieJava() {
        val r = Random(42)
        listOf(0, 1, 15, 16, 17, 100, 1000, 5000).forEach { n ->
            val key = r.nextBytes(32); val iv = r.nextBytes(12); val pt = r.nextBytes(n)
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv)) }
            assertArrayEquals("Länge $n", c.doFinal(pt), AesGcm.encrypt(key, iv, pt))
        }
    }

    @Test fun base64url() {
        val r = Random(1)
        repeat(50) { val b = r.nextBytes(r.nextInt(0, 40)); val e = B64u.encode(b)
            assertEquals(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b), e); assertArrayEquals(b, B64u.decode(e)) }
        assertEquals(22, B64u.encode(ByteArray(16)).length); assertEquals(43, B64u.encode(ByteArray(32)).length); assertEquals(16, B64u.encode(ByteArray(12)).length)
    }

    @Test fun kopplung() {
        val r = Random(7)
        val l = ParentLink.create({ r.nextBytes(it) }, test = true, nickname = "  Mia  ")
        assertEquals(22, l.id.length); assertEquals(22, l.write.length); assertEquals(43, l.key.length); assertEquals("Mia", l.nickname)
        assertEquals("https://trainer-test.mathenachhilfe-online.com/eltern/v1/b/${l.id}", l.uploadUrl)
        assertEquals("https://staging.mathenachhilfe-online.com/eltern-bericht/#${l.id}.${l.key}", l.parentUrl)
        assertEquals("https://www.mathenachhilfe-online.com/eltern-bericht/#${l.id}.${l.key}", l.copy(test = false).parentUrl)
        assertEquals(l, ParentLink.parse(l.encode()))
        assertTrue(ParentLink.isTestHost("experimentopia-test.mathenachhilfe-online.com")); assertFalse(ParentLink.isTestHost("experimentopia.mathenachhilfe-online.com"))
    }

    private fun decodeQr(q: Qr): String {
        val px = 6; val quiet = 4; val n = (q.size + 2 * quiet) * px
        val img = java.awt.image.BufferedImage(n, n, java.awt.image.BufferedImage.TYPE_INT_RGB)
        for (y in 0 until n) for (x in 0 until n) { val mx = x / px - quiet; val my = y / px - quiet
            img.setRGB(x, y, if (mx in 0 until q.size && my in 0 until q.size && q[mx, my]) 0 else 0xFFFFFF) }
        val bmp = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(com.google.zxing.client.j2se.BufferedImageLuminanceSource(img)))
        return com.google.zxing.qrcode.QRCodeReader().decode(bmp).text
    }

    @Test fun qrCodeLesbar() {
        val r = Random(3)
        listOf("A", "Hallo Experimentopia", "https://staging.mathenachhilfe-online.com/eltern-bericht/#" + ParentLink.create({ r.nextBytes(it) }, true).let { "${it.id}.${it.key}" },
            "x".repeat(150), "Ä ö ü ß – 😀").forEach { t -> val q = Qr.encode(t)!!; assertEquals(t, decodeQr(q)) }
        assertNull(Qr.encode("x".repeat(400)))
    }

    @Test fun bericht() {
        val day = 86_400_000L; val now = 20_400L * day + 15 * 3_600_000L
        val ids = Topic.OPTIK.experiments().map { it.id } + Topic.STROM.experiments().take(4).map { it.id }
        val j = ids.mapIndexed { i, id -> NoteRow(i.toLong(), id, now - (i % 3) * day - i * 120_000L, "g", "r", right = id.startsWith("x"), sure = 1, explainedFirstTry = true) } +
            NoteRow(99, "w1", now - 100 * day, "g", "r", true, 1, true)   // zu alt für den Zeitraum
        val res = ids.map { ResultRow(it, 2, true) }
        val json = ParentReport.build(res, j, 7, "Mia", now)
        assertTrue(json.encodeToByteArray().size < ParentReport.MAX_BYTES)
        val o = Json.parseToJsonElement(json).jsonObject
        assertEquals(1, o["format"]!!.jsonPrimitive.int); assertEquals("experimentopia", o["app"]!!.jsonPrimitive.content); assertEquals("Experimentopia", o["appName"]!!.jsonPrimitive.content)
        assertEquals("Mia", o["name"]!!.jsonPrimitive.content); assertEquals(7, o["klasse"]!!.jsonPrimitive.int)
        assertEquals(ParentReport.iso(now), o["stand"]!!.jsonPrimitive.content); assertTrue(o["stand"]!!.jsonPrimitive.content.matches(Regex("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\dZ")))
        assertEquals(ids.size, o["aufgaben"]!!.jsonPrimitive.int); assertEquals(3, o["tageAktiv"]!!.jsonPrimitive.int)
        assertEquals((11 * 100 + 7) / 15, o["richtigProzent"]!!.jsonPrimitive.int)
        assertEquals("physik-optik", o["staerken"]!!.jsonArray[0].jsonObject["thema"]!!.jsonPrimitive.content)
        assertEquals("physik-elektrizitaet", o["schwach"]!!.jsonArray[0].jsonObject["thema"]!!.jsonPrimitive.content)
        assertEquals(15, o["fortschritt"]!!.jsonArray.first { it.jsonObject["titel"]!!.jsonPrimitive.content == "Physik" }.jsonObject["geschafft"]!!.jsonPrimitive.int)
        assertEquals(3, o["letzte"]!!.jsonArray.size)
        // ohne Spitzname und Klasse: nur Pflichtfelder plus Zahlen, keine Personendaten
        val bare = Json.parseToJsonElement(ParentReport.build(emptyList(), emptyList(), 0, "", now)).jsonObject
        assertNull(bare["name"]); assertNull(bare["klasse"]); assertEquals(0, bare["aufgaben"]!!.jsonPrimitive.int)
    }

    @Test fun lernzeitUndDatum() {
        assertEquals(0, ParentReport.minutes(emptyList()))
        assertEquals(3, ParentReport.minutes(listOf(0L)))
        assertEquals(13, ParentReport.minutes(listOf(0L, 5 * 60_000L, 10 * 60_000L)))
        assertEquals(6, ParentReport.minutes(listOf(0L, 60 * 60_000L)))   // zwei getrennte Sitzungen
        assertEquals("1970-01-01T00:00:00Z", ParentReport.iso(0)); assertEquals("2026-11-18T15:44:47Z", ParentReport.iso(1795016687000L))
    }

    @Test fun versiegeltUndEntschluesselbar() {
        val r = Random(9); val l = ParentLink.create({ r.nextBytes(it) }, true); val iv = r.nextBytes(12)
        val sealed = ParentReport.seal("""{"format":1}""", l.key, iv)
        val (ivS, ct) = sealed.split(".")
        assertEquals(16, ivS.length)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(B64u.decode(l.key), "AES"), GCMParameterSpec(128, B64u.decode(ivS))) }
        assertEquals("""{"format":1}""", String(c.doFinal(B64u.decode(ct))))
    }
}

/** Für den Rundlauf-Test (tools/eltern-rundlauf.mjs): legt mit RUNDLAUF=<Datei> einen echt versiegelten Bericht ab */
class ParentsRoundtripExport {
    @Test fun export() {
        val out = System.getenv("RUNDLAUF") ?: return
        val r = java.security.SecureRandom()
        val l = ParentLink.create({ ByteArray(it).also { b -> r.nextBytes(b) } }, test = true, nickname = "Rundlauf")
        val now = System.currentTimeMillis()
        val j = Catalog.all.take(6).mapIndexed { i, e -> NoteRow(i.toLong(), e.id, now - i * 300_000L, "g", "r", i % 2 == 0, 1, true) }
        val json = ParentReport.build(j.map { ResultRow(it.experimentId, 2, it.right) }, j, 5, l.nickname, now)
        val body = ParentReport.seal(json, l.key, ByteArray(12).also { r.nextBytes(it) })
        java.io.File(out).writeText(buildJsonObject { put("id", l.id); put("write", l.write); put("key", l.key); put("body", body); put("json", json); put("parentUrl", l.parentUrl) }.toString())
    }
}

/** Sicherung des Lernstands: Entschlüsseln, Aufbau nach Spezifikation, Link einlesen, Monatssummen */
class BackupTest {
    private val r = Random(11)
    @Test fun entschluesseln() {
        val key = r.nextBytes(32); val iv = r.nextBytes(12); val pt = "Hallo Sicherung äöü".encodeToByteArray()
        val ct = AesGcm.encrypt(key, iv, pt)
        assertArrayEquals(pt, AesGcm.decrypt(key, iv, ct))
        assertNull("falscher Schlüssel", AesGcm.decrypt(r.nextBytes(32), iv, ct))
        assertNull("verändert", AesGcm.decrypt(key, iv, ct.copyOf().also { it[0] = (it[0] + 1).toByte() }))
    }

    @Test fun aufbauUndLesen() {
        val l = ParentLink.create({ r.nextBytes(it) }, true, "Mia")
        val data = mapOf("experimentopia.ergebnisse" to "[]", "experimentopia.procode" to "EX-123", "experimentopia.klasse" to "5")
        val plain = Backup.build("2.9.0", 1_795_538_000_000L, 3_600_000L, l.write, l.nickname, data)
        val o = Json.parseToJsonElement(plain).jsonObject
        assertEquals(1, o["format"]!!.jsonPrimitive.int); assertEquals("experimentopia", o["app"]!!.jsonPrimitive.content); assertEquals("2.9.0", o["v"]!!.jsonPrimitive.content)
        assertTrue(o["stand"]!!.jsonPrimitive.content.endsWith("+01:00")); assertEquals(l.write, o["schreib"]!!.jsonPrimitive.content)
        // über den Server-Weg: versiegeln, als {"d": …} zurück, öffnen, lesen
        val sealed = ParentReport.seal(plain, l.key, r.nextBytes(12))
        val back = Backup.read(Backup.open("""{"d":"$sealed","t":"x"}""", l.key)!!) as Backup.Read.Ok
        assertEquals(data, back.data); assertEquals(l.write, back.write); assertEquals("Mia", back.nickname)
        assertNull(Backup.open("""{"d":"$sealed"}""", ParentLink.create({ r.nextBytes(it) }, true).key))
        assertEquals(Backup.Read.OtherApp, Backup.read(plain.replace("\"experimentopia\"", "\"rechenblitz\"")))
        assertEquals(Backup.hash("Mia", data), Backup.hash("Mia", data.toList().reversed().toMap())); assertNotEquals(Backup.hash("Mia", data), Backup.hash("Mia", data + ("x" to "1")))
        assertEquals("https://trainer-test.mathenachhilfe-online.com/eltern/v1/s/${l.id}", l.backupUrl)
    }

    @Test fun linkEinlesen() {
        val l = ParentLink.create({ r.nextBytes(it) }, true)
        assertEquals(PastedLink(l.id, l.key, true), Backup.parseLink("  " + l.parentUrl + "\n"))
        assertEquals(PastedLink(l.id, l.key, false), Backup.parseLink(l.copy(test = false).parentUrl))
        assertEquals(PastedLink(l.id, l.key, null), Backup.parseLink("${l.id}.${l.key}"))
        assertNull(Backup.parseLink("https://example.com")); assertNull(Backup.parseLink("${l.id}.${l.key.dropLast(1)}"))
    }

    @Test fun kuerzen() {
        val now = 100L * 86_400_000L
        val j = Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(NoteRow.serializer()), listOf(
            NoteRow(1, "x1", now - 10 * 86_400_000L, "g", "r", true, 1, true), NoteRow(2, "x2", now - 95 * 86_400_000L, "g", "r", true, 1, true)))
        val out = Backup.shrink(mapOf("j" to j, "k" to "v"), "j", now)
        assertEquals(1, Json.parseToJsonElement(out["j"]!!).jsonArray.size); assertEquals("v", out["k"])
    }

    @Test fun monatssummen() {
        val oct = 20_390L * 86_400_000L; val nov = 20_410L * 86_400_000L   // 2025-10-28 / 2025-11-17
        val j = listOf(NoteRow(1, "x1", oct, "g", "r", true, 1, true), NoteRow(2, "x2", oct + 300_000, "g", "r", false, 1, true), NoteRow(3, "x3", nov, "g", "r", true, 1, true))
        val m = Monthly.merge(emptyMap(), j, 0)
        assertEquals(listOf("2025-10", "2025-11"), m.keys.toList()); assertEquals(MonthSum(8, 2, 1), m["2025-10"]); assertEquals(MonthSum(3, 1, 1), m["2025-11"])
        // Forscherbuch hat den Oktober abgegeben: die Summe bleibt
        val later = Monthly.merge(m, j.drop(2), 0)
        assertEquals(MonthSum(8, 2, 1), later["2025-10"]); assertEquals(m, Monthly.decode(Monthly.encode(later)))
        val many = (1..15).associate { "2025-${it.toString().padStart(2, '0')}".let { k -> (if (it > 12) "2026-0${it - 12}" else k) to MonthSum(1, 1, 1) } }
        assertEquals(12, Monthly.verlauf(many).size); assertEquals("2025-04", Monthly.verlauf(many).first().key)
        // im Bericht
        val o = Json.parseToJsonElement(ParentReport.build(emptyList(), j, 5, "", nov + 1000, 0, m)).jsonObject
        val v = o["verlauf"]!!.jsonArray
        assertEquals("2025-10", v[0].jsonObject["monat"]!!.jsonPrimitive.content); assertEquals(50, v[0].jsonObject["richtigProzent"]!!.jsonPrimitive.int); assertEquals(8, v[0].jsonObject["min"]!!.jsonPrimitive.int)
    }
}
