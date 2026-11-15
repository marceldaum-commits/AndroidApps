package de.kurvenflug.core.model

/** Ein Freund von Volt, der irgendwo in der Galaxie gestrandet ist – nach der Rettung Teil der Crew. */
data class CrewMember(
    val n: Int,
    val name: String,
    val emoji: String,
    val role: String,
    /** Notruf vor der Rettung */
    val call: String,
    /** Dank nach der Rettung */
    val thanks: String,
    /** Sprüche auf der Startseite (Tipps, Witze, Lob) */
    val lines: List<String>,
)

/**
 * Staffel 2 – damit es nach der fertigen Aurora weitergeht:
 * - **Gold-Teile**: Ein Bauteil wird golden, wenn das ganze Kapitel mit 3 ⭐ geschafft ist (sauber rechnen statt raten).
 * - **Rettungsmissionen**: 8 Freunde senden Notrufe. Jede Rettung = 5 gemischte Level aus verschiedenen Themen
 *   (gemischtes Üben wirkt laut Studien besonders gut). Gerettete Freunde ziehen in die Crew ein.
 * Freigeschaltet wird nach und nach: die ersten vier über gefundene Bauteile, die nächsten über die fertige Aurora und Gold.
 */
object Season2 {
    const val PREFIX = "rescue-"

    val crew = listOf(
        CrewMember(1, "Pixel", "🤖", "Putzroboter", "Piep … Hilfe! Ich bin in einem Asteroidenfeld stecken geblieben und mein Akku ist fast leer!",
            "Danke! Ich putze ab jetzt die Aurora – blitzblank!", listOf("Ich hab die Kommastellen poliert. ✨", "Ordnung ist das halbe Rechnen!", "Hast du heute schon was gerechnet? Ich hab schon geputzt. 🧽")),
        CrewMember(2, "Kapitänin Kora", "🏴‍☠️", "Weltraum-Piratin", "Ahoi! Meine Schatzkarte ist voller Gleichungen – und mein Schiff steckt fest!",
            "Arrr, du rechnest besser als meine ganze Mannschaft! Ich bin dabei!", listOf("Ein Schatz ist nur ein Punkt (x | y) – wer rechnet, findet ihn! 🗺️", "Arrr! Drei Sterne sind mehr wert als Gold!", "Auch Piratinnen machen Fehler. Dann rechnen wir eben nochmal.")),
        CrewMember(3, "Professor Quark", "🧪", "Physik-Genie", "Faszinierend! Mein Labor driftet in ein Schwarzes Loch. Das ist … ungünstig.",
            "Eine Rettung mit perfekter Flugbahn! Ich nehme das in meine Forschung auf.", listOf("Wusstest du? Auf dem Mond fliegt alles sechsmal so hoch! 🌙", "Energie geht nie verloren – sie wandelt sich nur um.", "Licht ist schneller als alles – aber im Glas wird es langsamer! 💎")),
        CrewMember(4, "Blink", "👾", "Alien-Kind", "Blib blub! Ich hab mich auf dem Heimweg verflogen. Kannst du Kurven?",
            "Blib! Du bist mein bester Erdling-Freund!", listOf("Auf meinem Planeten rechnen wir mit acht Fingern! 🖐️🖐️", "Blib blub – das heißt „Gut gemacht“!", "Eine Parabel sieht aus wie unser Raumschiff von unten. 🛸")),
        CrewMember(5, "Turbo", "🏎️", "Renn-Drohne", "Wrrrm! Ich war zu schnell und bin aus der Kurve geflogen!",
            "Wrrrm! Mit dir nehme ich jede Kurve!", listOf("Schnell ist gut – genau ist besser! 🏁", "v = Weg : Zeit. Ich bin sehr, sehr v! 💨", "Erst rechnen, dann Vollgas!")),
        CrewMember(6, "Luna", "🐱", "Mond-Katze", "Miau … ich bin auf einem Kometen eingeschlafen und weit weg getrieben.",
            "Schnurr … danke! Ich passe jetzt auf dich auf.", listOf("Miau. Eine kurze Runde, dann ein Nickerchen? 😺", "Ich zähle gern Sterne. Du hast schon viele!", "Schnurr … Pausen sind auch wichtig.")),
        CrewMember(7, "Gizmo", "🔧", "Mechanikerin", "Mein Werkzeugkasten ist explodiert und alle Schrauben fliegen herum!",
            "Mit dir repariere ich jeden Antrieb. Ab jetzt schraube ich an der Aurora!", listOf("Jede Formel ist wie ein Bauplan. 🔩", "Kaputt? Gibt's nicht – nur „noch nicht fertig“!", "Ich hab die Triebwerke auf 3 Sterne getunt. ⭐")),
        CrewMember(8, "Nova", "✨", "Sternengeist", "Ich bin ein Sternengeist … und mein Stern verlöscht. Nur du kannst ihn wieder zünden.",
            "Mein Stern leuchtet wieder! Ich zeige dir den Weg zu einer neuen Galaxie …", listOf("Jeder Stern am Himmel war mal eine Rechnung. ✨", "Hinter der Galaxie wartet noch mehr …", "Du leuchtest heller als ich!")),
    )

    fun member(n: Int) = crew.firstOrNull { it.n == n }
    fun id(n: Int) = "$PREFIX$n"
    fun isRescue(id: String) = id.startsWith(PREFIX)

    /** Gold: alle Level des Kapitels mit 3 ⭐ */
    fun gold(chapters: List<Chapter>, stars: Map<String, Int>): List<Part> =
        Story.parts.filter { p -> chapters.firstOrNull { it.number == p.chapter }?.levels?.all { (stars[it.id] ?: 0) >= 3 } == true }

    /** Was braucht Rettung n noch? null = verfügbar */
    fun lock(n: Int, parts: Int, gold: Int): String? = when (n) {
        in 1..4 -> listOf(3, 7, 11, 15)[n - 1].let { need -> if (parts >= need) null else "Noch ${need - parts} Bauteile finden" }
        5 -> if (parts >= Story.parts.size) null else "Erst die Aurora fertig bauen"
        else -> listOf(3, 6, 9)[n - 6].let { need -> if (parts >= Story.parts.size && gold >= need) null else "$need Gold-Teile nötig (hast $gold)" }
    }

    /** Kapitel ohne Level-Generator – daraus kommen die festen Etappen (gemischtes Üben über alle Themen) */
    private val fixedPool = listOf(15, 16, 8, 13, 11, 9, 12, 17, 18, 14, 10)

    /** Die 5 Etappen einer Rettung: abwechselnd neu gewürfelt und aus anderen Kapiteln, gegen Ende schwerer. */
    fun stages(n: Int): List<Level> {
        val kinds = Gen.kinds
        fun gen(k: Int, diff: Int) = Gen.level(kinds[Math.floorMod(n * 3 + k, kinds.size)], n * 7_919L + k * 104_729L, diff)
        fun fixed(k: Int): Level {
            val c = Levels.chapters.first { it.number == fixedPool[Math.floorMod(n * 2 + k * 5, fixedPool.size)] }
            return c.levels[Math.floorMod(n + k, c.levels.size)]
        }
        val hard = if (n >= 5) 2 else 1
        return listOf(gen(0, 1), fixed(1), gen(2, hard), fixed(3), gen(4, 2))
            .mapIndexed { i, l -> l.copy(id = "$PREFIX$n-${i + 1}", number = i + 1, title = "Etappe ${i + 1} von 5") }
    }
}
