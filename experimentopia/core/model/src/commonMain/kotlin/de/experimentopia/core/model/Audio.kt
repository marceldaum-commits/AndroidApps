package de.experimentopia.core.model

/**
 * Klang- und Vorlese-Einstellungen – gleich in Android und Web.
 * [voice]: "" = beste Stimme des Geräts/Browsers, [NATURAL] = „Thorsten“, sonst Name einer Geräte-/Browser-Stimme.
 */
data class AudioPrefs(
    val music: Boolean = true,
    val musicVol: Float = 0.5f,
    val sfxVol: Float = 0.9f,
    val read: Boolean = true,
    val autoRead: Boolean = false,
    val voice: String = "",
    val rate: Float = 1f,
) {
    fun encode() = listOf("musik=${if (music) 1 else 0}", "musikLaut=$musicVol", "effektLaut=$sfxVol", "vorlesen=${if (read) 1 else 0}",
        "auto=${if (autoRead) 1 else 0}", "tempo=$rate", "stimme=${voice.replace(";", "")}").joinToString(";")

    companion object {
        const val NATURAL = "thorsten"
        fun decode(s: String?): AudioPrefs {
            if (s.isNullOrBlank()) return AudioPrefs()
            val m = s.split(';').mapNotNull { p -> val i = p.indexOf('='); if (i <= 0) null else p.substring(0, i) to p.substring(i + 1) }.toMap()
            val d = AudioPrefs()
            fun f(k: String, def: Float) = m[k]?.toFloatOrNull()?.coerceIn(0f, 2f) ?: def
            return AudioPrefs(m["musik"]?.let { it == "1" } ?: d.music, f("musikLaut", d.musicVol), f("effektLaut", d.sfxVol),
                m["vorlesen"]?.let { it == "1" } ?: d.read, m["auto"]?.let { it == "1" } ?: d.autoRead, m["stimme"] ?: d.voice, f("tempo", d.rate).coerceIn(0.6f, 1.6f))
        }
    }
}

/** Musik-Welten: Jede klingt eigen (Instrumente, Tonart, Tempo) */
enum class World { LABOR, PHYSIK, CHEMIE, BIOLOGIE }

fun Island.world() = when (this) { Island.CHEMIE -> World.CHEMIE; Island.BIO -> World.BIOLOGIE; else -> World.PHYSIK }
fun Subject.world() = when (this) { Subject.PHYSIK -> World.PHYSIK; Subject.CHEMIE -> World.CHEMIE; Subject.BIOLOGIE -> World.BIOLOGIE }

/** Was vorgelesen wird – genau diese Texte bekommt die natürliche Stimme als vorab erzeugte Lautschrift. */
object ReadTexts {
    fun story(e: Experiment) = e.story
    fun question(e: Experiment) = e.predict.question + " Zur Wahl: " + e.predict.choices.joinToString(", ") { it.text } + "."
    fun task(e: Experiment) = "Dein Auftrag: " + e.task
    fun reveal(e: Experiment) = e.reveal
    /** Was Blubb bei der Auflösung sagt – richtig vermutet oder Aha-Moment */
    fun revealSay(e: Experiment, right: Boolean) = if (right) e.reveal else "Überraschung! ${e.reveal} Genau so entdecken Forscher Neues – wer sich wundert, lernt am meisten."
    fun why(e: Experiment) = "Warum? " + e.why.first()
    fun fact(e: Experiment) = "Merksatz: " + e.fact
    fun transfer(e: Experiment) = e.transfer.question + " Zur Wahl: " + e.transfer.choices.joinToString(", ") { it.text } + "."
    fun safety(id: String) = Safety.note(id)?.let { "Sicherheit zuerst: $it" }
    fun theory(t: Topic, pro: Boolean): String {
        val c = Theory.of(t)
        return (listOf(t.title + ".", c.idea) + c.basics.map { it.text } + listOf("Im Alltag: " + c.everyday) + (if (pro) c.pro.map { it.text } else emptyList())).joinToString(" ")
    }
    /** Feste Sätze der Oberfläche, die Blubb sagt */
    val ui = listOf(
        "Was glaubst du? Es gibt kein Falsch – Forscher vermuten immer zuerst.",
        "Hallo, ich bin Blubb! Wasser hat die Formel H₂O, und auf der Erde fällt alles mit 9,81 m/s².",
    )

    /** Alle festen Texte (für das Lautschrift-Werkzeug) */
    fun all(): List<String> = Catalog.all.flatMap { e ->
        listOfNotNull(story(e), question(e), task(e), revealSay(e, true), revealSay(e, false), why(e), fact(e), transfer(e), safety(e.id)) + e.why
    } + Topic.entries.flatMap { listOf(theory(it, false), theory(it, true)) } + Island.entries.map { it.title } + ui
}
