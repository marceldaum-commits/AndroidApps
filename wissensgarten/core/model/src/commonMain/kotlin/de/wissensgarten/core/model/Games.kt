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
        // Ähnlichkeit je Antwort nur einmal berechnen (nicht bei jedem Vergleich des Sortierens) – sonst blockiert der
        // Vorab-Katalog der Stimme (alle Karten aller Pakete) den Browser beim Start für viele Sekunden
        fun withSim(list: List<String>) = list.map { it to Difficulty.similarity(it, card.back) }
        fun order(list: List<String>): List<String> = when (tier.distractors) {
            Difficulty.Pick.FAR -> withSim(list).sortedWith(compareBy<Pair<String, Double>> { it.second }.thenBy { it.first }).map { it.first }
            Difficulty.Pick.NEAR -> withSim(list).sortedWith(compareByDescending<Pair<String, Double>> { it.second }.thenBy { it.first }).map { it.first }
            // fest je Karten-Inhalt (nicht je Gerät): so lassen sich Brummi-Radio-Fragen vorab vertonen
            Difficulty.Pick.MIXED -> list.map { it to (it.hashCode() xor card.front.hashCode()) }.sortedBy { it.second }.map { it.first }
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
        SpokenAnswer.normalize(answer) == nurWorte(answer.lowercase()).trim().replace(Regex("\\s+"), " ")

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
        // „Welches Wort ist ein Nomen?“: ohne sichtbare Auswahl wären viele Antworten richtig – dann nicht frei eintippen/sprechen
        val freeOk = !(card.isChoice && Radio.needsChoices(card.front))
        // Freie Eingabe: Anteil steigt mit der Stufe (0 % Keimling … 100 % Profi)
        if (hash.mod(100) < tier.freePercent) {
            // Auswahl-Karten nur dann eintippen, wenn die Antwort ein Wort oder eine Zahl ist („Nach etwa 12 Jahren“ wäre unfair)
            if (freeOk && canType && typePossible(card.back) && (!card.isChoice || singleToken(card.back))) return GameMode.TYPE
            if (freeOk && canSpeak && speakPossible(card.back)) return GameMode.SPEAK
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
            if (stage >= Stage.SHOOT && freeOk && canSpeak && speakPossible(card.back)) add(GameMode.SPEAK)
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
/** Ein Wort oder eine Zahl (ggf. mit Einheit) – eindeutig eintippbar. */
fun singleToken(answer: String): Boolean = answer.trim().let { a -> ' ' !in a || TypedAnswer.number(a) != null }

/** Lässt sich die Antwort gut tippen? (kurz, keine Formelzeichen wie π, ², ·) */
fun typePossible(answer: String): Boolean =
    answer.length in 1..30 && answer.all { it.isLetterOrDigit() || it in " -–,.'’/()€%" }

/**
 * Getippte Antwort prüfen – verzeiht Kleinigkeiten, ohne zu schludern. Gilt eine Aufgabe auf mehreren Wegen, zählt jeder:
 * Klammer-Alternativen („Kyjiw (Kiew)“, „Nomen (Namenwort / Substantiv)“), Zahlwort statt Ziffer („acht“ = 8),
 * Bruch, Dezimalzahl und Prozent (½ = 1/2 = 0,5 = 50 %), ausgeschriebene Einheiten („50 Prozent“, „100 Grad“),
 * Aufzählungen in anderer Reihenfolge („Remus und Romulus“), englische Kurzformen („I'm“ = „I am“).
 * Die Frage ([question]) entscheidet mit: Reihenfolge-Fragen bleiben streng, „Kürze …“ verlangt den Bruch,
 * und ein Zahlwort, das schon in der Frage steht („Welche Zahl ist six?“), zählt nicht als Antwort.
 */
object TypedAnswer {
    enum class Verdict { RIGHT, ALMOST, WRONG }

    private fun better(a: Verdict, b: Verdict) = if (a.ordinal <= b.ordinal) a else b
    private fun worse(a: Verdict, b: Verdict) = if (a.ordinal >= b.ordinal) a else b

    private fun clean(s: String) = tiefDigits(s.trim().replace('–', '-').replace('’', '\'').replace(Regex("\\s+"), " "))

    /** „CO₂“ = „CO2“ (Hochzahlen bleiben: „10⁶“ ist nicht „106“) */
    private fun tiefDigits(s: String) = buildString(s.length) { s.forEach { append(if (it in '₀'..'₉') '0' + (it - '₀') else it) } }

    private val NUMBER = Regex("^(-?)\\s*(\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,(\\d+))?\\s*(€|%|°C|[A-Za-zµΩ]{1,4})?$")

    private val ARTICLES = setOf("der", "die", "das", "den", "dem", "des", "ein", "eine", "einen", "einem", "einer", "the", "a", "an", "le", "la", "les", "l", "un", "une", "el", "los", "las", "to")
    private val WORD_SPLIT = Regex("\\s+|['’](?=[A-Za-zÀ-ÿ])")
    private fun words(s: String) = s.trim().trimEnd('.', '!', '?', ';').split(WORD_SPLIT).filter { it.isNotBlank() }
    private fun bare(s: String): String = joined(words(s).dropWhile { it.lowercase() in ARTICLES }.joinToString(" "))
    /** Artikel vorne („la“ in „la maison“), sonst null */
    private fun article(s: String) = words(s).firstOrNull()?.lowercase()?.takeIf { it in ARTICLES && words(s).size > 1 }

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

    // ------------------------------------------------------------------ Zahlen: Wert, Schreibweise, Einheit

    /** [form]: „dec“ (Dezimalzahl) oder der Bruch, wie er dasteht („2/3“, „3 2/5“) – für Fragen, die eine Schreibweise verlangen */
    private class Num(val value: Double, val unit: String, val form: String)

    private val VULGAR = mapOf('½' to "1/2", '⅓' to "1/3", '⅔' to "2/3", '¼' to "1/4", '¾' to "3/4", '⅕' to "1/5", '⅖' to "2/5",
        '⅗' to "3/5", '⅘' to "4/5", '⅙' to "1/6", '⅚' to "5/6", '⅛' to "1/8", '⅜' to "3/8", '⅝' to "5/8", '⅞' to "7/8")
    private val FRACTION = Regex("^(?:(\\d+)\\s+)?(\\d+)\\s*/\\s*(\\d+)\\s*(.*)$")
    private val DECIMAL = Regex("^(\\d{1,3}(?:\\.\\d{3})+|\\d{1,3}(?: \\d{3})+|\\d+)(?:,(\\d+))?\\s*(.*)$")
    private val DECIMAL_DOT = Regex("^(\\d+)\\.(\\d+)\\s*(.*)$")
    private val UNIT_OK = Regex("^[A-Za-zÄÖÜäöüßµΩ°%€ ./²³]{0,16}$")
    private val UNITS = mapOf(
        "prozent" to "%", "euro" to "€", "eur" to "€", "cent" to "ct", "grad celsius" to "°", "grad" to "°", "°c" to "°",
        "meter" to "m", "metern" to "m", "zentimeter" to "cm", "millimeter" to "mm", "kilometer" to "km",
        "gramm" to "g", "kilogramm" to "kg", "kilo" to "kg", "tonne" to "t", "tonnen" to "t", "liter" to "l", "milliliter" to "ml",
        "sekunde" to "s", "sekunden" to "s", "minute" to "min", "minuten" to "min", "stunde" to "h", "stunden" to "h",
        "tag" to "tage", "tagen" to "tage", "jahr" to "jahre", "jahren" to "jahre",
        "volt" to "v", "ampere" to "a", "ohm" to "ω", "watt" to "w", "newton" to "n", "bytes" to "byte", "bits" to "bit",
    )
    private fun unitNorm(u: String) = u.lowercase().replace(Regex("\\s+"), " ").trim().trimEnd('.').let { UNITS[it] ?: it }

    private fun parseNum(s0: String): Num? {
        var s = buildString { s0.trim().forEach { c -> VULGAR[c]?.let { append(" ").append(it) } ?: append(if (c == '⁄') '/' else c) } }.trim()
        val neg = s.startsWith("-") || s.startsWith("−")
        if (neg) s = s.drop(1).trim()
        fun unit(u: String): String? = u.trim().takeIf { UNIT_OK.matches(it) }?.let(::unitNorm)
        FRACTION.matchEntire(s)?.let { m ->
            val whole = m.groupValues[1].toLongOrNull() ?: 0L
            val n = m.groupValues[2].toLongOrNull() ?: return null
            val d = m.groupValues[3].toLongOrNull()?.takeIf { it != 0L } ?: return null
            val u = unit(m.groupValues[4]) ?: return null
            val v = whole + n.toDouble() / d
            val form = (if (m.groupValues[1].isNotEmpty()) "$whole " else "") + "$n/$d"
            return Num(if (neg) -v else v, u, form)
        }
        DECIMAL.matchEntire(s)?.let { m ->
            unit(m.groupValues[3])?.let { u ->
                val v = (m.groupValues[1].replace(".", "").replace(" ", "") + m.groupValues[2].let { if (it.isEmpty()) "" else ".$it" }).toDoubleOrNull() ?: return null
                return Num(if (neg) -v else v, u, "dec")
            }
        }
        DECIMAL_DOT.matchEntire(s)?.let { m ->
            val u = unit(m.groupValues[3]) ?: return null
            val v = "${m.groupValues[1]}.${m.groupValues[2]}".toDoubleOrNull() ?: return null
            return Num(if (neg) -v else v, u, "dec")
        }
        return null
    }

    private val FORM = Regex("Bruch|kürz|erweiter|gemischte|Dezimal|Kommazahl|Prozent|Kehrwert|Nachkommastelle|Promille", RegexOption.IGNORE_CASE)
    /** Verlangt die Frage eine bestimmte Schreibweise („Kürze 12/18“, „0,75 als Bruch?“, „1/8 als Dezimalzahl?“)? */
    fun formRequired(question: String) = FORM.containsMatchIn(question)

    private fun numbersMatch(t: Num, a: Num, formRequired: Boolean): Boolean {
        val unitOk = t.unit.isEmpty() || a.unit.isEmpty() || t.unit == a.unit
        if (unitOk && kotlin.math.abs(t.value - a.value) < 1e-9) return !formRequired || t.form == a.form || (t.form == "dec" && a.form == "dec")
        if (formRequired) return false
        // Anteil und Prozent: 1/2 = 0,5 = 50 %
        if (t.unit == "%" && a.unit.isEmpty() && kotlin.math.abs(t.value / 100 - a.value) < 1e-9) return true
        if (a.unit == "%" && t.unit.isEmpty() && kotlin.math.abs(a.value / 100 - t.value) < 1e-9) return true
        return false
    }

    // ------------------------------------------------------------------ Zahlwörter (Deutsch, Englisch)

    private val DE_NUM = mapOf("null" to 0, "eins" to 1, "ein" to 1, "eine" to 1, "zwei" to 2, "drei" to 3, "vier" to 4, "fünf" to 5,
        "sechs" to 6, "sieben" to 7, "acht" to 8, "neun" to 9, "zehn" to 10, "elf" to 11, "zwölf" to 12, "dreizehn" to 13,
        "vierzehn" to 14, "fünfzehn" to 15, "sechzehn" to 16, "siebzehn" to 17, "achtzehn" to 18, "neunzehn" to 19,
        "zwanzig" to 20, "dreißig" to 30, "dreissig" to 30, "vierzig" to 40, "fünfzig" to 50, "sechzig" to 60, "siebzig" to 70,
        "achtzig" to 80, "neunzig" to 90, "hundert" to 100, "einhundert" to 100, "tausend" to 1000, "eintausend" to 1000)
    private val EN_NUM = mapOf("zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7,
        "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15,
        "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30, "forty" to 40,
        "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90, "hundred" to 100, "thousand" to 1000)
    private val DE_TENS = listOf("zwanzig", "dreißig", "dreissig", "vierzig", "fünfzig", "sechzig", "siebzig", "achtzig", "neunzig")

    /** „acht“, „Eight“, „einundzwanzig“, „twenty-one“ → Wert; sonst null */
    fun numberWord(word: String): Int? {
        val w = word.lowercase().trim().trimEnd('.', '!')
        DE_NUM[w]?.let { return it }
        EN_NUM[w]?.let { return it }
        for (ten in DE_TENS) if (w.endsWith("und$ten")) {
            val one = DE_NUM[w.removeSuffix("und$ten")]?.takeIf { it in 1..9 } ?: return null
            return DE_NUM.getValue(ten) + one
        }
        val p = w.split('-', ' ')
        if (p.size == 2) { val t = EN_NUM[p[0]]; val o = EN_NUM[p[1]]; if (t != null && o != null && t in 20..90 && t % 10 == 0 && o in 1..9) return t + o }
        return null
    }

    /** Wörter, die schon in der Frage stehen – die zählen nicht als eigene Antwort */
    private fun questionWords(question: String) = nurWorte(question.lowercase()).split(' ').filter { it.isNotBlank() }.toSet()

    /** „acht Tage“ → „8 Tage“ (nur wenn das Zahlwort nicht schon in der Frage steht) */
    private fun withDigits(typed: String, question: String): String? {
        val words = typed.trim().split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        val qw = questionWords(question)
        for (n in minOf(2, words.size) downTo 1) {
            val head = words.take(n).joinToString(" ")
            val v = numberWord(head) ?: continue
            if (words.take(n).any { it.lowercase().trimEnd('.', '!') in qw }) return null
            return (listOf(v.toString()) + words.drop(n)).joinToString(" ")
        }
        return null
    }

    // ------------------------------------------------------------------ Aufzählungen, Kurzformen, Präpositionen

    private val ORDER = Regex(
        "Reihenfolge|der Reihe nach|nacheinander|von \\S+ nach \\S+|aufsteigend|absteigend|sortier|ordne|zuerst|Epoche|bedeutet|Bedeutung|übersetz|Titel|Motto|Redewendung|Sprichwort|Zitat",
        RegexOption.IGNORE_CASE,
    )
    /** Kommt es auf die Reihenfolge an (oder ist die Antwort ein fester Ausdruck wie „Sturm und Drang“)? */
    fun orderMatters(question: String) = ORDER.containsMatchIn(question)

    private val LIST_A = Regex(",\\s+|\\s+(?:und|and|&)\\s+")
    private val LIST_T = Regex("\\s*[,;&]\\s*|\\s+(?:und|and|et|y)\\s+")

    /** „Gelb, Rot, Blau“ → [Gelb, Rot, Blau]; „Romulus und Remus“ → [Romulus, Remus]; keine Aufzählung → eine Angabe */
    fun listParts(answer: String): List<String> {
        val p = answer.trim().trimEnd('.', '!').split(LIST_A).map { it.trim() }.filter { it.isNotEmpty() }
        return if (p.size >= 2 && p.all { it.split(' ').size <= 4 }) p else listOf(answer)
    }

    private fun unordered(typed: String, answer: String, strict: Boolean, question: String): Verdict {
        val ap = listParts(answer)
        if (ap.size < 2 || orderMatters(question)) return Verdict.WRONG
        val tp = typed.trim().trimEnd('.', '!').split(LIST_T).map { it.trim() }.filter { it.isNotEmpty() }
        if (tp.size != ap.size) return Verdict.WRONG
        val res = tp.map { t -> ap.map { a -> checkOne(t, a, strict, question) } }
        fun assign(i: Int, used: Set<Int>, worst: Verdict): Verdict {
            if (i == tp.size) return worst
            var best = Verdict.WRONG
            for (j in ap.indices) if (j !in used && res[i][j] != Verdict.WRONG) {
                best = better(best, assign(i + 1, used + j, worse(worst, res[i][j])))
                if (best == Verdict.RIGHT) return best
            }
            return best
        }
        return assign(0, emptySet(), Verdict.RIGHT)
    }

    private val I = RegexOption.IGNORE_CASE
    private val EN_LONG = listOf(
        Regex("\\bwon't\\b", I) to "will not", Regex("\\bcan't\\b", I) to "cannot", Regex("\\bcan not\\b", I) to "cannot",
        Regex("\\bshan't\\b", I) to "shall not",
        Regex("\\b(do|does|did|is|are|was|were|have|has|had|would|could|should|must|need)n't\\b", I) to "$1 not", Regex("\\bI'm\\b", I) to "I am",
        Regex("'re\\b", I) to " are", Regex("'ve\\b", I) to " have", Regex("'ll\\b", I) to " will", Regex("'d\\b", I) to " would",
        Regex("\\b(it|he|she|that|what|there|here|who|where|how)'s\\b", I) to "$1 is",
    )
    /** Englische Kurzformen ausschreiben: „I'm“ → „I am“, „didn't“ → „did not“, „It's“ → „It is“ */
    fun englishLong(s: String): String {
        var x = s.replace('’', '\'')
        if ('\'' !in x && !x.contains("can not", ignoreCase = true)) return x
        EN_LONG.forEach { (r, w) -> x = r.replace(x, w) }
        return x
    }

    private val PREP = setOf("in", "im", "ins", "an", "am", "auf", "bei", "beim", "mit", "durch", "gegen", "aus", "zum", "zur", "unter", "um")
    /** „In der Schweiz“ → „der Schweiz“ (Artikel fallen beim Vergleich weg); null ohne Präposition vorne */
    fun withoutPreposition(answer: String): String? {
        val w = answer.trim().split(' ').filter { it.isNotBlank() }
        return if (w.size >= 2 && w[0].lowercase() in PREP) w.drop(1).joinToString(" ") else null
    }

    // ------------------------------------------------------------------ Prüfen

    /**
     * RIGHT: genau so (Leerzeichen egal) oder gleichwertig (siehe oben). ALMOST: kleiner Tippfehler.
     * @param strictSpelling Deutsch/Rechtschreibung: Dort zählt „fast“ nicht – die Schreibweise ist ja der Lernstoff.
     * @param question die Frage der Karte (für Reihenfolge, Schreibweise von Zahlen und Zahlwörter)
     */
    fun check(typed: String, answer: String, strictSpelling: Boolean = false, question: String = ""): Verdict {
        // Klammer-Alternativen gelten überall – in Deutsch wird jede Variante streng (Schreibweise) geprüft
        var best = Verdict.WRONG
        for (v in alternatives(answer)) {
            best = better(best, checkOne(typed, v, strictSpelling, question))
            if (best != Verdict.RIGHT) best = better(best, unordered(typed, v, strictSpelling, question))
            if (best == Verdict.RIGHT) return best
        }
        return best
    }

    /** „Kyjiw (Kiew)“ → [„Kyjiw (Kiew)“, „Kyjiw“, „Kiew“]; „Nomen (Namenwort / Substantiv)“ → auch beide Klammer-Formen */
    fun alternatives(answer: String): List<String> {
        // nur „Wort (Alternative)“ mit Leerzeichen vor der Klammer – Formeln wie „cos(x)“ bleiben ganz
        val m = Regex("^(.+?)\\s+\\(([^()]+)\\)\\s*$").matchEntire(answer.trim()) ?: return listOf(answer)
        val main = m.groupValues[1].trim()
        // „h / (4π)“ ist eine Formel, „Münster (mit Frankreich) und Osnabrück (…)“ hat Klammern mittendrin
        if ('(' in main || main.last() in "/·=+−-*×^:") return listOf(answer)
        val alts = m.groupValues[2].split(" / ").map { it.trim() }
        return (listOf(answer, main) + alts).filter { it.isNotBlank() }.distinct()
    }

    /** Akzente weg (Reykjavík = Reykjavik), Umlaute bleiben (Bar ≠ Bär). */
    private fun plain(s: String): String = buildString {
        for (c in s) append(when (c) {
            'á', 'à', 'â', 'ã', 'å' -> 'a'; 'Á', 'À', 'Â', 'Ã', 'Å' -> 'A'
            'é', 'è', 'ê', 'ë' -> 'e'; 'É', 'È', 'Ê', 'Ë' -> 'E'
            'í', 'ì', 'î', 'ï' -> 'i'; 'Í', 'Ì', 'Î', 'Ï' -> 'I'
            'ó', 'ò', 'ô', 'õ' -> 'o'; 'Ó', 'Ò', 'Ô', 'Õ' -> 'O'
            'ú', 'ù', 'û' -> 'u'; 'Ú', 'Ù', 'Û' -> 'U'
            'ñ' -> 'n'; 'Ñ' -> 'N'; 'ç' -> 'c'; 'Ç' -> 'C'; 'č' -> 'c'; 'Č' -> 'C'
            else -> c
        })
    }

    /** Unterscheiden sich zwei Antworten nur in Akzenten („le velo“/„le vélo“)? Beim Tippen bewusst gleich gewertet. */
    fun onlyAccents(x: String, y: String) = x != y && plain(clean(x)).equals(plain(clean(y)), ignoreCase = true)

    private fun sameBare(t: String, a: String) = bare(t).isNotEmpty() && plain(bare(t)).equals(plain(bare(a)), ignoreCase = true)

    private fun checkOne(typed: String, answer: String, strictSpelling: Boolean, question: String = ""): Verdict {
        val t = clean(typed); val a = clean(answer)
        if (t.isEmpty()) return Verdict.WRONG
        if (t == a) return Verdict.RIGHT
        // Zahlen: exakt nach Wert vergleichen (0,5 = 0,50 = 1/2 = ½; Vorzeichen zählt; kein „fast richtig“ für 0,2462 statt 0,2461)
        parseNum(a)?.let { an ->
            val tn = parseNum(t) ?: withDigits(t, question)?.let(::parseNum) ?: return Verdict.WRONG
            return if (numbersMatch(tn, an, formRequired(question))) Verdict.RIGHT else Verdict.WRONG
        }
        // Außer in Rechtschreibung zählen Groß/klein sowie Bindestrich-/Leerzeichen-Varianten nicht als Fehler
        // („RuBisCO“ = „Rubisco“, „Marshallplan“ = „Marshall-Plan“)
        if (!strictSpelling && joined(t).equals(joined(a), ignoreCase = true)) return Verdict.RIGHT
        // … und ein fehlender/zusätzlicher Artikel oder Schlusspunkt („Blauwal“ = „Der Blauwal“, „read“ = „to read“) –
        // aber ein anderer Artikel ist ein Fehler („le maison“ statt „la maison“: höchstens „fast“)
        val wrongArticle = article(t) != null && article(a) != null && article(t) != article(a)
        if (!strictSpelling && !wrongArticle && sameBare(t, a)) return Verdict.RIGHT
        if (!strictSpelling) {
            // „I am full“ = „I'm full.“
            val te = englishLong(t); val ae = englishLong(a)
            if ((te != t || ae != a) && !wrongArticle && sameBare(te, ae)) return Verdict.RIGHT
            // „Schweiz“ = „In der Schweiz“ (Wo …?)
            withoutPreposition(a)?.let { if (sameBare(t, it)) return Verdict.RIGHT }
        }
        val nt = SpokenAnswer.normalize(englishLong(t)); val na = SpokenAnswer.normalize(englishLong(a))
        if (na.isEmpty()) return Verdict.WRONG
        val close = nt == na || SpokenAnswer.levenshtein(nt, na) <= maxOf(1, na.length / 6) && na.length >= 5
        return when {
            !close -> Verdict.WRONG
            strictSpelling -> Verdict.WRONG
            else -> Verdict.ALMOST
        }
    }
}

/**
 * Vergleich für gesprochene Antworten: Groß/klein, Satzzeichen und Artikel egal, kleine Erkennungsfehler erlaubt.
 * Wie beim Tippen zählen Klammer-Alternativen, Aufzählungen in anderer Reihenfolge und englische Kurzformen;
 * dazu „halb 8“ für „halb acht“ (Spracherkennung schreibt Ziffern), „Schweiz“ für „In der Schweiz“ und
 * bei „Wer …?“-Fragen der Nachname („Einstein“ für „Albert Einstein“).
 */
object SpokenAnswer {
    private val articles = setOf("der", "die", "das", "the", "a", "an", "le", "la", "les", "l", "un", "une", "el", "los", "las")

    fun normalize(s: String): String = s.lowercase()
        .let(::nurWorte)
        .split(Regex("\\s+")).filter { it.isNotBlank() && it !in articles }.joinToString(" ")

    fun matches(heard: String, answer: String, question: String = ""): Boolean =
        TypedAnswer.alternatives(answer).any { a ->
            // einmal wörtlich, einmal mit ausgeschriebenen Kurzformen („I'm“ = „I am“)
            matchOne(heard, a, question, false) || matchOne(heard, a, question, true)
        }

    private fun close(h: String, a: String): Boolean {
        if (a.isEmpty()) return false
        if (h == a || h.contains(a)) return true
        return levenshtein(h, a) <= maxOf(1, a.length / 6)
    }

    private val PARTICLES = setOf("van", "von", "da", "de", "di", "du", "der", "zu", "le", "la", "del")

    private fun matchOne(heard: String, answer: String, question: String, long: Boolean): Boolean {
        fun n(s: String) = normalize(if (long) TypedAnswer.englishLong(s) else s)
        val a = n(answer)
        if (a.isEmpty()) return false
        // Spracherkennung schreibt Zahlen als Ziffern: „halb 8“ → „halb acht“, wenn die Antwort das Zahlwort enthält
        val aw = a.split(' ')
        val h = n(heard).split(' ').filter { it.isNotEmpty() }.joinToString(" ") { w ->
            w.toIntOrNull()?.let { v -> aw.firstOrNull { TypedAnswer.numberWord(it) == v } } ?: w
        }
        if (h.isEmpty()) return false
        if (close(h, a)) return true
        // „In der Schweiz“: „Schweiz“ genügt; „to read“: „read“ genügt
        TypedAnswer.withoutPreposition(answer)?.let { if (close(h, normalize(it))) return true }
        if (aw.size >= 2 && aw[0] == "to" && close(h, aw.drop(1).joinToString(" "))) return true
        // „Wer entwickelte …?“ – „Einstein“ ist eine vollständige Antwort
        val raw = answer.trim().split(' ').filter { it.isNotBlank() }
        val person = question.trimStart().startsWith("Wer ", ignoreCase = true) && !question.contains("Vor- und Nachname") &&
            raw.size in 2..5 && raw.first().first().isUpperCase() && raw.last().first().isUpperCase() &&
            raw.all { it.first().isUpperCase() || it.lowercase() in PARTICLES }
        if (person) for (k in 1 until aw.size) {
            val rest = aw.drop(k).joinToString(" ")
            if (rest.length >= 3 && close(h, rest) && h.length >= 3) return true
        }
        // Aufzählung in anderer Reihenfolge („Deimos und Phobos“)
        val parts = TypedAnswer.listParts(answer)
        if (parts.size >= 2 && !TypedAnswer.orderMatters(question)) {
            val skip = setOf("und", "and", "et", "y")
            val want = parts.flatMap { normalize(it).split(' ') }.filter { it.isNotEmpty() && it !in skip }.sorted().joinToString(" ")
            val got = h.split(' ').filter { it !in skip }.sorted().joinToString(" ")
            if (want.isNotEmpty() && close(got, want)) return true
        }
        return false
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

/**
 * Alles außer Buchstaben, Ziffern und Leerzeichen wird Leerzeichen (früher `Regex("[^\\p{L}\\p{N} ]")`).
 * Ohne Regex: Kotlin/Wasm (WebApp) kennt `\\p{N}` nicht und brach dort ab (Brummi-Radio, Sprech-Spiel).
 */
fun nurWorte(s: String): String = buildString(s.length) { s.forEach { append(if (it.isLetterOrDigit() || it == ' ') it else ' ') } }
