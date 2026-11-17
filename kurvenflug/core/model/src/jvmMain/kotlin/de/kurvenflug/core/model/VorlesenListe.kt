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

    fun json(texts: List<String>): String {
        fun esc(s: String) = buildString { s.forEach { c -> when (c) { '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c) } } }
        return texts.joinToString(",\n", "{\"format\": 1, \"app\": \"kurvenflug\", \"texte\": [\n", "\n]}\n") { "  {\"text\": \"${esc(it)}\", \"sprache\": \"de\"}" }
    }
}

fun main(args: Array<String>) {
    val out = File(args[0])
    val texts = VorlesenListe.texts(args.drop(1).map(::File))
    texts.forEach { t ->
        check(Normalizer.isNormalized(t, Normalizer.Form.NFC)) { "Text nicht NFC: $t" }
        check(t.length <= VorlesenListe.MAX) { "Text länger als ${VorlesenListe.MAX} Zeichen (${t.length}): ${t.take(80)}…" }
    }
    out.parentFile.mkdirs(); out.writeText(VorlesenListe.json(texts))
    println("vorlesen/texte.json: ${texts.size} Texte, ${texts.sumOf { it.length }} Zeichen")
}
