package pt.umaia.tapevent.core.util

import com.google.firebase.Timestamp
import pt.umaia.tapevent.core.repository.APP_ZONE
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Date formatting shared by every screen that shows an event date. */
object DateFormats {
    private val shortDateTime = DateTimeFormatter.ofPattern("dd/MM · HH:mm")
    private val fullDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val time = DateTimeFormatter.ofPattern("HH:mm")

    fun toZoned(timestamp: Timestamp): ZonedDateTime =
        timestamp.toDate().toInstant().atZone(APP_ZONE)

    /** e.g. "24/11 · 18:00" */
    fun shortDateTime(timestamp: Timestamp?): String =
        timestamp?.let { toZoned(it).format(shortDateTime) } ?: "No date"

    /** e.g. "24/11/2026" */
    fun fullDate(timestamp: Timestamp?): String =
        timestamp?.let { toZoned(it).format(fullDate) } ?: "No date"

    /** e.g. "18:00" */
    fun time(timestamp: Timestamp?): String =
        timestamp?.let { toZoned(it).format(time) } ?: "--:--"
}
