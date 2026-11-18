package de.wissensgarten.content

import de.wissensgarten.core.model.Games
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.PackJson
import de.wissensgarten.core.model.SpokenAnswer
import de.wissensgarten.core.model.TypedAnswer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aufgaben mit mehreren richtigen Antworten: Die Kontrolle (Eintippen, Sprechen, Auswahl) muss jede richtige Antwort
 * gelten lassen – und darf keinen Ablenker als richtig werten. Läuft über alle freien Pakete und, falls der private
 * Plus-Ordner vorliegt (`WG_PLUS_DIR` oder `website_backup/wissensgarten-plus` neben dem Repo), auch über die Plus-Pakete.
 *
 * Je Karte:
 * - jede Klammer-Alternative („Kyjiw (Kiew)“, „Nomen (Namenwort / Substantiv)“) wird akzeptiert,
 * - Zahlwort und Ziffer zählen gleich („acht“ = 8, „eight“ = 8),
 * - Aufzählungen gelten auch umgekehrt – außer bei Reihenfolge-Fragen und festen Ausdrücken,
 * - kein Ablenker (eigene `choices` und Antworten anderer Karten desselben Pakets) wird als richtig gewertet,
 * - steht ein Wort aus [SYNONYME] als Antwort da, zählen auch die anderen Wörter seiner Gruppe.
 */
class AntwortKontrolleTest {
    private val packsDir = File("src/main/assets/packs")
    private val plusDir: File? = (System.getenv("WG_PLUS_DIR")?.let(::File)
        ?: generateSequence(File("").absoluteFile) { it.parentFile }.map { File(it, "website_backup/wissensgarten-plus") }.firstOrNull { it.isDirectory })
        ?.takeIf { it.isDirectory }

    private fun packs(dir: File) = dir.listFiles { f -> f.extension == "json" && f.name != "versions.json" && f.name != "vorlesen-texte.json" }!!
        .sortedBy { it.name }.map { PackJson.parse(it.readText()) }

    private val all: List<Pack> by lazy { packs(packsDir) + (plusDir?.let(::packs) ?: emptyList()) }

