package de.wissensgarten.core.model

import kotlin.random.Random

/**
 * Unkraut: Karten, die immer wieder vergessen werden (mind. 2× „Vergessen“) und noch nicht sitzen.
 * Gezupft ist eine Karte, bis sie erneut vergessen wird – gemerkt wird dazu die Zahl der Fehler beim Zupfen.
 */
object Weeds {
    const val MIN_LAPSES = 2
    const val SETTLED = 7.0
    const val PER_ROUND = 6
    /** Alle so viele gezupften Unkräuter wächst eine Sonderblume. */
    const val FLOWER_EVERY = 5

    fun isWeed(m: Memory) = !m.isSeed && m.lapses >= MIN_LAPSES && m.stability < SETTLED
    fun active(cards: List<StudyCard>, pulled: Map<String, Int>): List<StudyCard> =
        cards.filter { isWeed(it.memory) && (pulled[it.id] ?: -1) < it.memory.lapses }
            .sortedWith(compareByDescending<StudyCard> { it.memory.lapses }.thenBy { it.memory.stability })
}

/** Gewitter-Blitzrunde: 60 Sekunden, ohne Verlieren. Ändert den Lernplan nicht – nur Tautropfen. */
object Blitz {
    const val SECONDS = 60
    fun dew(correct: Int) = (correct / 2).coerceAtMost(20)
    /** Himmel: 0 = dunkles Gewitter, 1 = Regenbogen (ab 12 richtigen). */
    fun brightness(correct: Int) = (correct / 12f).coerceIn(0f, 1f)
    fun pool(cards: List<StudyCard>) = cards.filter { !it.memory.isSeed && it.back.length <= 40 }
}

/** Rhythmus-Runde: Die Antworten leuchten nacheinander im Takt auf – tippen, wenn die richtige leuchtet. */
object Rhythm {
    const val BPM = 96
    const val BEAT_MS = 60_000L / BPM
    const val QUESTIONS = 8
    enum class Hit(val label: String, val points: Int) { PERFECT("Perfekt! 🎵", 3), GOOD("Gut! 🎶", 2), MISS("Fast! 🎼", 0) }
    /** Welche Antwort leuchtet nach [elapsedMs]? */
    fun lit(elapsedMs: Long, options: Int) = ((elapsedMs / BEAT_MS) % options).toInt()
    /** Richtige Antwort zählt immer („Gut“); im Takt getippt, während sie leuchtet: „Perfekt“. Falsche: „Fast“. */
    fun judge(elapsedMs: Long, chosen: Int, correct: Int, options: Int): Hit {
        if (chosen != correct) return Hit.MISS
        if (lit(elapsedMs, options) != correct) return Hit.GOOD
        val inBeat = elapsedMs % BEAT_MS
        return if (inBeat < BEAT_MS * 0.6) Hit.PERFECT else Hit.GOOD
    }
    fun dew(points: Int) = (points / 3).coerceAtMost(10)
}

/** Brummis Bienenstock: Jede Wiederholung füllt Honig, 10 Wiederholungen = eine Wabe. Räume zum Einrichten. */
object Hive {
    const val REPS_PER_COMB = 10
    data class Room(val id: String, val name: String, val emoji: String, val needCombs: Int, val furniture: List<Pair<String, String>>)
    val rooms = listOf(
        Room("eingang", "Eingang", "🚪", 0, listOf("🌵" to "Kaktus", "🍯" to "Honigtopf", "🔔" to "Glocke")),
        Room("schlafen", "Brummis Bett", "🛏️", 5, listOf("🐻" to "Kuschelbär", "🌙" to "Nachtlicht", "📚" to "Gute-Nacht-Buch")),
        Room("lesen", "Leseecke", "📖", 15, listOf("🛋️" to "Sofa", "🕯️" to "Kerze", "🗺️" to "Schatzkarte")),
        Room("kueche", "Honigküche", "🍯", 30, listOf("🥞" to "Pfannkuchen", "☕" to "Kakao", "🍰" to "Kuchen")),
        Room("disco", "Disco", "🕺", 50, listOf("🎧" to "DJ-Pult", "🎸" to "Gitarre", "🥁" to "Trommel")),
        Room("terrasse", "Blumen-Terrasse", "🌺", 80, listOf("🌻" to "Sonnenblume", "⛱️" to "Sonnenschirm", "🔭" to "Fernrohr")),
        Room("thron", "Königinnen-Saal", "👑", 120, listOf("💎" to "Kristall", "🎻" to "Geige", "🏆" to "Pokal")),
    )
    fun combs(totalReps: Int) = totalReps / REPS_PER_COMB
    fun open(totalReps: Int) = rooms.filter { combs(totalReps) >= it.needCombs }
    fun next(totalReps: Int) = rooms.firstOrNull { combs(totalReps) < it.needCombs }
    /** Füllstand der aktuellen Wabe 0..1 */
    fun combProgress(totalReps: Int) = (totalReps % REPS_PER_COMB) / REPS_PER_COMB.toFloat()
}

