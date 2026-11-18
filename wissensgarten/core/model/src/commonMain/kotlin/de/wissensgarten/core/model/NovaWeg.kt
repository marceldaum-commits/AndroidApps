package de.wissensgarten.core.model

import kotlin.concurrent.Volatile
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** Sprechtempo der Stimme: feste Stufen, 1,0 = Tempo des Servers. Gespeichert wird die Stufe in Prozent (75 … 150). */
object Sprechtempo {
    val STUFEN = listOf(75, 90, 100, 115, 130, 150)
    const val NORMAL = 100

    /** Nächste Stufe zu einem (auch alten, frei gewählten) Prozentwert – z. B. alte 120 % → 115 % */
    fun stufe(percent: Int): Int = STUFEN.minBy { kotlin.math.abs(it - percent) }

    fun faktor(percent: Int): Float = stufe(percent) / 100f

    /** Anzeige: „1,15×“ */
    fun label(percent: Int): String {
        val p = stufe(percent); val frac = p % 100
        return "${p / 100}," + (if (frac == 0) "0" else frac.toString().padStart(2, '0').trimEnd('0')) + "×"
    }
}

/**
 * Ein Text, der erst in der App entsteht (z. B. „Hör noch mal: …“ im Brummi-Radio). Es gibt ihn ausschließlich über [Laufzeit] –
 * dort entsteht er aus Zahlen und Karten, nie aus freiem Text. [teile] sind die eingesetzten Karten-Inhalte: Nur wenn jeder Teil
 * nachweislich aus einem freien Wissenspaket stammt ([NovaKatalog]), darf der Satz an `/jetzt`. So können keine Namen, eigenen
 * Karten oder Angaben zum Kind an den Server geraten – auch nicht, wenn jemand eine eigene Karte in ein Paket-Beet schreibt.
 */
class LaufzeitText internal constructor(val text: String, val teile: List<String> = emptyList()) {
    override fun toString() = text
}

/**
 * Alle Laufzeit-Sätze an einer Stelle, immer gleich formuliert (gleicher Satz → nur einmal vertont), immer ganze Sätze.
 * Karten-Inhalte nur aus Wissenspaketen ([ausPaket] = true): Eigene Karten (selbst geschrieben, Klassen-Codes, Safari)
 * gehen nie an den Server – dann `null`, und die Stimme des Geräts liest. Zusätzlich prüft [NovaKatalog] jeden Teil.
 */
object Laufzeit {
    fun radioFrage(q: Radio.Question, ausPaket: Boolean): LaufzeitText? = if (!ausPaket || q.mode == Radio.Mode.FREE) null else
        LaufzeitText(Radio.prompt(q), when (q.mode) {
            Radio.Mode.LEARN -> listOf(q.card.front, q.card.back)
            Radio.Mode.CHOICE -> listOf(q.card.front) + q.options
            else -> listOf(q.card.front)
        })
    fun radioAufloesung(q: Radio.Question, ausPaket: Boolean): LaufzeitText? = if (ausPaket) LaufzeitText(Radio.selfReveal(q), listOf(q.card.back)) else null
    fun radioKorrektur(q: Radio.Question, ausPaket: Boolean): LaufzeitText? = if (ausPaket) LaufzeitText(Radio.correction(q), listOf(q.card.back)) else null
    fun radioSpaeter(q: Radio.Question, ausPaket: Boolean): LaufzeitText? = if (ausPaket) LaufzeitText(Radio.later(q), listOf(q.card.back)) else null
    fun radioNochmal(card: StudyCard, ausPaket: Boolean): LaufzeitText? = if (ausPaket) LaufzeitText(Radio.again(card), listOf(card.back)) else null
    fun radioSchluss(correct: Int, total: Int): LaufzeitText = LaufzeitText(Radio.summary(correct, total))
    fun palast(right: Int, total: Int): LaufzeitText = LaufzeitText(palastText(right, total))
    fun bonus(dew: Int, silben: Boolean): LaufzeitText = LaufzeitText(bonusText(dew, silben))
    fun kapitelOffen(number: Int): LaufzeitText = LaufzeitText(kapitelOffenText(number))
    fun kapitelWartet(plants: Int): LaufzeitText = LaufzeitText(kapitelWartetText(plants))

