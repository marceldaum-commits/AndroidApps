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

/**
 * Schwierigkeit wächst mit der Pflanze: Je sicherer eine Karte sitzt, desto schwerer wird sie abgefragt –
 * mehr Antwortmöglichkeiten, ähnlichere Ablenker, mehr freie Eingabe, längere Abstände. Auf der Profi-Stufe (Baum)
 * gibt es keine Auswahl mehr. Nachweis: DifficultyTest (content) prüft, dass jede Stufe strikt schwerer ist.
 */
object Difficulty {
    enum class Pick { FAR, MIXED, NEAR }
    data class Tier(val stage: Stage, val label: String, val options: Int, val distractors: Pick, val freePercent: Int)

    fun tier(stage: Stage): Tier = when (stage) {
        Stage.SEED -> Tier(stage, "Entdecken", 3, Pick.FAR, 0)
        Stage.SPROUT -> Tier(stage, "Leicht", 3, Pick.FAR, 0)
        Stage.SHOOT -> Tier(stage, "Mittel", 4, Pick.MIXED, 20)
        Stage.BUSH -> Tier(stage, "Knifflig", 4, Pick.NEAR, 45)
        Stage.BLOOM -> Tier(stage, "Schwer", 4, Pick.NEAR, 75)
        Stage.TREE -> Tier(stage, "Profi", 4, Pick.NEAR, 100)
    }

    /** Ähnlichkeit zweier Antworten 0…1 (normierte Levenshtein-Distanz, Groß/klein egal). */
    fun similarity(a: String, b: String): Double {
        val x = a.lowercase(); val y = b.lowercase()
        val m = maxOf(x.length, y.length)
        if (m == 0) return 1.0
        var prev = IntArray(y.length + 1) { it }
        for (i in 1..x.length) {
            val cur = IntArray(y.length + 1); cur[0] = i
            for (j in 1..y.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (x[i - 1] == y[j - 1]) 0 else 1)
            prev = cur
        }
        return 1.0 - prev[y.length].toDouble() / m
    }
}

/** Ergebnis eines Mini-Spiels – wird in eine Bewertung für die Vergessenskurve übersetzt. */
enum class GameResult(val rating: Rating) { PERFECT(Rating.GOOD), SHAKY(Rating.HARD), WRONG(Rating.AGAIN) }

object Games {
    private val LETTERS_OK = Regex("^[A-Za-zÄÖÜäöüßÀ-ÿ]{3,12}$")

