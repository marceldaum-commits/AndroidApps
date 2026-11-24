package de.raumlicht.core.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Die festen Vorlese-Texte der App – alles, was Lux wörtlich gleich vorliest (ohne Zufallszahlen und ohne Eingaben des Nutzers).
 * Daraus entsteht beim Bauen `webapp/vorlesen/texte.json`; das Tonstudio vertont jeden Text einmal vorab.
 * Texte, die erst in der Aufgabe entstehen (Rückmeldungen auf Eingaben, endloses Üben, Tagesaufgabe), stehen hier nicht.
 */
object Vorlesen {
    const val FORMAT = 1
    const val APP = "raumlicht"
    const val LANGUAGE = "de"
    /** Obergrenze je Text, die der Stimmen-Server annimmt */
    const val MAX_CHARS = 1500

    /** Feste Sätze der Oberfläche, die Lux spricht (Einstellungen, Baumeister) */
    val phrases = listOf("Hallo, ich bin Lux!", "Hallo, ich bin Lux. So klinge ich jetzt.", "Geschafft!", "Der Schwung reicht nicht.", "Im Ziel, aber der Auftrag fehlt noch.")

    /** Was Lux nach dem zweiten Fehlversuch anhängt */
    const val TIP_OFFER = " Tipp gefällig? 💡"
    /** Ansage einer neuen Regel in der Ballerbude */
    const val BALLOON_CALL = "Jetzt: "

    /** Die festen Texte, so wie sie in der App stehen (noch nicht vorlesefertig) */
    fun raw(): Set<String> {
        val fixed = LinkedHashSet<String>()
        fun collect(l: Level) {
            fixed += l.mission; fixed += l.tip
            runCatching { fixed += l.evaluate(l.solution).message; l.finale?.invoke(l.solution)?.acts?.forEach { fixed += it.say } }
            // häufigster Fehlversuch: „Los“ gedrückt, ohne etwas zu ändern – diese Rückmeldung steht fest, mit und ohne Tipp-Angebot
            runCatching { val out = l.evaluate(l.start); if (!out.success) { fixed += out.message; fixed += out.message + TIP_OFFER } }
        }
        Levels.chapters.forEach { c -> fixed += c.intro; c.levels.forEach { collect(it) } }
        Exams.all.forEach { e -> fixed += e.intro; e.parts.forEach { collect(it.level) } }
        fixed += phrases
        // Theorie-Karten: die Idee liest Lux vor, wenn man die Karte öffnet
        (Theory.cards + Theory.general).forEach { fixed += it.idea }
        SpotKind.entries.forEach { fixed += it.title }
        // Geisterfahrt: Urteil zu jeder Punktzahl · Ballerbude: jede Regel
        for (score in 0..100) fixed += Ghost.verdict(score)
        BalloonGame.RULES.forEach { fixed += BALLOON_CALL + it.text }
        return fixed
    }

    /**
     * Vorhersehbare Rückmeldungen der festen Aufgaben – Sätze, die zwar erst nach einer Eingabe entstehen, deren Wortlaut sich aber aufzählen lässt:
     * bei Aufgaben mit wenigen Einstellmöglichkeiten (bis [SMALL] Kombinationen) jede mögliche Rückmeldung, bei den übrigen die Rückmeldungen auf Eingaben,
     * die an genau einer Stelle um 1 neben der Lösung oder neben dem Startwert liegen. Jeweils auch mit dem Tipp-Angebot.
     * Bewusst sparsam: Das endlose Üben mit seinen Zufallszahlen gehört nicht dazu – dort erzeugt der Server den Satz beim ersten Gebrauch.
     */
    fun predictableRaw(): Set<String> {
        val out = LinkedHashSet<String>()
        fun note(l: Level, v: Values) { runCatching { val o = l.evaluate(v); if (!o.success) { out += o.message; out += o.message + TIP_OFFER } } }
        fun collect(l: Level) {
            val sizes = l.controls.flatMap { c -> c.start.indices.map { i -> c.max[i] - c.min[i] + 1 } }
            val combos = sizes.fold(1L) { a, b -> (a * b).coerceAtMost(1_000_000) }
            if (combos <= SMALL) {
                // alle Kombinationen durchzählen
                var all = listOf<Values>(emptyMap())
                for (c in l.controls) { var parts = listOf(emptyList<Int>())
                    for (i in c.start.indices) parts = parts.flatMap { p -> (c.min[i]..c.max[i]).map { p + it } }
                    all = all.flatMap { v -> parts.map { v + (c.key to it) } } }
                all.forEach { note(l, it) }
            } else for (base in listOf(l.solution, l.start)) for (c in l.controls) for (i in c.start.indices) for (d in listOf(-1, 1)) {
                val list = (base[c.key] ?: c.start).toMutableList(); val x = list[i] + d
                if (x in c.min[i]..c.max[i]) { list[i] = x; note(l, base + (c.key to list.toList())) }
            }
        }
        Levels.chapters.forEach { c -> c.levels.forEach(::collect) }
        Exams.all.forEach { e -> e.parts.forEach { collect(it.level) } }
        return out - raw()
    }
    /** bis zu so vielen Einstell-Kombinationen wird eine Aufgabe ganz durchgezählt */
    const val SMALL = 64L

