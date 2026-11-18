package de.kurvenflug.core.model

/**
 * Kleiner QR-Code-Erzeuger (Byte-Modus, Fehlerkorrektur M, Versionen 1–10) für den Kopplungs-Link „Eltern-Handy koppeln“.
 * Ohne fremde Bibliothek, rein in Kotlin (läuft auf Android und im Browser). Aufbau nach ISO/IEC 18004.
 * [modules][y][x] = true heißt dunkel.
 */
class QrCode private constructor(val size: Int, private val m: Array<BooleanArray>) {
    operator fun get(x: Int, y: Int) = m[y][x]

    companion object {
        // Fehlerkorrektur M je Version 1–10: (EC-Codewörter je Block, Blöcke Gruppe 1, Datenwörter G1, Blöcke G2, Datenwörter G2)
        private val M = arrayOf(
            intArrayOf(10, 1, 16, 0, 0), intArrayOf(16, 1, 28, 0, 0), intArrayOf(26, 1, 44, 0, 0), intArrayOf(18, 2, 32, 0, 0),
            intArrayOf(24, 2, 43, 0, 0), intArrayOf(16, 4, 27, 0, 0), intArrayOf(18, 4, 31, 0, 0), intArrayOf(22, 2, 38, 2, 39),
            intArrayOf(22, 3, 36, 2, 37), intArrayOf(26, 4, 43, 1, 44),
        )
        private val ALIGN = arrayOf(intArrayOf(), intArrayOf(6, 18), intArrayOf(6, 22), intArrayOf(6, 26), intArrayOf(6, 30), intArrayOf(6, 34),
            intArrayOf(6, 22, 38), intArrayOf(6, 24, 42), intArrayOf(6, 26, 46), intArrayOf(6, 28, 50))

        /** QR-Code für [text] (UTF-8); Maske 0. Wirft, wenn der Text für Version 10 zu lang ist. */
        fun encode(text: String, mask: Int = 0): QrCode {
            val data = text.encodeToByteArray()
            val version = (1..10).firstOrNull { v -> val e = M[v - 1]; e[1] * e[2] + e[3] * e[4] >= data.size + 2 }
                ?: throw IllegalArgumentException("Text zu lang für den QR-Code")
            val e = M[version - 1]
            val capacity = e[1] * e[2] + e[3] * e[4]
            // Bitstrom: Modus 0100, Länge (8 Bit bis Version 9, sonst 16), Daten, Endmarke, Auffüllen
            val bits = ArrayList<Boolean>()
            fun put(v: Int, n: Int) { for (i in n - 1 downTo 0) bits += (v ushr i) and 1 == 1 }
            put(4, 4); put(data.size, if (version < 10) 8 else 16)
            data.forEach { put(it.toInt() and 0xFF, 8) }
            put(0, minOf(4, capacity * 8 - bits.size))
            while (bits.size % 8 != 0) bits += false
            val words = ArrayList<Int>()
            for (i in bits.indices step 8) { var b = 0; for (j in 0 until 8) b = (b shl 1) or (if (bits[i + j]) 1 else 0); words += b }
            var pad = 0xEC; while (words.size < capacity) { words += pad; pad = if (pad == 0xEC) 0x11 else 0xEC }
            // Blöcke + Reed-Solomon, dann verschachteln
            val blocks = ArrayList<IntArray>(); val ecs = ArrayList<IntArray>()
            var k = 0
            for (g in 0..1) repeat(if (g == 0) e[1] else e[3]) {
                val n = if (g == 0) e[2] else e[4]
                val b = IntArray(n) { words[k + it] }; k += n
                blocks += b; ecs += reedSolomon(b, e[0])
            }
            val out = ArrayList<Int>()
            for (i in 0 until (blocks.maxOf { it.size })) blocks.forEach { if (i < it.size) out += it[i] }
            for (i in 0 until e[0]) ecs.forEach { out += it[i] }

            val size = version * 4 + 17
            val mod = Array(size) { BooleanArray(size) }
            val fn = Array(size) { BooleanArray(size) }
            fun set(x: Int, y: Int, dark: Boolean) { mod[y][x] = dark; fn[y][x] = true }
            // Suchmuster mit Rand
            for ((cx, cy) in listOf(3 to 3, size - 4 to 3, 3 to size - 4))
                for (dy in -4..4) for (dx in -4..4) {
                    val x = cx + dx; val y = cy + dy
                    if (x in 0 until size && y in 0 until size) { val d = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)); set(x, y, d != 2 && d != 4) }
                }
            // Taktlinien
            for (i in 0 until size) { if (!fn[6][i]) set(i, 6, i % 2 == 0); if (!fn[i][6]) set(6, i, i % 2 == 0) }
            // Ausrichtungsmuster
            val al = ALIGN[version - 1]
            for (ay in al) for (ax in al) {
                if ((ax == 6 && ay == 6) || (ax == 6 && ay == al.last()) || (ax == al.last() && ay == 6)) continue
                for (dy in -2..2) for (dx in -2..2) set(ax + dx, ay + dy, maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1)
            }
            // Format- und Versionsbereiche vormerken (Werte folgen nach der Maske)
            drawFormat(mask, size, ::set)
            if (version >= 7) drawVersion(version, size, ::set)
            set(8, size - 8, true)   // dunkles Modul
            // Daten im Zickzack
            var bit = 0
            val allBits = out.size * 8
            var right = size - 1
            while (right >= 1) {
                if (right == 6) right = 5
                for (vert in 0 until size) for (j in 0..1) {
                    val x = right - j
                    val upward = ((right + 1) and 2) == 0
                    val y = if (upward) size - 1 - vert else vert
                    if (!fn[y][x] && bit < allBits) { mod[y][x] = (out[bit ushr 3] ushr (7 - (bit and 7))) and 1 == 1; bit++ }
                }
                right -= 2
            }
            // Maske auf Datenmodule
            for (y in 0 until size) for (x in 0 until size) if (!fn[y][x]) {
                val inv = when (mask) {
                    0 -> (x + y) % 2 == 0; 1 -> y % 2 == 0; 2 -> x % 3 == 0; 3 -> (x + y) % 3 == 0
                    4 -> (x / 3 + y / 2) % 2 == 0; 5 -> x * y % 2 + x * y % 3 == 0; 6 -> (x * y % 2 + x * y % 3) % 2 == 0
                    else -> ((x + y) % 2 + x * y % 3) % 2 == 0
                }
                if (inv) mod[y][x] = !mod[y][x]
            }
            drawFormat(mask, size, ::set)
            return QrCode(size, mod)
        }

        private fun drawFormat(mask: Int, size: Int, set: (Int, Int, Boolean) -> Unit) {
            val data = (0 shl 3) or mask   // Fehlerkorrektur M = 00
            var rem = data
            repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
            val bits = ((data shl 10) or rem) xor 0x5412
            fun b(i: Int) = (bits ushr i) and 1 != 0
            for (i in 0..5) set(8, i, b(i))
            set(8, 7, b(6)); set(8, 8, b(7)); set(7, 8, b(8))
            for (i in 9..14) set(14 - i, 8, b(i))
            for (i in 0..7) set(size - 1 - i, 8, b(i))
            for (i in 8..14) set(8, size - 15 + i, b(i))
        }

        private fun drawVersion(version: Int, size: Int, set: (Int, Int, Boolean) -> Unit) {
            var rem = version
            repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
            val bits = (version shl 12) or rem
            for (i in 0 until 18) {
                val bit = (bits ushr i) and 1 != 0
                val a = size - 11 + i % 3; val b = i / 3
                set(a, b, bit); set(b, a, bit)
            }
        }

        /** Reed-Solomon-Rest über GF(256) mit Polynom 0x11D */
        private fun reedSolomon(data: IntArray, degree: Int): IntArray {
            fun mul(x: Int, y: Int): Int { var z = 0; for (i in 7 downTo 0) { z = (z shl 1) xor ((z ushr 7) * 0x11D); z = z xor (((y ushr i) and 1) * x) }; return z }
            val gen = IntArray(degree); gen[degree - 1] = 1
            var root = 1
            repeat(degree) {
                for (j in 0 until degree) { gen[j] = mul(gen[j], root); if (j + 1 < degree) gen[j] = gen[j] xor gen[j + 1] }
                root = mul(root, 0x02)
            }
            val res = IntArray(degree)
            for (b in data) {
                val factor = b xor res[0]
                for (i in 0 until degree - 1) res[i] = res[i + 1]
                res[degree - 1] = 0
                for (i in 0 until degree) res[i] = res[i] xor mul(gen[i], factor)
            }
            return res
        }
    }
}
