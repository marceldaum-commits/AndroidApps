package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature

/** Freischalt-Codes: echte Signatur gegen den Schlüssel, falsche App und falscher Schlüssel werden abgelehnt. */
class LicenseTest {
    private val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val pk = kp.public.encoded.takeLast(32).joinToString("") { "%02x".format(it) }
    private fun code(app: Int, product: Int = 1, serial: Long = 1001, day: Int = 2000): String {
        val payload = byteArrayOf(1, app.toByte(), product.toByte(), (serial shr 24).toByte(), (serial shr 16).toByte(), (serial shr 8).toByte(), serial.toByte(), (day shr 8).toByte(), day.toByte())
        val sig = Signature.getInstance("Ed25519").apply { initSign(kp.private); update(payload) }.sign()
        return License.encode(payload + sig)
    }
    @Test fun echterCodeGilt() {
        val c = code(Pro.APP_ID)
        assertTrue(c.startsWith("EX-"))
        assertEquals(1001L, License.check(c, pk, 20_000, Pro.APP_ID)?.serial)
        // auch aus einem Freischalt-Link
        assertNotNull(License.check(License.extract("https://experimentopia.mathenachhilfe-online.com/?pro=" + c.replace("-", "")), pk, 20_000, Pro.APP_ID))
    }
    @Test fun falscheAppOderSchluessel() {
        assertNull(License.check(code(1), pk, 20_000, Pro.APP_ID))           // Kurvenflug-Code
        assertNull(License.check(code(Pro.APP_ID), "00".repeat(32), 20_000, Pro.APP_ID))
        assertNull(License.check("EX-12345", pk, 20_000, Pro.APP_ID))
    }
}
