package de.wissensgarten.app.ui.fx

import de.wissensgarten.core.model.LaufzeitText
import de.wissensgarten.core.model.Nova
import de.wissensgarten.core.model.NovaAblauf
import de.wissensgarten.core.model.NovaKatalog
import de.wissensgarten.core.model.NovaOffline
import de.wissensgarten.core.model.NovaQuelle
import de.wissensgarten.core.model.NovaWeg
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.Sprechtempo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Feste Sätze rund um die Stimme */
object Stimme {
    /** Kurzer Satz für die Probe-Taste */
    const val PROBE = "Hallo, ich bin Brummi! So klinge ich, wenn ich dir vorlese."
    /** Pflichtkennzeichnung der KI-Stimme */
    const val NAME = "🤖 KI-generierte Stimme"
}

/**
 * Was die Plattform für Nova können muss: Dateien speichern und laden ([NovaQuelle]), MP3 abspielen
 * (Tempo ohne Tonhöhen-Änderung) und als Notlösung die Stimme des Geräts.
 * Ohne Cookies, ohne Herkunftsangabe (Referrer), ohne Geräte-Kennung.
 */
interface NovaGeraet : NovaQuelle {
    /**
     * Datei h abspielen und warten, bis sie zu Ende ist. false = ging nicht (dann liest die Gerätestimme).
     * Web: Sperrt der Browser das Abspielen vor der ersten Berührung, wartet die Datei auf das erste Antippen.
     */
    suspend fun abspielen(h: String, tempo: Float): Boolean
    fun audioStopp()
    /** Gibt es eine Stimme des Geräts? */
    val geraetBereit: Boolean
    /** Stimme des Geräts (etwas höher gestimmt, wie bisher) – wartet, bis sie fertig ist */
    suspend fun geraetSprechen(text: String, locale: Locale, tempo: Float)
    fun geraetStopp()
    /** Die öffentliche Vorlese-Liste `vorlesen/texte.json` (in App bzw. WebApp eingebaut) – null, wenn sie fehlt */
    suspend fun liste(): String?
}

/**
 * Brummis Stimme „Nova“ – für Android und Web derselbe Ablauf ([NovaAblauf], [NovaWeg], [NovaKatalog]):
 * Gerätespeicher → vorab vertonte Datei vom Stimm-Server → (`/jetzt` ist seit 20.11.2026 abgeschaltet, nie mehr anfragen) → sonst die Gerätestimme.
 * Bis ≈ 3 s wird auf die KI-Stimme gewartet ([laedt] zeigt ab ≈ 0,4 s „🎧 …“).
 */
class NovaVoice(
    private val g: NovaGeraet,
    private val scope: CoroutineScope,
    override val offline: NovaOfflineSteuerung? = null,
) : Voice {
    override var onSpeaking: (Boolean) -> Unit = {}
    private val weg = NovaWeg(g)
    private val katalog = NovaKatalog()
    private val ablauf = NovaAblauf(weg, katalog, scope)
    private var tempo = 1f
    private var job: Job? = null
    private var turn = 0
    private val laedtFlow = MutableStateFlow(false)
    private val hinweisFlow = MutableStateFlow<String?>(null)
    override val laedt: StateFlow<Boolean> get() = laedtFlow
    override val hinweis: StateFlow<String?> get() = hinweisFlow

    /** Vorlese-Liste einmal im Hintergrund lesen (lokal, schnell) */
    private var listeJob: Deferred<Unit>? = null
    private fun listeLaden(): Deferred<Unit> {
        // fertig, aber ohne Liste (z. B. kurz offline im Web) → noch einmal versuchen
        listeJob?.let { if (!it.isCompleted || katalog.listeDa) return it }
        return scope.async {
            val json = runCatching { g.liste() }.getOrNull() ?: return@async
            val eintraege = withContext(Dispatchers.Default) { runCatching { NovaOffline.liste(json) }.getOrNull() }
            if (eintraege != null) katalog.liste(eintraege)
        }.also { listeJob = it }
    }

    override val isReady get() = g.geraetBereit || !weg.offline

    override fun setRate(percent: Int) { tempo = Sprechtempo.faktor(percent) }

    override fun pakete(packs: List<Pack>) {
        listeLaden()
        scope.launch(Dispatchers.Default) { katalog.paketeSchrittweise(packs) }
    }

    override fun speak(text: String, locale: Locale) = launch(text, locale, null)
    override fun speak(text: LaufzeitText, locale: Locale) = launch(text.text, locale, text)

    private fun launch(text: String, locale: Locale, rt: LaufzeitText?) {
        quiet()
        job = scope.launch { say(text, locale, rt) }
    }

    override suspend fun speakAndWait(text: String, locale: Locale) { quiet(); waitFor(text, locale, null) }
    override suspend fun speakAndWait(text: LaufzeitText, locale: Locale) { quiet(); waitFor(text.text, locale, text) }

    private suspend fun waitFor(text: String, locale: Locale, rt: LaufzeitText?) {
        val vorher = turn
        // Sicherheitsnetz, falls Wiedergabe oder Gerätestimme hängen
        withTimeoutOrNull(90_000) { say(text, locale, rt) }
        // Hat inzwischen etwas anderes zu sprechen begonnen (z. B. das Kind tippt Brummi an), endet auch die Vorlese-Kette
        // (Frage → Antwort → Erklärung) – sonst würde sie den neuen Satz gleich wieder abschneiden.
        if (turn > vorher + 1) throw kotlinx.coroutines.CancellationException("Brummi sagt schon etwas anderes")
    }

    /** Laufendes Vorlesen beenden (ohne onSpeaking – das übernimmt der nächste Satz bzw. [stop]) */
    private fun quiet() {
        turn++
        job?.cancel(); job = null
        laedtFlow.value = false
        g.audioStopp(); g.geraetStopp()
    }

    override fun stop() { quiet(); onSpeaking(false) }

    private suspend fun say(raw: String, locale: Locale, rt: LaufzeitText?) {
        val sprache = locale.language.takeIf { it in Nova.SPRACHEN } ?: "de"
        val text = Nova.normalisiert(Nova.sprechText(raw, sprache))
        if (text.none { it.isLetterOrDigit() }) return
        val my = ++turn
        onSpeaking(true)
        try {
            withTimeoutOrNull(LISTE_MS) { listeLaden().await() }
            if (my != turn) return
            val plan = ablauf.plan(text, sprache, rt) { on -> laedtFlow.value = on && my == turn }
            if (my != turn) return
            // Lange Texte über /jetzt: ganze Sätze nacheinander. Klappt schon die erste Datei nicht, liest die Gerätestimme.
            for ((i, h) in plan.dateien.withIndex()) {
                if (my != turn) return
                if (!g.abspielen(h, tempo)) { if (i > 0) return else break }
                if (i == plan.dateien.lastIndex) return
            }
            // Abgebrochen (nächster Satz, Bildschirm verlassen)? Dann nie die Gerätestimme für diesen Satz
            currentCoroutineContext().ensureActive()
            if (my != turn) return
            plan.meldung?.let { hinweisFlow.value = it }
            g.geraetSprechen(text, locale, tempo)
        } finally {
            if (my == turn) { laedtFlow.value = false; onSpeaking(false) }
        }
    }

    companion object {
        /** So lange höchstens auf die (lokale) Vorlese-Liste warten */
        const val LISTE_MS = 1_500L
    }
}
