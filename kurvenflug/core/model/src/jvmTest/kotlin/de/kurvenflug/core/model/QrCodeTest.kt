package de.kurvenflug.core.model

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals

/** QR-Code für „Eltern-Handy koppeln“: am 18.11. Modul für Modul gegen die Python-Bibliothek „qrcode“ (Fehlerkorrektur M, alle 8 Masken,
 *  Versionen 1, 4, 8, 10) geprüft – identisch. Dieser Test hält das Ergebnis fest. */
class QrCodeTest {
    @Test fun matchesReference() {
        val link = "https://staging.mathenachhilfe-online.com/eltern-bericht/#AAAAAAAAAAAAAAAAAAAAAA.BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
        val q = QrCode.encode(link)
        assertEquals(49, q.size)   // Version 8
        val bits = (0 until q.size).joinToString("") { y -> (0 until q.size).joinToString("") { x -> if (q[x, y]) "1" else "0" } }
        val h = MessageDigest.getInstance("SHA-256").digest(bits.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
        assertEquals("69a70fa48fedca82", h)
    }
}
