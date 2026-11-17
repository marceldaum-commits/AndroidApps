package de.wissensgarten.core.model

/** Unicode-Normalform C (zusammengesetzte Zeichen) – Android: java.text.Normalizer, Web: String.prototype.normalize. */
expect fun nfc(s: String): String

/**
 * Brummis Stimme „Nova“ (KI-Stimme von OpenAI, vertont von unserem Server – die App selbst spricht nie mit OpenAI).
 *
 * Jeder Vorlese-Text hat einen festen Namen: h = die ersten 32 Hex-Zeichen von SHA-256 über „nova|sprache|text“ (Text normalisiert:
 * NFC, jeder Leerraum ein Leerzeichen, vorne/hinten getrimmt). Die Audiodatei liegt unter [BASE]/nova/h[0:2]/h.mp3.
 * Feste Texte vertont der Server vorab aus der Liste `vorlesen/texte.json` der WebApp (erzeugt von `VorlesenListeTest`).
 *
 * Wichtig: Liste und App benutzen genau dieselben Funktionen ([sprache], [sprechText]) – sonst passt der Name nicht.
 */
object Nova {
    const val BASE = "https://stimmen.mathenachhilfe-online.com"
    val SPRACHEN = setOf("de", "en", "fr", "es")

    /** Höchstlänge eines Textes in der Vorab-Liste */
    const val MAX_LISTE = 1500

    private val GERMAN_HINT = Regex("(?<![\\p{L}])(der|die|das|ein|eine|ist|was|wie|welche[rs]?|heißt|und|auf|von|mit|nicht)(?![\\p{L}])|[äöüßÄÖÜ]", RegexOption.IGNORE_CASE)

    /** Sprache eines Vorlese-Textes: bei Sprach-Fächern die Fremdsprache, außer der Text ist erkennbar deutsch. */
    fun sprache(subject: String?, text: String): String {
        val foreign = when (subject) {
            "englisch" -> "en"
            "franzoesisch" -> "fr"
            "spanisch" -> "es"
            else -> return "de"
        }
        return if (GERMAN_HINT.containsMatchIn(text)) "de" else foreign
    }

    /** Wörter für Rechenzeichen und Einheiten je Sprache */
    private class W(
        val plus: String, val minus: String, val mal: String, val durch: String, val gleich: String, val ungefaehr: String,
        val ungleich: String, val kleiner: String, val groesser: String, val kleinerGleich: String, val groesserGleich: String,
        val plusMinus: String, val wurzel: String, val hoch: String, val prozent: String, val promille: String, val euro: String,
        val grad: String, val celsius: String, val also: String, val oder: String, val betrag: String, val paragraf: String, val und: String,
    )
    private val WORDS = mapOf(
        "de" to W("plus", "minus", "mal", "durch", "gleich", "ungefähr", "ungleich", "kleiner als", "größer als", "kleiner oder gleich",
            "größer oder gleich", "plus minus", "Wurzel aus", "hoch", "Prozent", "Promille", "Euro", "Grad", "Grad Celsius", "also", "oder",
            "Betrag von", "Paragraf", "und"),
        "en" to W("plus", "minus", "times", "divided by", "equals", "approximately", "is not equal to", "is less than", "is greater than",
            "is less than or equal to", "is greater than or equal to", "plus or minus", "the square root of", "to the power of", "percent",
            "per mille", "euros", "degrees", "degrees Celsius", "so", "or", "the absolute value of", "section", "and"),
        "fr" to W("plus", "moins", "fois", "divisé par", "égale", "environ", "différent de", "inférieur à", "supérieur à", "inférieur ou égal à",
            "supérieur ou égal à", "plus ou moins", "racine carrée de", "puissance", "pour cent", "pour mille", "euros", "degrés",
            "degrés Celsius", "donc", "ou", "valeur absolue de", "paragraphe", "et"),
        "es" to W("más", "menos", "por", "dividido por", "igual a", "aproximadamente", "distinto de", "menor que", "mayor que", "menor o igual que",
            "mayor o igual que", "más o menos", "raíz cuadrada de", "elevado a", "por ciento", "por mil", "euros", "grados", "grados Celsius",
            "entonces", "o", "valor absoluto de", "párrafo", "y"),
    )

