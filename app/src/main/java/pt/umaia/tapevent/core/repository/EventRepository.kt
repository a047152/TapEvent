package pt.umaia.tapevent.core.repository

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.EventStatus
import pt.umaia.tapevent.core.model.TimeSlot
import pt.umaia.tapevent.core.util.awaitResult
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

/** All event dates are interpreted in Portugal's time zone. */
val APP_ZONE: ZoneId = ZoneId.of("Europe/Lisbon")

data class NewEvent(
    val title: String,
    val description: String,
    val format: String,
    val theme: String,
    val start: ZonedDateTime,
    val location: String,
    val capacity: Int
)

class EventRepository {

    private val events = Firebase.firestore.collection("events")

    /** Saves a new event as "pendente". It only shows in Discover after an admin approves it. */
    suspend fun createEvent(event: NewEvent, organizerId: String, organizerName: String): String {
        val data = mapOf(
            "title" to event.title,
            "description" to event.description,
            "format" to event.format,
            "theme" to event.theme,
            "startAt" to Timestamp(Date.from(event.start.toInstant())),
            "location" to event.location,
            "organizerId" to organizerId,
            "organizerName" to organizerName,
            "capacity" to event.capacity,
            "freeSeats" to event.capacity,
            "status" to EventStatus.PENDING,
            "timeSlot" to timeSlotFor(event.start),
            "isWeekend" to isWeekend(event.start),
            "createdAt" to FieldValue.serverTimestamp()
        )
        val ref = events.add(data).awaitResult()
        return ref.id
    }

    /**
     * Live list of events waiting for approval, soonest first.
     * Sorted on the device, so no composite index is needed.
     */
    fun observePendingEvents(): Flow<List<Event>> = callbackFlow {
        val registration = events
            .whereEqualTo("status", EventStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty()
                    .map(Event::fromDocument)
                    .sortedBy { it.startAt?.seconds ?: Long.MAX_VALUE }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Admin decision: status becomes "aprovado" or "rejeitado". */
    suspend fun reviewEvent(eventId: String, approve: Boolean, adminUid: String) {
        val update = mapOf(
            "status" to if (approve) EventStatus.APPROVED else EventStatus.REJECTED,
            "reviewedBy" to adminUid,
            "reviewedAt" to FieldValue.serverTimestamp()
        )
        events.document(eventId).update(update).awaitResult()
    }

    companion object {
        fun timeSlotFor(start: ZonedDateTime): String = when (start.hour) {
            in 0..11 -> TimeSlot.MORNING
            in 12..18 -> TimeSlot.AFTERNOON
            else -> TimeSlot.NIGHT
        }

        fun isWeekend(start: ZonedDateTime): Boolean =
            start.dayOfWeek == DayOfWeek.SATURDAY || start.dayOfWeek == DayOfWeek.SUNDAY
    }
}
