package de.wissensgarten.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import de.wissensgarten.core.model.Platform
import de.wissensgarten.core.model.Scheduler
import kotlinx.datetime.LocalDate

/** Zurück-Taste (Android) bzw. nichts (Web – dort gibt es die Zurück-Knöpfe in der Oberfläche). */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

/** Text in Graphem-Cluster zerlegen (ein Emoji mit Hautfarbe/ZWJ bleibt zusammen). */
expect fun graphemes(s: String): List<String>

/** Bild als PNG-Bytes und zurück (zum Speichern selbst gemalter Merkbilder). */
expect fun ImageBitmap.encodePng(): ByteArray
expect fun decodeImage(bytes: ByteArray): ImageBitmap?

/** Uhrzeit und Kalender nach Ortszeit – ohne java.util.Calendar. */
object Clock {
    fun now(): Long = Platform.nowMs()
    /** Stunde 0–23 (mit Bruchteil) */
    fun hourOf(ms: Long): Float = ((ms + Platform.tzOffsetMs(ms)).mod(Scheduler.DAY_MS)) / 3_600_000f
    fun date(ms: Long): LocalDate = LocalDate.fromEpochDays(Scheduler.localDay(ms).toInt())
    /** Wochentag 0 = Montag … 6 = Sonntag */
    fun weekday(ms: Long): Int = date(ms).dayOfWeek.ordinal

    /** Name des Wochentags für einen lokalen Epoch-Tag (z. B. Termin einer Klassenarbeit) */
    fun weekdayName(epochDay: Long): String = DAYS[LocalDate.fromEpochDays(epochDay.toInt()).dayOfWeek.ordinal]

    private val DAYS = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    private val MONTHS = listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
    /** „Montag, 3. März“ */
    /** „18. Nov. 2026, 15:40“ (Ortszeit) */
    fun dateTimeText(ms: Long): String {
        val h = hourOf(ms); val hh = h.toInt(); val mm = ((h - hh) * 60).toInt()
        return shortDate(date(ms)) + ", " + hh.toString().padStart(2, '0') + ":" + mm.toString().padStart(2, '0')
    }
    fun longDate(d: LocalDate) = "${DAYS[d.dayOfWeek.ordinal]}, ${d.day}. ${MONTHS[d.month.ordinal]}"
    /** „3. Mär. 2026“ */
    fun shortDate(d: LocalDate) = "${d.day}. ${MONTHS[d.month.ordinal].take(3)}${if (MONTHS[d.month.ordinal].length > 4) "." else ""} ${d.year}"
    /** „3. März 2026“ */
    fun fullDate(d: LocalDate) = "${d.day}. ${MONTHS[d.month.ordinal]} ${d.year}"
}

/** Eine Nachkommastelle mit Komma, z. B. 1,5 */
fun oneDecimal(v: Double): String {
    val r = kotlin.math.round(v * 10).toLong()
    return "${r / 10},${kotlin.math.abs(r % 10)}"
}
