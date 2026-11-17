package de.wissensgarten.content

import de.wissensgarten.core.model.Nova
import de.wissensgarten.core.model.PackJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Liste aller FESTEN Vorlese-Texte für Brummis Stimme „Nova“. Unser Stimm-Server vertont sie vorab
 * (Dateiname = [Nova.hash] über den gesprochenen Text). Erzeugt automatisch aus:
 * - den freien Wissenspaketen (Frage, Antwort, info, Intro-Szenen, Theorie, Silben-Klatschen – keine Auswahl-Antworten),
 * - festen Sätzen der Lern-Logik ([Nova.festeTexte]: Geschichte, Feste, Brummi-Radio, Safari),
 * - Brummis festen Sätzen in der Oberfläche (ohne Platzhalter), gesucht im Quelltext ([BrummiSaetze]).
 *
 * Ausgeliefert als `webApp/src/wasmJsMain/resources/vorlesen/texte.json` (→ `webapp/vorlesen/texte.json`).
 * Passt die Datei nicht mehr zum Inhalt, schreibt der Test sie neu und schlägt fehl – dann committen
 * und `./gradlew :webApp:exportWebApp` laufen lassen.
 *
 * Plus-Texte stehen NIE in dieser öffentlichen Liste. Liegt der private Plus-Ordner vor (`WG_PLUS_DIR` oder
 * `website_backup/wissensgarten-plus` neben dem Repo), entsteht dort zusätzlich `vorlesen-texte.json` mit den Plus-Texten.
 */
class VorlesenListeTest {
    private val packsDir = File("src/main/assets/packs")
    private val out = File("../webApp/src/wasmJsMain/resources/vorlesen/texte.json")
    private val exported = File("../webapp/vorlesen/texte.json")

    private val plusDir: File? = (System.getenv("WG_PLUS_DIR")?.let(::File)
        ?: generateSequence(File("").absoluteFile) { it.parentFile }.map { File(it, "website_backup/wissensgarten-plus") }.firstOrNull { it.isDirectory })
        ?.takeIf { it.isDirectory }

    private fun packs(dir: File) = dir.listFiles { f -> f.extension == "json" && f.name != "versions.json" && f.name != "vorlesen-texte.json" }!!
        .sortedBy { it.name }.map { PackJson.parse(it.readText()) }

    /** Vorab vertonte Laufzeit-Sätze (Brummi-Radio) – in der Liste mit `"laufzeit": true` markiert (nicht für „offline laden“) */
    private fun laufzeit(packs: List<de.wissensgarten.core.model.Pack>): Set<Nova.Eintrag> =
        (packs.flatMap { Nova.packLaufzeit(it) } - packs.flatMap { Nova.packTexte(it) }.toSet() - Nova.festeTexte().toSet()).toSet()

    private fun freeTexts(): List<Nova.Eintrag> {
        val free = packs(packsDir).filter { !it.isPlus }
        val list = free.flatMap { Nova.packTexte(it) } + Nova.festeTexte() +
            BrummiSaetze.find(listOf(File("../shared/src/commonMain/kotlin"), File("../app/src/main/java/de/wissensgarten/app/ui"))).mapNotNull { raw ->
                Nova.normalisiert(Nova.sprechText(raw, "de")).takeIf { t -> t.any { it.isLetterOrDigit() } }?.let { Nova.Eintrag(it, "de") }
            } +
            // häufige Laufzeit-Sätze aus festen Paketdaten (Brummi-Radio) – hinten, damit die Reihenfolge der übrigen bleibt
            free.flatMap { Nova.packLaufzeit(it) }
        return list.distinct()
    }

    private fun json(texts: List<Nova.Eintrag>, laufzeit: Set<Nova.Eintrag>): String = buildString {
        append("{\"format\": 1, \"app\": \"wissensgarten\", \"texte\": [\n")
        texts.forEachIndexed { i, e ->
            append("  {\"text\": ").append(quote(e.text)).append(", \"sprache\": ").append(quote(e.sprache))
            append(if (e in laufzeit) ", \"laufzeit\": true}" else "}")
            append(if (i < texts.lastIndex) ",\n" else "\n")
        }
        append("]}\n")
    }