    /** Einheiten (nur Deutsch): „km/h“ → „Kilometer pro Stunde“, „cm²“ → „Quadratzentimeter“ … */
    private val UNITS = listOf(
        "km/h" to "Kilometer pro Stunde", "m/s²" to "Meter pro Quadratsekunde", "m/s" to "Meter pro Sekunde", "N/kg" to "Newton pro Kilogramm",
        "g/mol" to "Gramm pro Mol", "N/m²" to "Newton pro Quadratmeter",
        "mm²" to "Quadratmillimeter", "cm²" to "Quadratzentimeter", "dm²" to "Quadratdezimeter", "km²" to "Quadratkilometer", "m²" to "Quadratmeter",
        "mm³" to "Kubikmillimeter", "cm³" to "Kubikzentimeter", "dm³" to "Kubikdezimeter", "m³" to "Kubikmeter",
    ).map { (u, w) -> Regex("(?<![\\p{L}])" + Regex.escape(u) + "(?![\\p{L}\\p{Nd}])") to w }

    private val EMOJI = Regex("[\\p{So}\\p{Cs}\\uFE0F\\u200D\\u20E3]")
    private const val SUP = "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻"
    private const val SUB = "₀₁₂₃₄₅₆₇₈₉"
    private val SUP_RUN = Regex("[⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻]+")
    private val SUB_RUN = Regex("[₀₁₂₃₄₅₆₇₈₉]+")
    private val ABS = Regex("\\|([^|]{1,30})\\|")
    private val BLANK = Regex("_{2,}")
    private val NEG = Regex("(^|[ (\\[])[-−](?=[\\p{Nd}])")
    private val ASCII_MINUS = Regex("(?<=[\\p{Nd}) ]) - (?=[\\p{Nd}(])")
    private val SPACE_BEFORE = Regex(" +([.,!?:;)“”])")
    private val SPACE_AFTER = Regex("([(„]) +")
    private val PUNCT_RUN = Regex("([.,!?:;])( ?[,;])+")

    /**
     * Text so, wie Nova ihn spricht: ohne Emojis, Rechenzeichen und Einheiten als Wörter, Lücken „___“ als Pause.
     * Abkürzungen („z. B.“) und Klammern bleiben – die KI-Stimme liest sie natürlich.
     */
    fun sprechText(text: String, sprache: String = "de"): String {
        val w = WORDS[sprache] ?: WORDS.getValue("de")
        var s = text
        // Grad zuerst – ° ist ein „Symbol“ und fiele sonst mit den Emojis weg
        s = s.replace("°C", " ${w.celsius}").replace("°", " ${w.grad}")
        s = EMOJI.replace(s, " ")
        if (sprache == "de") UNITS.forEach { (r, word) -> s = r.replace(s, " $word") }
        // Hochzahlen: „Na⁺“ → „Na plus“ (Ladung), „a²“ → „a hoch 2“, „sin⁻¹“ → „sin hoch minus 1“; tiefgestellt: „H₂O“ → „H2O“
        s = SUP_RUN.replace(s) { m ->
            val v = m.value
            if (v.all { it == '⁺' || it == '⁻' } || (v.length >= 2 && v.last() in "⁺⁻" && v.dropLast(1).none { it in "⁺⁻" }))
                " " + v.dropLast(1).map { SUP.indexOf(it).toString() }.joinToString("") + (if (v.last() == '⁺') " ${w.plus} " else " ${w.minus} ")
            else " ${w.hoch} " + v.map { c -> when (c) { '⁻' -> "${w.minus} "; '⁺' -> "${w.plus} "; else -> SUP.indexOf(c).toString() } }.joinToString("") + " "
        }
        s = SUB_RUN.replace(s) { m -> m.value.map { SUB.indexOf(it).toString() }.joinToString("") }
        s = ABS.replace(s) { m -> " ${w.betrag} ${m.groupValues[1]} " }
        s = BLANK.replace(s, " … ")
        s = slashes(s, w)
        s = dots(s, w)
        s = s.replace("|", ", ")
        s = NEG.replace(s) { it.groupValues[1] + w.minus + " " }
        s = ASCII_MINUS.replace(s, " ${w.minus} ")
        s = s.replace("√", " ${w.wurzel} ").replace("±", " ${w.plusMinus} ").replace("−", " ${w.minus} ").replace("+", " ${w.plus} ")
            .replace("×", " ${w.mal} ").replace("÷", " ${w.durch} ").replace("≈", " ${w.ungefaehr} ")
            .replace("≠", " ${w.ungleich} ").replace("≤", " ${w.kleinerGleich} ").replace("≥", " ${w.groesserGleich} ")
            .replace(" < ", " ${w.kleiner} ").replace(" > ", " ${w.groesser} ").replace("=", " ${w.gleich} ").replace("^", " ${w.hoch} ")
            .replace("⇒", ", ${w.also} ").replace("→", ", ").replace("‰", " ${w.promille}").replace("%", " ${w.prozent}")
            .replace("€", " ${w.euro}").replace("§", " ${w.paragraf} ").replace("&", " ${w.und} ")
        s = einLeerzeichen(s)
        s = SPACE_BEFORE.replace(s, "$1")
        s = SPACE_AFTER.replace(s, "$1")
        s = PUNCT_RUN.replace(s, "$1")
        return s.trimStart(',', ';', ':', '.', ' ').trim()
    }

