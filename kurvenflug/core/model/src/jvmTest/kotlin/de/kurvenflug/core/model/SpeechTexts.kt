package de.kurvenflug.core.model

/**
 * Alle Texte aus dem Spiel-Modell, die vorgelesen werden können: Aufträge, Tipps, Kapitel-Erklärungen, Theorie,
 * Rechen-Stern-Fragen und Antworten, Rechenwege, Live-Rechnung, Geschichte.
 * „fest“ = steht wörtlich fest (wird als ganzer Satz in Lautschrift umgesetzt), „wort“ = Beispiel für veränderliche Texte.
 */
object SpeechTexts {
    private val levels: List<Level> by lazy {
        Levels.all + Levels.chapters.mapNotNull { ChapterExtreme.forChapter(it.number) } +
            Gen.kinds.flatMap { k -> (0..Tier.MAX).flatMap { t -> (1L..6L).map { Gen.level(k, it * 977 + t, t) } } } +
            Bosses.all.flatMap { it.stages } + (1..20).flatMap { Andromeda.planet(it).levels } + (1..12).flatMap { Season2.stages(it) } +
            SeasonEvent.entries.flatMap { Seasons.levels(it, 2026) } + (0..30).map { Daily.forDay(20_400L + it) }
    }

    fun all(): List<Pair<String, String>> {
        val fixed = LinkedHashSet<String>(); val more = LinkedHashSet<String>()
        val sure = Levels.all.map { it.id }.toSet()
        levels.forEach { l ->
            val into = if (l.id in sure) fixed else more
            into += l.task; into += l.tip; more += l.title
            l.quiz?.let { more += it }
            Understand.check(l)?.let { c -> into += "Rechen-Stern: ${c.question}"; into += c.options }
            runCatching { Walkthrough.of(l) }.getOrNull()?.forEach { more += it.solved() }
            Understand.live(l, l.solution)?.let { more += it }
            Understand.live(l, l.start)?.let { more += it }
        }
        Levels.chapters.forEach { c ->
            fixed += c.intro; more += c.title
            Story.arrival(c.number)?.let { fixed += it }
            c.levels.firstOrNull()?.let { l -> Theories.of(l, c)?.let { t -> t.formula?.let { fixed += it } } }
        }
        fixed += Story.intro; fixed += Speech.PROBE
        return fixed.filter { it.isNotBlank() }.map { "fest" to it } + (more - fixed).filter { it.isNotBlank() }.map { "wort" to it }
    }
}
