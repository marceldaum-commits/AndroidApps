package de.wissensgarten.core.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Dauerhafter Speicher für „Alle Nova-Texte offline laden“ – getrennt vom automatischen Zwischenspeicher,
 * damit dessen Größengrenze die von Hand geladenen Dateien nie wegräumt. Der Abspielweg findet beide.
 */
interface NovaOfflineSpeicher {
    /** Alle Namen (h), die dauerhaft offline in dieser Qualität liegen */
    suspend fun offlineHashes(q: NovaQualitaet): Set<String>
    /**
     * Datei h dauerhaft sichern (Standard: auch aus dem Zwischenspeicher übernehmen, sonst laden).
     * HTTP-Status (200 = da) oder -1 (Netzfehler)
     */
    suspend fun sichern(url: String, h: String, q: NovaQualitaet): Int
    /** Kleine Textdatei holen (Liste, Server-Stand) – null bei Fehler */
    suspend fun text(url: String): String?
    /** Belegter Platz aller Nova-Dateien (Zwischenspeicher + offline) in Bytes */
    suspend fun belegt(): Long
    /** Alle Nova-Dateien löschen */
    suspend fun loeschen()
}

/** Fassung der Offline-Dateien: Standard (40 kbit/s) oder Bestqualität (unkomprimiert, 128 kbit/s). Beim Abspielen hat Bestqualität Vorrang. */
enum class NovaQualitaet(val label: String) {
    STANDARD("Standard"), BEST("Bestqualität");
    fun url(h: String) = if (this == BEST) Nova.urlOriginal(h) else Nova.url(h)
}

/** Rechnen und Laden für „Alle Nova-Texte offline laden“ – gleich für Android und Web. */
object NovaOffline {
    /** Live-Host der WebApp: dessen Liste und Server-Stand nimmt die Android-App */
    const val LIVE_HOST = "wissensgarten.mathenachhilfe-online.com"
    /** Server-Stand der Plus-Texte */
    const val PLUS_STAND = "lokal-packs"
    /** Schätzung je Datei, falls der Server keine Größe nennt */
    const val SCHAETZUNG_BYTES = 55_000L
    const val PARALLEL = 4
    /** So viele Netzfehler hintereinander: abbrechen (offline) – beim nächsten Tippen geht es weiter */
    const val MAX_NETZFEHLER = 6

    fun standUrl(name: String) = "${Nova.BASE}/nova/stand-$name.json"
    fun listeUrl(host: String) = "https://$host/vorlesen/texte.json"

    /** Faktor Bestqualität zu Standard, falls der Server keine Größe der Originale nennt */
    const val FAKTOR_BEST = 3

    /** Server-Stand: Zahl der Texte und Gesamtgröße der vorhandenen Dateien (Standard: bytes, Bestqualität: bytes_original) */
    data class Stand(val texte: Int, val bytes: Long?, val bytesOriginal: Long? = null)

    fun stand(json: String?): Stand? = runCatching {
        val o = Json.parseToJsonElement(json!!).jsonObject
        val vorhanden = o["vorhanden"]?.jsonPrimitive?.intOrNull
        Stand(vorhanden ?: o["texte"]!!.jsonPrimitive.int, o["bytes"]?.jsonPrimitive?.longOrNull, o["bytes_original"]?.jsonPrimitive?.longOrNull)
    }.getOrNull()

    /**
     * Liste `vorlesen/texte.json` → Einträge (Texte sind schon so, wie Nova sie spricht).
     * @param ohneLaufzeit vorab vertonte Laufzeit-Sätze (`"laufzeit": true`, z. B. Brummi-Radio) weglassen – „Alle Nova-Texte
     *   offline laden“ holt nur die festen Texte, sonst würde der Download mehr als doppelt so groß
     */
    fun liste(json: String, ohneLaufzeit: Boolean = false): List<Nova.Eintrag> = Json.parseToJsonElement(json).jsonObject["texte"]!!.jsonArray
        .filter { !ohneLaufzeit || it.jsonObject["laufzeit"]?.jsonPrimitive?.booleanOrNull != true }
        .map {
            val o = it.jsonObject
            Nova.Eintrag(o["text"]!!.jsonPrimitive.content, o["sprache"]!!.jsonPrimitive.content)
        }

    fun hashes(eintraege: Collection<Nova.Eintrag>): List<String> = eintraege.map { Nova.hash(it.text, it.sprache) }.distinct()