    private fun quote(s: String) = buildString {
        append('"')
        for (c in s) when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c < ' ' -> append("\\u%04x".format(c.code))
            else -> append(c)
        }
        append('"')
    }

    private fun stats(name: String, texts: List<Nova.Eintrag>) = name + ": " + texts.groupBy { it.sprache }.toSortedMap()
        .map { (l, t) -> "$l ${t.size} Texte / ${t.sumOf { it.text.length }} Zeichen" }.joinToString(", ")

    private fun check(texts: List<Nova.Eintrag>) {
        assertTrue("zu viele Texte: ${texts.size}", texts.size <= 40_000)
        texts.forEach { e ->
            assertTrue("Sprache ${e.sprache}", e.sprache in Nova.SPRACHEN)
            assertTrue("zu lang (${e.text.length}): ${e.text.take(80)}", e.text.length <= Nova.MAX_LISTE)
            assertEquals(Nova.normalisiert(e.text), e.text)
        }
        assertEquals("doppelt", texts.size, texts.distinct().size)
    }

    @Test fun listeIstAktuell() {
        val free = freeTexts()
        check(free)
        println(stats("Frei", free))
        val text = json(free, laufzeit(packs(packsDir).filter { !it.isPlus }))

        plusDir?.let { dir ->
            val known = free.toHashSet()
            val plus = packs(dir).flatMap { Nova.packTexte(it) + Nova.packLaufzeit(it) }.distinct().filter { it !in known }
            check(plus)
            println(stats("Plus", plus) + " → ${File(dir, "vorlesen-texte.json")}")
            File(dir, "vorlesen-texte.json").writeText(json(plus, laufzeit(packs(dir))))
        }

        if (!out.exists() || out.readText() != text) {
            out.parentFile.mkdirs(); out.writeText(text)
            fail("vorlesen/texte.json war nicht aktuell und wurde neu geschrieben – bitte committen und ./gradlew :webApp:exportWebApp laufen lassen.")
        }
        if (exported.exists()) assertEquals("webapp/vorlesen/texte.json ist veraltet – ./gradlew :webApp:exportWebApp laufen lassen.", text, exported.readText())
    }

    @Test fun keinePlusTexteInDerOeffentlichenListe() {
        val dir = plusDir ?: return
        val pub = out.takeIf { it.exists() }?.readText() ?: return
        val free = freeTexts().toHashSet()
        // Jede Plus-Frage, die nicht zufällig auch frei vorkommt, darf nicht in der öffentlichen Liste stehen
        packs(dir).flatMap { p -> p.cards.map { Nova.Eintrag(Nova.normalisiert(Nova.sprechText(it.q, Nova.sprache(p.subject, it.q))), Nova.sprache(p.subject, it.q)) } }
            .filter { it !in free && it.text.length > 25 }
            .forEach { assertTrue("Plus-Text öffentlich: ${it.text}", !pub.contains(quote(it.text))) }
    }

    @Test fun brummisSaetzeWerdenGefunden() {
        val s = BrummiSaetze.find(listOf(File("../shared/src/commonMain/kotlin"), File("../app/src/main/java/de/wissensgarten/app/ui")))
        listOf("Super! 🌟", "Zzz … bis morgen!", "Fang den richtigen Ballon!", "Weiter geht's!", "Oh! Was ist denn richtig?",
            "Hallo, ich bin Brummi! So klinge ich, wenn ich dir vorlese.", "Willkommen in meinem Zuhause! Richte die Räume ein, wie du magst. 🐝")
            .forEach { assertTrue("fehlt: $it", it in s) }
        // Sätze mit Platzhaltern (Namen, Zahlen, Antworten) sind keine festen Texte
        assertTrue(s.none { '$' in it })
    }
}

/**
 * Sucht Brummis feste Sätze im Quelltext der Oberfläche (`shared` und `app/…/ui`, nicht die Plattform-Brücken): Zeichenketten ohne Platzhalter, die bei `speech =`, `line =`, `error =` (Sprechblase im Klassen-Code),
 * `val say =`, `say(…)`, `speak(…)`, `speakAndWait(…)`, `finish(…)` (Brummi-lernt-Spiel), `PROBE =` oder in `object BrummiSays` stehen.
 */
object BrummiSaetze {
    private data class Lit(val start: Int, val end: Int, val value: String, val template: Boolean)

    private val TRIGGER = Regex("""(?<![\w.])(?:speech|line|error)\s*=(?!=)|\(mood, line\)\s*=|\bval say\s*=|\bsay\(|\.speak\(|\.speakAndWait\(|\bfinish\(|\bPROBE\s*=""")

