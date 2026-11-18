package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

/** Der selbst gebaute QR-Code: Feld für Feld gleich mit einer Referenz, und der Inhalt lässt sich wieder herauslesen. */
class QrTest {
    /** Referenz (Bibliothek segno, Version 6-L, Muster 3) für einen randvollen Text von 134 Zeichen – jede Zeile als Hex-Zahl */
    private val text = "https://www.mathenachhilfe-online.com/eltern-bericht/#aB3_-xyZ.https://www.mathenachhilfe-online.com/eltern-bericht/#aB3_-xyZ.https://"
    private val reference = listOf("1fdb8fa1f7f", "104a95b1941", "1759a47e55d", "1750cc68a5d", "1750e31fa5d", "104dd215241", "1fd5555557f", "42c77d00", "1e5d6f87c9d", "580f739f7b", "ff150f9c94", "1d9ab758362", "3d7f93c82d", "17229a9fa4b", "7458a0cae7", "32a208205a", "f45b7ea71a", "f2ff1bb128", "1d564cb8e2", "1e3b6f5190d", "1dd046b5cd4", "120fdfa9ff9", "185b48648ca", "137d05c031", "11e2487c82f", "2a51e1fa47", "12fccbd56f5", "61c25b5121", "145a93e81b8", "1b26572920", "16e5b1ce54c", "5a8597799d", "ccf3887ff7", "1e16e9f15", "1fca5160f52", "1048b53c519", "1746e2789f6", "1750a6573bc", "17554edc204", "105312a5162", "1fdaa1ce752")

    @Test fun gleichMitDerReferenz() {
        val q = Qr.encode(text, fixedMask = 3)!!
        assertEquals(41, q.size); assertEquals(reference, q.map { r -> r.fold(java.math.BigInteger.ZERO) { a, b -> a.shiftLeft(1).add(if (b) java.math.BigInteger.ONE else java.math.BigInteger.ZERO) }.toString(16) })
    }

    /** Liest die Datenwörter wieder aus (Muster 0) – so wie ein Lesegerät */
    private fun read(q: List<BooleanArray>): List<Int> {
        val n = q.size; val fn = Array(n) { BooleanArray(n) }
        for (i in 0 until n) { fn[6][i] = true; fn[i][6] = true }
        for ((cx, cy) in listOf(3 to 3, n - 4 to 3, 3 to n - 4)) for (dy in -4..4) for (dx in -4..4) { val x = cx + dx; val y = cy + dy; if (x in 0 until n && y in 0 until n) fn[y][x] = true }
        for (i in 0..8) { fn[8][i] = true; fn[i][8] = true }; for (i in 0..7) { fn[8][n - 1 - i] = true; fn[n - 1 - i][8] = true }
        if (n >= 25) { val a = n - 7; for (dy in -2..2) for (dx in -2..2) fn[a + dy][a + dx] = true }      // ein Ausrichtungsmuster (Versionen 2 bis 6)
        val bits = ArrayList<Int>(); var right = n - 1
        while (right >= 1) { if (right == 6) right = 5
            for (vert in 0 until n) for (j in 0..1) { val x = right - j; val up = ((right + 1) and 2) == 0; val y = if (up) n - 1 - vert else vert
                if (!fn[y][x]) bits += (if (q[y][x]) 1 else 0) xor (if ((x + y) % 2 == 0) 1 else 0) }
            right -= 2 }
        val cw = bits.chunked(8).filter { it.size == 8 }.map { it.fold(0) { a, b -> (a shl 1) or b } }
        // Version 6 (41 Felder) verschränkt zwei Blöcke zu je 68 Datenwörtern: abwechselnd Block 1, Block 2
        return if (n == 41) (0 until 68).map { cw[it * 2] } + (0 until 68).map { cw[it * 2 + 1] } else cw
    }

    @Test fun kopplungsLinkLaesstSichWiederLesen() {
        for (link in listOf("https://www.mathenachhilfe-online.com/eltern-bericht/#" + "aB3_-xyZ01".repeat(2) + "Qw." + "Zk9-".repeat(10) + "abc",
            "https://staging.mathenachhilfe-online.com/eltern-bericht/#" + "q".repeat(22) + "." + "W".repeat(43), "Hallo Welt")) {
            val q = Qr.encode(link, fixedMask = 0)!!; assertTrue(q.size in listOf(21, 41)); assertTrue(q.all { it.size == q.size })
            val cw = read(q); assertEquals(4, cw[0] shr 4)                                              // Modus „Byte“
            val len = ((cw[0] and 15) shl 4) or (cw[1] shr 4); assertEquals(link.encodeToByteArray().size, len)
            val bytes = ByteArray(len) { i -> (((cw[i + 1] and 15) shl 4) or (cw[i + 2] shr 4)).toByte() }; assertEquals(link, bytes.decodeToString())
        }
    }

    @Test fun grenzenUndWahlDesMusters() {
        assertNull(Qr.encode("x".repeat(Qr.MAX_BYTES + 1))); assertEquals(53, Qr.encode("x".repeat(Qr.MAX_BYTES))!!.size); assertEquals(21, Qr.encode("")!!.size)
        // ohne Vorgabe wählt der Code selbst ein Muster – und liefert jedes Mal dasselbe Bild
        val a = Qr.encode("https://example.org/#abc")!!; val b = Qr.encode("https://example.org/#abc")!!; assertEquals(a.map { it.toList() }, b.map { it.toList() })
        assertTrue((0..7).any { k -> Qr.encode("https://example.org/#abc", k)!!.map { it.toList() } == a.map { it.toList() } })
        // die drei Suchmuster stehen in den Ecken
        for ((x, y) in listOf(0 to 0, a.size - 7 to 0, 0 to a.size - 7)) { assertTrue(a[y][x] && a[y + 6][x + 6] && a[y + 3][x + 3]); assertFalse(a[y + 1][x + 1]) }
    }
}
