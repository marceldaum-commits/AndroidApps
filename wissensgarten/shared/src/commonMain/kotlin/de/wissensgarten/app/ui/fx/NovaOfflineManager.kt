package de.wissensgarten.app.ui.fx

import de.wissensgarten.core.model.Nova
import de.wissensgarten.core.model.NovaOffline
import de.wissensgarten.core.model.NovaOfflineSpeicher
import de.wissensgarten.core.model.NovaQualitaet
import de.wissensgarten.core.model.Pack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Was die Einstellungen zu „Alle Nova-Texte offline laden“ zeigen */
data class OfflineZustand(
    val geprueft: Boolean = false,
    val gesamt: Int = 0,
    /** fehlende Dateien je Fassung (Standard zählt Bestqualität als vorhanden) */
    val fehlend: Map<NovaQualitaet, Int> = emptyMap(),
    /** noch zu ladende Bytes je Fassung (aus dem Server-Stand, sonst geschätzt) */
    val rest: Map<NovaQualitaet, Long> = emptyMap(),
    /** belegter Platz aller Nova-Dateien */
    val belegt: Long = 0,
    val laedt: NovaQualitaet? = null,
    val fertig: Int = 0,
    val ziel: Int = 0,
    val hinweis: String? = null,
)

/** Steuerung für die Einstellungen (eigene Schnittstelle, damit Bildschirmfotos ohne Netz gehen) */
interface NovaOfflineSteuerung {
    val zustand: StateFlow<OfflineZustand>
    /** Pakete für die Liste: freie (nur als Rückfall) und – wenn Plus aktiv – die geladenen Plus-Pakete */
    fun pakete(frei: List<Pack>, plus: List<Pack>) {}
    fun pruefen()
    fun laden(q: NovaQualitaet)
    fun abbrechen()
    fun loeschen()
}

/**
 * „Alle Nova-Texte offline laden“ – gleich für Android und Web.
 * Liste: die Vorlese-Liste der WebApp (`vorlesen/texte.json`, dieselben Texte und Namen wie der Server); Plus-Texte aus den
 * geladenen Plus-Paketen ([Nova.packTexte]). Größe: Server-Stand `stand-<host>.json` (+ `stand-lokal-packs.json` bei Plus),
 * abzüglich dessen, was schon auf dem Gerät liegt. Laufzeit-Texte gehören nicht dazu (`/jetzt` ist abgeschaltet).
 *
 * @param listeUrl Web: `vorlesen/texte.json` (eigener Host); Android: die Liste des Live-Hosts
 * @param host Name für den Server-Stand (Web: der eigene Host; Android: [NovaOffline.LIVE_HOST])
 */
class NovaOfflineManager(
    private val s: NovaOfflineSpeicher,
    private val scope: CoroutineScope,
    private val listeUrl: String,
    private val host: String,
) : NovaOfflineSteuerung {
    override val zustand = MutableStateFlow(OfflineZustand())
    private var frei: List<Pack> = emptyList()
    private var plus: List<Pack> = emptyList()
    private var liste: List<String>? = null
    private var job: Job? = null
    private var pruefJob: Job? = null

    override fun pakete(frei: List<Pack>, plus: List<Pack>) {
        val p = plus.filter { it.isPlus && !it.preview }
        val changed = p.map { it.id to it.version } != this.plus.map { it.id to it.version }
        this.frei = frei.filter { !it.isPlus && !it.preview }; this.plus = p
        if (changed && zustand.value.geprueft) pruefen()
    }

    private suspend fun hashes(): List<String> {
        val free = liste ?: (s.text(listeUrl)?.let { runCatching { NovaOffline.hashes(NovaOffline.liste(it, ohneLaufzeit = true)) }.getOrNull() }
            // Rückfall ohne Liste: dieselbe Rechnung aus den eingebauten Paketen (ohne Brummis Oberflächen-Sätze)
            ?: NovaOffline.hashes(frei.flatMap { Nova.packTexte(it) } + Nova.festeTexte()).takeIf { frei.isNotEmpty() })
            ?.also { liste = it } ?: emptyList()
        return (free + NovaOffline.hashes(plus.flatMap { Nova.packTexte(it) })).distinct()
    }

    private suspend fun staende(): List<NovaOffline.Stand?> =
        listOf(NovaOffline.stand(s.text(NovaOffline.standUrl(host)))) +
            (if (plus.isNotEmpty()) listOf(NovaOffline.stand(s.text(NovaOffline.standUrl(NovaOffline.PLUS_STAND)))) else emptyList())

    override fun pruefen() {
        if (job?.isActive == true) return
        pruefJob?.cancel()
        pruefJob = scope.launch { zustand.value = rechne(zustand.value.hinweis) }
    }

    private suspend fun rechne(hinweis: String?): OfflineZustand {
        val all = hashes()
        val best = s.offlineHashes(NovaQualitaet.BEST)
        val std = s.offlineHashes(NovaQualitaet.STANDARD)
        val fehlend = mapOf(
            NovaQualitaet.STANDARD to all.count { it !in std && it !in best },
            NovaQualitaet.BEST to all.count { it !in best },
        )
        val st = staende()
        return OfflineZustand(
            geprueft = true, gesamt = all.size, fehlend = fehlend,
            rest = fehlend.mapValues { (q, n) -> NovaOffline.restBytes(st, n, q) },
            belegt = s.belegt(),
            hinweis = if (all.isEmpty()) "Die Liste der Sätze ist gerade nicht erreichbar. Bitte später noch einmal versuchen." else hinweis,
        )
    }

    override fun laden(q: NovaQualitaet) {
        if (job?.isActive == true) return
        pruefJob?.cancel()
        job = scope.launch {
            zustand.value = zustand.value.copy(laedt = q, fertig = 0, ziel = 0, hinweis = null)
            val all = hashes()
            if (all.isEmpty()) { zustand.value = rechne(null); return@launch }
            val r = NovaOffline.lade(all, s, q) { f, n -> zustand.value = zustand.value.copy(laedt = q, fertig = f, ziel = n) }
            val text = when {
                r.offline -> "Keine Verbindung. Tipp nochmal – dann geht es dort weiter."
                r.netzfehler > 0 -> "${r.netzfehler} Sätze haben nicht geklappt. Tipp nochmal – dann lädt Brummi nur noch die fehlenden."
                r.gibtEsNicht > 0 -> "✅ Fertig! ${r.gibtEsNicht} Sätze gibt es noch nicht – die liest die Stimme des Geräts."
                else -> "✅ Fertig! Brummi liest jetzt auch ohne Internet mit der KI-Stimme vor."
            }
            zustand.value = rechne(text)
        }
    }

    override fun abbrechen() {
        job?.cancel(); job = null
        zustand.value = zustand.value.copy(laedt = null, hinweis = "Angehalten. Beim nächsten Tippen geht es dort weiter.")
        pruefen()
    }

    override fun loeschen() {
        job?.cancel(); job = null
        scope.launch { s.loeschen(); zustand.value = rechne("Offline-Stimme gelöscht.") }
    }
}
