package de.raumlicht.core.model

import kotlinx.coroutines.flow.StateFlow

/**
 * Alles, was sich die App merkt – außer den Sternen je Aufgabe. Die Bildschirme kennen nur diese Schnittstelle;
 * Android legt die Werte in den App-Einstellungen ab, die WebApp im Speicher des Browsers. Nichts davon verlässt das Gerät.
 */
interface Prefs {
    val sound: StateFlow<Boolean>; fun setSound(v: Boolean)
    val music: StateFlow<Boolean>; fun setMusic(v: Boolean)
    val vibration: StateFlow<Boolean>; fun setVibration(v: Boolean)
    /** Lux liest ihre Sprechblasen laut vor */
    val voice: StateFlow<Boolean>; fun setVoice(v: Boolean)
    /** Lautstärke in Prozent */
    val volume: StateFlow<Int>; fun setVolume(v: Int)

    /** Kapitel-Erklärung schon gesehen? */
    fun introSeen(chapter: Int): Boolean
    fun markIntroSeen(chapter: Int)

    val jamBest: StateFlow<Int>; /** @return true bei neuem Rekord */ fun recordJam(streak: Int): Boolean
    val lageBest: StateFlow<Int>; fun recordLage(score: Int): Boolean
    val balloonBest: StateFlow<Int>; fun recordBalloons(points: Int): Boolean
    /** Abi-Simulation: beste Punktzahl je Aufgabe */
    val exams: StateFlow<Map<String, Int>>; fun recordExam(id: String, points: Int)

    val skin: StateFlow<String>; fun setSkin(c: String)
    /** Leuchtspur und Aufsatz von Lux */
    val look: StateFlow<Pair<String, String>>; fun setLook(trail: String, hat: String)
    /** Fehler-Tagebuch als Text */
    val diary: StateFlow<String>; fun setDiary(s: String)
    /** Klausur-Planer (leer = kein Plan) */
    val plan: StateFlow<String>; fun setPlan(s: String)

    /** Zuletzt gebaute Lichtshow, eigene Funktion und eigener Flug */
    var show: String
    var ride: String
    var flight: String

    /** Neu anfangen: Rekorde, Abi-Punkte, Tagebuch, gesehene Erklärungen und Lux' Aussehen zurücksetzen (Ton-Einstellungen und Plan bleiben) */
    /** Abi-Pass freigeschaltet? Wirkt erst, wenn Pro eingeschaltet ist ([Pro.ENABLED]); gesetzt wird es später vom Kauf bzw. vom Freischalt-Code. */
    val pro: StateFlow<Boolean>
    fun setPro(active: Boolean)
    /** Zuletzt gewählte Schwierigkeit im endlosen Üben (leer = alle Stufen) */
    var jamLevel: String
    fun resetProgress()
}
