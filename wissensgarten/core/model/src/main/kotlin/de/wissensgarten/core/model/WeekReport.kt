package de.wissensgarten.core.model

/**
 * Eltern-Wochenbericht – wird nur auf dem Gerät aus dem Lern-Protokoll berechnet, nichts wird gesendet.
 * Ein Eintrag im Protokoll: welche Karte wann mit welcher Bewertung (Rating.ordinal) gegossen wurde.
 */
object WeekReport {
    data class Entry(val cardId: String, val time: Long, val rating: Int)
    data class CardInfo(val front: String, val subject: String, val stability: Double, val lapses: Int)

    data class Subject(val name: String, val reviews: Int, val rightPct: Int)
    data class Report(
        val days: List<Boolean>,      // die letzten 7 Kalendertage, ältester zuerst
        val reviews: Int,
        val rightPct: Int,
        val minutes: Int,             // geschätzt
        val subjects: List<Subject>,
        val strong: List<String>,     // sitzt richtig gut
        val hard: List<String>,       // fällt noch schwer
        val tip: String,
    ) { val activeDays get() = days.count { it } }

    /** Grobe Schätzung: eine Karte dauert etwa 12 Sekunden. */
    const val SECONDS_PER_CARD = 12

    fun build(entries: List<Entry>, cards: Map<String, CardInfo>, now: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): Report {
        val today = Scheduler.localDay(now, zone)
        val week = entries.filter { today - Scheduler.localDay(it.time, zone) in 0..6 }
        val dayset = week.map { Scheduler.localDay(it.time, zone) }.toSet()
        val days = (6 downTo 0).map { (today - it) in dayset }
        fun ok(e: Entry) = e.rating != Rating.AGAIN.ordinal
        val pct = { l: List<Entry> -> if (l.isEmpty()) 0 else l.count(::ok) * 100 / l.size }
        val subjects = week.groupBy { cards[it.cardId]?.subject ?: "eigene" }
            .map { (s, l) -> Subject(s, l.size, pct(l)) }.sortedByDescending { it.reviews }
        val byCard = week.groupBy { it.cardId }.filterKeys { it in cards }
        val hard = byCard.map { (id, l) -> id to l.count { !ok(it) } }.filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenByDescending { cards.getValue(it.first).lapses })
            .take(5).map { cards.getValue(it.first).front }
        val strong = byCard.filter { (_, l) -> l.all(::ok) }.keys.map { it to cards.getValue(it) }
            .sortedByDescending { it.second.stability }.take(5).map { it.second.front }
        val r = pct(week); val active = days.count { it }
        val tip = when {
            week.isEmpty() -> "Diese Woche war Pause. Ein kurzer Start hilft: 5 Minuten Gießen am gleichen Ort, zur gleichen Zeit."
            active <= 2 -> "Lieber oft und kurz: 5 Minuten an 4 Tagen merken sich besser als 20 Minuten an einem Tag."
            r < 60 -> "Viele Karten sind noch neu. Weniger neue Samen pro Tag (Einstellungen) machen das Gießen leichter."
            hard.isNotEmpty() -> "Fragt doch mal beim Abendessen nach „${hard.first()}“ – Erklären festigt Wissen besonders gut."
            else -> "Läuft super! Lobt die Ausdauer, nicht nur die Punkte – das motiviert langfristig."
        }
        return Report(days, week.size, r, (week.size * SECONDS_PER_CARD + 59) / 60, subjects, strong, hard, tip)
    }
}

/** Gesunde Lernzeit: Tageslimit in Minuten (0 = frei) plus von Eltern freigegebene Extra-Minuten. */
object StudyTime {
    fun isOver(usedMs: Long, limitMin: Int, extraMin: Int): Boolean = limitMin > 0 && usedMs >= (limitMin + extraMin) * 60_000L
}
