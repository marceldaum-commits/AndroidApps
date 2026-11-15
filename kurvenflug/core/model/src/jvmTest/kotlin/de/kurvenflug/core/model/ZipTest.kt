package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Das Kotlin-Deflate (Browser) muss mit java.util.zip (Android) in beide Richtungen zusammenpassen. */
class ZipTest {
    private val samples = listOf("", "a", "Hallo Welt! Hallo Welt! Hallo Welt!", "äöü ß 🚀 " .repeat(40),
        """{"title":"Hausaufgabe 8b","refs":["2-6","3-1"],"order":["r0","r1"]}""".repeat(5)) +
        List(20) { i -> val r = Random(i); String(CharArray(r.nextInt(1, 3000)) { "abcdefgh {}:,0123456789"[r.nextInt(if (i % 2 == 0) 4 else 23)] }) }

    @Test fun pureDeflateIsReadableByJava() {
        for (s in samples) assertEquals(s, zipInflate(PureZip.deflate(s.encodeToByteArray())).decodeToString())
    }

    @Test fun javaDeflateIsReadableByPureInflate() {
        for (s in samples) assertEquals(s, PureZip.inflate(zipDeflate(s.encodeToByteArray())).decodeToString())
    }

    @Test fun brokenDataFailsCleanly() {
        val r = Random(1)
        repeat(300) { runCatching { PureZip.inflate(ByteArray(r.nextInt(1, 60)) { r.nextInt().toByte() }) } }
        assertNull(Codec.decodeLevel("KFL1:kaputt"))
    }

    /** Werte für den Gleichstands-Test erzeugen (einmalig von Hand ausgeführt) */
    @Test fun printParityValues() {
        val code = Codec.encodeChallenge(ChallengeSpec("Hausaufgabe", refs = listOf("2-6", "3-1"), order = listOf("r0", "r1")))
        val fp = ParityTest().fingerprint()
        File(System.getProperty("java.io.tmpdir"), "parity.txt").writeText(
            "EXPECTED=" + stableHash(fp).toString(16) + "/" + fp.length + "\nCODE=" + code + "\nCHECK=" + ChallengeResult.checkOf("Test", "Mia", listOf(1, 2, 1), "15.11.2026") + "\n")
    }
}
