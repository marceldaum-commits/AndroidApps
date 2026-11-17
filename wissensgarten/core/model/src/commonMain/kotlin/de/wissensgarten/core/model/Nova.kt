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
        s = ohneSymbole(s)
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

    /**
     * Emojis und andere Symbole (Unicode-Kategorie „So“), Variantenwähler, Verbinder und einzelne Ersatzzeichen werden zu Leerzeichen –
     * genau wie früher `Regex("[\\p{So}\\p{Cs}\\uFE0F\\u200D\\u20E3]")` auf dem JVM. Bewusst ohne Regex und mit fester Tabelle:
     * Kotlin/Wasm (WebApp) kennt `\\p{So}` für Emojis nicht, und ältere Android-Geräte kennen neuere Emojis nicht – dann passte der
     * Dateiname nicht zur Vorlese-Liste und die Gerätestimme las statt Nova.
     */
    fun ohneSymbole(text: String): String {
        val sb = StringBuilder(text.length); var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                val cp = 0x10000 + ((c.code - 0xD800) shl 10) + (text[i + 1].code - 0xDC00)
                if (symbol(cp)) sb.append(' ') else sb.append(c).append(text[i + 1])
                i += 2; continue
            }
            if (c.isSurrogate() || c == '\uFE0F' || c == '\u200D' || c == '\u20E3' || symbol(c.code)) sb.append(' ') else sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun symbol(cp: Int): Boolean {
        // binäre Suche in [von, bis]-Paaren
        var lo = 0; var hi = SO.size / 2 - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            when {
                cp < SO[2 * mid] -> hi = mid - 1
                cp > SO[2 * mid + 1] -> lo = mid + 1
                else -> return true
            }
        }
        return false
    }

    /** Unicode-Kategorie „So“ (Other Symbol) als Bereiche von–bis – erzeugt mit Character.getType auf dem JVM (184 Bereiche, Java 21.0.12.1) */
    private val SO = intArrayOf(
        0xA6, 0xA6, 0xA9, 0xA9, 0xAE, 0xAE, 0xB0, 0xB0, 0x482, 0x482, 0x58D, 0x58E,
        0x60E, 0x60F, 0x6DE, 0x6DE, 0x6E9, 0x6E9, 0x6FD, 0x6FE, 0x7F6, 0x7F6, 0x9FA, 0x9FA,
        0xB70, 0xB70, 0xBF3, 0xBF8, 0xBFA, 0xBFA, 0xC7F, 0xC7F, 0xD4F, 0xD4F, 0xD79, 0xD79,
        0xF01, 0xF03, 0xF13, 0xF13, 0xF15, 0xF17, 0xF1A, 0xF1F, 0xF34, 0xF34, 0xF36, 0xF36,
        0xF38, 0xF38, 0xFBE, 0xFC5, 0xFC7, 0xFCC, 0xFCE, 0xFCF, 0xFD5, 0xFD8, 0x109E, 0x109F,
        0x1390, 0x1399, 0x166D, 0x166D, 0x1940, 0x1940, 0x19DE, 0x19FF, 0x1B61, 0x1B6A, 0x1B74, 0x1B7C,
        0x2100, 0x2101, 0x2103, 0x2106, 0x2108, 0x2109, 0x2114, 0x2114, 0x2116, 0x2117, 0x211E, 0x2123,
        0x2125, 0x2125, 0x2127, 0x2127, 0x2129, 0x2129, 0x212E, 0x212E, 0x213A, 0x213B, 0x214A, 0x214A,
        0x214C, 0x214D, 0x214F, 0x214F, 0x218A, 0x218B, 0x2195, 0x2199, 0x219C, 0x219F, 0x21A1, 0x21A2,
        0x21A4, 0x21A5, 0x21A7, 0x21AD, 0x21AF, 0x21CD, 0x21D0, 0x21D1, 0x21D3, 0x21D3, 0x21D5, 0x21F3,
        0x2300, 0x2307, 0x230C, 0x231F, 0x2322, 0x2328, 0x232B, 0x237B, 0x237D, 0x239A, 0x23B4, 0x23DB,
        0x23E2, 0x2426, 0x2440, 0x244A, 0x249C, 0x24E9, 0x2500, 0x25B6, 0x25B8, 0x25C0, 0x25C2, 0x25F7,
        0x2600, 0x266E, 0x2670, 0x2767, 0x2794, 0x27BF, 0x2800, 0x28FF, 0x2B00, 0x2B2F, 0x2B45, 0x2B46,
        0x2B4D, 0x2B73, 0x2B76, 0x2B95, 0x2B97, 0x2BFF, 0x2CE5, 0x2CEA, 0x2E50, 0x2E51, 0x2E80, 0x2E99,
        0x2E9B, 0x2EF3, 0x2F00, 0x2FD5, 0x2FF0, 0x2FFB, 0x3004, 0x3004, 0x3012, 0x3013, 0x3020, 0x3020,
        0x3036, 0x3037, 0x303E, 0x303F, 0x3190, 0x3191, 0x3196, 0x319F, 0x31C0, 0x31E3, 0x3200, 0x321E,
        0x322A, 0x3247, 0x3250, 0x3250, 0x3260, 0x327F, 0x328A, 0x32B0, 0x32C0, 0x33FF, 0x4DC0, 0x4DFF,
        0xA490, 0xA4C6, 0xA828, 0xA82B, 0xA836, 0xA837, 0xA839, 0xA839, 0xAA77, 0xAA79, 0xFD40, 0xFD4F,
        0xFDCF, 0xFDCF, 0xFDFD, 0xFDFF, 0xFFE4, 0xFFE4, 0xFFE8, 0xFFE8, 0xFFED, 0xFFEE, 0xFFFC, 0xFFFD,
        0x10137, 0x1013F, 0x10179, 0x10189, 0x1018C, 0x1018E, 0x10190, 0x1019C, 0x101A0, 0x101A0, 0x101D0, 0x101FC,
        0x10877, 0x10878, 0x10AC8, 0x10AC8, 0x1173F, 0x1173F, 0x11FD5, 0x11FDC, 0x11FE1, 0x11FF1, 0x16B3C, 0x16B3F,
        0x16B45, 0x16B45, 0x1BC9C, 0x1BC9C, 0x1CF50, 0x1CFC3, 0x1D000, 0x1D0F5, 0x1D100, 0x1D126, 0x1D129, 0x1D164,
        0x1D16A, 0x1D16C, 0x1D183, 0x1D184, 0x1D18C, 0x1D1A9, 0x1D1AE, 0x1D1EA, 0x1D200, 0x1D241, 0x1D245, 0x1D245,
        0x1D300, 0x1D356, 0x1D800, 0x1D9FF, 0x1DA37, 0x1DA3A, 0x1DA6D, 0x1DA74, 0x1DA76, 0x1DA83, 0x1DA85, 0x1DA86,
        0x1E14F, 0x1E14F, 0x1ECAC, 0x1ECAC, 0x1ED2E, 0x1ED2E, 0x1F000, 0x1F02B, 0x1F030, 0x1F093, 0x1F0A0, 0x1F0AE,
        0x1F0B1, 0x1F0BF, 0x1F0C1, 0x1F0CF, 0x1F0D1, 0x1F0F5, 0x1F10D, 0x1F1AD, 0x1F1E6, 0x1F202, 0x1F210, 0x1F23B,
        0x1F240, 0x1F248, 0x1F250, 0x1F251, 0x1F260, 0x1F265, 0x1F300, 0x1F3FA, 0x1F400, 0x1F6D7, 0x1F6DC, 0x1F6EC,
        0x1F6F0, 0x1F6FC, 0x1F700, 0x1F776, 0x1F77B, 0x1F7D9, 0x1F7E0, 0x1F7EB, 0x1F7F0, 0x1F7F0, 0x1F800, 0x1F80B,
        0x1F810, 0x1F847, 0x1F850, 0x1F859, 0x1F860, 0x1F887, 0x1F890, 0x1F8AD, 0x1F8B0, 0x1F8B1, 0x1F900, 0x1FA53,
        0x1FA60, 0x1FA6D, 0x1FA70, 0x1FA7C, 0x1FA80, 0x1FA88, 0x1FA90, 0x1FABD, 0x1FABF, 0x1FAC5, 0x1FACE, 0x1FADB,
        0x1FAE0, 0x1FAE8, 0x1FAF0, 0x1FAF8, 0x1FB00, 0x1FB92, 0x1FB94, 0x1FBCA,
    )

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

    /** Bestqualität (unkomprimiert) – nur für „Offline laden“ */
    fun urlOriginal(h: String): String = "$BASE/nova/original/${h.substring(0, 2)}/$h.mp3"

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

    /**
     * Häufige Laufzeit-Sätze aus einem Paket, die vorab in die Liste dürfen (sie entstehen nur aus festen Paketdaten):
     * Brummi-Radio „Etwas Neues: … Sag es mir nach!“ (jede neue Karte), „Hör noch mal: … Das üben wir bald wieder.“ und
     * „Fast! Die Antwort ist: …“. Alle deutsch gesprochen, wie im Radio. Seltenere Sätze kommen bei Bedarf über `/jetzt`.
     */
    fun packLaufzeit(pack: Pack): List<Eintrag> = buildList {
        val peers = pack.cards.map { it.a }.distinct()
        pack.cards.forEach { c ->
            val card = StudyCard(c.id, "", c.q, c.a, c.choices, c.info, Memory())
            add(eintrag(Radio.prompt(Radio.Question(card, Radio.Mode.LEARN)), "de"))
            add(eintrag(Radio.again(card), "de"))
            add(eintrag(Radio.correction(Radio.Question(card, Radio.Mode.FREE)), "de"))
            // Auswahl-Fragen (A, B, C, D) je Schwierigkeitsstufe – auf jedem Gerät gleich, wenn das Beet das ganze Paket ist
            RADIO_STUFEN.forEach { st ->
                val q = Radio.question(card.copy(memory = Memory(stability = st, reps = 1, lastReview = 1)), peers)
                if (q.mode == Radio.Mode.CHOICE || q.mode == Radio.Mode.SELF) add(eintrag(Radio.prompt(q), "de"))
                if (q.mode == Radio.Mode.SELF) add(eintrag(Radio.selfReveal(q), "de"))
            }
        }
    }.filterNotNull().distinct().filter { it.text.length <= MAX_LISTE }

    /** Stabilität je Schwierigkeitsstufe (Keimling, Spross, Busch und höher) – bestimmt die Zahl der Antworten im Radio */
    private val RADIO_STUFEN = listOf(1.0, 3.0, 10.0)

    /** Laufzeit-Sätze aus Zahlen (endlich viele): Radio-Schluss, Gedächtnis-Palast, Bonusrunden, Abenteuer-Kapitel */
    fun zahlenTexte(): List<Eintrag> = buildList {
        for (total in 1..Laufzeit.RADIO_MAX) for (correct in 0..total) add(Radio.summary(correct, total))
        for (total in 1..Loci.places.size) for (right in 0..total) add(Laufzeit.palastText(right, total))
        for (tau in Laufzeit.BONUS_TAU) { add(Laufzeit.bonusText(tau, true)); add(Laufzeit.bonusText(tau, false)) }
        Story.chapters.drop(1).forEach { add(Laufzeit.kapitelOffenText(it.number)) }
        val maxGap = Story.chapters.zipWithNext { a, b -> b.unlockAt - a.unlockAt }.maxOrNull() ?: 0
        for (n in 1..maxGap) add(Laufzeit.kapitelWartetText(n))
    }.distinct().mapNotNull { eintrag(it, "de") }

    /**
     * Langen Text in ganze Sätze teilen, die je höchstens [max] Zeichen haben (für `/jetzt`). Zu lange Einzelsätze werden an
     * Komma/Gedankenstrich geteilt; geht auch das nicht → leere Liste (dann liest die Gerätestimme).
     */
    fun saetze(text: String, max: Int): List<String> {
        fun pack(parts: List<String>, sep: String): List<String>? {
            val out = ArrayList<String>(); var cur = ""
            for (p in parts) {
                if (p.length > max) return null
                cur = if (cur.isEmpty()) p else if (cur.length + sep.length + p.length <= max) cur + sep + p else { out += cur; p }
            }
            if (cur.isNotEmpty()) out += cur
            return out
        }
        val sentences = text.split(SATZ_ENDE).map { it.trim() }.filter { it.isNotEmpty() }.flatMap { s ->
            if (s.length <= max) listOf(s) else pack(s.split(TEIL_ENDE).map { it.trim() }.filter { it.isNotEmpty() }, " ") ?: return emptyList()
        }
        return pack(sentences, " ") ?: emptyList()
    }
    private val SATZ_ENDE = Regex("(?<=[.!?…])\\s+")
    private val TEIL_ENDE = Regex("(?<=[,;:–])\\s+")

    /** Feste Sätze aus der Lern-Logik: Abenteuer-Geschichte, Feste, Brummi-Radio, Wissens-Safari, Zahlen-Sätze */
    fun festeTexte(): List<Eintrag> = buildList {
        Story.chapters.forEach { ch -> ch.scenes.forEach { add(eintrag(it.text, "de")) } }
        Seasons.FESTIVALS.forEach { add(eintrag(it.greeting, "de")) }
        Radio.FIXED.forEach { add(eintrag(it, "de")) }
        Safari.words.forEach { wd ->
            add(eintrag(Safari.say(wd), "de")); add(eintrag(wd.en, "en"))
            listOf(true, false).forEach { e -> val c = Safari.card(wd, e); add(eintrag(c.q, "de")); add(eintrag(c.a, "de")); add(eintrag(c.info, "de")) }
        }
        addAll(zahlenTexte())
    }.filterNotNull()
}