    /** Vorlesefertig (Vektoren und Formeln ausgeschrieben, siehe [Speech.clean]), ohne Leerraum-Reste, jeder Text nur einmal */
    fun fixedTexts(): List<String> = raw().map { normalize(Speech.clean(it)) }.filter { it.isNotEmpty() }.distinct()
    /** Vorhersehbare Rückmeldungen, vorlesefertig – ohne die, die schon bei den festen Texten stehen */
    fun predictableTexts(): List<String> = fixedTexts().toSet().let { fixed -> predictableRaw().map { normalize(Speech.clean(it)) }.filter { it.isNotEmpty() && it !in fixed && it.length <= PREDICTABLE_MAX_CHARS }.distinct() }
    /** Alles, was vorab vertont wird (feste Texte, dann vorhersehbare Rückmeldungen) */
    /**
     * Vorlesen ist seit 2.18.14 abgeschaltet (Auftrag vom 18.11.2026: „erstmal rausnehmen, später evtl. wieder einfügen“) – der Code bleibt.
     * Solange false: kein Schalter „Lux spricht“, kein Sprechtempo, kein Offline-Laden, keine Abrufe an einen Stimmen-Server, keine Gerätestimme, und die ausgelieferte Textliste ist leer.
     * Zum Wiedereinschalten genügt true (und eine neue Version).
     */
    const val ENABLED = false
    /** Halbsatz für den Datenschutz-Hinweis in den Einstellungen: was fürs Vorlesen ins Internet geht (leer, solange es abgeschaltet ist) */
    val privacyNote get() = if (ENABLED) " – und zum Vorlesen, wenn du „Lux spricht“ einschaltest. $PRIVACY" else "."
    fun texts(): List<String> = fixedTexts() + predictableTexts()

    /** Jeder Leerraum wird zu einem Leerzeichen, vorne und hinten abgeschnitten – so rechnet auch das Tonstudio */
    fun normalize(text: String) = text.replace(Regex("\\s+"), " ").trim()

    // ---- Stimme: fertige Sprachdateien aus dem Tonstudio (eigener Server, KI-generiert)
    /** So wird die Stimme in der App genannt – die Kennzeichnung „KI-generiert“ ist Pflicht */
    const val VOICE_LABEL = "KI-generierte Stimme"
    /** Sprechtempo in Prozent – die Stufen des Reglers. 100 = so, wie die Aufnahme klingt; schneller oder langsamer spielt die App dieselbe Datei bei gleicher Tonhöhe ab. */
    val TEMPOS = listOf(75, 90, 100, 115, 130, 150)
    /** Die nächste erlaubte Stufe zu einem gemerkten Wert */
    fun tempo(percent: Int) = TEMPOS.minBy { kotlin.math.abs(it - percent) }
    /** Anzeige: 115 → „1,15×“ */
    fun tempoLabel(percent: Int) = "${percent / 100},${(percent % 100).toString().padStart(2, '0').trimEnd('0').ifEmpty { "0" }}×"
    /** Was beim Vorlesen mit Daten passiert (steht in den Einstellungen von Android und WebApp) */
    const val PRIVACY = "Vorgelesen wird mit einer KI-generierten Stimme. Die Sprachdateien sind fertig vorbereitet und liegen auf unserem eigenen Server in Deutschland; " +
        "die App lädt nur diese Dateien. Es werden keine Namen, Konten oder Geräte-Kennungen übertragen, und unser Server speichert keine Zugriffsprotokolle."
    /** Kennung der Tonstudio-Stimme im Namen einer Datei (Deutsch: „de-1“) */
    const val VOICE = "de-1"
    /** Längste vorhersehbare Rückmeldung, die noch mit vorab vertont wird */
    const val PREDICTABLE_MAX_CHARS = 300

    /** Der Text, wie er gesprochen und für den Dateinamen gerechnet wird: vorlesefertig ([Speech.clean]) und ohne Leerraum-Reste. Leer = nichts zu sagen. */
    fun spoken(text: String) = normalize(Speech.clean(text))
    /** Feste Kennung eines vorlesefertigen Textes (die ersten 32 Hex-Zeichen von SHA-256("de-1|de|" + Text)) – die Textliste für das Tonstudio führt sie als ID */
    fun key(spoken: String, language: String = LANGUAGE) = Sha256.hex("$VOICE|$language|$spoken").take(32)

    /** Inhalt von `vorlesen/texte.json` */
    fun json(enabled: Boolean = ENABLED): String = buildJsonObject {
        put("format", FORMAT); put("app", APP)
        put("texte", JsonArray(if (!enabled) emptyList() else fixedTexts().map { t -> buildJsonObject { put("text", JsonPrimitive(t)); put("sprache", LANGUAGE) } } +
            // Rückmeldungen, die erst nach einer Eingabe fallen, aber vorhersehbar sind: der Server spricht sie vorab (der Schlüssel „laufzeit“ ist nur ein Vermerk)
            predictableTexts().map { t -> buildJsonObject { put("text", JsonPrimitive(t)); put("sprache", LANGUAGE); put("laufzeit", true) } }))
    }.toString()
}
