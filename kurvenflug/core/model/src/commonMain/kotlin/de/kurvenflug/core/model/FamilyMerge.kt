package de.kurvenflug.core.model

/**
 * Familien-Speicher (Bauplan v1.1, Marcel 19.11.): Zusammenführen zweier Stände desselben Kindes, z. B. Handy und Tablet,
 * wenn der Server einen Konflikt meldet. Grundsatz: nie still überschreiben, nie Fortschritt verlieren.
 *
 * Die Daten eines Kindes sind eine Liste logischer Schlüssel → Text:
 * - „progress“: Zeilen „levelId|sterne|zellen|versuche|geschafftAm“ – je Level mehr Sterne, Zellen vereinigt (Bit-Oder),
 *   mehr Versuche (Maximum, nicht Summe: dieselbe Geschichte kann auf beiden Geräten stehen), frühestes Geschafft-Datum.
 * - „plays“: Zeilen „levelId|sterne|versuche|sekunden|zeitpunkt“ – Vereinigung nach levelId + Zeitpunkt.
 * - „monate“: Monatssummen – Maximum je Feld ([MonthSums]).
 * - alles andere (Einstellungen, Name, Sammlung): gleich → übernehmen; nur auf einer Seite → übernehmen;
 *   verschieden → [Result.conflicts] (im Elternbereich nachfragen), bis dahin gilt der eigene Wert.
 */
object FamilyMerge {
    const val PROGRESS = "progress"
    const val PLAYS = "plays"
    const val MONTHS = "monate"

    data class Result(val merged: Map<String, String>, val conflicts: List<String>)

    fun progress(a: String?, b: String?): String {
        val all = (lines(a) + lines(b)).mapNotNull { l -> l.split('|').takeIf { it.size >= 5 && it[0].isNotEmpty() } }
        return all.groupBy { it[0] }.entries.sortedBy { it.key }.map { (id, rows) ->
            val stars = rows.maxOf { it[1].toIntOrNull() ?: 0 }
            val cells = rows.fold(0) { acc, r -> acc or (r[2].toIntOrNull() ?: 0) }
            val attempts = rows.maxOf { it[3].toIntOrNull() ?: 0 }
            val solved = rows.mapNotNull { it[4].toLongOrNull()?.takeIf { t -> t > 0 } }.minOrNull() ?: 0L
            "$id|$stars|$cells|$attempts|$solved"
        }.joinToString("\n")
    }

    fun plays(a: String?, b: String?): String {
        val all = (lines(a) + lines(b)).mapNotNull { l -> l.split('|').takeIf { it.size >= 5 && it[0].isNotEmpty() && it[4].toLongOrNull() != null } }
        return all.groupBy { it[0] + "|" + it[4] }.values
            .map { rows -> rows.maxByOrNull { it[1].toIntOrNull() ?: 0 }!! }
            .sortedWith(compareBy({ it[4].toLong() }, { it[0] }))
            .joinToString("\n") { it.joinToString("|") }
    }

    fun months(a: String?, b: String?): String {
        val x = MonthSums.parse(a).associateBy { it.month }; val y = MonthSums.parse(b).associateBy { it.month }
        return MonthSums.format((x.keys + y.keys).sorted().map { m ->
            val p = x[m]; val q = y[m]
            if (p == null) q!! else if (q == null) p
            else MonthSum(m, maxOf(p.minutes, q.minutes), maxOf(p.tasks, q.tasks), maxOf(p.solved, q.solved))
        })
    }

    /** [mine] = Stand dieses Geräts, [theirs] = Stand vom Server */
    fun kind(mine: Map<String, String>, theirs: Map<String, String>): Result {
        val out = LinkedHashMap<String, String>(); val conflicts = ArrayList<String>()
        for (k in (mine.keys + theirs.keys).sorted()) {
            val a = mine[k]; val b = theirs[k]
            out[k] = when {
                k == PROGRESS -> progress(a, b)
                k == PLAYS -> plays(a, b)
                k == MONTHS -> months(a, b)
                a == null -> b!!
                b == null || a == b -> a
                else -> { conflicts += k; a }
            }
        }
        return Result(out, conflicts)
    }

    private fun lines(t: String?) = t.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
}