    // „Bring Brummi was bei“: Brummi rät ([vermutung] = die Karte mit Brummis Antwort als Rückseite) und lernt dazu
    fun lehrerRaet(card: StudyCard, vermutung: StudyCard, ausPaket: Boolean): LaufzeitText? =
        if (ausPaket) LaufzeitText(lehrerRaetText(card.front, vermutung.back), listOf(card.front, vermutung.back)) else null
    fun lehrerHatteRecht(card: StudyCard, ausPaket: Boolean): LaufzeitText? =
        if (ausPaket) LaufzeitText(lehrerHatteRechtText(card.back), listOf(card.back)) else null
    fun lehrerEigentlich(card: StudyCard, ausPaket: Boolean): LaufzeitText? =
        if (ausPaket) LaufzeitText(lehrerEigentlichText(card.back), listOf(card.back)) else null
    fun lehrerDanke(card: StudyCard, ausPaket: Boolean): LaufzeitText? =
        if (ausPaket) LaufzeitText(lehrerDankeText(card.back), listOf(card.back)) else null
    fun lehrerNichtRichtig(card: StudyCard, ausPaket: Boolean): LaufzeitText? =
        if (ausPaket) LaufzeitText(lehrerNichtRichtigText(card.back), listOf(card.back)) else null

    fun palastText(right: Int, total: Int) = "$right von $total Orten erinnert – dein Gedächtnis-Palast wächst! 🏰"
    fun bonusText(dew: Int, silben: Boolean) = "+$dew 💧 " + if (silben) "Silben-Profi!" else "Bonus!"
    fun kapitelOffenText(number: Int) = "Weiter geht's! Kapitel $number wartet schon auf uns!"
    fun kapitelWartetText(plants: Int) =
        "Damit es weitergeht, braucht der Garten Kraft. Lass noch $plants Pflanzen wachsen – dann geht unser Abenteuer weiter!"
    fun lehrerRaetText(front: String, guess: String) = "Ich glaube: „$front“ → $guess!"
    fun lehrerHatteRechtText(answer: String) = "Doch, ich hatte recht! Es ist wirklich $answer. 😅"
    fun lehrerEigentlichText(answer: String) = "Hm, eigentlich ist es $answer. Jetzt wissen wir's beide!"
    fun lehrerDankeText(answer: String) = "Danke! Jetzt weiß ich's: $answer! Du bist ein toller Lehrer! 🌟"
    fun lehrerNichtRichtigText(answer: String) = "Hmm, das ist es auch nicht – richtig ist $answer."

    /** Höchstzahl Karten einer Radio-Runde (für die Vorab-Liste der Schluss-Sätze) */
    const val RADIO_MAX = 12
    /** Mögliche Tautropfen einer Bonusrunde (Silben: 3–8, Zuordnen: 2–10) */
    val BONUS_TAU = 2..10
}

/**
 * Wie mit einem Vorlese-Text umzugehen ist, wenn er nicht schon auf dem Gerät liegt.
 * Entscheidet [NovaKatalog] – nach dem Inhalt, nicht nach der Aufrufstelle.
 */
enum class NovaArt {
    /** Öffentlich bekannt (Vorlese-Liste, freie Pakete): vorab vertonte Datei laden, fehlt sie → `/jetzt` */
    OEFFENTLICH,
    /** Laufzeit-Satz, dessen Teile alle aus freien Paketen stammen: Datei laden, fehlt sie → `/jetzt` */
    LAUFZEIT,
    /** Aus einem Plus-Paket: Datei laden, fehlt sie → kurz warten, noch einmal schauen, dann Gerätestimme – nie `/jetzt` */
    PLUS,
    /** Unbekannt (eigene Karten, Klassen-Codes, Namen …): gar nicht ans Netz – gleich die Gerätestimme */
    PRIVAT,
    /** Liste und Pakete noch nicht da: Datei laden, aber nie `/jetzt` */
    UNBEKANNT,
}