/** Garten-Tiere: ziehen bei Lernserien ein und bleiben für immer. Ohne aktuelle Serie schlafen sie nur. */
object Critters {
    data class Critter(val id: String, val emoji: String, val name: String, val days: Int, val line: String)
    val all = listOf(
        Critter("igel", "🦔", "Igel Ivo", 3, "Ivo raschelt im Laub und sucht Schnecken."),
        Critter("vogel", "🐦", "Vogel Pieps", 7, "Pieps hat ein Nest im Baum gebaut."),
        Critter("frosch", "🐸", "Frosch Quaki", 14, "Quaki springt um den Teich."),
        Critter("fuchs", "🦊", "Fuchs Flo", 30, "Flo bewacht nachts den Garten."),
        Critter("eule", "🦉", "Eule Ella", 60, "Ella weiß fast alles – fast so viel wie du."),
        Critter("einhorn", "🦄", "Einhorn Luna", 100, "Luna kommt nur zu echten Gartenprofis!"),
    )
    /** Aktuelle Serie: aufeinanderfolgende Lerntage bis heute (oder gestern – heute zählt noch nicht als verpasst). */
    fun streak(days: Set<Long>, today: Long): Int {
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }
    fun earned(best: Int) = all.filter { best >= it.days }
    fun next(best: Int) = all.firstOrNull { best < it.days }
}

/** Gedächtnis-Spaziergang (Loci-Methode): schwierige Karten an feste Orte im Garten legen und später ablaufen. */
object Loci {
    data class Place(val emoji: String, val name: String, val on: String, val x: Float, val y: Float)
    val places = listOf(
        Place("🚪", "Gartentor", "hängt am Gartentor", 0.10f, 0.82f),
        Place("🌲", "Tanne", "sitzt oben auf der Tanne", 0.32f, 0.70f),
        Place("⛲", "Brunnen", "badet im Brunnen", 0.58f, 0.78f),
        Place("🦆", "Teich", "schwimmt im Teich", 0.86f, 0.66f),
        Place("⛺", "Zelt", "schläft im Zelt", 0.72f, 0.42f),
        Place("🌳", "Baum", "klettert auf den Baum", 0.44f, 0.36f),
        Place("🏠", "Vogelhaus", "wohnt im Vogelhaus", 0.18f, 0.40f),
        Place("🌻", "Sonnenblume", "versteckt sich in der Sonnenblume", 0.30f, 0.14f),
    )
    fun story(placeIndex: Int, card: StudyCard): String {
        val p = places[placeIndex]
        val pic = card.img?.let { "$it " } ?: ""
        return "Stell dir vor: $pic„${card.back}“ ${p.on}! ${p.emoji}"
    }
    /** Vorschläge: Unkraut zuerst, dann wackelige Karten. */
    fun suggest(cards: List<StudyCard>): List<StudyCard> =
        cards.filter { !it.memory.isSeed }.sortedWith(compareByDescending<StudyCard> { it.memory.lapses }.thenBy { it.memory.stability }).take(12)
    fun encode(m: Map<Int, String>) = m.entries.joinToString(";") { "${it.key}=${it.value}" }
    fun decode(s: String?): Map<Int, String> = s.orEmpty().split(';').mapNotNull { e ->
        val i = e.indexOf('='); if (i <= 0) null else e.substring(0, i).toIntOrNull()?.takeIf { it in places.indices }?.let { it to e.substring(i + 1) }
    }.toMap()
}

