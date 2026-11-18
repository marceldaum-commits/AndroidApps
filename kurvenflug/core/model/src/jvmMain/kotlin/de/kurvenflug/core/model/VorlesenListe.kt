package de.kurvenflug.core.model

import java.io.File
import java.text.Normalizer

/**
 * Erzeugt beim Bauen die Liste aller festen Vorlese-Texte für das Stimmen-Werk (KI-Stimme Nova): vorlesen/texte.json.
 * Format (Vorgabe Leitstand): {"format": 1, "app": "kurvenflug", "texte": [{"text": "…", "sprache": "de"}, …]}.
 * Texte stehen so drin, wie sie gesprochen werden ([Speech.clean]). Dazu kommen Volts feste Sätze aus der Oberfläche:
 * Zeichenketten ohne Platzhalter, die in einem `speech = …`-Ausdruck stehen.
 *
 * Aufruf: VorlesenListeKt <ausgabe.json> <Quellordner der Oberfläche>…
 */
object VorlesenListe {
    const val MAX = 1500
    private val literal = Regex(""""((?:[^"\\\n]|\\.)*)"""")

    /** Feste Sätze aus `speech = "…"` bzw. `speech = when { … -> "…" }` (ohne $-Platzhalter) */
    fun uiLines(source: String): List<String> {
        val out = ArrayList<String>(); val lines = source.lines(); var i = 0
        while (i < lines.size) {
            if (Regex("""\bspeech\s*=""").containsMatchIn(lines[i])) {
                var depth = 0; var j = i
                do {
                    val l = lines[j]; depth += l.count { it == '{' } - l.count { it == '}' }
                    literal.findAll(l.substringAfter("speech").let { if (j == i) it else l }).forEach { m ->
                        val v = m.groupValues[1]; if ('$' !in v && ' ' in v) out += v.replace("\\n", " ").replace("\\\"", "\"")
                    }
                    j++
                } while (depth > 0 && j < lines.size)
                i = j
            } else i++
        }
        return out
    }

    fun texts(uiSources: List<File>): List<String> {
        val raw = ReadTexts.fixed() + uiSources.flatMap { d -> d.walkTopDown().filter { it.extension == "kt" }.sortedBy { it.path }.flatMap { uiLines(it.readText()) } }
        return raw.map { Nova.normalize(Speech.clean(it)) }.filter { it.isNotBlank() && it.any(Char::isLetter) }.distinct()
    }

    /** Vorhersehbare Laufzeit-Sätze (gesprochene Form), ohne die schon festen */
    fun runtime(fixed: List<String>, today: Long): List<String> {
        val have = fixed.toHashSet()
        return VoltLines.runtime(today).map { Nova.normalize(Speech.clean(it)) }.filter { it.isNotBlank() && it.any(Char::isLetter) && it !in have }.distinct()
    }

    /** Liste, solange das Vorlesen abgeschaltet ist ([Nova.ENABLED]) */
    const val EMPTY = "{\"format\":1,\"app\":\"kurvenflug\",\"texte\":[]}\n"

    fun json(texts: List<String>, runtime: List<String> = emptyList()): String {
        fun esc(s: String) = buildString { s.forEach { c -> when (c) { '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c) } } }
        val all = texts.map { "  {\"text\": \"${esc(it)}\", \"sprache\": \"de\"}" } + runtime.map { "  {\"text\": \"${esc(it)}\", \"sprache\": \"de\", \"laufzeit\": true}" }
        return all.joinToString(",\n", "{\"format\": 1, \"app\": \"kurvenflug\", \"texte\": [\n", "\n]}\n")
    }
}

fun main(args: Array<String>) {
    val out = File(args[0])
    if (!Nova.ENABLED) {   // Vorlesen abgeschaltet: leere Liste, damit das Stimmen-Werk nichts vertont
        out.parentFile.mkdirs(); out.writeText(VorlesenListe.EMPTY); println("vorlesen/texte.json: Vorlesen abgeschaltet – leere Liste"); return
    }
    val texts = VorlesenListe.texts(args.drop(1).map(::File))
    texts.forEach { t ->
        check(Normalizer.isNormalized(t, Normalizer.Form.NFC)) { "Text nicht NFC: $t" }
        check(t.length <= VorlesenListe.MAX) { "Text länger als ${VorlesenListe.MAX} Zeichen (${t.length}): ${t.take(80)}…" }
    }
    val today = CalDate.today().toEpochDay()
    val runtime = VorlesenListe.runtime(texts, today).onEach { t ->
        check(Normalizer.isNormalized(t, Normalizer.Form.NFC)) { "Text nicht NFC: $t" }
        check(t.length <= VorlesenListe.MAX) { "Text länger als ${VorlesenListe.MAX} Zeichen: ${t.take(80)}…" } }
    out.parentFile.mkdirs(); out.writeText(VorlesenListe.json(texts, runtime))
    println("vorlesen/texte.json: ${texts.size} feste Texte (${texts.sumOf { it.length }} Zeichen), ${runtime.size} Laufzeit-Sätze (${runtime.sumOf { it.length }} Zeichen)")
}
