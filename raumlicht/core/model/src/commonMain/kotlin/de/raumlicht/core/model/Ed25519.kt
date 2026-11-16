package de.raumlicht.core.model

/**
 * Ed25519-Signaturprüfung (RFC 8032) in reinem Kotlin, nach TweetNaCl (gemeinfrei). Nur Prüfen – Signieren macht
 * der Lizenz-Dienst mit dem geheimen Schlüssel. Feldelemente als 16 Ziffern zur Basis 2¹⁶.
 */
internal object Ed25519 {
    private fun gf(vararg v: Long) = LongArray(16).also { v.copyInto(it) }
    private val gf0 = gf()
    private val gf1 = gf(1)
    private val D = gf(0x78a3, 0x1359, 0x4dca, 0x75eb, 0xd8ab, 0x4141, 0xa4d, 0x70, 0xe898, 0x7779, 0x4079, 0x8cc7, 0xfe73, 0x2b6f, 0x6cee, 0x5203)
    private val D2 = gf(0xf159, 0x26b2, 0x9b94, 0xebd6, 0xb156, 0x8283, 0x149a, 0xe0, 0xd130, 0xeef3, 0x80f2, 0x198e, 0xfce7, 0x56df, 0xd9dc, 0x2406)
    private val X = gf(0xd51a, 0x8f25, 0x2d60, 0xc956, 0xa7b2, 0x9525, 0xc760, 0x692c, 0xdc5c, 0xfdd6, 0xe231, 0xc0a4, 0x53fe, 0xcd6e, 0x36d3, 0x2169)
    private val Y = gf(0x6658, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666)
    private val I = gf(0xa0b0, 0x4a0e, 0x1b27, 0xc4ee, 0xe478, 0xad2f, 0x1806, 0x2f43, 0xd7a7, 0x3dfb, 0x99, 0x2b4d, 0xdf0b, 0x4fc1, 0x2480, 0x2b83)
    private val L = longArrayOf(0xed, 0xd3, 0xf5, 0x5c, 0x1a, 0x63, 0x12, 0x58, 0xd6, 0x9c, 0xf7, 0xa2, 0xde, 0xf9, 0xde, 0x14,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x10)

    private fun set(r: LongArray, a: LongArray) = a.copyInto(r)

    private fun car(o: LongArray) {
        for (i in 0 until 16) {
            o[i] += 1L shl 16
            val c = o[i] shr 16
            if (i < 15) o[i + 1] += c - 1 else o[0] += c - 1 + 37 * (c - 1)
            o[i] -= c shl 16
        }
    }

    private fun sel(p: LongArray, q: LongArray, b: Int) {
        val c = (b - 1).toLong().inv()
        for (i in 0 until 16) { val t = c and (p[i] xor q[i]); p[i] = p[i] xor t; q[i] = q[i] xor t }
    }

    private fun pack25519(o: ByteArray, n: LongArray) {
        val m = gf(); val t = n.copyOf()
        car(t); car(t); car(t)
        repeat(2) {
            m[0] = t[0] - 0xffed
            for (i in 1 until 15) {
                m[i] = t[i] - 0xffff - ((m[i - 1] shr 16) and 1)
                m[i - 1] = m[i - 1] and 0xffff
            }
            m[15] = t[15] - 0x7fff - ((m[14] shr 16) and 1)
            val b = ((m[15] shr 16) and 1).toInt()
            m[14] = m[14] and 0xffff
            sel(t, m, 1 - b)
        }
        for (i in 0 until 16) { o[2 * i] = (t[i] and 0xff).toByte(); o[2 * i + 1] = (t[i] shr 8).toByte() }
    }

    private fun neq(a: LongArray, b: LongArray): Boolean {
        val c = ByteArray(32); val d = ByteArray(32)
        pack25519(c, a); pack25519(d, b)
        return !c.contentEquals(d)
    }

    private fun par(a: LongArray): Int { val d = ByteArray(32); pack25519(d, a); return d[0].toInt() and 1 }

    private fun unpack25519(o: LongArray, n: ByteArray) {
        for (i in 0 until 16) o[i] = (n[2 * i].toLong() and 0xff) + ((n[2 * i + 1].toLong() and 0xff) shl 8)
        o[15] = o[15] and 0x7fff
    }

    private fun add(o: LongArray, a: LongArray, b: LongArray) { for (i in 0 until 16) o[i] = a[i] + b[i] }
    private fun sub(o: LongArray, a: LongArray, b: LongArray) { for (i in 0 until 16) o[i] = a[i] - b[i] }
    private fun mul(o: LongArray, a: LongArray, b: LongArray) {
        val t = LongArray(31)
        for (i in 0 until 16) for (j in 0 until 16) t[i + j] += a[i] * b[j]
        for (i in 0 until 15) t[i] += 38 * t[i + 16]
        for (i in 0 until 16) o[i] = t[i]
        car(o); car(o)
    }
    private fun sq(o: LongArray, a: LongArray) = mul(o, a, a)

    private fun inv(o: LongArray, i: LongArray) {
        val c = i.copyOf()
        for (a in 253 downTo 0) { sq(c, c); if (a != 2 && a != 4) mul(c, c, i) }
        set(o, c)
    }

