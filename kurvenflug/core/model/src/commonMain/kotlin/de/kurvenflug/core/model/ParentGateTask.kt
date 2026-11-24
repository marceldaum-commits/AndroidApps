package de.kurvenflug.core.model

/**
 * Eltern-Tor (Standard ELTERN-TOR.md, Recht/Marcel 19.–21.11.): Malaufgabe 13–19 × 6–9, IN WORTEN („siebzehn mal acht“),
 * Eingabe über eine eigene Ziffern-Tastatur. Nach einem Fehler sofort eine neue Aufgabe. Kein Zeitdruck, jederzeit abbrechen.
 * Einmal gelöst bleibt das Tor 10 Minuten offen – nur im Arbeitsspeicher ([GateMemory]), nie gespeichert.
 * Kein Vorlese-Knopf, keine Gerätestimme (Marcel 21.11.: „erst ohne, dann Tonstudio“).
 */
data class ParentGateTask(val a: Int, val b: Int) {
    val answer get() = a * b
    /** z. B. „siebzehn mal acht“ */
    val words get() = "${word(a)} mal ${word(b)}"
    fun isRight(input: String) = input.trim().toIntOrNull() == answer

    companion object {
        val A_RANGE = 13..19
        val B_RANGE = 6..9
        private val WORDS = mapOf(6 to "sechs", 7 to "sieben", 8 to "acht", 9 to "neun", 13 to "dreizehn", 14 to "vierzehn",
            15 to "fünfzehn", 16 to "sechzehn", 17 to "siebzehn", 18 to "achtzehn", 19 to "neunzehn")
        fun word(n: Int) = WORDS[n] ?: error("kein Wort für $n")

        /** Neue Aufgabe aus einer Zufallszahl; [previous] wird nicht wiederholt (nach einem Fehler gleich eine andere). */
        fun next(random: Int, previous: ParentGateTask? = null): ParentGateTask {
            val all = A_RANGE.flatMap { a -> B_RANGE.map { b -> ParentGateTask(a, b) } }.filter { it != previous }
            return all[(random and Int.MAX_VALUE) % all.size]
        }
    }
}

/** 10 Minuten offen nach dem Lösen, nur im Arbeitsspeicher (kein Speichern, kein Neustart-Überleben). */
class GateMemory(private val openMillis: Long = OPEN_MILLIS) {
    private var until = 0L
    fun open(now: Long) { until = now + openMillis }
    fun isOpen(now: Long) = now < until
    fun close() { until = 0L }
    companion object { const val OPEN_MILLIS = 10 * 60 * 1000L }
}
