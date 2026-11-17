package de.wissensgarten.core.model

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
 * Ein Text, der erst in der App entsteht und deshalb nicht vorab vertont ist (z. B. „Hör noch mal: …“ im Brummi-Radio).
 * Nur solche Texte darf die App an `/jetzt` schicken. Es gibt sie ausschließlich über [Laufzeit] – dort entstehen sie aus
 * Zahlen und Karten der Wissenspakete, nie aus freiem Text. So können keine Namen oder Angaben zum Kind hineingeraten.
 */
class LaufzeitText internal constructor(val text: String) {
    override fun toString() = text
}

/**
 * Alle Laufzeit-Sätze an einer Stelle, immer gleich formuliert (gleicher Satz → nur einmal vertont).
 * Karten-Inhalte nur aus Wissenspaketen ([ausPaket] = true): Eigene Karten (selbst geschrieben, Klassen-Codes, Safari)
 * gehen nie an den Server – dann `null`, und die Stimme des Geräts liest.
 */
object Laufzeit {
    fun radioFrage(q: Radio.Question, ausPaket: Boolean): LaufzeitText? =
        if (!ausPaket || q.mode == Radio.Mode.FREE) null else LaufzeitText(Radio.prompt(q))
    fun radioAufloesung(q: Radio.Question, ausPaket: Boolean): LaufzeitText? = if (ausPaket) LaufzeitText(Radio.selfReveal(q)) else null
    fun radioKorrektur(q: Radio.Question, ausPaket: Boolean): LaufzeitText? = if (ausPaket) LaufzeitText(Radio.correction(q)) else null
    fun radioSpaeter(q: Radio.Question, ausPaket: Boolean): LaufzeitText? = if (ausPaket) LaufzeitText(Radio.later(q)) else null
    fun radioNochmal(card: StudyCard, ausPaket: Boolean): LaufzeitText? = if (ausPaket) LaufzeitText(Radio.again(card)) else null
    fun radioSchluss(correct: Int, total: Int): LaufzeitText = LaufzeitText(Radio.summary(correct, total))
    fun palast(right: Int, total: Int): LaufzeitText = LaufzeitText(palastText(right, total))
    fun bonus(dew: Int, silben: Boolean): LaufzeitText = LaufzeitText(bonusText(dew, silben))
    fun kapitelOffen(number: Int): LaufzeitText = LaufzeitText(kapitelOffenText(number))
    fun kapitelWartet(plants: Int): LaufzeitText = LaufzeitText(kapitelWartetText(plants))

    fun palastText(right: Int, total: Int) = "$right von $total Orten erinnert – dein Gedächtnis-Palast wächst! 🏰"
    fun bonusText(dew: Int, silben: Boolean) = "+$dew 💧 " + if (silben) "Silben-Profi!" else "Bonus!"
    fun kapitelOffenText(number: Int) = "Weiter geht's! Kapitel $number wartet schon auf uns!"
    fun kapitelWartetText(plants: Int) =
        "Damit es weitergeht, braucht der Garten Kraft. Lass noch $plants Pflanzen wachsen – dann geht unser Abenteuer weiter!"
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
 * Entscheidung, ob Nova spricht: Gerätespeicher → vorab vertonte Datei (/nova/…) → nur bei Laufzeit-Texten `/jetzt` →
 * sonst Stimme des Geräts. Fehler und „offline“ führen still zur Gerätestimme; danach eine Weile kein neuer Versuch,
 * damit Brummi nicht jedes Mal auf das Netz wartet.
 */
class NovaWeg(private val q: NovaQuelle, private val now: () -> Long = { Platform.nowMs() }) {
    enum class Ergebnis { SPEICHER, NOVA, JETZT, GERAET }

    private val fehlt = HashMap<String, Long>()
    private var netzPauseBis = 0L
    private var jetztPauseBis = 0L

    /** Gerade keine Verbindung (letzter Versuch scheiterte vor kurzem) */
    val offline: Boolean get() = now() < netzPauseBis

    /** @param text gesprochener Text ([Nova.sprechText]); @param laufzeit nur für [LaufzeitText] */
    suspend fun finde(text: String, sprache: String, laufzeit: Boolean): Ergebnis {
        if (text.isBlank() || sprache !in Nova.SPRACHEN) return Ergebnis.GERAET
        val h = Nova.hash(text, sprache)
        if (runCatching { q.imSpeicher(h) }.getOrDefault(false)) return Ergebnis.SPEICHER
        val t = now()
        if (t < netzPauseBis) return Ergebnis.GERAET
        val missing = fehlt[h]?.let { t - it < FEHLT_MS } == true
        if (!missing) {
            val s = runCatching { q.laden(Nova.url(h), h) }.getOrDefault(-1)
            when {
                s == 200 -> return Ergebnis.NOVA
                s == 404 -> fehlt[h] = t
                s < 0 -> { netzPauseBis = t + NETZ_PAUSE_MS; return Ergebnis.GERAET }
                else -> return Ergebnis.GERAET
            }
        }
        if (!laufzeit || text.length > JETZT_MAX || t < jetztPauseBis) return Ergebnis.GERAET
        val s = runCatching { q.jetzt(text, sprache, h) }.getOrDefault(-1)
        return when (s) {
            200 -> { fehlt.remove(h); Ergebnis.JETZT }
            204 -> Ergebnis.GERAET
            else -> { jetztPauseBis = t + JETZT_PAUSE_MS; Ergebnis.GERAET }
        }
    }

    companion object {
        const val JETZT_MAX = 300
        /** Fehlende Datei erst nach einer Stunde wieder anfragen (der Server vertont nach und nach) */
        const val FEHLT_MS = 60 * 60_000L
        const val NETZ_PAUSE_MS = 60_000L
        const val JETZT_PAUSE_MS = 10 * 60_000L
        const val BASE = Nova.BASE
    }
}
