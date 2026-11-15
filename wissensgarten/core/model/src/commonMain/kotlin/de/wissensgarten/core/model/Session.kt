package de.wissensgarten.core.model

/** Eine Karte, wie sie in einer Gieß-Runde gespielt wird. */
data class StudyCard(
    val id: String,
    val bedId: String,
    val front: String,
    val back: String,
    val choices: List<String>?,
    val info: String?,
    val memory: Memory,
    /** Merkbild (Emojis), falls vorhanden */
    val img: String? = null,
) {
    val isChoice: Boolean get() = !choices.isNullOrEmpty()

    /** Antwortmöglichkeiten in stabiler, aber gemischter Reihenfolge. */
    fun options(): List<String> {
        val all = (choices.orEmpty() + back).distinct()
        val seed = id.hashCode()
        return all.sortedBy { (it.hashCode() * 31 + seed).hashCode() }
    }
}

object SessionBuilder {
    /**
     * Zuerst die durstigsten Pflanzen, dann neue Samen bis zum Tageslimit.
     * Samen werden reihum aus den Beeten gezogen, damit nicht nur ein Fach drankommt.
     */
    fun build(cards: List<StudyCard>, now: Long, seedsLeft: Int): List<StudyCard> {
        val thirsty = cards.filter { Scheduler.isThirsty(it.memory, now) }
            .sortedBy { Scheduler.retrievability(it.memory, now) }
        val seedsByBed = cards.filter { it.memory.isSeed }.groupBy { it.bedId }.values.map { it.toMutableList() }
        val seeds = mutableListOf<StudyCard>()
        var i = 0
        while (seeds.size < seedsLeft && seedsByBed.any { it.isNotEmpty() }) {
            val bed = seedsByBed[i % seedsByBed.size]
            if (bed.isNotEmpty()) seeds += bed.removeAt(0)
            i++
        }
        return thirsty + seeds
    }

    fun seedsLeft(perDay: Int, seedDay: Long, seedsToday: Int, now: Long): Int {
        val today = Scheduler.localDay(now)
        return (perDay - if (seedDay == today) seedsToday else 0).coerceAtLeast(0)
    }
}
