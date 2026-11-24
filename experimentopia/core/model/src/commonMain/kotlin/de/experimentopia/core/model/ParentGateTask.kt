package de.experimentopia.core.model

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Eltern-Tor nach dem gemeinsamen Standard der Lern-Apps (ELTERN-TOR.md, Stand 19.–21.11.2026):
 * eine Malaufgabe 13–19 × 6–9, in Worten („siebzehn mal acht“), Eingabe über eine eigene Ziffern-Tastatur,
 * nach EINEM Fehler sofort eine neue Aufgabe, kein Zeitdruck, Abbrechen jederzeit.
 * Kein Vorlese-Knopf und keine Gerätestimme, bis die Tonstudio-Stimme läuft (Marcel 21.11.2026).
 */
object ParentGateTask {
    data class Task(val a: Int, val b: Int) {
        val answer: Int get() = a * b
        /** „siebzehn mal acht“ – ohne eine einzige Ziffer, damit es Erstklässler nicht „ausrechnen“ können, bevor sie lesen */
        val spoken: String get() = "${words(a)} mal ${words(b)}"
        /** Beschreibung für Bildschirmleser */
        val description: String get() = "Rechenaufgabe für Erwachsene: $spoken. Gib das Ergebnis mit den Ziffern ein."
    }

    val A_RANGE = 13..19
    val B_RANGE = 6..9
    const val MAX_DIGITS = 3

    private val ones = mapOf(6 to "sechs", 7 to "sieben", 8 to "acht", 9 to "neun")
    private val teens = mapOf(13 to "dreizehn", 14 to "vierzehn", 15 to "fünfzehn", 16 to "sechzehn", 17 to "siebzehn", 18 to "achtzehn", 19 to "neunzehn")

    /** Zahlwort für die Zahlen der Aufgabe (6–9 und 13–19) */
    fun words(n: Int): String = ones[n] ?: teens[n] ?: error("Zahl außerhalb der Tor-Aufgaben: $n")

    /** Neue Aufgabe; [previous] wird nicht gleich noch einmal gestellt */
    fun next(random: Random, previous: Task? = null): Task {
        while (true) {
            val t = Task(A_RANGE.random(random), B_RANGE.random(random))
            if (t != previous) return t
        }
    }

    fun check(task: Task, typed: String): Boolean = typed.isNotEmpty() && typed.all { it in '0'..'9' } && typed.toIntOrNull() == task.answer

    /** Ziffer anhängen (höchstens [MAX_DIGITS] Stellen) bzw. letzte entfernen */
    fun press(typed: String, key: Char): String = when {
        key == '⌫' -> typed.dropLast(1)
        key in '0'..'9' && typed.length < MAX_DIGITS -> typed + key
        else -> typed
    }
}

/**
 * „Merken“ des Tors: Nach richtiger Antwort bleibt es [ttl] offen (Standard 10 Minuten) – nur im Arbeitsspeicher,
 * nie gespeichert. Beim Schließen der App ist es wieder zu.
 */
class ParentGateMemory(private val ttl: Duration = 10.minutes) {
    private var passedAt: TimeSource.Monotonic.ValueTimeMark? = null
    val isOpen: Boolean get() = passedAt?.let { it.elapsedNow() < ttl } == true
    fun pass() { passedAt = TimeSource.Monotonic.markNow() }
    fun close() { passedAt = null }
    companion object { val shared = ParentGateMemory() }
}