    /**
     * Wie viel noch zu laden ist: Durchschnittsgröße laut Server-Stand × fehlende Dateien.
     * Was schon auf dem Gerät liegt, zählt nicht mit. Ohne Angabe vom Server: geschätzt ([SCHAETZUNG_BYTES]).
     */
    fun restBytes(staende: List<Stand?>, fehlend: Int, q: NovaQualitaet = NovaQualitaet.STANDARD): Long {
        if (fehlend <= 0) return 0
        val std = durchschnitt(staende) { it.bytes } ?: SCHAETZUNG_BYTES
        val avg = if (q == NovaQualitaet.BEST) durchschnitt(staende) { it.bytesOriginal } ?: (std * FAKTOR_BEST) else std
        return avg * fehlend
    }

    /** Gibt der Server die Größe an (sonst ist die Zahl geschätzt)? */
    fun genau(staende: List<Stand?>, q: NovaQualitaet): Boolean =
        staende.isNotEmpty() && staende.all { s -> s != null && (if (q == NovaQualitaet.BEST) s.bytesOriginal else s.bytes) != null }

    private fun durchschnitt(staende: List<Stand?>, size: (Stand) -> Long?): Long? {
        val known = staende.filterNotNull().filter { size(it) != null && it.texte > 0 }
        return if (known.isEmpty()) null else known.sumOf { size(it)!! } / known.sumOf { it.texte }
    }

    /** Megabyte zum Anzeigen (gerundet, mindestens 1 bei kleinen Resten) */
    fun mb(bytes: Long): Long = if (bytes <= 0) 0 else maxOf(1, (bytes + 500_000) / 1_000_000)

    /** @param offline abgebrochen, weil keine Verbindung (zu viele Netzfehler hintereinander) */
    data class Ergebnis(val gesamt: Int, val vorhanden: Int, val geladen: Int, val gibtEsNicht: Int, val netzfehler: Int, val offline: Boolean)

    /**
     * Fehlende Dateien laden, [parallel] gleichzeitig. Schon gesicherte werden übersprungen – nach einem Abbruch geht es
     * beim nächsten Mal dort weiter. 404 (gibt es auf dem Server noch nicht) wird still übersprungen.
     * Bestqualität: liefert `original/` 404, wird die Standard-Datei gesichert.
     * @param fortschritt (fertig, gesamt) – fertig zählt auch, was schon da war
     */
    suspend fun lade(
        hashes: List<String>, s: NovaOfflineSpeicher, q: NovaQualitaet = NovaQualitaet.STANDARD,
        parallel: Int = PARALLEL, fortschritt: (Int, Int) -> Unit = { _, _ -> },
    ): Ergebnis {
        val alle = hashes.distinct()
        val best = s.offlineHashes(NovaQualitaet.BEST)
        val standard = s.offlineHashes(NovaQualitaet.STANDARD)
        // Standard: was in Bestqualität da ist, reicht (hat beim Abspielen ohnehin Vorrang)
        val schon = if (q == NovaQualitaet.BEST) best else standard + best
        val fehlt = alle.filter { it !in schon }
        val lock = Mutex()
        var fertig = alle.size - fehlt.size
        var geladen = 0; var nicht = 0; var netz = 0; var netzFolge = 0
        fortschritt(fertig, alle.size)
        val ch = Channel<String>(Channel.UNLIMITED)
        fehlt.forEach { ch.trySend(it) }
        ch.close()
        coroutineScope {
            repeat(parallel.coerceAtLeast(1)) {
                launch {
                    for (h in ch) {
                        if (lock.withLock { netzFolge >= MAX_NETZFEHLER }) break
                        var status = runCatching { s.sichern(q.url(h), h, q) }.getOrDefault(-1)
                        if (status == 404 && q == NovaQualitaet.BEST)
                            status = if (h in standard) 200 else runCatching { s.sichern(Nova.url(h), h, NovaQualitaet.STANDARD) }.getOrDefault(-1)
                        lock.withLock {
                            when {
                                status == 200 -> { geladen++; netzFolge = 0 }
                                status == 404 -> { nicht++; netzFolge = 0 }
                                else -> { netz++; netzFolge++ }
                            }
                            fertig++
                            fortschritt(fertig, alle.size)
                        }
                    }
                }
            }
        }
        return Ergebnis(alle.size, alle.size - fehlt.size, geladen, nicht, netz, netzFolge >= MAX_NETZFEHLER)
    }
}
