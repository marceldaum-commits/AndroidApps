package de.kurvenflug.core.model

/**
 * Boss-Kampf am Ende eines Kapitels: ein Alien-Mutterschiff mit drei Schild-Segmenten.
 * Jede geschaffte Etappe zerstört ein Segment. Sterne nach Fehlversuchen insgesamt.
 */
data class Boss(val chapter: Int, val name: String, val emoji: String, val stages: List<Level>) {
    val id get() = "boss-$chapter"
}

object Bosses {
    private val names = mapOf(
        1 to ("Kartograf Kraax" to "👾"), 7 to ("Doppelgänger Zwo" to "👯"), 2 to ("Linear-Lord" to "🛸"),
        3 to ("Bogen-Biest" to "🐙"), 4 to ("Katapult-Koloss" to "🦂"), 5 to ("Wellen-Wyrm" to "🐉"), 6 to ("Schwarzes Loch" to "🕳️"),
    )
    private val kindOf = mapOf(1 to Kind.POINT, 7 to Kind.RENDEZVOUS, 2 to Kind.LINE, 3 to Kind.PARABOLA, 4 to Kind.THROW)

    private fun orbitStage(k: Int, target: Body, rocks: List<Body>, masses: List<Double>, launch: Vec, sol: Pair<Double, Double>) =
        Level("boss-6-$k", 6, k, "Etappe $k", Kind.ORBIT, "Schild-Segment $k: Schwing dich zur Station!",
            listOf(target), listOf(Param("angle", "Winkel", -80.0, 80.0, 1.0, 0.0, "°"), Param("v", "Tempo", 1.0, 10.0, 0.5, 4.0)),
            mapOf("angle" to sol.first, "v" to sol.second), asteroids = rocks, masses = masses, launch = launch,
            tip = "Gravitation wirkt am stärksten nah am Planeten. Ganze Bahn einblenden hilft beim Tüfteln.")

    private fun stagesFor(chapter: Int): List<Level> = when (chapter) {
        6 -> listOf(
            orbitStage(1, Body(8.0, -6.0, 0.8), listOf(Body(-3.0, 2.0, 1.2), Body(2.0, -1.0, 1.3), Body(5.0, 5.0, 1.0)), listOf(20.0, 30.0, 15.0), Vec(-9.0, 7.0), -60.0 to 6.5),
            orbitStage(2, Body(-8.0, -7.0, 0.8), listOf(Body(0.0, 2.0, 1.3), Body(0.0, -3.0, 1.3)), listOf(30.0, 30.0), Vec(-9.0, 8.0), 30.0 to 2.0),
            orbitStage(3, Body(8.5, 8.0, 0.8), listOf(Body(0.0, 0.0, 2.0), Body(6.0, 3.0, 1.0), Body(-4.0, 6.0, 1.0)), listOf(60.0, 0.0, 0.0), Vec(-9.0, -8.0), 60.0 to 7.0),
        )
        // Wellen & Wachstum: Sinus, Wachstum, Sinus
        5 -> listOf(Kind.SINE, Kind.EXP, Kind.SINE).mapIndexed { i, k -> Gen.level(k, 5000L + i * 17, 2, "boss-5-${i + 1}") }
        else -> (1..3).map { k -> Gen.level(kindOf.getValue(chapter), chapter * 1000L + k * 37, 2, "boss-$chapter-$k") }
    }.mapIndexed { i, l -> l.copy(chapter = chapter, number = i + 1, title = "Etappe ${i + 1} von 3") }

    val all: List<Boss> = Levels.chapters.mapNotNull { c ->
        val (n, e) = names[c.number] ?: return@mapNotNull null
        Boss(c.number, n, e, stagesFor(c.number))
    }

    fun forChapter(chapter: Int) = all.firstOrNull { it.chapter == chapter }
    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** 3 Sterne ohne Fehlschuss, 2 bei höchstens zwei, sonst 1. */
    fun stars(misses: Int) = when { misses == 0 -> 3; misses <= 2 -> 2; else -> 1 }
}

/**
 * Duell-Code: Mission + Versuche des Herausforderers, z. B. „KF-3G7XQ-2“.
 * Kein Server nötig – der Code enthält alles. Gleiches Level für beide durch denselben Startwert.
 */
object DuelCode {
    data class Challenge(val seed: Long, val attempts: Int)

    fun level(seed: Long) = Gen.any(seed, 1, "duel-$seed").copy(title = "Duell-Mission")

    fun encode(seed: Long, attempts: Int) = "KF-${seed.toString(36).uppercase()}-$attempts"

