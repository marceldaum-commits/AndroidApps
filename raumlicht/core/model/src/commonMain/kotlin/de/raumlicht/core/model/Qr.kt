package de.raumlicht.core.model

/**
 * QR-Code selbst erzeugt – ohne fremden Baustein. Byte-Modus, Fehlerkorrektur „L“, Versionen 1 bis 9 (bis 230 Zeichen; ein Kopplungs-Link hat rund 125).
 * Ergebnis: Zeilen aus Feldern, true = dunkel. Gegen eine Referenz geprüft (QrTest).
 */
object Qr {
    private val TOTAL = intArrayOf(0, 26, 44, 70, 100, 134, 172, 196, 242, 292)       // Codewörter je Version
    private val EC = intArrayOf(0, 7, 10, 15, 20, 26, 18, 20, 24, 30)                 // Korrektur-Codewörter je Block (Stufe L)
    private val BLOCKS = intArrayOf(0, 1, 1, 1, 1, 1, 2, 2, 2, 2)
    private val ALIGN = arrayOf(intArrayOf(), intArrayOf(), intArrayOf(6, 18), intArrayOf(6, 22), intArrayOf(6, 26), intArrayOf(6, 30), intArrayOf(6, 34),
        intArrayOf(6, 22, 38), intArrayOf(6, 24, 42), intArrayOf(6, 26, 46))
    private fun dataCodewords(v: Int) = TOTAL[v] - EC[v] * BLOCKS[v]
    /** Längster Text (in Bytes), der hineinpasst */
    val MAX_BYTES = dataCodewords(9) - 2

    // Rechnen im Körper GF(256) mit dem Polynom x⁸ + x⁴ + x³ + x² + 1
    private fun mul(a: Int, b: Int): Int { var z = 0; for (i in 7 downTo 0) { z = (z shl 1) xor ((z ushr 7) * 0x11D); z = z xor (((b ushr i) and 1) * a) }; return z }
    private fun divisor(degree: Int): IntArray { val r = IntArray(degree); r[degree - 1] = 1; var root = 1
        repeat(degree) { for (j in r.indices) { r[j] = mul(r[j], root); if (j + 1 < r.size) r[j] = r[j] xor r[j + 1] }; root = mul(root, 2) }; return r }
    private fun remainder(data: IntArray, div: IntArray): IntArray { val r = IntArray(div.size)
        for (b in data) { val f = b xor r[0]; for (i in 0 until r.size - 1) r[i] = r[i + 1]; r[r.size - 1] = 0; for (i in r.indices) r[i] = r[i] xor mul(div[i], f) }; return r }

    private fun mask(k: Int, x: Int, y: Int) = when (k) {
        0 -> (x + y) % 2 == 0; 1 -> y % 2 == 0; 2 -> x % 3 == 0; 3 -> (x + y) % 3 == 0; 4 -> (x / 3 + y / 2) % 2 == 0
        5 -> x * y % 2 + x * y % 3 == 0; 6 -> (x * y % 2 + x * y % 3) % 2 == 0; else -> ((x + y) % 2 + x * y % 3) % 2 == 0 }

