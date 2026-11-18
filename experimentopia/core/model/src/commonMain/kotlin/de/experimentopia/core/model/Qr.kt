package de.experimentopia.core.model

import kotlin.math.abs

/**
 * QR-Code-Erzeuger (Byte-Modus, Fehlerkorrektur M, Version 1–10) nach ISO/IEC 18004 –
 * angelehnt an den Aufbau von Nayukis „QR Code generator“ (MIT). Reicht für Links bis etwa 210 Zeichen.
 * Geprüft durch Dekodieren mit ZXing ([QrTest]).
 */
class Qr private constructor(val size: Int) {
    private val dark = Array(size) { BooleanArray(size) }
    private val func = Array(size) { BooleanArray(size) }
    /** true = dunkles Feld (Zeile [y], Spalte [x]) */
    operator fun get(x: Int, y: Int) = dark[y][x]

    companion object {
        private val ECC_M = intArrayOf(-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26)
        private val BLOCKS_M = intArrayOf(-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5)
        const val MAX_VERSION = 10

        private fun rawModules(ver: Int): Int {
            var r = (16 * ver + 128) * ver + 64
            if (ver >= 2) { val n = ver / 7 + 2; r -= (25 * n - 10) * n - 55; if (ver >= 7) r -= 36 }
            return r
        }
        private fun dataCodewords(ver: Int) = rawModules(ver) / 8 - ECC_M[ver] * BLOCKS_M[ver]

        /** Erzeugt den QR-Code für [text] (UTF-8); null, wenn er zu lang ist */
        fun encode(text: String): Qr? {
            val bytes = text.encodeToByteArray()
            val ver = (1..MAX_VERSION).firstOrNull { v -> 4 + (if (v < 10) 8 else 16) + bytes.size * 8 <= dataCodewords(v) * 8 } ?: return null
            val bits = ArrayList<Boolean>()
            fun put(v: Int, n: Int) { for (i in n - 1 downTo 0) bits.add((v ushr i) and 1 == 1) }
            put(4, 4); put(bytes.size, if (ver < 10) 8 else 16); bytes.forEach { put(it.toInt() and 0xFF, 8) }
            val cap = dataCodewords(ver) * 8
            put(0, minOf(4, cap - bits.size))
            put(0, (8 - bits.size % 8) % 8)
            var pad = 0xEC
            while (bits.size < cap) { put(pad, 8); pad = pad xor 0xEC xor 0x11 }
            val data = ByteArray(bits.size / 8) { i -> var b = 0; for (k in 0..7) b = (b shl 1) or (if (bits[i * 8 + k]) 1 else 0); b.toByte() }
            val qr = Qr(ver * 4 + 17)
            qr.drawFunctions(ver)
            qr.drawCodewords(qr.interleave(ver, data))
            // Maske mit der kleinsten Strafpunktzahl wählen
            var best = 0; var bestScore = Int.MAX_VALUE
            for (m in 0..7) { qr.mask(m); qr.format(m); val s = qr.penalty(); if (s < bestScore) { bestScore = s; best = m }; qr.mask(m) }
            qr.mask(best); qr.format(best)
            return qr
        }

        private fun gfMul(x: Int, y: Int): Int {
            var z = 0
            for (i in 7 downTo 0) { z = (z shl 1) xor ((z ushr 7) * 0x11D); z = z xor (((y ushr i) and 1) * x) }
            return z and 0xFF
        }
        private fun divisor(degree: Int): IntArray {
            val r = IntArray(degree); r[degree - 1] = 1; var root = 1
            for (i in 0 until degree) {
                for (j in 0 until degree) { r[j] = gfMul(r[j], root); if (j + 1 < degree) r[j] = r[j] xor r[j + 1] }
                root = gfMul(root, 0x02)
            }
            return r
        }
        private fun remainder(data: List<Int>, div: IntArray): List<Int> {
            val r = MutableList(div.size) { 0 }
            for (b in data) {
                val f = b xor r.removeAt(0); r.add(0)
                for (i in div.indices) r[i] = r[i] xor gfMul(div[i], f)
            }
            return r
        }
    }

    private fun set(x: Int, y: Int, d: Boolean) { dark[y][x] = d; func[y][x] = true }

