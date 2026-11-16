package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import kotlin.random.Random

/** Eigene Krypto gegen die des JDK: SHA-512 und Ed25519 mit Zufallsdaten. */
class Ed25519CrossTest {
    @Test fun sha512MatchesJdk() {
        val r = Random(7)
        for (n in listOf(0, 1, 111, 112, 127, 128, 129, 255, 256, 1000)) {
            val m = r.nextBytes(n)
            assertArrayEquals("Länge $n", MessageDigest.getInstance("SHA-512").digest(m), Sha512.hash(m))
        }
    }

    @Test fun ed25519MatchesJdk() {
        val r = Random(11)
        repeat(20) { round ->
            val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
            val pk = kp.public.encoded.takeLast(32).toByteArray()   // X.509: 12 Byte Kopf + 32 Byte Schlüssel
            val msg = r.nextBytes(r.nextInt(0, 100))
            val sig = Signature.getInstance("Ed25519").apply { initSign(kp.private); update(msg) }.sign()
            assertTrue("Runde $round gültig", Ed25519.verify(msg, sig, pk))
            val bad = sig.copyOf().also { it[r.nextInt(64)] = (it[r.nextInt(64)] + 1).toByte() }
            if (!bad.contentEquals(sig)) assertFalse("Runde $round gefälscht", Ed25519.verify(msg, bad, pk))
            if (msg.isNotEmpty()) assertFalse("Runde $round andere Nachricht", Ed25519.verify(msg + 1, sig, pk))
        }
    }
}