    /** Findet einen Code auch mitten in einer WhatsApp-Nachricht. */
    fun decode(text: String): Challenge? {
        val m = Regex("""KF-([0-9A-Za-z]{1,8})-(\d{1,2})""").find(text) ?: return null
        val seed = m.groupValues[1].lowercase().toLongOrNull(36) ?: return null
        val att = m.groupValues[2].toIntOrNull()?.takeIf { it in 1..99 } ?: return null
        return Challenge(seed, att)
    }

    fun newSeed(random: kotlin.random.Random = kotlin.random.Random) = random.nextLong(36L * 36 * 36 * 36, 36L * 36 * 36 * 36 * 36)

    fun shareText(seed: Long, attempts: Int) =
        "🚀 Kurvenflug-Duell! Ich habe die Mission in $attempts ${if (attempts == 1) "Versuch" else "Versuchen"} geschafft. " +
            "Schaffst du's besser? Öffne Kurvenflug → Duell → Code eingeben: ${encode(seed, attempts)}"

    /** Ergebnis aus Sicht dessen, der die Herausforderung annimmt. */
    fun verdict(mine: Int, theirs: Int) = when {
        mine < theirs -> "Gewonnen! 🏆 Du brauchtest $mine, dein Gegner $theirs Versuche."
        mine == theirs -> "Unentschieden! 🤝 Beide $mine ${if (mine == 1) "Versuch" else "Versuche"}."
        else -> "Knapp verloren – $mine gegen $theirs Versuche. Revanche? 💪"
    }
}

/**
 * Asteroidensturm: ein Level nach dem anderen, jedes muss im ersten Versuch sitzen.
 * Kein Zeitdruck – nur der Rekord zählt. Es wird schrittweise schwerer.
 */
object Storm {
    /** Stufe der Welle: je 4 Wellen eine Stufe höher – ab Welle 17 „Extrem“. */
    fun tier(index: Int) = minOf(Tier.MAX, index / 4)

    fun level(runSeed: Long, index: Int): Level = tier(index).let { t ->
        Gen.any(runSeed * 101 + index, difficulty = t, id = "storm-$runSeed-$index").copy(title = "Welle ${index + 1} · ${Tier.label(t)}")
    }
}

/** Zahlen, aus denen sich die Abzeichen ergeben. */
data class Stats(
    val stars: Int = 0,
    val perfect: Int = 0,
    val chaptersDone: Int = 0,
    val cells: Int = 0,
    val dailies: Int = 0,
    val bosses: Int = 0,
    val stormBest: Int = 0,
    val duelWins: Int = 0,
    val duelsPlayed: Int = 0,
)

data class Badge(val id: String, val emoji: String, val title: String, val how: String, val earned: (Stats) -> Boolean)

object Badges {
    val all = listOf(
        Badge("first", "🚀", "Abheben", "Ersten Stern holen") { it.stars >= 1 },
        Badge("sharp", "🎯", "Scharfschütze", "10 Level beim ersten Versuch ohne Hilfe") { it.perfect >= 10 },
        Badge("genius", "🧠", "Rechengenie", "30 Level beim ersten Versuch ohne Hilfe") { it.perfect >= 30 },
        Badge("chapter", "📘", "Kapitel-Profi", "Ein ganzes Kapitel geschafft") { it.chaptersDone >= 1 },
        Badge("all", "🌌", "Weltraum-Meister", "Alle Kapitel geschafft") { it.chaptersDone >= Levels.chapters.size },
        Badge("cells", "⚡", "Energiebündel", "25 Energiezellen gesammelt") { it.cells >= 25 },
        Badge("daily", "🛰️", "Missions-Fan", "5 Tagesmissionen geschafft") { it.dailies >= 5 },
        Badge("boss", "👾", "Boss-Bezwinger", "Ersten Boss besiegt") { it.bosses >= 1 },
        Badge("bosses", "🏆", "Flottenadmiral", "Alle Bosse besiegt") { it.bosses >= Bosses.all.size },
        Badge("storm10", "☄️", "Sturmreiter", "10 Wellen im Asteroidensturm") { it.stormBest >= 10 },
        Badge("storm25", "🌪️", "Sturmbändiger", "25 Wellen im Asteroidensturm") { it.stormBest >= 25 },
        Badge("duel", "⚔️", "Herausforderer", "Erstes Duell gespielt") { it.duelsPlayed >= 1 },
        Badge("duelwin", "🥇", "Duell-Sieger", "5 Duelle gewonnen") { it.duelWins >= 5 },
        Badge("stars", "⭐", "Sternensammler", "100 Sterne") { it.stars >= 100 },
    )
    fun earned(s: Stats) = all.filter { it.earned(s) }
}