    /**
     * Antwortmöglichkeiten – Anzahl und Ähnlichkeit der Ablenker richten sich nach der Schwierigkeitsstufe der Karte
     * (siehe [Difficulty]): junge Pflanzen bekommen leicht unterscheidbare, große Pflanzen sehr ähnliche Ablenker.
     * Eigene Ablenker der Karte zuerst, dann Antworten anderer Karten aus dem Beet.
     */
    fun options(card: StudyCard, peers: List<String>, stage: Stage = Stage.of(card.memory)): List<String>? {
        val tier = Difficulty.tier(stage)
        val given = card.choices.orEmpty().filter { it != card.back }.distinct()
        val extra = peers.filter { it != card.back && it !in given }.distinct()
        fun order(list: List<String>): List<String> = when (tier.distractors) {
            Difficulty.Pick.FAR -> list.sortedWith(compareBy<String> { Difficulty.similarity(it, card.back) }.thenBy { it })
            Difficulty.Pick.NEAR -> list.sortedWith(compareByDescending<String> { Difficulty.similarity(it, card.back) }.thenBy { it })
            Difficulty.Pick.MIXED -> list.map { it to (it.hashCode() xor card.id.hashCode()) }.sortedBy { it.second }.map { it.first }
        }
        val wrong = (order(given) + order(extra)).take(tier.options - 1)
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
        if (card.memory.isSeed) return GameMode.DISCOVER
        val stage = Stage.of(card.memory)
        val tier = Difficulty.tier(stage)
        val hash = (card.id.hashCode() * 31 + card.memory.reps * 7)
        // Karten ohne vorgegebene Auswahl (v. a. Abi-/Experten-Pakete): ab dem Spross frei erinnern, wenn Tippen möglich ist
        if (stage >= Stage.SHOOT && !card.isChoice && canType && typePossible(card.back) && !calm) return GameMode.TYPE
        val opts = options(card, peers, stage) ?: return GameMode.FLIP
        if (calm) return if (card.id.hashCode() % 2 == 0) GameMode.CHOICE else GameMode.TEACH
        // Freie Eingabe: Anteil steigt mit der Stufe (0 % Keimling … 100 % Profi)
        if (hash.mod(100) < tier.freePercent) {
            if (canType && typePossible(card.back)) return GameMode.TYPE
            if (canSpeak && speakPossible(card.back)) return GameMode.SPEAK
            if (!canType && lettersPossible(card.back, simple)) return GameMode.LETTERS
            // Profi: keine Auswahl mehr – erst selbst erinnern, dann aufdecken (z. B. Formeln, lange Antworten)
            if (stage == Stage.TREE) return GameMode.FLIP
            if (lettersPossible(card.back, simple)) return GameMode.LETTERS
        }
        val pool = buildList {
            add(GameMode.CHOICE)
            add(GameMode.TEACH)
            if (balloonsPossible(opts)) { add(GameMode.BALLOONS); add(GameMode.BALLOONS) }
            if (tiltPossible(opts)) add(GameMode.TILT)
            // „Stimmt's?“ (50 % Rate-Chance) nur ganz am Anfang
            if (stage == Stage.SPROUT) { add(GameMode.SWIPE); add(GameMode.SWIPE) }
            // Buchstaben legen und Sprechen ab dem Spross
            if (stage >= Stage.SHOOT && lettersPossible(card.back, simple)) { add(GameMode.LETTERS); add(GameMode.LETTERS) }
            if (stage >= Stage.SHOOT && canSpeak && speakPossible(card.back)) add(GameMode.SPEAK)
        }
        return pool[hash.mod(pool.size)]
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

    private val NUMBER = Regex("^(-?)\\s*(\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,(\\d+))?\\s*(€|%|°C|[A-Za-zµΩ]{1,4})?$")

    /** „Marshall-Plan“ → „Marshallplan“: Bindestriche/Leerzeichen weg, der Buchstabe danach klein. */
    private fun joined(s: String): String = buildString {
        var cut = false
        for (c in s) { if (c == '-' || c == ' ') { cut = true; continue }; append(if (cut) c.lowercaseChar() else c); cut = false }
    }

    /** „1.250,5 €“ → (1250.5, „€“); null, wenn keine reine Zahl (ggf. mit Einheit). */
    fun number(s: String): Pair<Double, String>? {
        val m = NUMBER.matchEntire(s.trim())
            // „1.5“ mit Punkt als Komma getippt (englische Tastatur)
            ?: Regex("^-?\\s*\\d+\\.\\d+\\s*\\S*$").matchEntire(s.trim())?.let { NUMBER.matchEntire(s.trim().replace('.', ',')) }
            ?: return null
        val (sign, int, frac, unit) = m.destructured
        val v = (int.replace(".", "") + if (frac.isNotEmpty()) ".$frac" else "").toDoubleOrNull() ?: return null
        return (if (sign == "-") -v else v) to unit
    }

    /**
     * RIGHT: genau so (Leerzeichen egal). ALMOST: kleiner Tippfehler, andere Groß-/Kleinschreibung oder Artikel weggelassen.
     * @param strictSpelling Deutsch/Rechtschreibung: Dort zählt „fast“ nicht – die Schreibweise ist ja der Lernstoff.
     */
    fun check(typed: String, answer: String, strictSpelling: Boolean = false): Verdict {
        val t = clean(typed); val a = clean(answer)
        if (t.isEmpty()) return Verdict.WRONG
        if (t == a) return Verdict.RIGHT
        // Zahlen: exakt nach Wert vergleichen (0,5 = 0,50; Vorzeichen zählt; kein „fast richtig“ für 0,2462 statt 0,2461)
        number(a)?.let { (av, au) ->
            val (tv, tu) = number(t) ?: return Verdict.WRONG
            val unitOk = tu.isEmpty() || au.isEmpty() || tu.equals(au, ignoreCase = true)
            return if (kotlin.math.abs(av - tv) < 1e-9 && unitOk) Verdict.RIGHT else Verdict.WRONG
        }
        // „Marshallplan“ = „Marshall-Plan“: Bindestriche/Leerzeichen sind keine Fehler (außer in Rechtschreibung)
        if (!strictSpelling && joined(t) == joined(a)) return Verdict.RIGHT
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