/** Tagesaufgaben: drei kleine Aufgaben pro Tag, die Wochenkiste füllt sich – verpasste Tage zählen nicht. */
object Quests {
    enum class Kind(val emoji: String, val text: (Int) -> String, val amounts: List<Int>) {
        WATER("💧", { "Gieße $it Pflanzen" }, listOf(5, 10, 15)),
        WEED("🌿", { "Zupfe $it Unkraut" }, listOf(1, 2)),
        BLITZ("⚡", { "Spiele eine Blitzrunde" }, listOf(1)),
        RHYTHM("🎵", { "Spiele eine Rhythmus-Runde" }, listOf(1)),
        LOCI("🚶", { "Mach einen Gedächtnis-Spaziergang" }, listOf(1)),
        RIGHT("⭐", { "Beantworte $it Fragen richtig" }, listOf(8, 12, 20)),
        HIVE("🍯", { "Besuche Brummis Bienenstock" }, listOf(1)),
    }
    data class Quest(val kind: Kind, val amount: Int) { val text get() = kind.text(amount) }
    const val PER_DAY = 3
    const val CHEST_AT = 10
    const val CHEST_DEW = 40

    fun forDay(day: Long, profileId: String, hasWeeds: Boolean): List<Quest> {
        val r = Random(day * 31 + profileId.hashCode())
        val pool = Kind.entries.filter { it != Kind.WEED || hasWeeds }.filter { it != Kind.WATER }.shuffled(r)
        // Gießen ist immer dabei – das ist das Wichtigste
        return (listOf(Kind.WATER) + pool.take(PER_DAY - 1)).map { Quest(it, it.amounts.random(r)) }
    }
    fun done(q: Quest, progress: Map<String, Int>) = (progress[q.kind.name] ?: 0) >= q.amount
    fun week(day: Long) = (day + 3).floorDiv(7L)
    fun encode(m: Map<String, Int>) = m.entries.joinToString(";") { "${it.key}=${it.value}" }
    fun decode(s: String?): Map<String, Int> = s.orEmpty().split(';').mapNotNull { e ->
        val i = e.indexOf('='); if (i <= 0) null else e.substring(i + 1).toIntOrNull()?.let { e.substring(0, i) to it }
    }.toMap()
}

/** Garten-Film: Zeitraffer, rekonstruiert aus Anlage-Datum und heutigem Stand jeder Karte. */
object GardenFilm {
    data class Plant(val createdAt: Long, val lastReview: Long, val stability: Double, val reps: Int)
    data class Frame(val time: Long, val stages: List<Stage>)
    fun frames(plants: List<Plant>, now: Long, n: Int = 8): List<Frame> {
        if (plants.isEmpty()) return emptyList()
        val start = plants.minOf { it.createdAt }
        val span = (now - start).coerceAtLeast(1)
        return (0 until n).map { i ->
            val t = start + span * (i + 1) / n
            Frame(t, plants.filter { it.createdAt <= t }.map { p ->
                when {
                    p.reps == 0 -> Stage.SEED
                    t >= p.lastReview -> Stage.of(Memory(p.stability, 5.0, p.lastReview, p.reps))
                    else -> {
                        val f = (t - p.createdAt).toDouble() / (p.lastReview - p.createdAt).coerceAtLeast(1)
                        if (f < 0.05) Stage.SEED else Stage.of(Memory((p.stability * f).coerceAtLeast(0.1), 5.0, 0, 1))
                    }
                }
            })
        }
    }
}
