package pt.umaia.tapevent.core.repository

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pt.umaia.tapevent.core.model.EventStatus
import pt.umaia.tapevent.core.model.Registration
import pt.umaia.tapevent.core.model.RegistrationStatus
import pt.umaia.tapevent.core.util.awaitResult

class NoSeatsLeftException : Exception("No free seats left")
class EventNotAvailableException : Exception("Event is not approved or already started")
class AlreadyRegisteredException : Exception("Already registered for this event")
class CannotCancelException : Exception("Registration can no longer be cancelled")
class AttendanceNotOpenException : Exception("Attendance can only be confirmed after the event starts")

class RegistrationRepository {

    private val db = Firebase.firestore
    private val events = db.collection("events")
    private val registrations = db.collection("registrations")

    /** Live registration of this student for this event, or null if there is none. */
    fun observeRegistration(eventId: String, uid: String): Flow<Registration?> = callbackFlow {
        val registration = registrations.document(Registration.idFor(eventId, uid))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.let(Registration::fromDocument))
            }
        awaitClose { registration.remove() }
    }

    /** Live list of every registration of this student ("inscrito", "aviso" and "presente"). */
    fun observeMyRegistrations(uid: String): Flow<List<Registration>> = callbackFlow {
        val registration = registrations
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map(Registration::fromDocument))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Marks the student as present. Allowed from the event start time onwards,
     * and only for a registration that holds a seat ("inscrito").
     */
    suspend fun confirmAttendance(eventId: String, uid: String) {
        val eventRef = events.document(eventId)
        val registrationRef = registrations.document(Registration.idFor(eventId, uid))

        db.runTransaction { transaction ->
            val event = transaction.get(eventRef)
            val registration = transaction.get(registrationRef)

            val startAt = event.getTimestamp("startAt")
            val isOpen = registration.getString("status") == RegistrationStatus.REGISTERED &&
                    startAt != null && startAt <= Timestamp.now()
            if (!isOpen) throw AttendanceNotOpenException()

            transaction.update(
                registrationRef,
                mapOf(
                    "status" to RegistrationStatus.ATTENDED,
                    "checkedInAt" to FieldValue.serverTimestamp()
                )
            )
        }.awaitResult()
    }

    /**
     * Takes one free seat. Runs in a transaction, so two students can never
     * get the last seat at the same time: Firestore retries if the event changed.
     */
    suspend fun register(eventId: String, uid: String) {
        val eventRef = events.document(eventId)
        val registrationRef = registrations.document(Registration.idFor(eventId, uid))

        db.runTransaction { transaction ->
            val event = transaction.get(eventRef)
            val existing = transaction.get(registrationRef)

            val status = existing.getString("status")
            if (status == RegistrationStatus.REGISTERED || status == RegistrationStatus.ATTENDED) {
                throw AlreadyRegisteredException()
            }

            val startAt = event.getTimestamp("startAt")
            val isOpen = event.getString("status") == EventStatus.APPROVED &&
                    startAt != null && startAt > Timestamp.now()
            if (!isOpen) throw EventNotAvailableException()

            val freeSeats = event.getLong("freeSeats") ?: 0L
            if (freeSeats <= 0L) throw NoSeatsLeftException()

            transaction.update(eventRef, "freeSeats", freeSeats - 1)
            transaction.set(
                registrationRef,
                mapOf(
                    "eventId" to eventId,
                    "uid" to uid,
                    "organizerId" to event.getString("organizerId").orEmpty(),
                    "status" to RegistrationStatus.REGISTERED,
                    "eventStartAt" to startAt,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.awaitResult()
    }

    /**
     * Cancels a seat and gives it back to the event, in one transaction.
     * Only possible before the event starts and while the status is "inscrito".
     */
    suspend fun cancelRegistration(eventId: String, uid: String) {
        val eventRef = events.document(eventId)
        val registrationRef = registrations.document(Registration.idFor(eventId, uid))

        db.runTransaction { transaction ->
            val event = transaction.get(eventRef)
            val registration = transaction.get(registrationRef)

            val startAt = event.getTimestamp("startAt")
            val canCancel = registration.getString("status") == RegistrationStatus.REGISTERED &&
                    startAt != null && startAt > Timestamp.now()
            if (!canCancel) throw CannotCancelException()

            val freeSeats = event.getLong("freeSeats") ?: 0L
            val capacity = event.getLong("capacity") ?: freeSeats
            transaction.update(eventRef, "freeSeats", (freeSeats + 1).coerceAtMost(capacity))
            transaction.delete(registrationRef)
        }.awaitResult()
    }

    /** "Avisar-me se abrirem vagas": no seat is taken, only the wish is saved. */
    suspend fun requestNotification(eventId: String, uid: String, organizerId: String, startAt: Timestamp?) {
        registrations.document(Registration.idFor(eventId, uid)).set(
            mapOf(
                "eventId" to eventId,
                "uid" to uid,
                "organizerId" to organizerId,
                "status" to RegistrationStatus.NOTIFY,
                "eventStartAt" to startAt,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).awaitResult()
    }

    /** Removes the "notify me" request (no seat to give back). */
    suspend fun cancelNotification(eventId: String, uid: String) {
        registrations.document(Registration.idFor(eventId, uid)).delete().awaitResult()
    }
}
