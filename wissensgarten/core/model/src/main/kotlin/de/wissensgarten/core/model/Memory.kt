package de.wissensgarten.core.model

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/** Gedächtnis-Zustand einer Karte. stability = 0 bedeutet: noch nie gelernt (Samen). */
data class Memory(
    val stability: Double = 0.0,
    val difficulty: Double = 5.0,
    val lastReview: Long = 0L,
    val reps: Int = 0,
    val lapses: Int = 0,
) {
    val isSeed: Boolean get() = reps == 0
}

/** Selbsteinschätzung nach dem Aufdecken. */
enum class Rating(val label: String) {
    AGAIN("Vergessen"),
    HARD("Schwer"),
    GOOD("Gut"),
    EASY("Leicht"),
}

/** Wachstumsstufe der Pflanze, abgeleitet aus der Stabilität. */
enum class Stage(val label: String) {
    SEED("Samen"), SPROUT("Keimling"), SHOOT("Spross"), BUSH("Busch"), BLOOM("Blüte"), TREE("Baum");

    companion object {
        fun of(m: Memory): Stage = when {
            m.isSeed -> SEED
            m.stability < 2 -> SPROUT
            m.stability < 7 -> SHOOT
            m.stability < 21 -> BUSH
            m.stability < 60 -> BLOOM
            else -> TREE
        }
    }
}

/**
 * Wiederholung nach der Vergessenskurve, vereinfacht nach dem FSRS-Modell.
 * Erinnerung R(t) = (1 + t / (9·S))^-1 – nach S Tagen liegt sie genau bei 90 %.
 * Im Garten ist R die „Feuchtigkeit“: unter 90 % hat die Pflanze Durst.
 */
object Scheduler {
    const val DAY_MS = 24L * 60 * 60 * 1000

    /** Kalendertag nach Ortszeit – damit „heute“ um Mitternacht wechselt, nicht um 1 oder 2 Uhr (UTC). */
    fun localDay(ms: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): Long = Math.floorDiv(ms + zone.getOffset(ms), DAY_MS)
    const val TARGET = 0.9

    fun retrievability(m: Memory, now: Long): Double {
        if (m.isSeed) return 1.0
        val days = max(0.0, (now - m.lastReview).toDouble() / DAY_MS)
        return 1.0 / (1.0 + days / (9.0 * m.stability))
    }

    fun isThirsty(m: Memory, now: Long): Boolean = !m.isSeed && retrievability(m, now) < TARGET

    /** 0 = frisch, 1 = ganz welk. */
    fun wilt(m: Memory, now: Long): Float {
        if (m.isSeed) return 0f
        return ((0.95 - retrievability(m, now)) / 0.35).coerceIn(0.0, 1.0).toFloat()
    }

    fun dueAt(m: Memory): Long = m.lastReview + (m.stability * DAY_MS).toLong()

    fun review(m: Memory, rating: Rating, now: Long): Memory {
        if (m.isSeed) {
            val s = when (rating) { Rating.AGAIN -> 0.3; Rating.HARD -> 1.0; Rating.GOOD -> 2.5; Rating.EASY -> 7.0 }
            val d = when (rating) { Rating.AGAIN -> 7.0; Rating.HARD -> 6.0; Rating.GOOD -> 5.0; Rating.EASY -> 3.0 }
            return Memory(s, d, now, 1, if (rating == Rating.AGAIN) 1 else 0)
        }
        val r = retrievability(m, now)
        val dStep = when (rating) { Rating.AGAIN -> 1.5; Rating.HARD -> 0.6; Rating.GOOD -> 0.0; Rating.EASY -> -0.8 }
        val d = (m.difficulty + dStep + 0.05 * (5.0 - m.difficulty)).coerceIn(1.0, 10.0)
        val s = if (rating == Rating.AGAIN) {
            max(0.3, m.stability * 0.25)
        } else {
            val bonus = when (rating) { Rating.HARD -> 0.5; Rating.EASY -> 1.6; else -> 1.0 }
            val growth = exp(1.5) * (11.0 - d) * m.stability.pow(-0.2) * (exp(1.0 - r) - 1.0) * bonus
            max(m.stability, m.stability * (1.0 + growth))
        }
        return Memory(s, d, now, m.reps + 1, m.lapses + if (rating == Rating.AGAIN) 1 else 0)
    }

    fun previewDays(m: Memory, rating: Rating, now: Long): Double = review(m, rating, now).stability
}
