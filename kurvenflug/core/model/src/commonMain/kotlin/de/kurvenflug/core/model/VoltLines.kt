package de.kurvenflug.core.model

/**
 * Volts Sätze mit Zahlen oder Namen, die sich vorhersehen lassen – an einer Stelle, damit App und Vorlese-Liste
 * (vorlesen/texte.json, „laufzeit“) denselben Wortlaut haben und Nova sie vorab als ganze Sätze spricht.
 */
object VoltLines {
    const val DAILY_DAYS = 60
    fun andromeda(done: Int, nova: Int) = when {
        done == 0 -> "Willkommen in Andromeda! Hier ist alles eine Stufe schwerer – und jeder Planet bringt ein Teil für unser zweites Schiff, die ${Andromeda.SHIP}. 🌌"
        nova < Andromeda.PARTS -> "$done Planeten geschafft! Noch ${Andromeda.PARTS - nova} Teile, dann fliegt die ${Andromeda.SHIP}."
        else -> "Die ${Andromeda.SHIP} ist fertig! Andromeda ist endlos – wie weit kommst du? 🚀"
    }
    fun story(found: Int, gold: Int, complete: Boolean) = when {
        gold == Story.parts.size -> "Alles Gold! Die ${Story.SHIP} fliegt mit Warp-Antrieb – du bist ein echter Profi! 🌟"
        complete && gold > 0 -> "Schon $gold Gold-Teile! Jedes Kapitel mit lauter 3 ⭐ macht ein Teil golden. ✨"
        complete -> "Die ${Story.SHIP} fliegt! Jetzt machen wir sie golden: Schaff ein Kapitel ganz mit 3 ⭐ – dann glänzt sein Teil. ✨"
        found == 0 -> "Hier bauen wir die ${Story.SHIP} wieder zusammen. Schaff ein ganzes Kapitel – dann finden wir das erste Teil!"
        else -> "Schon $found Teile! Jeder Planet, den du ganz schaffst, bringt eins mehr. 🔧"
    }
    fun storyPart(p: Part, gold: Boolean) = if (gold) "Alle Level mit 3 ⭐ – mein ${p.name} glänzt jetzt golden!" else p.found
    fun sticker(name: String, emoji: String) = "Geschafft! Der $name-Sticker klebt jetzt in deinem Album. $emoji"
    fun guest(during: String) = "Ein Gast-Planet $during! 5 Etappen – und am Ende gibt's einen besonderen Sticker. Keine Eile: Er kommt jedes Jahr wieder."
    fun refresh(chapterTitle: String) = "Heute kurz auffrischen: $chapterTitle! Wer in Abständen wiederholt, behält es viel länger. 🧠"
    fun walkthroughDone(settings: List<String>) = "Super gerechnet! 🎉 Jetzt stell ${settings.joinToString(" und ")} ein und starte!"
    /** „x = 3“ … für den Rechenweg-Schluss */
    fun settingsOf(level: Level, steps: List<Step>) = steps.mapNotNull { s -> s.sets?.let { k -> "${level.params.first { it.key == k }.label} = ${s.answerText}" } }

    /** Vorhersehbare Laufzeit-Sätze (für vorlesen/texte.json mit "laufzeit": true) */
    fun runtime(today: Long): List<String> {
        val out = LinkedHashSet<String>()
        val maxStars = Levels.all.size * 3 + 200
        out += listOf(Greeting.home(12, 5, 1).second, Greeting.home(23, 0, 1).second, Greeting.home(12, 0, 0).second)
        for (s in 1..maxStars) { out += Greeting.home(8, 0, s).second; out += Greeting.home(12, 0, s).second }
        for (d in 0..Andromeda.PARTS + 2) out += andromeda(d, minOf(d, Andromeda.PARTS))   // Teile = geschaffte Planeten (höchstens 18)
        for (f in 0..Story.parts.size) out += story(f, 0, false)
        for (g in 0..Story.parts.size) { out += story(Story.parts.size, g, true) }
        Story.parts.forEach { out += storyPart(it, true); out += storyPart(it, false) }
        Stickers.all.forEach { out += sticker(it.name, it.emoji) }
        SeasonEvent.entries.forEach { e -> out += sticker(e.sticker.name, e.emoji); out += guest(e.during) }
        Levels.chapters.forEach { out += refresh(it.title) }
        ReadTexts.fixedLevels().forEach { l -> runCatching { Walkthrough.of(l) }.getOrNull()?.let { out += walkthroughDone(settingsOf(l, it)) } }
        // Tagesmissionen der nächsten 60 Tage (jeder Bau schiebt das Fenster weiter): Auftrag, Tipp, Rechen-Stern
        for (d in today..today + DAILY_DAYS) { val l = Daily.forDay(d); out += l.task; out += l.tip
            Understand.check(l)?.let { c -> out += ReadTexts.calcQuestion(c); out += ReadTexts.calcAnswer(c, runCatching { Walkthrough.of(l) }.getOrNull()) } }
        return out.filter { it.isNotBlank() }
    }
}
