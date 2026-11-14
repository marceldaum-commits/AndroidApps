package de.wissensgarten.core.model

import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Eine Klassenarbeit / ein Test: welches Beet, an welchem Tag (Epoch-Tag, lokal). */
@Serializable
data class Exam(val id: String, val bedId: String, val title: String, val day: Long, val createdAt: Long = 0)

/** Wo stehen wir? Für Garten-Karte und Brummi. */
data class ExamPlan(
    val exam: Exam,
    val daysLeft: Int,
    /** Anteil der Karten, die am Prüfungstag sitzen werden (0..1) */
    val fitness: Double,
    val readyCards: Int,
    val totalCards: Int,
    /** Karten für heute, in sinnvoller Reihenfolge */
    val today: List<StudyCard>,
    val phase: Phase,
) {
    enum class Phase { EARLY, BUILD, FINAL_DAY, EXAM_DAY, PAST }
    val fitnessPercent get() = (fitness * 100).roundToInt()
}

/**
 * Lernen auf einen Termin hin. Grundlage: Cepeda et al. 2008 – der beste Abstand zwischen Wiederholungen
 * ist ein Anteil der Zeit bis zum Test (bei ~1 Woche ca. 20–40 %, bei längeren Zeiträumen weniger).
 * Also: neue Karten früh säen, Wiederholungen über die Tage verteilen, am Vortag eine kurze Generalprobe,
 * am Prüfungstag nur ein Aufwärmen. Kein Pauken am Vorabend nötig.
 */
object ExamPlanner {
    /** kurze Einheiten: lieber mehrere kleine Runden als eine lange */
    const val MAX_PER_DAY = 20
    const val MIN_PER_DAY = 5
    /** Ab wann eine Karte „sitzt“: am Prüfungstag mit 90 % Wahrscheinlichkeit gewusst */
    const val READY = 0.9

    /** Kalendertag nach Ortszeit (wie im Kalender gewählt) */
    fun day(ms: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()) = Scheduler.localDay(ms, zone)

    /** Wird diese Karte am Prüfungstag sitzen? (Mitte des Prüfungstags) */
    fun ready(c: StudyCard, exam: Exam): Boolean =
        !c.memory.isSeed && Scheduler.retrievability(c.memory, exam.day * Scheduler.DAY_MS + 9 * 3_600_000L) >= READY

    fun plan(exam: Exam, cards: List<StudyCard>, now: Long): ExamPlan {
        val today = day(now)
        val daysLeft = (exam.day - today).toInt()
        val mine = cards.filter { it.bedId == exam.bedId }
        val readyCount = mine.count { ready(it, exam) }
        val fitness = if (mine.isEmpty()) 0.0 else readyCount.toDouble() / mine.size
        val phase = when {
            daysLeft < 0 -> ExamPlan.Phase.PAST
            daysLeft == 0 -> ExamPlan.Phase.EXAM_DAY
            daysLeft == 1 -> ExamPlan.Phase.FINAL_DAY
            daysLeft > 21 -> ExamPlan.Phase.EARLY
            else -> ExamPlan.Phase.BUILD
        }
        val retrAtExam = { c: StudyCard -> Scheduler.retrievability(c.memory, exam.day * Scheduler.DAY_MS) }
        val list: List<StudyCard> = when (phase) {
            ExamPlan.Phase.PAST -> emptyList()
            // Prüfungstag: nur kurz aufwärmen – die wackeligsten 10, kein Neues mehr
            ExamPlan.Phase.EXAM_DAY -> mine.filter { !it.memory.isSeed }.sortedBy { Scheduler.retrievability(it.memory, now) }.take(10)
            // Vortag: Generalprobe – alles, was noch nicht sitzt, aber nichts Neues mehr in großen Mengen
            ExamPlan.Phase.FINAL_DAY -> {
                val shaky = mine.filter { !it.memory.isSeed && !ready(it, exam) }.sortedBy(retrAtExam)
                val seeds = mine.filter { it.memory.isSeed }
                (shaky + seeds).take(MAX_PER_DAY)
            }
            else -> {
                // Abstand: höchstens alle ~20 % der Restzeit dieselbe Karte (mind. 1 Tag), außer sie ist durstig
                val gapMs = (maxOf(1.0, daysLeft * 0.2) * Scheduler.DAY_MS).toLong()
                val notReady = mine.filter { !ready(it, exam) }
                // Arbeit gleichmäßig auf die Tage vor dem Vortag verteilen
                val workDays = maxOf(1, daysLeft - 1)
                val quota = ceil(notReady.size.toDouble() / workDays).toInt().coerceIn(MIN_PER_DAY, MAX_PER_DAY)
                val thirsty = notReady.filter { !it.memory.isSeed && Scheduler.isThirsty(it.memory, now) }.sortedBy { Scheduler.retrievability(it.memory, now) }
                val seeds = notReady.filter { it.memory.isSeed }
                val early = notReady.filter { !it.memory.isSeed && it !in thirsty && now - it.memory.lastReview >= gapMs }.sortedBy(retrAtExam)
                // Neue Karten früh säen (Abstand braucht Zeit), dabei Wiederholungen nicht vergessen
                interleave(thirsty, seeds, early).take(quota)
            }
        }
        return ExamPlan(exam, daysLeft, fitness, readyCount, mine.size, list, phase)
    }

    private fun interleave(a: List<StudyCard>, b: List<StudyCard>, c: List<StudyCard>): List<StudyCard> {
        val out = LinkedHashSet<StudyCard>()
        out += a
        val bi = b.iterator(); val ci = c.iterator()
        while (bi.hasNext() || ci.hasNext()) {
            if (bi.hasNext()) out += bi.next()
            if (bi.hasNext()) out += bi.next()
            if (ci.hasNext()) out += ci.next()
        }
        return out.toList()
    }

    /** Was Brummi zum Stand sagt – ermutigend, nie Druck. */
    fun brummiSays(p: ExamPlan): String = when (p.phase) {
        ExamPlan.Phase.PAST -> "Wie ist es gelaufen? Erzähl's mir!"
        ExamPlan.Phase.EXAM_DAY -> "Heute ist der große Tag! Kurz aufwärmen – du schaffst das! 🍀"
        ExamPlan.Phase.FINAL_DAY -> if (p.fitness >= 0.8) "Morgen ist es so weit – du bist schon richtig fit! Eine kleine Generalprobe?"
            else "Morgen ist es so weit. Lass uns die wackeligen Karten noch einmal gießen!"
        ExamPlan.Phase.EARLY -> "Super, dass du so früh anfängst – so bleibt es am besten hängen!"
        ExamPlan.Phase.BUILD -> when {
            p.today.isEmpty() -> "Für heute ist alles erledigt. Morgen geht's weiter! 🌱"
            p.fitness >= 0.8 -> "Du bist schon zu ${p.fitnessPercent} % fit – weiter so!"
            p.readyCards == 0 -> "Noch ${p.daysLeft} Tage – genug Zeit! Heute ${p.today.size} Karten, dann wächst es Stück für Stück."
            else -> "Noch ${p.daysLeft} Tage. ${p.readyCards} Karten sitzen schon – heute ${p.today.size} dazu!"
        }
    }
}