    private fun drawFunctions(ver: Int) {
        for (i in 0 until size) { set(6, i, i % 2 == 0); set(i, 6, i % 2 == 0) }
        finder(3, 3); finder(size - 4, 3); finder(3, size - 4)
        val pos = alignPositions(ver)
        for (i in pos.indices) for (j in pos.indices) {
            if ((i == 0 && j == 0) || (i == 0 && j == pos.size - 1) || (i == pos.size - 1 && j == 0)) continue
            for (dy in -2..2) for (dx in -2..2) set(pos[i] + dx, pos[j] + dy, maxOf(abs(dx), abs(dy)) != 1)
        }
        format(0)
        if (ver >= 7) {
            var rem = ver
            repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
            val bits = (ver shl 12) or rem
            for (i in 0 until 18) { val b = (bits ushr i) and 1 == 1; val a = size - 11 + i % 3; val c = i / 3; set(a, c, b); set(c, a, b) }
        }
    }
    private fun finder(x: Int, y: Int) {
        for (dy in -4..4) for (dx in -4..4) {
            val d = maxOf(abs(dx), abs(dy)); val xx = x + dx; val yy = y + dy
            if (xx in 0 until size && yy in 0 until size) set(xx, yy, d != 2 && d != 4)
        }
    }
    private fun alignPositions(ver: Int): List<Int> {
        if (ver == 1) return emptyList()
        val n = ver / 7 + 2
        val step = (ver * 4 + n * 2 + 1) / (n * 2 - 2) * 2
        val r = mutableListOf(6); var p = size - 7
        while (r.size < n) { r.add(1, p); p -= step }
        return r
    }
    /** Formatinformation: Fehlerkorrektur M (Bits 00) und Maske */
    private fun format(mask: Int) {
        val data = (0 shl 3) or mask
        var rem = data
        repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
        val bits = ((data shl 10) or rem) xor 0x5412
        fun b(i: Int) = (bits ushr i) and 1 == 1
        for (i in 0..5) set(8, i, b(i))
        set(8, 7, b(6)); set(8, 8, b(7)); set(7, 8, b(8))
        for (i in 9 until 15) set(14 - i, 8, b(i))
        for (i in 0 until 8) set(size - 1 - i, 8, b(i))
        for (i in 8 until 15) set(8, size - 15 + i, b(i))
        set(8, size - 8, true)
    }
    private fun interleave(ver: Int, data: ByteArray): ByteArray {
        val nBlocks = BLOCKS_M[ver]; val eccLen = ECC_M[ver]
        val raw = rawModules(ver) / 8
        val nShort = nBlocks - raw % nBlocks
        val shortLen = raw / nBlocks
        val div = divisor(eccLen)
        val blocks = ArrayList<List<Int>>()
        var k = 0
        for (i in 0 until nBlocks) {
            val len = shortLen - eccLen + if (i < nShort) 0 else 1
            val dat = (k until k + len).map { data[it].toInt() and 0xFF }.toMutableList(); k += len
            val ecc = remainder(dat, div)
            if (i < nShort) dat.add(0)
            blocks.add(dat + ecc)
        }
        val out = ArrayList<Byte>()
        for (i in blocks[0].indices) for (j in blocks.indices) if (i != shortLen - eccLen || j >= nShort) out.add(blocks[j][i].toByte())
        return out.toByteArray()
    }
    private fun drawCodewords(data: ByteArray) {
        var i = 0; var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) for (j in 0..1) {
                val x = right - j; val up = ((right + 1) and 2) == 0
                val y = if (up) size - 1 - vert else vert
                if (!func[y][x] && i < data.size * 8) { dark[y][x] = ((data[i ushr 3].toInt() ushr (7 - (i and 7))) and 1) == 1; i++ }
            }
            right -= 2
        }
    }
    private fun mask(m: Int) {
        for (y in 0 until size) for (x in 0 until size) {
            val inv = when (m) {
                0 -> (x + y) % 2 == 0; 1 -> y % 2 == 0; 2 -> x % 3 == 0; 3 -> (x + y) % 3 == 0
                4 -> (x / 3 + y / 2) % 2 == 0; 5 -> x * y % 2 + x * y % 3 == 0; 6 -> (x * y % 2 + x * y % 3) % 2 == 0
                else -> ((x + y) % 2 + x * y % 3) % 2 == 0
            }
            if (inv && !func[y][x]) dark[y][x] = !dark[y][x]
        }
    }
    /** Vereinfachte Strafpunkte: lange Reihen, 2×2-Blöcke, Hell-Dunkel-Verhältnis */
    private fun penalty(): Int {
        var p = 0
        for (pass in 0..1) for (a in 0 until size) {
            var run = 1
            for (b in 1 until size) {
                val cur = if (pass == 0) dark[a][b] else dark[b][a]; val prev = if (pass == 0) dark[a][b - 1] else dark[b - 1][a]
                if (cur == prev) { run++; if (run == 5) p += 3 else if (run > 5) p++ } else run = 1
            }
        }
        for (y in 0 until size - 1) for (x in 0 until size - 1) { val c = dark[y][x]; if (c == dark[y][x + 1] && c == dark[y + 1][x] && c == dark[y + 1][x + 1]) p += 3 }
        val total = size * size; val d = dark.sumOf { r -> r.count { it } }
        p += (abs(d * 20 - total * 10) + total - 1) / total * 10
        return p
    }
}
