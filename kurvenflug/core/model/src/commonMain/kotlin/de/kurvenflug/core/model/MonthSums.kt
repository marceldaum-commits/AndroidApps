package de.kurvenflug.core.model

/**
 * Monatssummen für den Eltern-Bericht („verlauf“): Lernminuten, Durchgänge und geschaffte Durchgänge je Monat.
 * Dauerhaft gespeichert, weil die einzelnen Durchgänge nur 60 Tage aufgehoben werden. Beim Zusammenführen gilt je Wert das
 * Maximum aus Gespeichertem und neu Gezähltem – so schrumpft ein Monat nicht, wenn alte Durchgänge aufgeräumt werden.
 * Gespeichert als Zeilen „2026-11|min|aufgaben|richtig“.
 */
data class MonthSum(val month: String, val minutes: Int, val tasks: Int, val solved: Int) {
    val percent get() = if (tasks == 0) 0 else (solved * 100 + tasks / 2) / tasks
}

object MonthSums {
    const val MAX = 12

    fun parse(text: String?): List<MonthSum> = text.orEmpty().lines().mapNotNull { l ->
        val p = l.split('|'); if (p.size != 4) null else MonthSum(p[0], p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0)
    }.filter { it.month.matches(Regex("\\d{4}-\\d{2}")) }

    fun format(list: List<MonthSum>) = list.joinToString("\n") { "${it.month}|${it.minutes}|${it.tasks}|${it.solved}" }

    /** [offsetMin]: Zeitzone des Geräts (Monat nach Ortszeit) */
    fun merge(stored: List<MonthSum>, plays: List<PlayRecord>, offsetMin: (Long) -> Int = { utcOffsetMinutes(it) }): List<MonthSum> {
        val counted = plays.groupBy { r ->
            val d = CalDate.ofEpochDay(Math2.floorDiv(r.at + offsetMin(r.at) * 60_000L, Weekly.DAY))
            "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
        }.mapValues { (m, l) -> MonthSum(m, (l.sumOf { it.seconds.coerceAtLeast(0) } + 30) / 60, l.size, l.count { it.stars > 0 }) }
        val old = stored.associateBy { it.month }
        return (old.keys + counted.keys).sorted().map { m ->
            val a = old[m]; val b = counted[m]
            if (a == null) b!! else if (b == null) a else MonthSum(m, maxOf(a.minutes, b.minutes), maxOf(a.tasks, b.tasks), maxOf(a.solved, b.solved))
        }.takeLast(24)
    }
}