    /**
     * Schrägstrich: „1/2“ bleibt (die Stimme liest Brüche), „der Lehrer / die Lehrerin“ und „am/is/are“ → „oder“,
     * sonst (Formeln wie „x/3“, „G/H“) → „durch“.
     */
    private fun slashes(text: String, w: W): String {
        val sb = StringBuilder(text)
        var i = sb.indexOf("/")
        while (i >= 0) {
            var l = i; if (l > 0 && sb[l - 1] == ' ') l--
            var ls = l; while (ls > 0 && sb[ls - 1].isLetterOrDigit()) ls--
            var r = i + 1; if (r < sb.length && sb[r] == ' ') r++
            var re = r; while (re < sb.length && sb[re].isLetterOrDigit()) re++
            val left = sb.substring(ls, l); val right = sb.substring(r, re)
            val digits = left.isNotEmpty() && right.isNotEmpty() && left.all { it.isDigit() } && right.all { it.isDigit() }
            if (digits) { i = sb.indexOf("/", i + 1); continue }
            val words = left.isNotEmpty() && right.isNotEmpty() && left.all { it.isLetter() } && right.all { it.isLetter() } && (left.length >= 2 || right.length >= 2)
            val rep = if (words) " ${w.oder} " else " ${w.durch} "
            sb.setRange(l, r, rep)
            i = sb.indexOf("/", l + rep.length)
        }
        return sb.toString()
    }

    /** Malpunkt „a · b“ → „mal“; als Trenner zwischen Wörtern („1918 Weimarer Republik · 1933 …“) → Pause */
    private fun dots(text: String, w: W): String {
        val sb = StringBuilder(text)
        var i = sb.indexOf("·")
        while (i >= 0) {
            var l = i - 1; while (l >= 0 && sb[l] == ' ') l--
            var r = i + 1; while (r < sb.length && sb[r] == ' ') r++
            val prevOk = l >= 0 && (sb[l].isDigit() || sb[l] == ')' || (sb[l].isLetter() && (l == 0 || !sb[l - 1].isLetter())))
            val nextOk = r < sb.length && (sb[r].isDigit() || sb[r] == '(' || sb[r] == '√' || (sb[r].isLetter() && (r + 1 >= sb.length || !sb[r + 1].isLetter())))
            val rep = if (prevOk && nextOk) " ${w.mal} " else ", "
            sb.setRange(l + 1, r, rep)
            i = sb.indexOf("·", l + 1 + rep.length)
        }
        return sb.toString()
    }

