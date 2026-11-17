package de.wissensgarten.app.ui.fx

import de.wissensgarten.core.model.LaufzeitText
import de.wissensgarten.core.model.Nova
import de.wissensgarten.core.model.NovaQuelle
import de.wissensgarten.core.model.NovaWeg
import de.wissensgarten.core.model.Sprechtempo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Feste Sätze rund um die Stimme */
object Stimme {
    /** Kurzer Satz für die Probe-Taste */
    const val PROBE = "Hallo, ich bin Brummi! So klinge ich, wenn ich dir vorlese."
    /** Pflichtkennzeichnung der KI-Stimme */
    const val NAME = "Nova – KI-generierte Stimme"
}

/**
 * Was die Plattform für Nova können muss: Dateien speichern und laden ([NovaQuelle]), MP3 abspielen
 * (Tempo ohne Tonhöhen-Änderung) und als stille Notlösung die Stimme des Geräts.
 * Ohne Cookies, ohne Herkunftsangabe (Referrer), ohne Geräte-Kennung.
 */
interface NovaGeraet : NovaQuelle {
    /** Datei h abspielen und warten, bis sie zu Ende ist. false = ging nicht (dann liest die Gerätestimme). */
    suspend fun abspielen(h: String, tempo: Float): Boolean
    fun audioStopp()
    /** Gibt es eine Stimme des Geräts? */
    val geraetBereit: Boolean
    /** Stimme des Geräts (etwas höher gestimmt, wie bisher) – wartet, bis sie fertig ist */
    suspend fun geraetSprechen(text: String, locale: Locale, tempo: Float)
    fun geraetStopp()
}

/**
 * Brummis Stimme „Nova“ – für Android und Web derselbe Ablauf ([NovaWeg]):
 * Gerätespeicher → vorab vertonte Datei vom Stimm-Server → (nur Laufzeit-Texte) `/jetzt` → sonst still die Gerätestimme.
 */
class NovaVoice(private val g: NovaGeraet, private val scope: CoroutineScope) : Voice {
    override var onSpeaking: (Boolean) -> Unit = {}
    private val weg = NovaWeg(g)
    private var tempo = 1f
    private var job: Job? = null
    private var turn = 0

    override val isReady get() = g.geraetBereit || !weg.offline

    override fun setRate(percent: Int) { tempo = Sprechtempo.faktor(percent) }

    override fun speak(text: String, locale: Locale) = launch(text, locale, laufzeit = false)
    override fun speak(text: LaufzeitText, locale: Locale) = launch(text.text, locale, laufzeit = true)

    private fun launch(text: String, locale: Locale, laufzeit: Boolean) {
        quiet()
        job = scope.launch { say(text, locale, laufzeit) }
    }

    override suspend fun speakAndWait(text: String, locale: Locale) { quiet(); waitFor(text, locale, false) }
    override suspend fun speakAndWait(text: LaufzeitText, locale: Locale) { quiet(); waitFor(text.text, locale, true) }

    private suspend fun waitFor(text: String, locale: Locale, laufzeit: Boolean) {
        // Sicherheitsnetz, falls Wiedergabe oder Gerätestimme hängen
        withTimeoutOrNull(90_000) { say(text, locale, laufzeit) }
    }

    /** Laufendes Vorlesen beenden (ohne onSpeaking – das übernimmt der nächste Satz bzw. [stop]) */
    private fun quiet() {
        turn++
        job?.cancel(); job = null
        g.audioStopp(); g.geraetStopp()
    }

    override fun stop() { quiet(); onSpeaking(false) }

    private suspend fun say(raw: String, locale: Locale, laufzeit: Boolean) {
        val sprache = locale.language.takeIf { it in Nova.SPRACHEN } ?: "de"
        val text = Nova.normalisiert(Nova.sprechText(raw, sprache))
        if (text.none { it.isLetterOrDigit() }) return
        val my = ++turn
        onSpeaking(true)
        try {
            val r = weg.finde(text, sprache, laufzeit)
            if (my != turn) return
            if (r != NovaWeg.Ergebnis.GERAET && g.abspielen(Nova.hash(text, sprache), tempo)) return
            if (my != turn) return
            g.geraetSprechen(text, locale, tempo)
        } finally {
            if (my == turn) onSpeaking(false)
        }
    }
}
