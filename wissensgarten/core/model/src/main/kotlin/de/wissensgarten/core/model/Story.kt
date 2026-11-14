package de.wissensgarten.core.model

/** Ein Kapitel aus Brummis Abenteuer. Wird freigeschaltet, sobald genug Pflanzen gewachsen sind. */
data class Chapter(val number: Int, val title: String, val emoji: String, val unlockAt: Int, val scenes: List<IntroScene>)

/**
 * „Brummi und die Wissensblume“ – eine Geschichte, die mit dem Lernfortschritt weitergeht.
 * Freischaltung über die Zahl gewachsener Pflanzen (nicht über Zeit oder Geld).
 */
object Story {
    val chapters = listOf(
        Chapter(1, "Der graue Morgen", "🌫️", 0, listOf(
            IntroScene("🌸✨", "Mitten im Garten wuchs die Wissensblume. Sie leuchtete in allen Farben – von allem, was hier je gelernt wurde."),
            IntroScene("🌫️😮", "Doch eines Morgens kroch grauer Nebel heran: der Vergessens-Nebel. Die Blume wurde blass, ganz blass …"),
            IntroScene("🐝❓", "„Was ist los?“, summte Brummi erschrocken. „Ohne die Wissensblume verblüht unser ganzer Garten!“"),
            IntroScene("🌱💧", "Die alte Eule flüsterte: „Nur wer Neues lernt und Altes gießt, bringt das Leuchten zurück.“ Brummi schaut dich an: „Hilfst du mir?“"),
        )),
        Chapter(2, "Der erste Funke", "💡", 5, listOf(
            IntroScene("💡🌱", "Mit jeder Pflanze, die du gießt, springt ein kleiner Funke in die Luft. Brummi fängt ihn vorsichtig ein."),
            IntroScene("🐝✨", "„Schau!“, ruft Brummi. „Die Funken sind gewachsenes Wissen. Fünf haben wir schon!“"),
            IntroScene("🌸💛", "Als Brummi die Funken zur Wissensblume trägt, leuchtet ein einziges Blütenblatt wieder gelb."),
            IntroScene("🌫️👀", "Der Nebel zieht sich ein Stück zurück. Aber in der Ferne grummelt er noch. Wir müssen weitermachen!"),
        )),
        Chapter(3, "Schnecki weiß es besser", "🐌", 15, listOf(
            IntroScene("🐌🏃", "Am Wegrand trifft Brummi Schnecki. „Du bist aber langsam!“, lacht Brummi."),
            IntroScene("🐌🧠", "„Langsam ja“, sagt Schnecki, „aber ich vergesse nie etwas. Ich wiederhole jeden Tag ein kleines bisschen.“"),
            IntroScene("☀️💧", "Brummi staunt: Lieber jeden Tag ein paar Minuten gießen als einmal ganz viel. So mag das Gehirn es am liebsten."),
            IntroScene("🌸🧡", "Ein zweites Blütenblatt leuchtet – orange wie Schneckis Haus."),
        )),
        Chapter(4, "Der Nebelwald", "🌲", 30, listOf(
            IntroScene("🌲🌫️", "Hinter dem Hügel liegt der Nebelwald. Hier ist der Vergessens-Nebel so dicht, dass man kaum die eigenen Flügel sieht."),
            IntroScene("🐝😨", "Brummi zittert. „Ich weiß nicht mehr, wo es langgeht!“"),
            IntroScene("🗺️⭐", "Da erinnert sie sich an deinen Abenteuerpfad: Jede geschaffte Station leuchtet wie ein Stern im Nebel."),
            IntroScene("✨🌲", "Station für Station finden sie den Weg hinaus. Wissen ist wie eine Laterne!"),
        )),
        Chapter(5, "Das Rätsel der Eule", "🦉", 50, listOf(
            IntroScene("🦉📜", "Mitten im Wald sitzt Professorin Eule auf einem alten Buch. „Wer weiter will, muss mein Rätsel lösen.“"),
            IntroScene("❓🤔", "„Was wird größer, je mehr man davon teilt?“ Brummi grübelt und grübelt."),
            IntroScene("🧠💡", "„Wissen!“, ruft Brummi. „Wenn ich dir etwas erkläre, wissen wir es beide – und ich merke es mir noch besser!“"),
            IntroScene("🦉🌟", "Die Eule nickt zufrieden. „Darum: Bring anderen etwas bei. Das ist der stärkste Zauber.“"),
        )),
        Chapter(6, "Der Fluss der Fehler", "🌊", 80, listOf(
            IntroScene("🌊💦", "Vor ihnen rauscht ein wilder Fluss. Auf jedem Stein steht ein Fehler, den Brummi schon gemacht hat."),
            IntroScene("🐝😟", "„Oh nein, so viele Fehler!“, seufzt Brummi."),
            IntroScene("🐸💚", "Quaki hüpft heran: „Fehler sind keine Steine im Weg – sie SIND der Weg! Auf jedem lernst du etwas.“"),
            IntroScene("🌉✨", "Stein für Stein springt Brummi hinüber. Am anderen Ufer leuchtet das dritte Blütenblatt grün."),
        )),
        Chapter(7, "Der Berg der Serien", "⛰️", 120, listOf(
            IntroScene("⛰️🔥", "Ein hoher Berg ragt in den Himmel. Ganz oben brennt ein Feuer – das Feuer der langen Serien."),
            IntroScene("🐝💪", "Brummi fliegt höher und höher. Fünf richtige Antworten, zehn, zwanzig …"),
            IntroScene("🌬️😅", "Plötzlich bläst ein Windstoß sie zurück. Falsche Antwort! Aber Brummi lacht: „Dann fang ich eben neu an.“"),
            IntroScene("🔥⭐", "Oben angekommen, glüht das vierte Blütenblatt rot wie das Gipfelfeuer."),
        )),
        Chapter(8, "Die Höhle des Nebels", "🕳️", 170, listOf(
            IntroScene("🕳️🌫️", "In einer dunklen Höhle wohnt er: der Vergessens-Nebel selbst. Ganz grau und ganz müde."),
            IntroScene("🌫️😢", "„Ich mache das nicht mit Absicht“, schnieft der Nebel. „Ich komme einfach, wenn niemand mehr gießt.“"),
            IntroScene("🐝🤝", "Brummi überlegt. „Dann müssen wir dich nicht besiegen. Wir müssen nur dranbleiben!“"),
            IntroScene("💧🌈", "Der Nebel lächelt zum ersten Mal – und wird ein kleines bisschen durchsichtiger."),
        )),
        Chapter(9, "Alle Freunde kommen", "🐾", 230, listOf(
            IntroScene("🐞🦔🦋", "Auf dem Rückweg warten alle Gartenfreunde: Marie, Ignaz, Flatter, Nussi und viele mehr."),
            IntroScene("🎶🎉", "Sie haben jeden Funken aufbewahrt, den Brummi unterwegs verloren hatte."),
            IntroScene("🐝❤️", "„Allein hätte ich das nie geschafft“, sagt Brummi gerührt. „Danke – vor allem dir!“"),
            IntroScene("🌸💜", "Das fünfte Blütenblatt leuchtet lila wie die Abenddämmerung."),
        )),
        Chapter(10, "Die Wissensblume leuchtet", "🌸", 300, listOf(
            IntroScene("🌸🌈", "Zurück im Garten legt Brummi den letzten Funken in die Wissensblume. Sie strahlt heller als je zuvor!"),
            IntroScene("🌫️👋", "Der Vergessens-Nebel winkt zum Abschied. „Ich komme nur noch, wenn ihr lange nicht gießt“, verspricht er."),
            IntroScene("🏆🐝", "Brummi überreicht dir eine goldene Gießkanne. „Du bist jetzt Meistergärtner!“"),
            IntroScene("🌱🚀", "Doch im Garten keimen schon neue Samen … Das Abenteuer geht weiter – jeden Tag ein bisschen. ✨"),
        )),
    )

    fun unlocked(grownPlants: Int) = chapters.filter { grownPlants >= it.unlockAt }
    fun next(grownPlants: Int) = chapters.firstOrNull { grownPlants < it.unlockAt }
}
