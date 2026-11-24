package de.experimentopia.core.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Familien-Speicher (Marcel 19.11.2026: „Sofort bauen“; Bauplan FAMILIEN-SPEICHER v1.7, Recht freigegeben mit Auflagen A6 und V8).
 * Gesichert wird der Lernstand dieses Kindes verschlüsselt auf unserem Server, der Schlüssel liegt nur auf den Geräten der Familie.
 * Verschlüsselt, gepackt und übertragen wird mit der gemeinsamen Bibliothek familie.js (unverändert, siehe web/…/familie.js).
 * Hier steht nur, was unabhängig davon gilt: welche Teile des Lernstands, wie zwei Stände sicher zusammengeführt werden und die Texte.
 * Rechte (Forscher-Pass) und Einstellungen eines Geräts gehören NICHT in den Kind-Stand.
 */
object Familie {
    const val APP = "experimentopia"
    /** Bestandteile des Kind-Stands (Namen im Browser-Speicher ohne Vorsilbe „experimentopia.“) */
    val TEILE = listOf("ergebnisse", "forscherbuch", "abzeichen", "laborlaeufe", "verlauf")

    /** Pflichttext (wörtlich von Marcel vorgegeben; Recht: nur EIN Einwilligungstext, nichts vorausgewählt) */
    const val PFLICHTTEXT = "Eure Lernstände werden verschlüsselt auf unserem Server in Deutschland gespeichert. Der Schlüssel liegt auf euren Geräten. " +
        "Damit wir euch helfen können, falls ihr den Familien-Zugang verliert, hinterlegen wir zusätzlich eine verschlüsselte Notfall-Kopie. " +
        "Sie lässt sich nur mit einem getrennt und offline aufbewahrten Schlüssel von Mathenachhilfe Online öffnen. " +
        "Technisch könnten wir damit eure Lernstände lesen; wir tun das nicht und nutzen ihn nur, wenn ihr uns um Wiederherstellung bittet. " +
        "Jede Wiederherstellung seht ihr hier im Protokoll."
    const val DATENSCHUTZ_LINK_TEXT = "Mehr dazu in der Datenschutzerklärung (Abschnitt 10m)"
    const val DATENSCHUTZ_URL = "https://www.mathenachhilfe-online.com/datenschutz/#10m"
    const val KNOPF_ANLEGEN = "Einverstanden – Familie anlegen"
    const val KNOPF_BEITRETEN = "Einverstanden – Familie beitreten"
    /** Dauerhaft auf der Familien-Seite und direkt bei Link teilen, QR-Code und Notfall-Blatt */
    const val SICHERHEIT = "Wer den Familien-Link oder den QR-Code hat, kann die Lernstände eurer Familie öffnen, ändern und löschen. " +
        "Teilt ihn nur mit Personen, denen ihr vertraut, und bewahrt das Notfall-Blatt sicher auf."
    const val LOESCHEN_WARNUNG = "Das löscht die Familie in ALLEN Lern-Apps: alle Lernstände aller Kinder auf unserem Server, die Notfall-Kopie und das Protokoll. " +
        "Gekaufte Codes werden wieder frei und lassen sich in einer neuen Familie erneut einlösen. Die Lernstände auf diesem Gerät bleiben erhalten."
    const val FAMILIE_WEG = "Diese Familie gibt es nicht mehr. Lernstand bleibt auf diesem Gerät."

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Zwei Stände zusammenführen – immer sicher und ohne Verlust, nie still überschreiben:
     * Ergebnisse je Versuch (meiste Sterne, früheste erste Vermutung, spätestes Spielen), Forscherbuch (Vereinigung, neueste zuerst, höchstens 500),
     * Abzeichen (Vereinigung), Laborläufe (Maximum), Monatssummen (je Monat Maximum).
     * Unlesbare Teile bleiben so, wie sie lokal sind.
     */
    fun merge(lokal: Map<String, String>, fern: Map<String, String>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        for (k in TEILE) {
            val a = lokal[k]; val b = fern[k]
            val m = when {
                a == null && b == null -> null
                a == null -> b
                b == null -> a
                a == b -> a
                else -> runCatching { mergeTeil(k, a, b) }.getOrDefault(a)
            }
            if (m != null) out[k] = m
        }
        return out
    }

    private fun mergeTeil(k: String, a: String, b: String): String = when (k) {
        "ergebnisse" -> {
            val s = ListSerializer(ResultRow.serializer())
            val l = (json.decodeFromString(s, a) + json.decodeFromString(s, b)).groupBy { it.experimentId }.map { (id, rs) ->
                val first = rs.filter { it.firstAt > 0 }.minByOrNull { it.firstAt } ?: rs.first()
                ResultRow(id, rs.maxOf { it.stars }, first.firstGuessRight, rs.maxOf { it.plays }, rs.map { it.firstAt }.filter { it > 0 }.minOrNull() ?: 0, rs.maxOf { it.lastAt })
            }.sortedBy { it.experimentId }
            json.encodeToString(s, l)
        }
        "forscherbuch" -> {
            val s = ListSerializer(NoteRow.serializer())
            val all = (json.decodeFromString(s, a) + json.decodeFromString(s, b)).distinctBy { it.experimentId to it.at }.sortedByDescending { it.at }.take(500)
            // laufende Nummern neu vergeben (neueste = größte), damit die Oberfläche eindeutige Schlüssel hat
            json.encodeToString(s, all.mapIndexed { i, n -> n.copy(id = (all.size - i).toLong()) })
        }
        "abzeichen" -> (a.split(',') + b.split(',')).filter { it.isNotBlank() }.distinct().sorted().joinToString(",")
        "laborlaeufe" -> maxOf(a.toIntOrNull() ?: 0, b.toIntOrNull() ?: 0).toString()
        "verlauf" -> {
            val x = Monthly.decode(a); val y = Monthly.decode(b)
            Monthly.encode((x.keys + y.keys).sorted().associateWith { m ->
                val p = x[m] ?: MonthSum(); val q = y[m] ?: MonthSum()
                if (q.aufgaben > p.aufgaben) q else if (p.aufgaben > q.aufgaben) p else MonthSum(maxOf(p.min, q.min), p.aufgaben, maxOf(p.richtig, q.richtig))
            })
        }
        else -> a
    }

    /** Hat [fern] etwas, das [lokal] noch nicht hat? (dann muss lokal übernommen werden) */
    fun bringtNeues(lokal: Map<String, String>, fern: Map<String, String>) = merge(lokal, fern) != lokal.filterKeys { it in TEILE }

    /** Familien-Nummer eintippen zum Löschen: Eingabe vom Telefon normalisiert (ohne Bindestrich/Leerzeichen, groß, O→0, I/L→1) */
    fun nummerGleich(eingabe: String, nummer: String) = norm(eingabe) == norm(nummer) && norm(nummer).isNotEmpty()
    private fun norm(t: String) = t.uppercase().filterNot { it == '-' || it.isWhitespace() }.replace('O', '0').replace('I', '1').replace('L', '1')
}
