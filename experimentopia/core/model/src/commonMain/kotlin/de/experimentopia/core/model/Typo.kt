package de.experimentopia.core.model

/**
 * Satz-Feinheiten für die Anzeige: Zahl und Einheit bleiben zusammen („5 %“ bricht auf schmalen Handys nicht mehr als „5“ / „%“ um).
 * Dafür steht zwischen beiden ein geschütztes Leerzeichen; [Speech.clean] macht daraus wieder ein normales (Vorlesen und Nova-Dateinamen bleiben gleich).
 */
object Typo {
    const val NBSP = ' '
    private val unit = Regex("""(\d) (?=(?:%|‰|°C|°|×|Hz|mol|min|km|cm|mm|nm|kg|mg|mL|kJ|kΩ|Ω|[mkMµ]?[gmLlsJWVANK])(?![\p{L}\d]))""")
    fun keep(s: String): String = unit.replace(s) { it.groupValues[1] + NBSP }

    fun Choice.typeset() = copy(text = keep(text))
    fun Quiz.typeset() = copy(question = keep(question), choices = choices.map { it.typeset() }, explain = keep(explain))
    fun Experiment.typeset() = copy(story = keep(story), predict = predict.typeset(), task = keep(task), reveal = keep(reveal),
        why = why.map { keep(it) }, fact = keep(fact), transfer = transfer.typeset())
}