    /** Der QR-Code zu [text] – oder null, wenn der Text zu lang ist. [fixedMask] nur für Tests (sonst wird das ruhigste Muster gewählt). */
    fun encode(text: String, fixedMask: Int? = null): List<BooleanArray>? {
        val bytes = text.encodeToByteArray()
        val v = (1..9).firstOrNull { dataCodewords(it) * 8 >= 12 + 8 * bytes.size } ?: return null
        // 1. Datenbits: Modus „Byte“ (0100), Länge (8 Bit), die Bytes, Abschluss, Füllbytes
        val cap = dataCodewords(v); val bits = ArrayList<Boolean>(cap * 8)
        fun put(value: Int, n: Int) { for (i in n - 1 downTo 0) bits += ((value ushr i) and 1) == 1 }
        put(4, 4); put(bytes.size, 8); for (b in bytes) put(b.toInt() and 0xFF, 8)
        put(0, minOf(4, cap * 8 - bits.size)); put(0, (8 - bits.size % 8) % 8)
        var pad = 0xEC; while (bits.size < cap * 8) { put(pad, 8); pad = pad xor 0xEC xor 0x11 }
        val data = IntArray(cap) { i -> (0 until 8).fold(0) { a, j -> (a shl 1) or (if (bits[i * 8 + j]) 1 else 0) } }
        // 2. Blöcke mit Fehlerkorrektur, dann verschränken
        val nb = BLOCKS[v]; val per = cap / nb; val div = divisor(EC[v])
        val blocks = (0 until nb).map { b -> data.copyOfRange(b * per, (b + 1) * per) }; val ecs = blocks.map { remainder(it, div) }
        val all = ArrayList<Int>(TOTAL[v]); for (i in 0 until per) for (b in blocks) all += b[i]; for (i in 0 until EC[v]) for (e in ecs) all += e[i]
        // 3. Feld mit festen Mustern
        val n = 17 + 4 * v; val m = Array(n) { BooleanArray(n) }; val fn = Array(n) { BooleanArray(n) }
        fun set(x: Int, y: Int, dark: Boolean) { m[y][x] = dark; fn[y][x] = true }
        for (i in 0 until n) { set(6, i, i % 2 == 0); set(i, 6, i % 2 == 0) }
        fun finder(cx: Int, cy: Int) { for (dy in -4..4) for (dx in -4..4) { val x = cx + dx; val y = cy + dy
            if (x in 0 until n && y in 0 until n) { val d = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)); set(x, y, d != 2 && d != 4) } } }
        finder(3, 3); finder(n - 4, 3); finder(3, n - 4)
        val al = ALIGN[v]
        for (i in al.indices) for (j in al.indices) { if ((i == 0 && j == 0) || (i == 0 && j == al.size - 1) || (i == al.size - 1 && j == 0)) continue
            for (dy in -2..2) for (dx in -2..2) set(al[i] + dx, al[j] + dy, maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1) }
        fun format(k: Int) { val d = (1 shl 3) or k; var rem = d; repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }; val f = ((d shl 10) or rem) xor 0x5412
            fun bit(i: Int) = ((f ushr i) and 1) == 1
            for (i in 0..5) set(8, i, bit(i)); set(8, 7, bit(6)); set(8, 8, bit(7)); set(7, 8, bit(8)); for (i in 9..14) set(14 - i, 8, bit(i))
            for (i in 0..7) set(n - 1 - i, 8, bit(i)); for (i in 8..14) set(8, n - 15 + i, bit(i)); set(8, n - 8, true) }
        format(0)
        if (v >= 7) { var rem = v; repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }; val vb = (v shl 12) or rem
            for (i in 0..17) { val bit = ((vb ushr i) and 1) == 1; val a = n - 11 + i % 3; val b = i / 3; set(a, b, bit); set(b, a, bit) } }
        // 4. Daten im Zickzack von unten rechts
        var i = 0; var right = n - 1
        while (right >= 1) { if (right == 6) right = 5
            for (vert in 0 until n) for (j in 0..1) { val x = right - j; val up = ((right + 1) and 2) == 0; val y = if (up) n - 1 - vert else vert
                if (!fn[y][x] && i < all.size * 8) { m[y][x] = ((all[i ushr 3] ushr (7 - (i and 7))) and 1) == 1; i++ } }
            right -= 2 }
        // 5. Maske: die mit den wenigsten Strafpunkten (lange Reihen, 2×2-Blöcke, Ungleichgewicht)
        fun apply(k: Int) { for (y in 0 until n) for (x in 0 until n) if (!fn[y][x] && mask(k, x, y)) m[y][x] = !m[y][x] }
        fun penalty(): Int { var p = 0
            for (line in 0 until n) for (dir in 0..1) { var run = 0; var last = false
                for (t in 0 until n) { val c = if (dir == 0) m[line][t] else m[t][line]; if (t > 0 && c == last) { run++; if (run == 5) p += 3 else if (run > 5) p++ } else { run = 1; last = c } } }
            for (y in 0 until n - 1) for (x in 0 until n - 1) if (m[y][x] == m[y][x + 1] && m[y][x] == m[y + 1][x] && m[y][x] == m[y + 1][x + 1]) p += 3
            val dark = m.sumOf { r -> r.count { it } }; p += (kotlin.math.abs(dark * 20 - n * n * 10) / (n * n)) * 10; return p }
        val best = fixedMask ?: (0..7).minByOrNull { k -> apply(k); format(k); val p = penalty(); apply(k); p }!!
        apply(best); format(best)
        return m.toList()
    }
}