    /**
     * Häufige gleichwertige Begriffe. Ist eins davon die Antwort einer Karte, müssen die anderen ebenfalls zählen
     * (über Klammer-Alternativen) – oder die Karte steht mit Grund in [EINDEUTIG].
     */
    private val SYNONYME = listOf(
        listOf("Samstag", "Sonnabend"), listOf("Apfelsine", "Orange"), listOf("Eiweiß", "Protein"), listOf("Eiweiße", "Proteine"),
        listOf("Möhre", "Karotte"), listOf("Den Haag", "Haag"), listOf("Kiew", "Kyjiw"), listOf("Peking", "Beijing"),
        listOf("Tokio", "Tokyo"), listOf("Neu-Delhi", "New Delhi"), listOf("Vatikanstadt", "Vatikan"), listOf("Mount Everest", "Everest"),
        listOf("Kohlenstoffdioxid", "Kohlendioxid"), listOf("Kohlenstoffmonoxid", "Kohlenmonoxid"), listOf("Fotosynthese", "Photosynthese"),
        listOf("Kalzium", "Calcium"), listOf("Silicium", "Silizium"), listOf("Iod", "Jod"), listOf("Traubenzucker", "Glucose"),
        listOf("Aorta", "Hauptschlagader"), listOf("Iris", "Regenbogenhaut"), listOf("Netzhaut", "Retina"), listOf("Chloroplasten", "Blattgrünkörner"),
        listOf("Spaltöffnungen", "Stomata"), listOf("Produzenten", "Erzeuger"), listOf("Destruenten", "Zersetzer"),
        listOf("Arterien", "Schlagadern"), listOf("Venen", "Blutadern"), listOf("Staubblätter", "Staubgefäße"),
        listOf("Elektronenpaarbindung", "Atombindung"), listOf("Waagerecht", "horizontal"), listOf("Gleichwarm", "warmblütig"),
        listOf("Seismograf", "Seismograph"), listOf("Pangäa", "Pangaea"), listOf("Kolosseum", "Colosseum"), listOf("Nirwana", "Nirvana"),
        listOf("Haddsch", "Hadsch"), listOf("Glykolyse", "Glycolyse"), listOf("Citratzyklus", "Zitronensäurezyklus"),
        listOf("Desoxyribonukleinsäure", "Desoxyribonucleinsäure"), listOf("tRNA", "Transfer-RNA"),
        listOf("Nomen", "Namenwort", "Substantiv"), listOf("Verb", "Tunwort", "Zeitwort"), listOf("Adjektiv", "Wiewort", "Eigenschaftswort"),
        listOf("Pronomen", "Fürwörter"), listOf("Präsens", "Gegenwart"), listOf("Präteritum", "Imperfekt"),
        listOf("Plusquamperfekt", "Vorvergangenheit"), listOf("Personifikation", "Personifizierung"), listOf("Alliteration", "Stabreim"),
        listOf("Hyperbel", "Übertreibung"), listOf("Euphemismus", "Beschönigung"), listOf("Onomatopoesie", "Lautmalerei"),
        listOf("Umstellprobe", "Verschiebeprobe"), listOf("Vorhersage", "Voraussage"),
        listOf("Novemberpogrome", "Reichspogromnacht"), listOf("Auschwitz-Birkenau", "Auschwitz"), listOf("Watt", "Wattenmeer"),
        listOf("Baikalsee", "Baikal"), listOf("Radweg", "Fahrradweg"), listOf("Minusgrade", "Frost"), listOf("Zellmembran", "Plasmamembran"),
        listOf("Boolean", "bool"), listOf("Integer", "int"), listOf("Parallel", "Parallelschaltung"), listOf("Treuhandanstalt", "Treuhand"),
        listOf("Frankfurt am Main", "Frankfurt"), listOf("Washington, D.C.", "Washington"), listOf("Zentrale Planwirtschaft", "Planwirtschaft"),
        listOf("Versailler Vertrag", "Vertrag von Versailles"), listOf("Sonnenlicht", "Licht"), listOf("Regenjacke", "Regenmantel"),
        listOf("Sonnencreme", "Sonnenmilch"), listOf("Handschuhe", "Fäustlinge"), listOf("Junge", "Knabe"), listOf("fürchten", "Angst haben"),
        listOf("sagen", "sprechen"), listOf("0 Uhr", "Mitternacht"),
        // Englisch: britisch/amerikanisch und gleich übliche Wörter
        listOf("colour", "color"), listOf("grey", "gray"), listOf("favourite", "favorite"), listOf("autumn", "fall"),
        listOf("trousers", "pants"), listOf("hello", "hi"), listOf("thank you", "thanks"), listOf("goodbye", "bye"),
        listOf("station", "train station"), listOf("break", "recess"),
        // Französisch
        listOf("le vélo", "la bicyclette"),
    )

    /** Karten, bei denen die Frage eindeutig nach einem bestimmten Wort fragt (Lösung b) – Grund steht dabei. */
    private val EINDEUTIG = mapOf(
        "gs-englisch-farben-zahlen/006" to "„orange“ ist hier die Farbe, nicht die Frucht",
        "gs-englisch-farben-zahlen/032" to "„orange“ ist hier die Farbe, nicht die Frucht",
        "s1-physik-einheiten-formeln/003" to "„Watt“ ist hier die Einheit der Leistung, nicht das Wattenmeer",
    )

    private fun key(p: Pack, id: String) = "${p.id}/$id"

    private fun norm(s: String) = s.trim().trimEnd('.', '!').lowercase().removePrefix("der ").removePrefix("die ").removePrefix("das ")
        .removePrefix("le ").removePrefix("la ").removePrefix("the ")

    // Zahlwörter für den Test (unabhängig von der App erzeugt)
    private val deEins = listOf("null", "eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn", "elf", "zwölf",
        "dreizehn", "vierzehn", "fünfzehn", "sechzehn", "siebzehn", "achtzehn", "neunzehn")
    private val deZehner = listOf("", "", "zwanzig", "dreißig", "vierzig", "fünfzig", "sechzig", "siebzig", "achtzig", "neunzig")
    private val enEins = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve",
        "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
    private val enZehner = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    private fun deutsch(n: Int): String = when {
        n < 20 -> deEins[n]
        n % 10 == 0 -> deZehner[n / 10]
        else -> (if (n % 10 == 1) "ein" else deEins[n % 10]) + "und" + deZehner[n / 10]
    }
    private fun englisch(n: Int): String = when {
        n < 20 -> enEins[n]
        n % 10 == 0 -> enZehner[n / 10]
        else -> enZehner[n / 10] + "-" + enEins[n % 10]
    }

