package de.kurvenflug.core.model

/**
 * Messbare Schwierigkeit eines Levels – damit „schwerer“ nachweisbar ist (DifficultyTest, Meldung an den Leitstand).
 */
object Difficulty {
    data class Metrics(
        /** einstellbare Werte (Unbekannte) */
        val params: Int,
        /** mittlerer Radius der Zielzonen in Kästchen – kleiner = enger */
        val tolerance: Double,
        /** Asteroiden/Hindernisse */
        val obstacles: Int,
        /** Rechenschritte: Unbekannte + Bedingungen (Stationen) + Rechenzeichen im Funkspruch + Klippe */
        val steps: Int,
        /** Lösungsbereich: Anteil der Reglerstellungen, die noch treffen, wenn man einen Regler allein bewegt (Mittel) */
        val window: Double,
    ) {
        /** Gesamtwert: je größer, desto schwerer */
        val score: Double get() = params + steps + obstacles * 0.5 + 1.0 / tolerance + 20 * (1 - window)
    }

    private val ops = Regex(""" [+−:·] |√|\(""")

    fun steps(l: Level): Int {
        val riddle = if (l.hidden) ops.findAll(l.task.substringAfter("!").replace("Funkspruch:", "")).count() else 0
        val cliff = if (l.kind == Kind.THROW && l.launch.y > -9.0) 1 else 0
        return l.params.size + l.targets.size + riddle + cliff
    }

    /** Anteil treffender Stellungen je Regler (die anderen auf der Lösung), gemittelt über alle Regler. */
    fun window(l: Level): Double = l.params.map { p ->
        val n = ((p.max - p.min) / p.step + 1e-9).toInt() + 1
        val ok = (0 until n).count { i -> Flight.fly(l, l.solution + (p.key to p.snap(p.min + i * p.step))).success }
        ok.toDouble() / n
    }.average()

    fun of(l: Level) = Metrics(l.params.size, l.targets.map { it.r }.average(), l.asteroids.size, steps(l), window(l))
}
