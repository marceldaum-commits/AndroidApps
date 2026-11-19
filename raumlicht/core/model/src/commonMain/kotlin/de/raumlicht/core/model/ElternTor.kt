package de.raumlicht.core.model

import kotlin.random.Random

/**
 * Eltern-Tor (gemeinsamer Standard der Lern-Apps, 19.11.2026): Vor dem Weg zum Shop steht eine Malaufgabe in Worten – „siebzehn mal acht“.
 * Eingabe über eine eigene Ziffern-Tastatur, nach EINEM Fehler sofort eine neue Aufgabe, kein Zeitdruck, jederzeit abbrechen.
 * Einmal gelöst, bleibt das Tor [OPEN_MS] offen – nur im Arbeitsspeicher, nichts wird gespeichert.
 * In Raumlicht (Oberstufe, Marcels Entscheidung vom 19.11.2026) stehen dahinter nur die beiden Shop-Knöpfe; Preise und sachliche Infos bleiben sichtbar.
 */
object ElternTor {
    const val OPEN_MS = 10 * 60 * 1000L
    const val TITLE = "Für Erwachsene"
    const val NOTE = "Wer unter 18 ist, kauft nicht selbst. Bitte löse die Aufgabe, um zum Shop zu kommen."
    const val WRONG = "Das stimmt nicht – hier ist eine neue Aufgabe."
    private val TEENS = mapOf(13 to "dreizehn", 14 to "vierzehn", 15 to "fünfzehn", 16 to "sechzehn", 17 to "siebzehn", 18 to "achtzehn", 19 to "neunzehn")
    private val ONES = mapOf(6 to "sechs", 7 to "sieben", 8 to "acht", 9 to "neun")
    data class Task(val a: Int, val b: Int) {
        val answer get() = a * b
        /** Die Aufgabe in Worten – Ziffern stehen nirgends auf dem Bildschirm */
        val words get() = "${TEENS.getValue(a)} mal ${ONES.getValue(b)}"
        fun solves(input: String) = input.trim().toIntOrNull() == answer
    }
    /** Eine neue Aufgabe: 13–19 mal 6–9; nie dieselbe wie [not] */
    fun fresh(random: Random, not: Task? = null): Task { while (true) { val t = Task(random.nextInt(13, 20), random.nextInt(6, 10)); if (t != not) return t } }
    /** Ist das Tor noch offen? [openedAt] = Zeitpunkt des letzten Lösens (0 = nie) */
    fun open(openedAt: Long, now: Long) = openedAt > 0 && now >= openedAt && now - openedAt < OPEN_MS
}