    /** Zahl mit höchstens einer Einheit („8“, „8 Tage“, „100 °C“, „20 m/s“) – keine Rechenwege oder Formeln */
    private val SMALL_NUMBER = Regex("^(\\d{1,2})(?:\\s*([%€°]|°C)|\\s+([A-Za-zÄÖÜäöüß/²³]+(?: [A-Za-zÄÖÜäöüß]+)?))?$")

    @Test fun everyRightAnswerCountsAndNoDistractorDoes() {
        val errors = mutableListOf<String>()
        var cards = 0
        var plusCards = 0
        for (p in all) {
            val strict = p.subject == "deutsch"
            val answers = p.cards.map { it.a }.distinct()
            for (c in p.cards) {
                cards++
                if (p.isPlus) plusCards++
                val k = key(p, c.id)
                fun err(msg: String) { errors += "$k: $msg" }
                val alts = TypedAnswer.alternatives(c.a)

                // 1. jede Klammer-Alternative zählt (Tippen, und Sprechen, wo die Karte gesprochen wird)
                for (alt in alts) {
                    if (TypedAnswer.check(alt, c.a, strict, c.q) != TypedAnswer.Verdict.RIGHT) err("Alternative „$alt“ wird beim Tippen nicht akzeptiert")
                    if (Games.speakPossible(alt) && !SpokenAnswer.matches(alt, c.a, c.q)) err("Alternative „$alt“ wird beim Sprechen nicht akzeptiert")
                }

                // 2. Zahlwort = Ziffer
                for (alt in alts) {
                    val m = SMALL_NUMBER.matchEntire(alt.trim()) ?: continue
                    val n = m.groupValues[1].toInt()
                    val unit = m.groupValues[2] + m.groupValues[3]
                    val rest = if (unit.isEmpty()) "" else " $unit"
                    val qWords = c.q.lowercase().split(Regex("[^\\p{L}]+")).toSet()
                    for (w in listOf(deutsch(n), englisch(n))) {
                        if (w in qWords) continue
                        if (TypedAnswer.check(w + rest, c.a, strict, c.q) != TypedAnswer.Verdict.RIGHT) err("Zahlwort „$w$rest“ zählt nicht für „${c.a}“")
                    }
                }

                // 3. Aufzählungen in anderer Reihenfolge (außer Reihenfolge-Fragen)
                if (!TypedAnswer.orderMatters(c.q)) for (alt in alts) {
                    val parts = TypedAnswer.listParts(alt)
                    if (parts.size < 2) continue
                    val rev = parts.reversed().joinToString(", ")
                    if (TypedAnswer.check(rev, c.a, strict, c.q) != TypedAnswer.Verdict.RIGHT) err("umgekehrte Aufzählung „$rev“ zählt nicht")
                    if (Games.speakPossible(alt) && !SpokenAnswer.matches(parts.reversed().joinToString(" und "), c.a, c.q)) err("umgekehrte Aufzählung zählt beim Sprechen nicht")
                }

                // 4. kein Ablenker darf als richtig gelten: eigene choices und – wenn die Karte weniger als 3 eigene hat und
                //    die Auswahl daher mit Antworten anderer Karten desselben Pakets aufgefüllt wird – auch diese
                val peers = if (c.choices.orEmpty().size < 3) answers.filter { it != c.a }.map { it to "Antwort einer anderen Karte" } else emptyList()
                val distractors = c.choices.orEmpty().map { it to "Ablenker" } + peers
                for ((d, art) in distractors) {
                    if (alts.any { TypedAnswer.onlyAccents(d, it) }) continue // „le velo“: beim Tippen bewusst nachsichtig, in der Auswahl eindeutig
                    if (TypedAnswer.check(d, c.a, strict, c.q) == TypedAnswer.Verdict.RIGHT) err("$art „$d“ wird als richtig gewertet")
                }

                // 5. Synonym-Liste
                if (k !in EINDEUTIG) for (group in SYNONYME) {
                    if (alts.none { a -> group.any { norm(it) == norm(a) } }) continue
                    for (other in group) if (TypedAnswer.check(other, c.a, strict, c.q) != TypedAnswer.Verdict.RIGHT)
                        err("gleichwertig „$other“ wird nicht akzeptiert (Antwort „${c.a}“)")
                }
            }
        }
        println("AntwortKontrolleTest: $cards Karten geprüft (davon $plusCards Plus) in ${all.size} Paketen")
        assertTrue(errors.size.toString() + " Fälle:\n" + errors.joinToString("\n"), errors.isEmpty())
    }
}
