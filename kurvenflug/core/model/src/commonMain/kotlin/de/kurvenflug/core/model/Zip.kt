package de.kurvenflug.core.model

/** Rohes Deflate (RFC 1951, ohne Kopf) – auf Android über java.util.zip, im Browser über [PureZip]. */
expect fun zipDeflate(data: ByteArray): ByteArray
/** Entpackt rohes Deflate; wirft bei kaputten Daten oder mehr als [limit] Bytes. */
expect fun zipInflate(data: ByteArray, limit: Int = 100_000): ByteArray

/**
 * Deflate/Inflate in reinem Kotlin – damit geteilte Codes (Level, Challenges, Ergebnisse) zwischen
 * Handy und Browser hin und her funktionieren. Packt mit festen Huffman-Codes und LZ77.
 */
object PureZip {
    private val LEN_BASE = intArrayOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
    private val LEN_EXTRA = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)
    private val DIST_BASE = intArrayOf(1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577)
    private val DIST_EXTRA = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)
    private val CL_ORDER = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

    // ------------------------------------------------------------------ Packen

    private class BitOut {
        var buf = ByteArray(256); var size = 0; var acc = 0; var n = 0
        fun bits(v: Int, count: Int) {
            acc = acc or ((v and ((1 shl count) - 1)) shl n); n += count
            while (n >= 8) { put(acc and 0xFF); acc = acc ushr 8; n -= 8 }
        }
        /** Huffman-Code: höchstes Bit zuerst */
        fun code(c: Int, len: Int) { var r = 0; for (i in 0 until len) r = r or (((c shr i) and 1) shl (len - 1 - i)); bits(r, len) }
        fun put(b: Int) { if (size == buf.size) buf = buf.copyOf(size * 2); buf[size++] = b.toByte() }
        fun finish(): ByteArray { if (n > 0) put(acc and 0xFF); acc = 0; n = 0; return buf.copyOf(size) }
    }

    private fun lit(o: BitOut, v: Int) = when {
        v < 144 -> o.code(0x30 + v, 8)
        v < 256 -> o.code(0x190 + v - 144, 9)
        v < 280 -> o.code(v - 256, 7)
        else -> o.code(0xC0 + v - 280, 8)
    }

    fun deflate(data: ByteArray): ByteArray {
        val o = BitOut()
        o.bits(1, 1); o.bits(1, 2) // letzter Block, feste Huffman-Codes
        val head = IntArray(1 shl 15) { -1 }; val prev = IntArray(data.size.coerceAtLeast(1))
        fun hash(i: Int) = (((data[i].toInt() and 0xFF) shl 10) xor ((data[i + 1].toInt() and 0xFF) shl 5) xor (data[i + 2].toInt() and 0xFF)) and 0x7FFF
        fun insert(i: Int) { if (i + 2 < data.size) { val h = hash(i); prev[i] = head[h]; head[h] = i } }
        var i = 0
        while (i < data.size) {
            var bestLen = 0; var bestDist = 0
            if (i + 2 < data.size) {
                var j = head[hash(i)]; var chain = 0
                while (j >= 0 && i - j <= 32768 && chain++ < 128) {
                    var l = 0
                    while (l < 258 && i + l < data.size && data[j + l] == data[i + l]) l++
                    if (l > bestLen) { bestLen = l; bestDist = i - j; if (l == 258) break }
                    j = prev[j]
                }
            }
            if (bestLen >= 3) {
                var c = LEN_BASE.size - 1; while (LEN_BASE[c] > bestLen) c--
                lit(o, 257 + c); o.bits(bestLen - LEN_BASE[c], LEN_EXTRA[c])
                var d = DIST_BASE.size - 1; while (DIST_BASE[d] > bestDist) d--
                o.code(d, 5); o.bits(bestDist - DIST_BASE[d], DIST_EXTRA[d])
                repeat(bestLen) { insert(i); i++ }
            } else { lit(o, data[i].toInt() and 0xFF); insert(i); i++ }
        }
        lit(o, 256)
        return o.finish()
    }

    // ------------------------------------------------------------------ Entpacken

    private class Huff(lengths: IntArray, n: Int) {
        val count = IntArray(16); val symbol = IntArray(n)
        init {
            for (s in 0 until n) count[lengths[s]]++
            count[0] = 0
            val offs = IntArray(16); for (l in 1 until 16) offs[l] = offs[l - 1] + count[l - 1]
            for (s in 0 until n) if (lengths[s] != 0) symbol[offs[lengths[s]]++] = s
        }
    }

    private class BitIn(val d: ByteArray) {
        var pos = 0; var acc = 0; var n = 0
        fun bits(need: Int): Int {
            var v = acc
            while (n < need) { require(pos < d.size) { "Daten zu kurz" }; v = v or ((d[pos++].toInt() and 0xFF) shl n); n += 8 }
            acc = v ushr need; n -= need
            return v and ((1 shl need) - 1)
        }
        fun decode(h: Huff): Int {
            var code = 0; var first = 0; var index = 0
            for (len in 1 until 16) {
                code = code or bits(1); val c = h.count[len]
                if (code - c < first) return h.symbol[index + (code - first)]
                index += c; first = (first + c) shl 1; code = code shl 1
            }
            throw IllegalArgumentException("Ungültiger Code")
        }
    }

    private val fixedLit by lazy { Huff(IntArray(288) { when { it < 144 -> 8; it < 256 -> 9; it < 280 -> 7; else -> 8 } }, 288) }
    private val fixedDist by lazy { Huff(IntArray(30) { 5 }, 30) }

    fun inflate(data: ByteArray, limit: Int = 100_000): ByteArray {
        val inp = BitIn(data)
        var out = ByteArray(1024); var size = 0
        fun put(b: Int) { require(size < limit) { "Zu groß" }; if (size == out.size) out = out.copyOf(minOf(out.size * 2, limit + 1)); out[size++] = b.toByte() }
        do {
            val last = inp.bits(1); val type = inp.bits(2)
            when (type) {
                0 -> {
                    inp.acc = 0; inp.n = 0
                    require(inp.pos + 4 <= data.size)
                    val len = (data[inp.pos].toInt() and 0xFF) or ((data[inp.pos + 1].toInt() and 0xFF) shl 8)
                    inp.pos += 4
                    require(inp.pos + len <= data.size)
                    repeat(len) { put(data[inp.pos++].toInt() and 0xFF) }
                }
                1, 2 -> {
                    val (lit, dist) = if (type == 1) fixedLit to fixedDist else {
                        val nlen = inp.bits(5) + 257; val ndist = inp.bits(5) + 1; val ncode = inp.bits(4) + 4
                        val cl = IntArray(19); for (k in 0 until ncode) cl[CL_ORDER[k]] = inp.bits(3)
                        val clh = Huff(cl, 19)
                        val lens = IntArray(nlen + ndist); var k = 0
                        while (k < nlen + ndist) {
                            val sym = inp.decode(clh)
                            when {
                                sym < 16 -> lens[k++] = sym
                                sym == 16 -> { require(k > 0); val p = lens[k - 1]; repeat(3 + inp.bits(2)) { lens[k++] = p } }
                                sym == 17 -> repeat(3 + inp.bits(3)) { lens[k++] = 0 }
                                else -> repeat(11 + inp.bits(7)) { lens[k++] = 0 }
                            }
                        }
                        Huff(lens.copyOfRange(0, nlen), nlen) to Huff(lens.copyOfRange(nlen, nlen + ndist), ndist)
                    }
                    while (true) {
                        val sym = inp.decode(lit)
                        if (sym < 256) put(sym)
                        else if (sym == 256) break
                        else {
                            val li = sym - 257; require(li < 29)
                            val len = LEN_BASE[li] + inp.bits(LEN_EXTRA[li])
                            val di = inp.decode(dist); require(di < 30)
                            val d = DIST_BASE[di] + inp.bits(DIST_EXTRA[di])
                            require(d <= size) { "Abstand zu groß" }
                            repeat(len) { put(out[size - d].toInt() and 0xFF) }
                        }
                    }
                }
                else -> throw IllegalArgumentException("Ungültiger Block")
            }
        } while (last == 0)
        return out.copyOf(size)
    }
}
