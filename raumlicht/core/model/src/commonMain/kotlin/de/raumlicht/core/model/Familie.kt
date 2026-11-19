package de.raumlicht.core.model

/**
 * Vorbereitung für den gemeinsamen Familien-Speicher der Lern-Apps (`/familie/v1/`).
 * Noch AUS: Angebunden wird erst mit der fertigen `familie.js` des Mathe-Trainers und dem Server des Leitstands.
 * Hier steht nur, was Raumlicht dafür trennt: Lernstand des Kindes und Rechte (Freischalt-Codes).
 */
object Familie {
    /** erst einschalten, wenn Server und Baustein stehen */
    const val ENABLED = false
    /** Rechte gehören der Familie, nicht dem einzelnen Kind – sie stehen nie im Lernstand eines Kindes */
    val RIGHTS = setOf("proCode", "kiCode")

    /** Der Lernstand eines Kindes: alles, was auch die Sicherung enthält, aber ohne Rechte */
    fun child(data: Map<String, String>): Map<String, String> = data.filterKeys { Eltern.saved(it) && it !in RIGHTS }
    /** Die Rechte, getrennt abgelegt; leere Einträge zählen nicht */
    fun rights(data: Map<String, String>): Map<String, String> = data.filter { it.key in RIGHTS && it.value.isNotBlank() }
}
