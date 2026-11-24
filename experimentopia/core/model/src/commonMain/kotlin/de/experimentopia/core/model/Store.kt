package de.experimentopia.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** Bestes Ergebnis je Versuch – gleich für Android (Room) und Web (Browser-Speicher). */
@Serializable
data class ResultRow(val experimentId: String, val stars: Int, val firstGuessRight: Boolean, val plays: Int = 1, val firstAt: Long = 0, val lastAt: Long = 0)

/** Ein Eintrag im Forscherbuch. sure: 0 geraten, 1 ziemlich sicher, 2 ganz sicher */
@Serializable
data class NoteRow(val id: Long = 0, val experimentId: String, val at: Long, val guess: String, val result: String, val right: Boolean, val sure: Int, val explainedFirstTry: Boolean, val daily: Boolean = false)

/** Was nach einem Versuch gespeichert wird */
data class Finished(val id: String, val stars: Int, val guess: String, val result: String, val right: Boolean, val sure: Int, val explainedFirstTry: Boolean, val daily: Boolean)

/** Speicher für Fortschritt und Forscherbuch. Jede Plattform hat ihre eigene Umsetzung. */
interface LabStore {
    val results: Flow<List<ResultRow>>
    /** neueste zuerst */
    val journal: Flow<List<NoteRow>>
    suspend fun record(f: Finished)
    suspend fun reset()
}

/** Regeln für das Speichern – an einer Stelle, damit Android und Web gleich rechnen */
object Progress {
    /** Sterne sinken nie, die allererste Vermutung zählt, Spiele werden gezählt */
    fun merge(old: ResultRow?, f: Finished, now: Long) = ResultRow(
        experimentId = f.id,
        stars = maxOf(f.stars, old?.stars ?: 0),
        firstGuessRight = old?.firstGuessRight ?: f.right,
        plays = (old?.plays ?: 0) + 1,
        firstAt = old?.firstAt ?: now,
        lastAt = now,
    )
    fun note(f: Finished, now: Long, id: Long = 0) = NoteRow(id, f.id, now, f.guess, f.result, f.right, f.sure, f.explainedFirstTry, f.daily)
}

/** Einstellungen – nur auf dem Gerät, keine persönlichen Daten */
interface Prefs {
    val sound: StateFlow<Boolean>; fun setSound(v: Boolean)
    val vibration: StateFlow<Boolean>; fun setVibration(v: Boolean)
    /** Musik, Effekt-Lautstärke und Vorlesen (die Effekte selbst schaltet [sound]) */
    val audio: StateFlow<AudioPrefs>; fun setAudio(a: AudioPrefs)
    /** Forscher-Pass gekauft? Wirkt erst, wenn Pro eingeschaltet ist ([Pro.ENABLED]). */
    val pro: StateFlow<Boolean>; fun setPro(v: Boolean)
    /** Freischalt-Code des Forscher-Passes (von der Website, per Link oder eingetippt) */
    val proCode: StateFlow<String?>; fun setProCode(c: String?)
    /** Klassenstufe für den Filter (0 = alle) */
    val grade: StateFlow<Int>; fun setGrade(g: Int)
    var welcomed: Boolean
    val freeLabRuns: StateFlow<Int>; fun countFreeLab()
    var badgesSeen: Set<String>
    fun resetProgress()
    /**
     * Kleine Merkzettel (Text) für Hinweise: wann ein Hinweis zuletzt gezeigt wurde, ob Hinweise ausgeblendet sind.
     * Nur auf dem Gerät. Standard: nichts wird gemerkt (Android-Fassung, nicht ausgeliefert).
     */
    fun note(key: String): String? = null
    fun setNote(key: String, value: String?) {}
}