    fun find(roots: List<File>): Set<String> = roots.flatMap { r -> r.walkTopDown().filter { it.extension == "kt" }.toList() }
        .sortedBy { it.path }.flatMap { scan(it.readText()) }.toCollection(LinkedHashSet())

    fun scan(src: String): List<String> {
        val lits = ArrayList<Lit>()
        val code = mask(src, lits)
        val found = ArrayList<String>()
        fun take(from: Int, to: Int) = lits.filter { it.start >= from && it.end <= to && !it.template }.forEach { found += it.value }
        for (m in TRIGGER.findAll(code)) take(m.range.last + 1, exprEnd(code, m.range.last + 1, m.value.endsWith("(")))
        Regex("""object BrummiSays\s*\{""").find(code)?.let { m -> take(m.range.last, blockEnd(code, m.range.last)) }
        return found.filter { it.isNotBlank() }
    }

    /** Ende des Ausdrucks: bei „f(“ die schließende Klammer, sonst Zeilenende auf Ebene 0 (außer die nächste Zeile setzt fort) */
    private fun exprEnd(code: String, start: Int, call: Boolean): Int {
        var depth = if (call) 1 else 0; var i = start
        while (i < code.length) {
            val c = code[i]
            when (c) {
                '(', '{', '[' -> depth++
                ')', '}', ']' -> { depth--; if (depth < 0 || (call && depth == 0)) return i }
                ';' -> if (depth == 0) return i
                '\n' -> if (depth == 0) {
                    val before = code.substring(start, i).trimEnd()
                    val next = code.substring(i + 1).trimStart()
                    val cont = before.isEmpty() || before.endsWith("=") || before.endsWith("->") || before.endsWith("?:") || before.endsWith("else") ||
                        before.endsWith(",") || before.endsWith("+") || next.startsWith("else") || next.startsWith("?:") || next.startsWith(".") || next.startsWith("+")
                    if (!cont) return i
                }
            }
            i++
        }
        return code.length
    }

    private fun blockEnd(code: String, open: Int): Int {
        var depth = 0
        for (i in open until code.length) { when (code[i]) { '{' -> depth++; '}' -> { depth--; if (depth == 0) return i } } }
        return code.length
    }

    /** Kommentare und Zeichenketten-Inhalte durch Leerzeichen ersetzen; Zeichenketten merken (mit Platzhalter-Kennung) */
    private fun mask(src: String, lits: MutableList<Lit>): String {
        val sb = StringBuilder(src); var i = 0
        fun blank(a: Int, b: Int) { for (k in a until b) if (sb[k] != '\n') sb[k] = ' ' }
        while (i < src.length) {
            when {
                src.startsWith("//", i) -> { val e = src.indexOf('\n', i).let { if (it < 0) src.length else it }; blank(i, e); i = e }
                src.startsWith("/*", i) -> { val e = src.indexOf("*/", i + 2).let { if (it < 0) src.length else it + 2 }; blank(i, e); i = e }
                src.startsWith("\"\"\"", i) -> { val e = src.indexOf("\"\"\"", i + 3).let { if (it < 0) src.length else it + 3 }
                    lits += Lit(i, e, src.substring(i + 3, e - 3), true); blank(i + 1, e - 1); i = e }
                src[i] == '\'' -> { val e = if (src.startsWith("'\\", i)) src.indexOf('\'', i + 3) + 1 else i + 3; blank(i + 1, e - 1); i = e }
                src[i] == '"' -> {
                    val v = StringBuilder(); var template = false; var j = i + 1
                    while (j < src.length && src[j] != '"') {
                        val c = src[j]
                        if (c == '\\' && j + 1 < src.length) {
                            when (val n = src[j + 1]) { 'n' -> v.append('\n'); 't' -> v.append('\t'); 'u' -> { v.append(src.substring(j + 2, j + 6).toInt(16).toChar()); j += 4 }; else -> v.append(n) }
                            j += 2; continue
                        }
                        if (c == '$' && j + 1 < src.length && (src[j + 1] == '{' || src[j + 1].isLetter())) {
                            template = true
                            if (src[j + 1] == '{') { var d = 0; var k = j + 1
                                while (k < src.length) { if (src[k] == '{') d++ else if (src[k] == '}') { d--; if (d == 0) break }; k++ }
                                j = k + 1; continue }
                        }
                        v.append(c); j++
                    }
                    lits += Lit(i, j + 1, v.toString(), template); blank(i + 1, j); i = j + 1
                }
                else -> i++
            }
        }
        return sb.toString()
    }
}
