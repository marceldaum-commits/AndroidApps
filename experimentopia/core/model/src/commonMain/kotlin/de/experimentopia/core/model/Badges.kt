package de.experimentopia.core.model

/** Was für die Abzeichen zählt */
data class Stats(
    val done: Int = 0,
    val stars: Int = 0,
    /** Vermutung stimmte */
    val hits: Int = 0,
    /** Vermutung stimmte nicht – ein Aha-Moment, und das ist gut so! */
    val ahas: Int = 0,
    val explained: Int = 0,
    val islandsDone: Int = 0,
    val freeLabRuns: Int = 0,
    val dailies: Int = 0,
    /** Wie oft die Sicherheit zur Vermutung gepasst hat (sicher & richtig oder unsicher & falsch) */
    val calibrated: Int = 0,
)

data class Badge(val id: String, val emoji: String, val title: String, val text: String, val earned: (Stats) -> Boolean)

object Badges {
    val all = listOf(
        Badge("erster", "🔬", "Erster Versuch", "Den ersten Versuch gemacht") { it.done >= 1 },
        Badge("aha1", "💡", "Aha!", "Die erste Überraschung – genau so lernen Forscher") { it.ahas >= 1 },
        Badge("aha10", "🤯", "Überraschungs-Sammler", "10 Aha-Momente") { it.ahas >= 10 },
        Badge("treffer5", "🎯", "Gutes Gespür", "5 Vermutungen lagen richtig") { it.hits >= 5 },
        Badge("erklaer10", "🗣️", "Erklär-Profi", "Blubb 10-mal richtig erklärt") { it.explained >= 10 },
        Badge("zehn", "🧪", "Laborratte", "10 Versuche gemacht") { it.done >= 10 },
        Badge("dreissig", "🥼", "Laborchefin", "30 Versuche gemacht") { it.done >= 30 },
        Badge("insel", "🏝️", "Inselforscher", "Eine ganze Insel erforscht") { it.islandsDone >= 1 },
        Badge("archipel", "🗺️", "Entdecker von Experimentopia", "Alle Inseln erforscht") { it.islandsDone >= Island.entries.size },
        Badge("tueftler", "🛠️", "Tüftler", "5-mal im Freien Labor geforscht") { it.freeLabRuns >= 5 },
        Badge("taeglich", "📅", "Rätselfreund", "3 Rätsel des Tages gelöst") { it.dailies >= 3 },
        Badge("selbst", "🧭", "Kenn-ich-mich-aus", "10-mal richtig eingeschätzt, wie sicher du bist") { it.calibrated >= 10 },
        Badge("sterne", "🌟", "Sternensammler", "100 Sterne") { it.stars >= 100 },
    )
    fun earned(s: Stats) = all.filter { it.earned(s) }
}

/** Das Rätsel des Tages: jeden Tag ein anderer Versuch, für alle gleich. Frei auch ohne Pro. */
object Daily {
    fun forDay(epochDay: Long): Experiment {
        val all = Catalog.all
        // Schrittweite teilerfremd zur Anzahl → alle Versuche kommen dran, bevor sich etwas wiederholt
        val step = (1..all.size).first { it > 7 && gcd(it, all.size) == 1 }
        return all[((epochDay % all.size + all.size) % all.size * step % all.size).toInt()]
    }
    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}

/** Sterne für einen Versuch: ⭐ gemacht, ⭐ Blubb beim ersten Versuch richtig erklärt, ⭐ Knobelfrage richtig. Die Vermutung kostet nie Sterne. */
object Score {
    fun stars(explainedFirstTry: Boolean, transferRight: Boolean) = 1 + (if (explainedFirstTry) 1 else 0) + (if (transferRight) 1 else 0)
}

/** Selbsteinschätzung: Wie sicher warst du – und lagst du richtig? */
enum class Sure(val emoji: String, val title: String) { UNSICHER("🤔", "geraten"), ETWAS("🙂", "ziemlich sicher"), SICHER("😎", "ganz sicher") }

object Calibration {
    /** Passt die Sicherheit zum Ergebnis? */
    fun matches(sure: Sure, right: Boolean) = when (sure) { Sure.SICHER -> right; Sure.UNSICHER -> !right; Sure.ETWAS -> true }

    data class Row(val sure: Sure, val total: Int, val right: Int) { val rate get() = if (total == 0) 0.0 else right.toDouble() / total }

    fun rows(entries: List<Pair<Sure, Boolean>>) = Sure.entries.map { s -> val l = entries.filter { it.first == s }; Row(s, l.size, l.count { it.second }) }

    /** Ein Satz für das Forscherbuch */
    /** Alle Tipps (feste Sätze – so werden sie vorab mit Nova vertont) */
    val ALL = listOf(
        "Mach noch ein paar Versuche – dann zeigt dir Blubb, wie gut du dich selbst einschätzt.",
        "Wenn du dir ganz sicher warst, lagst du oft daneben. Spannend! Die Natur überrascht – lieber einmal mehr nachdenken.",
        "Wenn du dir sicher bist, liegst du meistens richtig. Du kennst dich gut aus!",
        "Du weißt mehr, als du denkst – beim Raten lagst du oft richtig. Trau dich!",
        "Du lernst gerade, wie gut deine Vermutungen sind. Weiter so!",
    )
    fun advice(rows: List<Row>): String {
        val sure = rows.first { it.sure == Sure.SICHER }; val guess = rows.first { it.sure == Sure.UNSICHER }
        return when {
            sure.total + guess.total < 3 -> ALL[0]
            sure.total >= 3 && sure.rate < 0.5 -> ALL[1]
            sure.total >= 3 && sure.rate >= 0.8 -> ALL[2]
            guess.total >= 3 && guess.rate >= 0.7 -> ALL[3]
            else -> ALL[4]
        }
    }
}
