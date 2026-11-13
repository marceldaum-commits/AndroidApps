package de.wissensgarten.app.data

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/**
 * Wiederholung nach der Vergessenskurve, vereinfacht nach dem FSRS-Modell.
 *
 * Erinnerung R(t) = (1 + t / (9·S))^-1 – nach S Tagen liegt sie genau bei 90 %.
 * Im Garten ist R die „Feuchtigkeit“ der Pflanze: unter 90 % hat sie Durst.
 */
object Scheduler {
    const val DAY_MS = 24L * 60 * 60 * 1000
    const val TARGET = 0.9

    fun retrievability(card: Card, now: Long): Double {
        if (card.isSeed) return 1.0
        val days = max(0.0, (now - card.lastReview).toDouble() / DAY_MS)
        return 1.0 / (1.0 + days / (9.0 * card.stability))
    }

    /** Durstig = fällig zum Gießen. */
    fun isThirsty(card: Card, now: Long): Boolean =
        !card.isSeed && retrievability(card, now) < TARGET

    /** Wie welk die Pflanze aussieht: 0 = frisch, 1 = ganz welk. */
    fun wilt(card: Card, now: Long): Float {
        if (card.isSeed) return 0f
        val r = retrievability(card, now)
        return ((0.95 - r) / 0.35).coerceIn(0.0, 1.0).toFloat()
    }

    fun dueAt(card: Card): Long =
        card.lastReview + (card.stability * DAY_MS).toLong()

    fun review(card: Card, grade: Grade, now: Long): Card {
        if (card.isSeed) {
            val s = when (grade) {
                Grade.AGAIN -> 0.3
                Grade.HARD -> 1.0
                Grade.GOOD -> 2.5
                Grade.EASY -> 7.0
            }
            val d = when (grade) {
                Grade.AGAIN -> 7.0
                Grade.HARD -> 6.0
                Grade.GOOD -> 5.0
                Grade.EASY -> 3.0
            }
            return card.copy(
                stability = s, difficulty = d, lastReview = now,
                reps = 1, lapses = if (grade == Grade.AGAIN) 1 else 0,
            )
        }

        val r = retrievability(card, now)
        val dStep = when (grade) {
            Grade.AGAIN -> 1.5
            Grade.HARD -> 0.6
            Grade.GOOD -> 0.0
            Grade.EASY -> -0.8
        }
        // leichter Zug zurück zur Mitte, damit Schwierigkeit nicht festklebt
        val d = (card.difficulty + dStep + 0.05 * (5.0 - card.difficulty)).coerceIn(1.0, 10.0)

        val s = if (grade == Grade.AGAIN) {
            max(0.3, card.stability * 0.25)
        } else {
            val bonus = when (grade) {
                Grade.HARD -> 0.5
                Grade.EASY -> 1.6
                else -> 1.0
            }
            val growth = exp(1.5) * (11.0 - d) * card.stability.pow(-0.2) *
                (exp((1.0 - r) * 1.0) - 1.0) * bonus
            // Wer zu früh gießt, bekommt wenig Wachstum – aber nie weniger als vorher.
            max(card.stability, card.stability * (1.0 + growth))
        }

        return card.copy(
            stability = s,
            difficulty = d,
            lastReview = now,
            reps = card.reps + 1,
            lapses = card.lapses + if (grade == Grade.AGAIN) 1 else 0,
        )
    }

    /** Nächster Abstand in Tagen – für die Beschriftung der Knöpfe. */
    fun previewDays(card: Card, grade: Grade, now: Long): Double =
        review(card, grade, now).stability
}
