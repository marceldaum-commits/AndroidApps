package de.kurvenflug.core.model

/** Ein Bauteil von Volts Mutterschiff „Aurora“ – auf jedem Planeten (Kapitel) treibt eins. */
data class Part(val chapter: Int, val name: String, val emoji: String, val found: String, val arrival: String)

/**
 * Die Geschichte, die alle Kapitel verbindet: Volts Mutterschiff Aurora ist in einem Meteoritensturm zerbrochen,
 * die Bauteile treiben über die Galaxie verstreut. Wer ein Kapitel ganz schafft, bringt ein Bauteil zurück.
 * Kein Zeitdruck, nichts geht verloren – das Schiff wächst einfach mit.
 */
object Story {
    const val SHIP = "Aurora"
    val intro = listOf(
        "Oh nein! Mein Mutterschiff, die $SHIP, ist in einem Meteoritensturm zerbrochen. 😱",
        "Ihre ${18} Bauteile treiben jetzt über die ganze Galaxie verstreut – auf jedem Planeten eins.",
        "Jeder Planet ist ein Kapitel. Schaffst du alle Level, finden wir das Bauteil! 🔧",
        "Im Hangar siehst du, wie die $SHIP wieder zusammenwächst. Hilfst du mir? 🚀",
    )

    val parts: List<Part> = listOf(
        Part(1, "Navi-Computer", "🧭", "Mein Navi-Computer! Ohne den hätte ich mich ewig verflogen.", "Hier auf dem Koordinaten-Planeten piept etwas – das klingt nach meinem Navi-Computer!"),
        Part(15, "Treibstofftank", "⛽", "Der Treibstofftank – zu ¾ voll, haha! 🍕", "Auf diesem Planeten ist alles in Stücke geteilt … irgendwo liegt mein Tank!"),
        Part(16, "Steuerruder", "🎚️", "Das Steuerruder! Jetzt kann ich wieder um jeden Winkel kurven.", "Lauter Winkel hier! Mein Steuerruder muss in der Nähe sein."),
        Part(8, "Frachtraum", "📦", "Der Frachtraum – doppelt so groß, doppelt so viel Platz!", "Auf dem Zuordnungs-Planeten wird alles abgewogen. Da hängt mein Frachtraum!"),
        Part(13, "Bord-Uhr", "⏱️", "Die Bord-Uhr tickt wieder. Pünktlich wie immer!", "Hier dreht sich alles um Weg und Zeit – und meine Bord-Uhr tickt irgendwo."),
        Part(11, "Suchscheinwerfer", "🔦", "Mein Suchscheinwerfer! Jetzt sehe ich auch in dunklen Ecken.", "Laserstrahlen überall! Mein Suchscheinwerfer ist bestimmt dazwischen."),
        Part(2, "Linker Flügel", "🪽", "Der linke Flügel – schnurgerade wie eine Gerade!", "Auf dem Geraden-Planeten blinkt etwas Langes, Glattes. Mein linker Flügel!"),
        Part(7, "Andockring", "🔗", "Der Andockring! Jetzt kann ich wieder an Stationen andocken.", "Zwei Sonden, ein Treffpunkt – hier liegt mein Andockring."),
        Part(9, "Funkantenne", "📡", "Die Funkantenne! Hallo, Galaxie, hört ihr mich?", "Störsender überall … aber dazwischen funkt meine Antenne!"),
        Part(3, "Rechter Flügel", "🪽", "Der rechte Flügel – schön geschwungen wie eine Parabel!", "Bögen, wohin man schaut. Mein rechter Flügel ist hier gelandet."),
        Part(18, "Landebeine", "🦿", "Landebeine! Jetzt lande ich genau an der Nullstelle. 🎯", "Wo landet man hier? Genau dort liegen meine Landebeine!"),
        Part(12, "Energiespeicher", "🔋", "Der Energiespeicher – randvoll mit Lageenergie!", "Rampen und Kugeln … mein Energiespeicher rollt hier irgendwo herum."),
        Part(17, "Kristallfenster", "💎", "Das Kristallfenster! Das Licht knickt so schön hindurch.", "Glitzernde Kristalle! Eins davon ist mein Cockpit-Fenster."),
        Part(4, "Schutzschild", "🛡️", "Der Schutzschild – kein Meteor kommt mehr durch!", "Auf diesem Planeten fliegt alles im Bogen – auch mein Schutzschild."),
        Part(14, "Zielcomputer", "🎯", "Mein Zielcomputer: sin, cos, tan – alles drin!", "Lauter rechte Winkel und Dreiecke. Mein Zielcomputer rechnet hier irgendwo."),
        Part(5, "Sonar", "🔊", "Das Sonar! Es schwingt wieder im Sinus-Takt.", "Wellen überall – mein Sonar schwingt bestimmt mit."),
        Part(10, "Rechenkern", "🧮", "Mein Rechenkern! log, Potenzen – alles wieder da.", "Auf dem Logarithmus-Planeten rechnet etwas ganz schnell. Mein Rechenkern!"),
        Part(6, "Gravitationsantrieb", "🚀", "Der Gravitationsantrieb! Die $SHIP ist komplett – wir fliegen wieder! 🎉", "Planeten, die an dir ziehen … hier wartet das wichtigste Teil: mein Antrieb!"),
    )

    fun part(chapter: Int) = parts.firstOrNull { it.chapter == chapter }

    /** Satz, mit dem Volt einen Planeten beim ersten Besuch begrüßt (vor der Kapitel-Erklärung). */
    fun arrival(chapter: Int): String? = part(chapter)?.let { "🪐 ${it.arrival} ${it.emoji}" }

    /** Gefundene Bauteile: Kapitel, in denen jedes Level geschafft ist. */
    fun found(chapters: List<Chapter>, stars: Map<String, Int>): List<Part> =
        parts.filter { p -> chapters.firstOrNull { it.number == p.chapter }?.levels?.all { (stars[it.id] ?: 0) > 0 } == true }
}