    private fun pow2523(o: LongArray, i: LongArray) {
        val c = i.copyOf()
        for (a in 250 downTo 0) { sq(c, c); if (a != 1) mul(c, c, i) }
        set(o, c)
    }

    private fun padd(p: Array<LongArray>, q: Array<LongArray>) {
        val a = gf(); val b = gf(); val c = gf(); val d = gf(); val e = gf(); val f = gf(); val g = gf(); val h = gf(); val t = gf()
        sub(a, p[1], p[0]); sub(t, q[1], q[0]); mul(a, a, t)
        add(b, p[0], p[1]); add(t, q[0], q[1]); mul(b, b, t)
        mul(c, p[3], q[3]); mul(c, c, D2)
        mul(d, p[2], q[2]); add(d, d, d)
        sub(e, b, a); sub(f, d, c); add(g, d, c); add(h, b, a)
        mul(p[0], e, f); mul(p[1], h, g); mul(p[2], g, f); mul(p[3], e, h)
    }

    private fun cswap(p: Array<LongArray>, q: Array<LongArray>, b: Int) { for (i in 0 until 4) sel(p[i], q[i], b) }

    private fun pack(r: ByteArray, p: Array<LongArray>) {
        val tx = gf(); val ty = gf(); val zi = gf()
        inv(zi, p[2]); mul(tx, p[0], zi); mul(ty, p[1], zi)
        pack25519(r, ty)
        r[31] = (r[31].toInt() xor (par(tx) shl 7)).toByte()
    }

    private fun scalarmult(p: Array<LongArray>, q: Array<LongArray>, s: ByteArray) {
        set(p[0], gf0); set(p[1], gf1); set(p[2], gf1); set(p[3], gf0)
        for (i in 255 downTo 0) {
            val b = (s[i / 8].toInt() shr (i and 7)) and 1
            cswap(p, q, b); padd(q, p); padd(p, p); cswap(p, q, b)
        }
    }

    private fun scalarbase(p: Array<LongArray>, s: ByteArray) {
        val q = arrayOf(X.copyOf(), Y.copyOf(), gf1.copyOf(), gf())
        mul(q[3], X, Y)
        scalarmult(p, q, s)
    }

    private fun modL(r: ByteArray, x: LongArray) {
        for (i in 63 downTo 32) {
            var carry = 0L
            var j = i - 32
            val k = i - 12
            while (j < k) {
                x[j] += carry - 16 * x[i] * L[j - (i - 32)]
                carry = (x[j] + 128) shr 8
                x[j] -= carry shl 8
                j++
            }
            x[j] += carry
            x[i] = 0
        }
        var carry = 0L
        for (j in 0 until 32) {
            x[j] += carry - (x[31] shr 4) * L[j]
            carry = x[j] shr 8
            x[j] = x[j] and 255
        }
        for (j in 0 until 32) x[j] -= carry * L[j]
        for (i in 0 until 32) {
            x[i + 1] += x[i] shr 8
            r[i] = (x[i] and 255).toByte()
        }
    }

    private fun reduce(h: ByteArray): ByteArray {
        val x = LongArray(64) { h[it].toLong() and 0xff }
        val r = ByteArray(32)
        modL(r, x)
        return r
    }

    private fun unpackneg(r: Array<LongArray>, p: ByteArray): Boolean {
        val t = gf(); val chk = gf(); val num = gf(); val den = gf(); val den2 = gf(); val den4 = gf(); val den6 = gf()
        set(r[2], gf1)
        unpack25519(r[1], p)
        sq(num, r[1]); mul(den, num, D); sub(num, num, r[2]); add(den, r[2], den)
        sq(den2, den); sq(den4, den2); mul(den6, den4, den2)
        mul(t, den6, num); mul(t, t, den)
        pow2523(t, t)
        mul(t, t, num); mul(t, t, den); mul(t, t, den); mul(r[0], t, den)
        sq(chk, r[0]); mul(chk, chk, den)
        if (neq(chk, num)) mul(r[0], r[0], I)
        sq(chk, r[0]); mul(chk, chk, den)
        if (neq(chk, num)) return false
        if (par(r[0]) == ((p[31].toInt() and 0xff) shr 7)) sub(r[0], gf0, r[0])
        mul(r[3], r[0], r[1])
        return true
    }

    /** true, wenn [sig] (64 Byte) eine gültige Signatur von [msg] zum öffentlichen Schlüssel [pk] (32 Byte) ist. */
    fun verify(msg: ByteArray, sig: ByteArray, pk: ByteArray): Boolean {
        if (sig.size != 64 || pk.size != 32) return false
        val q = Array(4) { gf() }; val p = Array(4) { gf() }
        if (!unpackneg(q, pk)) return false
        val h = reduce(Sha512.hash(sig.copyOfRange(0, 32) + pk + msg))
        scalarmult(p, q, h)
        scalarbase(q, sig.copyOfRange(32, 64))
        padd(p, q)
        val t = ByteArray(32)
        pack(t, p)
        return t.contentEquals(sig.copyOfRange(0, 32))
    }
}
