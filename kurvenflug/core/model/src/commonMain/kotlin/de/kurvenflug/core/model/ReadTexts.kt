package de.kurvenflug.core.model

/**
 * Alles, was Volt vorliest und schon beim Bauen feststeht – für die Liste vorlesen/texte.json (KI-Stimme Nova).
 * Die zusammengesetzten Vorlese-Texte (Rechen-Stern, Theorie am Stück) entstehen hier, damit App
 * und Liste genau denselben Wortlaut haben.
 */
object ReadTexts {
    private val ordinals = listOf("Erstens", "Zweitens", "Drittens", "Viertens")

    /** Rechen-Stern vor der Antwort: Frage und alle Möglichkeiten */
    fun calcQuestion(c: CalcCheck) = "Rechen-Stern: ${c.question} " + c.options.mapIndexed { i, o -> "${ordinals.getOrElse(i) { "" }}: $o." }.joinToString(" ")

    /** Rechen-Stern nach der Antwort: richtige Rechnung und Lösungsweg */
    fun calcAnswer(c: CalcCheck, worked: List<Step>?) =
        "Richtig ist: ${c.options[c.correct]}." + (worked?.let { w -> " So rechnet man's: " + w.joinToString(" ") { it.solved() + "." } } ?: "")

    /**
     * Hat das Kind mit anderen Werten getroffen als die hinterlegte Lösung (mehrere Lösungen, z. B. flacher oder steiler Wurf),
     * sagt die Lösungsanzeige ausdrücklich, dass sein Weg auch richtig ist. null = gleiche Lösung.
     */
    fun ownWay(l: Level, values: Map<String, Double>): String? {
        val differs = l.params.filter { p -> kotlin.math.abs((values[p.key] ?: p.start) - (l.solution[p.key] ?: p.start)) > 1.5 * p.step }
        if (differs.isEmpty()) return null
        val mine = l.params.joinToString(", ") { p -> "${p.label} = ${num(values[p.key] ?: p.start)}${p.unit}" }
        val stored = l.params.joinToString(", ") { p -> "${p.label} = ${num(l.solution[p.key] ?: p.start)}${p.unit}" }
        return "Deine Einstellung ($mine) ist auch richtig – diese Aufgabe hat mehrere Lösungen. Der Lösungsweg zeigt die Lösung $stored."
    }

    /** Theorie am Stück */
    fun theoryAll(t: Theory) = (listOf(t.title) + t.points + listOfNotNull(t.formula)).joinToString(" ") { it.trimEnd().let { p -> if (p.last() in ".!?:") p else "$p." } }

    /** Level mit festem Inhalt (keine Zufalls-Level wie Tagesmission oder Generator) */
    fun fixedLevels(): List<Level> = Levels.all + Levels.chapters.mapNotNull { ChapterExtreme.forChapter(it.number) } +
        Bosses.all.flatMap { it.stages } + (1..20).flatMap { Andromeda.planet(it).levels } + (1..12).flatMap { Season2.stages(it) }

    /** Alle festen Vorlese-Texte, wie sie angezeigt werden (vor [Speech.clean]). */
    fun fixed(): List<String> {
        val out = LinkedHashSet<String>()
        fixedLevels().forEach { l ->
            out += l.task; out += l.tip
            Understand.check(l)?.let { c -> out += calcQuestion(c); out += calcAnswer(c, runCatching { Walkthrough.of(l) }.getOrNull()) }
            Theories.of(l, Levels.chapters.firstOrNull { it.number == l.chapter })?.let { t ->
                out += t.points; t.formula?.let { out += it }; out += theoryAll(t)
            }
        }
        Levels.chapters.forEach { c -> out += c.intro; Story.arrival(c.number)?.let { out += it } }
        out += Story.intro; Story.parts.forEach { out += it.found }
        Season2.crew.forEach { m -> out += m.call; out += m.thanks; out += m.lines }
        out += Speech.PROBE
        return out.filter { it.isNotBlank() }
    }
}