/**
 * Was die App über ihre Vorlese-Texte weiß: die öffentliche Vorlese-Liste (`vorlesen/texte.json`, genau das, was der Server
 * vorab vertont) und die Pakete auf dem Gerät. Daraus folgt für jeden Text die [NovaArt]. Nur Öffentliches darf an `/jetzt`.
 */
class NovaKatalog {
    // Wird im Hintergrund berechnet und als Ganzes ersetzt (nie stückweise geändert)
    @Volatile private var liste: Set<String>? = null
    @Volatile private var frei: Set<String> = emptySet()
    @Volatile private var freiRoh: Set<String> = emptySet()
    @Volatile private var plus: Set<String> = emptySet()
    @Volatile private var plusRoh: Set<String> = emptySet()
    @Volatile private var paketeDa = false

    val listeDa: Boolean get() = liste != null

    private fun key(sprache: String, text: String) = "$sprache|$text"

    /** Die Vorlese-Liste (Texte schon so, wie Nova sie spricht) */
    fun liste(eintraege: Collection<Nova.Eintrag>) {
        liste = eintraege.mapTo(HashSet(eintraege.size * 2)) { key(it.sprache, it.text) }
    }

    /** Pakete auf dem Gerät: freie (falls neuer als die Liste) und Plus (nie an `/jetzt`) */
    fun pakete(packs: List<Pack>) {
        val (p, f) = packs.partition { it.isPlus }
        frei = (f.flatMap { Nova.packTexte(it) + Nova.packLaufzeit(it) } + Nova.festeTexte()).mapTo(HashSet()) { key(it.sprache, it.text) }
        freiRoh = f.flatMap { roh(it) }.toHashSet()
        plus = p.flatMap { Nova.packTexte(it) + Nova.packLaufzeit(it) }.mapTo(HashSet()) { key(it.sprache, it.text) }
        plusRoh = p.flatMap { roh(it) }.toHashSet() - freiRoh
        paketeDa = true
    }

    /** Wie [pakete], aber Paket für Paket mit Pausen – im Browser (ein Thread) bleibt die Oberfläche dabei flüssig */
    suspend fun paketeSchrittweise(packs: List<Pack>) {
        val (p, f) = packs.partition { it.isPlus }
        suspend fun texte(l: List<Pack>): HashSet<String> {
            val out = HashSet<String>()
            for (pack in l) { (Nova.packTexte(pack) + Nova.packLaufzeit(pack)).mapTo(out) { key(it.sprache, it.text) }; kotlinx.coroutines.yield() }
            return out
        }
        val fr = texte(f).also { s -> Nova.festeTexte().mapTo(s) { key(it.sprache, it.text) } }
        val frRoh = f.flatMap { roh(it) }.toHashSet()
        val pl = texte(p)
        frei = fr; freiRoh = frRoh; plus = pl; plusRoh = p.flatMap { roh(it) }.toHashSet() - frRoh
        paketeDa = true
    }

    private fun roh(p: Pack) = p.cards.flatMap { listOfNotNull(it.q.trim(), it.a.trim(), it.info?.trim()) + it.choices.orEmpty().map { c -> c.trim() } }

    private fun oeffentlich(k: String) = liste?.contains(k) == true || k in frei

    /** Ein Karten-Inhalt (roh), der in einem Laufzeit-Satz steckt: stammt er aus einem freien Paket? */
    private fun teilOeffentlich(teil: String): Boolean {
        val t = teil.trim()
        if (t in freiRoh) return true
        return Nova.SPRACHEN.any { s -> oeffentlich(key(s, Nova.normalisiert(Nova.sprechText(t, s)))) }
    }

    private fun unbekannt() = liste == null && !paketeDa

    /** Art eines festen Textes. @param text gesprochen und normalisiert ([Nova.sprechText], [Nova.normalisiert]) */
    fun art(text: String, sprache: String): NovaArt {
        val k = key(sprache, text)
        return when {
            oeffentlich(k) -> NovaArt.OEFFENTLICH
            k in plus -> NovaArt.PLUS
            unbekannt() -> NovaArt.UNBEKANNT
            else -> NovaArt.PRIVAT
        }
    }

