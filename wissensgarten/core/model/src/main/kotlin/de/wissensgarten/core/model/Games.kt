package de.wissensgarten.core.model

import kotlin.random.Random

/** Wie eine Karte in der Gieß-Runde gespielt wird. */
enum class GameMode {
    /** Neue Karte: Merkbild + Antwort entdecken, dann kurz prüfen */
    DISCOVER,
    /** Antworten steigen als Ballons auf */
    BALLOONS,
    /** Antwort aus Buchstaben-Kacheln legen */
    LETTERS,
    /** Behauptung: stimmt / stimmt nicht wischen */
    SWIPE,
    /** Klassische Auswahl */
    CHOICE,
    /** Aufdecken + Selbsteinschätzung (eigene Karten ohne Auswahl) */
    FLIP,
    /** Brummi rät (oft falsch) – das Kind korrigiert ihn (Lernen durch Lehren) */
    TEACH,
    /** Handy kippen: Gießkanne fängt die richtige Antwort-Wolke */
    TILT,
    /** Antwort laut sagen (Spracherkennung) */
    SPEAK,
    /** Antwort selbst eintippen – freies Erinnern wirkt am stärksten (ab Klasse 5) */
    TYPE,
}

/** Ergebnis eines Mini-Spiels – wird in eine Bewertung für die Vergessenskurve übersetzt. */
enum class GameResult(val rating: Rating) { PERFECT(Rating.GOOD), SHAKY(Rating.HARD), WRONG(Rating.AGAIN) }

object Games {
    private val LETTERS_OK = Regex("^[A-Za-zÄÖÜäöüßÀ-ÿ]{3,12}$")

    /** Antwortmöglichkeiten: eigene Ablenker der Karte, sonst Antworten anderer Karten aus dem Beet. */
    fun options(card: StudyCard, peers: List<String>): List<String>? {
        val given = card.choices.orEmpty().filter { it != card.back }
        val extra = peers.filter { it != card.back && it !in given }.distinct()
            .map { it to (it.hashCode() xor card.id.hashCode()) }.sortedBy { it.second }.map { it.first }
        val wrong = (given + extra).take(3)
        if (wrong.size < 2) return null
        val seed = card.id.hashCode() + card.memory.reps
        return (wrong + card.back).shuffled(Random(seed))
    }

    fun lettersPossible(answer: String, simple: Boolean): Boolean =
        LETTERS_OK.matches(answer) && (!simple || answer.length <= 8)

    fun balloonsPossible(options: List<String>) = options.all { it.length <= 22 }

    /**
     * Wählt das Spiel. Abwechslung entsteht über Karten-ID + Anzahl Wiederholungen,
     * damit dieselbe Karte beim nächsten Mal anders gespielt wird.
     */
    fun speakPossible(answer: String) = answer.length in 2..24 && answer.trim().split(Regex("\\s+")).size <= 3 &&
        answer.none { it.isDigit() || it in "=+×÷²³√π/()" } &&
        // Artikel gehören zur Antwort („la maison“, „der“)? Die Spracherkennung kann sie nicht sicher prüfen.
        SpokenAnswer.normalize(answer) == answer.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), " ").trim().replace(Regex("\\s+"), " ")

    fun tiltPossible(options: List<String>) = options.size <= 4 && options.all { it.length <= 16 }

    /**
     * @param canSpeak Spracherkennung auf dem Gerät verfügbar
     * @param calm Gute-Nacht-Runde: nur ruhige Spielarten
     */
    fun choose(card: StudyCard, peers: List<String>, simple: Boolean, canSpeak: Boolean = false, calm: Boolean = false, canType: Boolean = false): GameMode {
        val opts = options(card, peers)
        if (card.memory.isSeed) return GameMode.DISCOVER
        if (opts == null) return GameMode.FLIP
        if (calm) return if (card.id.hashCode() % 2 == 0) GameMode.CHOICE else GameMode.TEACH
        val pool = buildList {
            add(GameMode.CHOICE)
            add(GameMode.SWIPE)
            add(GameMode.TEACH)
            if (balloonsPossible(opts)) { add(GameMode.BALLOONS); add(GameMode.BALLOONS) }
            if (lettersPossible(card.back, simple)) { add(GameMode.LETTERS); add(GameMode.LETTERS) }
            if (tiltPossible(opts)) add(GameMode.TILT)
            if (canSpeak && speakPossible(card.back)) add(GameMode.SPEAK)
            // Tippen erst, wenn die Karte schon ein paarmal dran war – dann ist freies Erinnern machbar
            if (canType && card.memory.reps >= 2 && typePossible(card.back)) { add(GameMode.TYPE); add(GameMode.TYPE) }
        }
        val pick = Math.floorMod(card.id.hashCode() * 31 + card.memory.reps * 7, pool.size)
        return pool[pick]
    }

    /** Buchstaben-Kacheln: Antwort gemischt (nie in richtiger Reihenfolge). */
    /** Buchstaben in Original-Schreibweise – Groß/klein gehört bei Rechtschreibung dazu („Vogel“ ≠ „vogel“). */
    fun letterTiles(answer: String, seed: Int): List<Char> {
        val chars = answer.toList()
        val rnd = Random(seed)
        repeat(10) {
            val s = chars.shuffled(rnd)
            if (s != chars) return s
        }
        return chars.reversed()
    }

    /** „Stimmt's?“: zeigt mit 50 % die richtige, sonst eine falsche Antwort. */
    fun swipeStatement(card: StudyCard, options: List<String>): Pair<String, Boolean> {
        val rnd = Random(card.id.hashCode() * 13 + card.memory.reps)
        val truth = rnd.nextBoolean()
        return if (truth) card.back to true else options.filter { it != card.back }.random(rnd) to false
    }

    /** Karten für eine Zuordnen-Bonusrunde (kurze Fragen und Antworten, alles verschieden). */
    fun matchPairs(cards: List<StudyCard>, count: Int = 4): List<StudyCard>? {
        val ok = cards.filter { it.front.length <= 28 && it.back.length <= 22 }
            .distinctBy { it.back }.distinctBy { it.front }
        return if (ok.size >= count) ok.take(count) else null
    }
}


