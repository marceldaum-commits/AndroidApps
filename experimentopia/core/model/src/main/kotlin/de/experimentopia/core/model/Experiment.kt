package de.experimentopia.core.model

import kotlin.math.abs

/** Die sieben Inseln von Experimentopia. */
enum class Island(val title: String, val emoji: String, val color: Long, val subtitle: String, val grades: String) {
    WASSER("Wasserinsel", "🌊", 0xFF3DB8FF, "Schwimmen, Sinken, Dichte", "Kl. 3–7"),
    KRAFT("Kraftwerk-Insel", "⚖️", 0xFFFFA23D, "Hebel, Rollen, Rampen", "Kl. 3–8"),
    STROM("Blitzinsel", "⚡", 0xFFFFE04D, "Strom und Magnete", "Kl. 3–9"),
    CHEMIE("Chemie-Vulkan", "🌋", 0xFFFF5FA2, "Säuren, Laugen, Reaktionen", "Kl. 3–10"),
    WAERME("Feuer & Eis", "🔥", 0xFFFF6B4A, "Schmelzen, Kochen, Mischen", "Kl. 3–8"),
    LICHTKLANG("Licht & Klang", "🌈", 0xFFB98CFF, "Farben, Prisma, Töne", "Kl. 3–8"),
    FALL("Fallturm-Insel", "🚀", 0xFF3DF5C8, "Fallen, Mond, Pendel", "Kl. 3–10"),
}

/** Eine Antwortmöglichkeit, optional mit Farbklecks */
data class Choice(val text: String, val emoji: String = "", val color: Long? = null)

/** Eine Frage mit einer richtigen Antwort. Die Antwort wird im Katalog aus den Modellen berechnet, nicht hingeschrieben. */
data class Quiz(val question: String, val choices: List<Choice>, val answer: Int, val explain: String) {
    init { require(answer in choices.indices) { "Antwort außerhalb der Auswahl: $question" } }
}

/**
 * Ein Versuch nach dem Forscher-Kreislauf: Rätsel → Vermuten → Ausprobieren → Auflösung → Blubb erklären → Merksatz → Knobelfrage.
 */
data class Experiment(
    val id: String,
    val island: Island,
    val title: String,
    val emoji: String,
    val grades: IntRange,
    /** Das Rätsel, mit dem Blubb den Versuch ankündigt */
    val story: String,
    /** Vermutung vor dem Versuch */
    val predict: Quiz,
    val setup: Setup,
    /** Was im Labor zu tun ist */
    val task: String,
    /** Was passiert ist */
    val reveal: String,
    /** Blubb fragt „Warum?“ – die erste Begründung ist die richtige (die Oberfläche mischt) */
    val why: List<String>,
    /** Merksatz fürs Forscherbuch */
    val fact: String,
    /** Knobelfrage: Wissen auf eine neue Lage übertragen */
    val transfer: Quiz,
) {
    val gradeText get() = if (grades.first == grades.last) "Kl. ${grades.first}" else "Kl. ${grades.first}–${grades.last}"
}

/** Hilfen für berechnete Antworten */
internal object Pick {
    /** Index des Werts, der am nächsten an [x] liegt */
    fun nearest(values: List<Double>, x: Double) = values.indices.minBy { abs(values[it] - x) }
    fun index(b: Boolean) = if (b) 0 else 1
}