    /** Art eines Laufzeit-Satzes ([text] gesprochen und normalisiert): nur mit lauter öffentlichen Teilen an `/jetzt` */
    fun art(l: LaufzeitText, text: String, sprache: String): NovaArt {
        val k = key(sprache, text)
        return when {
            oeffentlich(k) -> NovaArt.OEFFENTLICH
            k in plus || l.teile.any { it.trim() in plusRoh } -> NovaArt.PLUS
            l.teile.isEmpty() || (!unbekannt() && l.teile.all { teilOeffentlich(it) }) -> NovaArt.LAUFZEIT
            unbekannt() -> NovaArt.UNBEKANNT
            else -> NovaArt.PRIVAT
        }
    }
}

/** Woher die Audiodatei kommt (für [NovaWeg]) – je Plattform umgesetzt (Android: Dateien + HTTP, Web: Cache Storage + fetch). */
interface NovaQuelle {
    /** Liegt die Datei h schon im Gerätespeicher? */
    suspend fun imSpeicher(h: String): Boolean
    /** Vorab vertonte Datei laden und bei 200 speichern. Liefert den HTTP-Status oder -1 (offline, Zeitüberschreitung …). */
    suspend fun laden(url: String, h: String): Int
    /** `POST /jetzt` – bei 200 die gelieferte Datei laden und unter h speichern. Liefert den HTTP-Status oder -1. */
    suspend fun jetzt(text: String, sprache: String, h: String): Int
}

/**
 * Woher Nova einen Text nimmt: Gerätespeicher → vorab vertonte Datei (/nova/…) → bei öffentlichen Texten `/jetzt` →
 * sonst die Stimme des Geräts. Die Gerätestimme ist nur die Notlösung: wirklich kein Netz (mehrere Fehler hintereinander,
 * dann nur kurz Pause), eine endgültige Absage des Servers, Plus-Texte, die (noch) fehlen, oder private Texte.
 * Wie lange die App auf Nova wartet (≈ 3 s), entscheidet `NovaVoice`.
 */
