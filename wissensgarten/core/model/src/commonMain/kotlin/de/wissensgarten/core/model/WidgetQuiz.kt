package de.wissensgarten.core.model

import kotlin.random.Random

/** Eine Mini-Frage fürs Startbildschirm-Widget: zwei Antworten, eine stimmt. */
data class WidgetQuestion(val card: StudyCard, val options: List<String>, val correct: Int)

object WidgetQuiz {
    /**
     * Bevorzugt durstige Karten (fällig), sonst bekannte Karten. Samen (neu) nicht – neue Dinge lernt man in der App mit Erklärung.
     * Antworten sollen kurz sein, damit sie auf einen Widget-Knopf passen.
     */
    fun pick(cards: List<StudyCard>, now: Long, random: Random = Random.Default): WidgetQuestion? {
        val usable = cards.filter { !it.memory.isSeed && it.back.length <= 28 && it.front.length <= 90 }
        val pool = usable.filter { Scheduler.isThirsty(it.memory, now) }.ifEmpty { usable }
        val card = pool.randomOrNull(random) ?: return null
        val wrong = card.choices.orEmpty().filter { it != card.back && it.length <= 28 }.randomOrNull(random)
            ?: usable.filter { it.bedId == card.bedId && it.back != card.back }.map { it.back }.randomOrNull(random)
            ?: usable.filter { it.back != card.back }.map { it.back }.randomOrNull(random)
            ?: return null
        val correct = random.nextInt(2)
        val options = if (correct == 0) listOf(card.back, wrong) else listOf(wrong, card.back)
        return WidgetQuestion(card, options, correct)
    }
}
