package de.kurvenflug.core.model

import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Gleichstand Android ↔ Web: Läuft auf der JVM (wie Android) UND in Kotlin/Wasm (wie im Browser).
 * Beide müssen denselben Fingerabdruck aller Level, Flugbahnen, Tagesmissionen und Codes liefern –
 * sonst würde ein Kind im Browser eine andere Aufgabe sehen als auf dem Handy.
 */
class ParityTest {
    private fun r(v: Double) = (v * 1000).roundToLong()

    private fun print(l: Level): String {
        val f = Flight.fly(l, l.solution)
        val pts = f.path.filterIndexed { i, _ -> i % 7 == 0 }.joinToString(";") { "${r(it.x)},${r(it.y)}" }
        return "${l.id}|${l.title}|${l.kind}|${l.solution.entries.sortedBy { it.key }.joinToString { "${it.key}=${r(it.value)}" }}|" +
            "${l.targets.joinToString { "${r(it.x)},${r(it.y)},${r(it.r)}" }}|${f.success}|${f.path.size}|$pts"
    }

    /** Was jetzt herauskommt – auf beiden Plattformen identisch. */
    fun fingerprint(): String {
        val sb = StringBuilder()
        Levels.chapters.forEach { c -> sb.append(c.number).append(c.title); c.levels.forEach { sb.append(print(it)) } }
        (20_000L..20_030L).forEach { sb.append(print(Daily.forDay(it))) }
        (1..6).forEach { n -> Andromeda.chapter(n).levels.forEach { sb.append(print(it)) } }
        SeasonEvent.entries.forEach { e -> Seasons.levels(e, 2026).forEach { sb.append(print(it)) } }
        (1..8).forEach { n -> Season2.stages(n).forEach { sb.append(print(it)) } }
        Levels.all.forEach { l -> Walkthrough.of(l)?.forEach { sb.append(it.text).append(it.choices.map(::r)) } }
        return sb.toString()
    }

    @Test fun sameOnEveryPlatform() {
        val fp = fingerprint()
        assertTrue(fp.length > 10_000)
        assertEquals(EXPECTED, stableHash(fp).toString(16) + "/" + fp.length, "Fingerabdruck weicht ab – Web und Android rechnen verschieden")
    }

    @Test fun codesWorkAcrossPlatforms() {
        // Mit java.util.zip (Android) gepackt – muss überall lesbar sein
        assertEquals("Hausaufgabe", Codec.decodeChallenge(ANDROID_CODE)?.title)
        val r = ChallengeResult("Test", "Mia", listOf(1, 2, 1), "15.11.2026", ChallengeResult.checkOf("Test", "Mia", listOf(1, 2, 1), "15.11.2026"))
        assertEquals(CHECK, r.check)
        assertEquals(r, Codec.decodeResults(Codec.encodeResult(r)).single())
        assertEquals(CalDate(2026, 11, 15), CalDate.ofEpochDay(CalDate(2026, 11, 15).toEpochDay()))
        assertEquals(7, CalDate(2026, 11, 15).dayOfWeek) // Sonntag
        assertEquals(CalDate(2026, 4, 5), Seasons.easter(2026))
    }

    companion object {
        const val EXPECTED = "190d8c1d/286158"
        const val ANDROID_CODE = "KFC1:q1YqySzJSVWyUvJILC1OLE1LT0xKVdJRKkpNK1ayilYy0jUD8ox1DZVidZTyi1JSi0CiRQYgJUCxWgA"
        const val CHECK = "2523"
    }
}