/** Vergleich für gesprochene Antworten: Groß/klein, Satzzeichen und Artikel egal, kleine Tippfehler erlaubt. */
/** Lässt sich die Antwort gut tippen? (kurz, keine Formelzeichen wie π, ², ·) */
fun typePossible(answer: String): Boolean =
    answer.length in 1..30 && answer.all { it.isLetterOrDigit() || it in " -–,.'’/()€%" }

/** Getippte Antwort prüfen – verzeiht Kleinigkeiten, ohne zu schludern. */
object TypedAnswer {
    enum class Verdict { RIGHT, ALMOST, WRONG }

    private fun clean(s: String) = s.trim().replace('–', '-').replace(Regex("\\s+"), " ")

    /**
     * RIGHT: genau so (Leerzeichen egal). ALMOST: kleiner Tippfehler, andere Groß-/Kleinschreibung oder Artikel weggelassen.
     * @param strictSpelling Deutsch/Rechtschreibung: Dort zählt „fast“ nicht – die Schreibweise ist ja der Lernstoff.
     */
    fun check(typed: String, answer: String, strictSpelling: Boolean = false): Verdict {
        val t = clean(typed); val a = clean(answer)
        if (t.isEmpty()) return Verdict.WRONG
        if (t == a) return Verdict.RIGHT
        val nt = SpokenAnswer.normalize(t); val na = SpokenAnswer.normalize(a)
        if (na.isEmpty()) return Verdict.WRONG
        val close = nt == na || SpokenAnswer.levenshtein(nt, na) <= maxOf(1, na.length / 6) && na.length >= 5
        return when {
            !close -> Verdict.WRONG
            strictSpelling -> Verdict.WRONG
            else -> Verdict.ALMOST
        }
    }
}

object SpokenAnswer {
    private val articles = setOf("der", "die", "das", "the", "a", "an", "le", "la", "les", "l", "un", "une", "el", "los", "las")

    fun normalize(s: String): String = s.lowercase()
        .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
        .split(Regex("\\s+")).filter { it.isNotBlank() && it !in articles }.joinToString(" ")

    fun matches(heard: String, answer: String): Boolean {
        val a = normalize(answer); val h = normalize(heard)
        if (a.isEmpty()) return false
        if (h == a || h.contains(a)) return true
        val tolerance = maxOf(1, a.length / 6)
        return levenshtein(h, a) <= tolerance
    }

    fun levenshtein(x: String, y: String): Int {
        val d = IntArray(y.length + 1) { it }
        for (i in 1..x.length) {
            var prev = d[0]; d[0] = i
            for (j in 1..y.length) {
                val tmp = d[j]
                d[j] = minOf(d[j] + 1, d[j - 1] + 1, prev + if (x[i - 1] == y[j - 1]) 0 else 1)
                prev = tmp
            }
        }
        return d[y.length]
    }
}