    /** Jeder Leerraum (auch Zeilenumbruch, geschütztes Leerzeichen) wird ein Leerzeichen; vorne/hinten getrimmt. */
    fun einLeerzeichen(s: String): String {
        val sb = StringBuilder(s.length); var space = false
        for (c in s) {
            if (c.isWhitespace()) { space = true; continue }
            if (space && sb.isNotEmpty()) sb.append(' ')
            space = false; sb.append(c)
        }
        return sb.toString()
    }

    /** Normalform für den Namen: NFC, Leerraum zusammengefasst, getrimmt (genau wie der Server) */
    fun normalisiert(text: String): String = einLeerzeichen(nfc(text))

    /** Name der Audiodatei (32 Hex-Zeichen) für einen bereits gesprochenen Text ([sprechText]) */
    fun hash(text: String, sprache: String): String = Sha256.hex("nova|$sprache|${normalisiert(text)}".encodeToByteArray()).substring(0, 32)

    /** Adresse der vorab vertonten Datei */
    fun url(h: String): String = "$BASE/nova/${h.substring(0, 2)}/$h.mp3"

    /** Was die Theorie-Ansicht vorliest: alle Erklär-Szenen eines Pakets am Stück */
    fun theorie(scenes: List<IntroScene>): String = scenes.joinToString(" ") { it.text }

    /** Ein Vorlese-Text für die Liste: so, wie Nova ihn spricht */
    data class Eintrag(val text: String, val sprache: String)

    private fun eintrag(raw: String?, sprache: String): Eintrag? {
        if (raw.isNullOrBlank()) return null
        val t = normalisiert(sprechText(raw, sprache))
        return if (t.any { it.isLetterOrDigit() }) Eintrag(t, sprache) else null
    }

    /**
     * Alle festen Texte, die die App aus einem Paket vorliest – genau wie an den Vorlese-Stellen der Oberfläche:
     * Frage, Antwort, Erklärung (Sprache nach [sprache]), Intro-Szenen und Theorie (deutsch), Silben-Klatschen (deutsch).
     * Auswahl-Antworten (choices) liest die App nicht vor.
     */
    fun packTexte(pack: Pack): List<Eintrag> = buildList {
        pack.intro.forEach { add(eintrag(it.text, "de")) }
        if (pack.intro.isNotEmpty()) add(eintrag(theorie(pack.intro), "de"))
        pack.cards.forEach { c ->
            add(eintrag(c.q, sprache(pack.subject, c.q)))
            add(eintrag(c.a, sprache(pack.subject, c.a)))
            add(eintrag(c.info, c.info?.let { sprache(pack.subject, it) } ?: "de"))
            if (pack.subject !in Syllables.NO_CLAP_SUBJECTS && Syllables.isClapWord(c.a)) {
                add(eintrag(Syllables.spoken(c.a.trim()), "de"))
                add(eintrag(c.a.trim(), "de"))
            }
        }
    }.filterNotNull()

    /** Feste Sätze aus der Lern-Logik: Abenteuer-Geschichte, Feste, Brummi-Radio, Wissens-Safari */
    fun festeTexte(): List<Eintrag> = buildList {
        Story.chapters.forEach { ch -> ch.scenes.forEach { add(eintrag(it.text, "de")) } }
        Seasons.FESTIVALS.forEach { add(eintrag(it.greeting, "de")) }
        Radio.FIXED.forEach { add(eintrag(it, "de")) }
        Safari.words.forEach { wd ->
            add(eintrag(Safari.say(wd), "de")); add(eintrag(wd.en, "en"))
            listOf(true, false).forEach { e -> val c = Safari.card(wd, e); add(eintrag(c.q, "de")); add(eintrag(c.a, "de")); add(eintrag(c.info, "de")) }
        }
    }.filterNotNull()
}
