package de.kurvenflug.core.model

import java.io.File

/** Sucht Level mit mehreren getrennten Lösungen (alle Reglerstellungen durchprobiert, Treffer zu Gruppen zusammengefasst). */
class MultiSolutionScan {
    fun clusters(l: Level, limit: Int = 60_000): List<Map<String, Double>>? {
        val ps = l.params
        val grids = ps.map { p -> generateSequence(p.min) { it + p.step }.takeWhile { it <= p.max + 1e-9 }.map { p.snap(it) }.toList() }
        val total = grids.fold(1L) { a, g -> a * g.size }
        if (total > limit) return null
        val hits = ArrayList<IntArray>()
        fun rec(i: Int, idx: IntArray) {
            if (i == ps.size) { val v = ps.indices.associate { ps[it].key to grids[it][idx[it]] }
                if (runCatching { Flight.fly(l, v).success }.getOrDefault(false)) hits += idx.clone(); return }
            for (j in grids[i].indices) { idx[i] = j; rec(i + 1, idx) }
        }
        rec(0, IntArray(ps.size))
        // Gruppen: Treffer, die sich in jedem Regler um höchstens 2 Schritte unterscheiden, gehören zusammen
        val group = IntArray(hits.size) { -1 }; var g = 0
        for (i in hits.indices) if (group[i] < 0) {
            val stack = ArrayDeque(listOf(i)); group[i] = g
            while (stack.isNotEmpty()) { val a = stack.removeLast()
                for (b in hits.indices) if (group[b] < 0 && hits[a].indices.all { k -> kotlin.math.abs(hits[a][k] - hits[b][k]) <= 2 }) { group[b] = g; stack += b } }
            g++
        }
        return (0 until g).map { gi -> val members = hits.indices.filter { group[it] == gi }; val mid = members[members.size / 2]
            ps.indices.associate { ps[it].key to grids[it][hits[mid][it]] } }
    }

    /** Vollständige Liste (dauert ca. 4 min) – bei Bedarf von Hand aufrufen, z. B. aus einem Test: MultiSolutionScan().scan() */
    fun scan() {
        val sb = StringBuilder()
        val levels = ReadTexts.fixedLevels() + (0..60).map { Daily.forDay(20_775L + it) }
        val t0 = System.currentTimeMillis()
        levels.forEach { l ->
            val c = clusters(l)
            if (c == null) sb.append("${l.id}\tZU GROSS\t${l.params.joinToString { it.key }}\n")
            else if (c.size > 1) sb.append("${l.id}\t${c.size} Lösungen\t${l.kind}\t${c.joinToString(" | ") { m -> m.entries.joinToString(",") { "${it.key}=${num(it.value)}" } }}\tSOLL ${l.solution.entries.joinToString(",") { "${it.key}=${num(it.value)}" }}\t${l.task.take(80)}\n")
        }
        sb.append("Dauer ${(System.currentTimeMillis() - t0) / 1000} s\n")
        File("build/multi.txt").writeText(sb.toString())
    }
}