class NovaWeg(
    private val q: NovaQuelle,
    private val now: () -> Long = { Platform.nowMs() },
    private val warte: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {
    enum class Ergebnis {
        SPEICHER, NOVA, JETZT,
        /** Gerätestimme, weil es für diesen Text (noch) keine Nova-Datei geben kann oder darf */
        GERAET,
        /** Gerätestimme, weil Nova gerade nicht erreichbar ist (Netz, Server-Störung) */
        NETZ,
    }

    /** Ergebnis und die Dateien (h) zum Abspielen – lange Texte über `/jetzt` in ganzen Sätzen nacheinander */
    data class Fund(val ergebnis: Ergebnis, val dateien: List<String> = emptyList()) {
        val nova: Boolean get() = ergebnis == Ergebnis.SPEICHER || ergebnis == Ergebnis.NOVA || ergebnis == Ergebnis.JETZT
    }

    private val fehlt = HashMap<String, Long>()
    private val abgelehnt = HashMap<String, Long>()
    private var netzFehler = 0
    private var netzPauseBis = 0L
    private var jetztPauseBis = 0L

    /** Gerade keine Verbindung (mehrere Versuche hintereinander scheiterten vor kurzem) */
    val offline: Boolean get() = now() < netzPauseBis

    private fun netzFehler() {
        if (++netzFehler >= NETZ_FEHLER) { netzPauseBis = now() + NETZ_PAUSE_MS; netzFehler = 0 }
    }

    private suspend fun lade(h: String): Int {
        val s = runCatching { q.laden(Nova.url(h), h) }.getOrDefault(-1)
        if (s < 0 || s >= 500) netzFehler() else netzFehler = 0
        return s
    }

    private suspend fun da(h: String) = runCatching { q.imSpeicher(h) }.getOrDefault(false)

    /**
     * @param text gesprochener Text ([Nova.sprechText], normalisiert)
     * @param erzeugt wird aufgerufen, sobald der Server den Satz neu vertont (`/jetzt`) – dann lohnt es sich, etwas länger zu warten
     */
    suspend fun finde(text: String, sprache: String, art: NovaArt, erzeugt: () -> Unit = {}): Fund {
        if (text.isBlank() || sprache !in Nova.SPRACHEN) return Fund(Ergebnis.GERAET)
        val h = Nova.hash(text, sprache)
        if (da(h)) return Fund(Ergebnis.SPEICHER, listOf(h))
        if (art == NovaArt.PRIVAT) return Fund(Ergebnis.GERAET)
        if (now() < netzPauseBis) return Fund(Ergebnis.NETZ)
        var frisch404 = false
        if (fehlt[h]?.let { now() - it < FEHLT_MS } != true) {
            val s = lade(h)
            when {
                s == 200 -> { fehlt.remove(h); return Fund(Ergebnis.NOVA, listOf(h)) }
                s == 404 -> { fehlt[h] = now(); frisch404 = true }
                s < 0 || s >= 500 -> return Fund(Ergebnis.NETZ)
                else -> return Fund(Ergebnis.GERAET)
            }
        }
        return when (art) {
            NovaArt.PLUS -> {
                // Plus nie an /jetzt: kurz warten (der Server vertont nach und nach) und noch einmal schauen
                if (frisch404) {
                    warte(PLUS_NOCHMAL_MS)
                    if (lade(h) == 200) { fehlt.remove(h); return Fund(Ergebnis.NOVA, listOf(h)) }
                }
                Fund(Ergebnis.GERAET)
            }
            NovaArt.OEFFENTLICH, NovaArt.LAUFZEIT -> jetzt(text, sprache, h, erzeugt)
            else -> Fund(Ergebnis.GERAET)
        }
    }

    private suspend fun jetzt(text: String, sprache: String, h: String, erzeugt: () -> Unit): Fund {
        if (abgelehnt[h]?.let { now() - it < ABGELEHNT_MS } == true) return Fund(Ergebnis.GERAET)
        if (now() < jetztPauseBis) return Fund(Ergebnis.NETZ)
        // Lange Texte (Theorie, Geschichten) in ganzen Sätzen – nie mitten im Satz geteilt
        val teile = if (text.length <= JETZT_MAX) listOf(text) else Nova.saetze(text, JETZT_MAX)
        if (teile.isEmpty()) return Fund(Ergebnis.GERAET)
        val dateien = ArrayList<String>()
        for (t in teile) {
            val ht = Nova.hash(t, sprache)
            dateien += ht
            if (da(ht)) continue
            if (t != text && lade(ht) == 200) continue
            erzeugt()
            var s = runCatching { q.jetzt(t, sprache, ht) }.getOrDefault(-1)
            // 204: Der Server vertont gerade nicht (z. B. zu viele neue Sätze in kurzer Zeit) – kurz warten, einmal noch
            if (s == 204) { warte(JETZT_NOCHMAL_MS); s = runCatching { q.jetzt(t, sprache, ht) }.getOrDefault(-1) }
            when {
                s == 200 -> netzFehler = 0
                s < 0 -> { netzFehler(); return Fund(Ergebnis.NETZ) }
                s == 429 || s == 403 || s >= 500 -> { jetztPauseBis = now() + JETZT_PAUSE_MS; return Fund(Ergebnis.NETZ) }
                // 204 auch beim zweiten Mal, 400, 413 …: diesen Satz eine Weile nicht mehr anfragen
                else -> { abgelehnt[h] = now(); return Fund(Ergebnis.GERAET) }
            }
        }
        fehlt.remove(h)
        return Fund(Ergebnis.JETZT, dateien)
    }

    companion object {
        const val JETZT_MAX = 300
        /** Fehlende Datei erst nach 10 Minuten wieder anfragen (bis dahin gleich `/jetzt` bzw. Gerätestimme) */
        const val FEHLT_MS = 10 * 60_000L
        /** So viele Netzfehler hintereinander → kurze Pause, in der gleich die Gerätestimme liest (ohne Warten) */
        const val NETZ_FEHLER = 2
        const val NETZ_PAUSE_MS = 20_000L
        /** Server überlastet oder Kontingent erschöpft: so lange keine `/jetzt`-Anfrage */
        const val JETZT_PAUSE_MS = 60_000L
        /** `/jetzt` antwortet 204 (gerade nicht): so lange warten, dann noch einmal fragen */
        const val JETZT_NOCHMAL_MS = 1_500L
        /** Nach endgültiger Absage diesen Satz so lange nicht mehr an `/jetzt` */
        const val ABGELEHNT_MS = 2 * 60_000L
        /** Plus-Text fehlt: so lange warten, dann noch einmal schauen */
        const val PLUS_NOCHMAL_MS = 1_200L
        const val BASE = Nova.BASE
    }
}

/**
 * Warten statt ausweichen: Für jeden Satz bis zu [wartenMs] (≈ 3 s) auf Nova warten – vertont der Server den Satz gerade neu
 * (`/jetzt`), bis zu [jetztWartenMs] (≈ 6 s). Ab [hinweisMs] (≈ 0,4 s) einen dezenten
 * Ladehinweis zeigen. Erst danach – oder wenn Nova sicher nicht erreichbar ist – liest die Gerätestimme; die Anfrage läuft im
 * Hintergrund weiter, damit der Satz beim nächsten Mal da ist. Einmal je Sitzung kommt dann die Meldung [MELDUNG].
 */
class NovaAblauf(
    private val weg: NovaWeg,
    private val katalog: NovaKatalog,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val wartenMs: Long = WARTEN_MS,
    private val hinweisMs: Long = HINWEIS_MS,
    private val jetztWartenMs: Long = JETZT_WARTEN_MS,
) {
    /** [dateien] nacheinander abspielen; leer = Gerätestimme. [meldung]: einmal je Sitzung „Nova gerade nicht erreichbar“ */
    data class Plan(val dateien: List<String>, val art: NovaArt, val ergebnis: NovaWeg.Ergebnis?, val meldung: String? = null)

    private var gemeldet = false

    /** @param text gesprochen und normalisiert; @param laufzeit der Laufzeit-Satz dazu (sonst fester Text); @param laden Ladehinweis an/aus */
    suspend fun plan(text: String, sprache: String, laufzeit: LaufzeitText?, laden: (Boolean) -> Unit = {}): Plan {
        val art = if (laufzeit != null) katalog.art(laufzeit, text, sprache) else katalog.art(text, sprache)
        var erzeugt = false
        val suche = scope.async { weg.finde(text, sprache, art) { erzeugt = true } }
        val hinweis = scope.launch { kotlinx.coroutines.delay(hinweisMs); if (suche.isActive) laden(true) }
        val fund = try {
            kotlinx.coroutines.withTimeoutOrNull(wartenMs) { suche.await() }
                // Vertont der Server den Satz gerade neu (≈ 1,5 s), lohnt sich etwas mehr Geduld
                ?: if (erzeugt) kotlinx.coroutines.withTimeoutOrNull(jetztWartenMs - wartenMs) { suche.await() } else null
        } finally { hinweis.cancel(); laden(false) }
        if (fund != null && fund.nova) return Plan(fund.dateien, art, fund.ergebnis)
        // Notfall: Zeit um oder Nova nicht erreichbar – nicht, wenn es für den Text (noch) keine Nova geben kann oder darf
        val notfall = fund == null || fund.ergebnis == NovaWeg.Ergebnis.NETZ
        val meldung = if (notfall && !gemeldet) { gemeldet = true; MELDUNG } else null
        return Plan(emptyList(), art, fund?.ergebnis, meldung)
    }

    companion object {
        const val WARTEN_MS = 3_000L
        /** Höchstens so lange, wenn der Server den Satz gerade neu vertont (`/jetzt`) */
        const val JETZT_WARTEN_MS = 6_000L
        const val HINWEIS_MS = 400L
        const val MELDUNG = "Nova ist gerade nicht erreichbar – Brummi liest kurz mit der Stimme des Geräts."
    }
}

