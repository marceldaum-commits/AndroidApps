package de.raumlicht.core.model

/**
 * Pro („Abi-Pass“): was frei bleibt und was dazugehört. **Vorbereitet, aber ausgeschaltet** ([ENABLED] = false) – solange ist alles frei,
 * es gibt keinen Kauf und keine Sperre. Eingeschaltet wird erst, wenn Bezahlwege und Lizenz-Dienst stehen (siehe MELDUNG_AN_LEITSTAND.md, „Konzept Pro“).
 * Hier stehen nur die Regeln; freigeschaltet wird später über [Prefs.pro].
 */
object Pro {
    /** Hauptschalter. Erst auf true setzen, wenn man Pro auch wirklich kaufen kann – und dann die Texte „keine Käufe“ anpassen. */
    const val ENABLED = false
    /** Die ersten Kapitel sind ganz frei … */
    const val FREE_CHAPTERS = 6
    /** … von jedem weiteren die ersten Aufgaben zum Reinschnuppern */
    const val FREE_PER_CHAPTER = 2

    /** Feste Aufgabe der Tour frei? Tagesaufgabe und erzeugte Übungsaufgaben haben eigene Regeln (siehe [locked]). */
    fun freeLevel(id: String): Boolean {
        val chapter = Levels.chapters.firstOrNull { c -> c.levels.any { it.id == id } } ?: return true
        return chapter.number <= FREE_CHAPTERS || chapter.levels.indexOfFirst { it.id == id } < FREE_PER_CHAPTER
    }
    fun freeExam(id: String) = id == Exams.all.first().id
    /** Im endlosen Üben ist die Stufe „Einstieg“ frei; „Abi“, „Profi“ und die Mischung aller Stufen gehören zu Pro */
    fun freeJam(level: JamLevel?) = level == JamLevel.EINSTIEG
    /** Ganze Bereiche, die zu Pro gehören (Namen der Ziele im App-Rahmen) */
    val areas = mapOf("baumeister" to "Achterbahn-Baumeister", "geisterfahrt" to "Geisterfahrt", "ringflug" to "Ringflug", "binomial" to "Binomial-Labor",
        "klausur" to "Klausur-Planer", "fehler-tagebuch" to "Fehler-Tagebuch", "lernstand" to "Lernstand")

    /** Gehört dieses Ziel zu Pro? (Unabhängig davon, ob Pro eingeschaltet oder gekauft ist.) */
    fun locked(route: String): Boolean { val p = route.split('/')
        return when (p[0]) {
            "level" -> p.size > 1 && !freeLevel(p[1])
            "abi" -> p.size > 1 && !freeExam(p[1])
            "jam" -> p.size > 1 && !freeJam(JamLevel.entries.firstOrNull { it.name == p.getOrNull(2) })
            else -> p[0] in areas
        } }
    /** Was hinter der Sperre liegt, in Worten – für die Hinweis-Seite */
    fun what(route: String): String { val p = route.split('/')
        return when (p[0]) {
            "level" -> Levels.byId(p.getOrElse(1) { "" })?.let { l -> "Aufgabe ${l.id} „${l.title}“" } ?: "Diese Aufgabe"
            "abi" -> Exams.all.firstOrNull { it.id == p.getOrNull(1) }?.let { "Prüfungsaufgabe „${it.title}“" } ?: "Diese Prüfungsaufgabe"
            "jam" -> JamLevel.entries.firstOrNull { it.name == p.getOrNull(2) }?.let { "Üben in der Stufe „${it.title}“" } ?: "Üben mit allen Stufen gemischt"
            else -> areas[p[0]] ?: "Dieser Bereich"
        } }
    /** Was der Abi-Pass bringt (für die Hinweis-Seite) */
    val benefits = listOf("📚 alle ${Levels.all.size} Aufgaben in ${Levels.chapters.size} Kapiteln", "🎓 alle ${Exams.all.size} Prüfungsaufgaben im Abi-Stil, auch auf Zeit",
        "🔥 Üben in den Stufen „Abi“ und „Profi“", "🏗️ Baumeister, Geisterfahrt, Ringflug und Binomial-Labor", "📅 Klausur-Planer, Fehler-Tagebuch und Lernstand")
    /** Was auch ohne Pro bleibt */
    val free = listOf("Kapitel 1 bis $FREE_CHAPTERS komplett", "aus jedem weiteren Kapitel die ersten $FREE_PER_CHAPTER Aufgaben", "Tagesaufgabe", "Üben in der Stufe „Einstieg“", "Kurve abfahren, Flug, Ballerbude, Zufallsmaschine, Galton-Brett")
}
