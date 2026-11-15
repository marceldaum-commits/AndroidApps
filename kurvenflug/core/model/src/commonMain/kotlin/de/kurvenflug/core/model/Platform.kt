package de.kurvenflug.core.model

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow

/** Jetzt in Millisekunden seit 1970 (UTC). */
expect fun nowMillis(): Long

/** Abstand der Ortszeit zu UTC in Minuten zum Zeitpunkt [millis] (Sommerzeit inklusive). */
expect fun utcOffsetMinutes(millis: Long): Int

/** Wie Javas Math.round: halbe Werte nach oben. Auf allen Plattformen gleich. */
fun roundHalfUp(x: Double): Long = floor(x + 0.5).toLong()

fun toRadians(deg: Double) = deg * (PI / 180.0)
fun toDegrees(rad: Double) = rad * (180.0 / PI)

/** String-Prüfsumme wie Javas String.hashCode – damit Codes zwischen Handy und Browser gleich sind. */
fun stableHash(s: String): Int { var h = 0; for (c in s) h = 31 * h + c.code; return h }

/**
 * Ein Kalendertag (ohne Uhrzeit), plattformunabhängig. [dayOfWeek]: 1 = Montag … 7 = Sonntag.
 */
data class CalDate(val year: Int, val monthValue: Int, val dayOfMonth: Int) : Comparable<CalDate> {
    fun toEpochDay(): Long {
        // Howard Hinnant, days_from_civil
        val y = (if (monthValue <= 2) year - 1 else year).toLong()
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (monthValue + 9) % 12
        val doy = (153 * mp + 2) / 5 + dayOfMonth - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }
    fun plusDays(n: Long) = ofEpochDay(toEpochDay() + n)
    fun minusDays(n: Long) = plusDays(-n)
    fun isBefore(o: CalDate) = this < o
    fun isAfter(o: CalDate) = this > o
    val dayOfWeek: Int get() = ((toEpochDay() + 3).mod(7L) + 1).toInt()
    override fun compareTo(other: CalDate) = toEpochDay().compareTo(other.toEpochDay())

    companion object {
        const val DAY_MS = 86_400_000L
        fun of(year: Int, month: Int, day: Int) = CalDate(year, month, day)
        fun ofEpochDay(epochDay: Long): CalDate {
            val z = epochDay + 719468
            val era = (if (z >= 0) z else z - 146096) / 146097
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
            val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
            val y = (yoe + era * 400 + (if (m <= 2) 1 else 0)).toInt()
            return CalDate(y, m, d)
        }
        /** Tag (Ortszeit) zum Zeitpunkt [millis] */
        fun at(millis: Long) = ofEpochDay(Math2.floorDiv(millis + utcOffsetMinutes(millis) * 60_000L, DAY_MS))
        fun today() = at(nowMillis())
        /** Mitternacht (Ortszeit) des heutigen Tages in Millisekunden */
        fun startOfToday(now: Long = nowMillis()): Long {
            val local = today().toEpochDay() * DAY_MS
            return local - utcOffsetMinutes(local - utcOffsetMinutes(now) * 60_000L) * 60_000L
        }
        /** Aktuelle Stunde (Ortszeit) */
        fun hourNow(now: Long = nowMillis()) = ((now + utcOffsetMinutes(now) * 60_000L).mod(DAY_MS) / 3_600_000L).toInt()
        val WEEKDAYS = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    }
}

internal object Math2 { fun floorDiv(a: Long, b: Long): Long { val q = a / b; return if ((a % b != 0L) && ((a < 0) != (b < 0))) q - 1 else q } }

/** Wie Javas Math.floorMod: Ergebnis hat das Vorzeichen des Teilers. */
fun floorModK(a: Int, b: Int): Int = a.mod(b)
fun floorModK(a: Long, b: Long): Long = a.mod(b)
fun floorModK(a: Long, b: Int): Int = a.mod(b.toLong()).toInt()
fun powK(a: Double, b: Double): Double = a.pow(b)
fun countBits(v: Int) = v.countOneBits()

/** „15.11.2026 14:05“ in Ortszeit */
fun formatDateTime(millis: Long): String {
    val d = CalDate.at(millis)
    val local = millis + utcOffsetMinutes(millis) * 60_000L
    val min = (local.mod(CalDate.DAY_MS) / 60_000L).toInt()
    fun p(v: Int) = v.toString().padStart(2, '0')
    return "${p(d.dayOfMonth)}.${p(d.monthValue)}.${d.year} ${p(min / 60)}:${p(min % 60)}"
}
