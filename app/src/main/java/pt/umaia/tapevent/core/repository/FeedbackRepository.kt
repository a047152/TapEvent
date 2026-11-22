package pt.umaia.tapevent.core.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pt.umaia.tapevent.core.model.Feedback
import pt.umaia.tapevent.core.model.Registration
import pt.umaia.tapevent.core.model.RegistrationStatus
import pt.umaia.tapevent.core.util.awaitResult

class AttendanceRequiredException : Exception("Feedback needs a confirmed attendance")
class FeedbackAlreadySentException : Exception("Feedback was already sent for this event")

class FeedbackRepository {

    private val db = Firebase.firestore
    private val feedback = db.collection("feedback")
    private val registrations = db.collection("registrations")

    /** This student's feedback for this event, or null if not sent yet. */
    suspend fun getFeedback(eventId: String, uid: String): Feedback? {
        val doc = feedback.document(Feedback.idFor(eventId, uid)).get().awaitResult()
        return doc.takeIf { it.exists() }?.let(Feedback::fromDocument)
    }

    /** Live list of all feedback for one organizer's events (Dashboard averages, "Feedback dos teus eventos"). */
    fun observeOrganizerFeedback(organizerId: String): Flow<List<Feedback>> = callbackFlow {
        val registration = feedback
            .whereEqualTo("organizerId", organizerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map(Feedback::fromDocument))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Saves the rating once. The transaction checks that the student confirmed
     * attendance ("presente") and has not sent feedback for this event before.
     */
    suspend fun submitFeedback(eventId: String, uid: String, organizerId: String, rating: Int, comment: String) {
        val registrationRef = registrations.document(Registration.idFor(eventId, uid))
        val feedbackRef = feedback.document(Feedback.idFor(eventId, uid))

        db.runTransaction { transaction ->
            val registration = transaction.get(registrationRef)
            if (registration.getString("status") != RegistrationStatus.ATTENDED) {
                throw AttendanceRequiredException()
            }
            if (transaction.get(feedbackRef).exists()) throw FeedbackAlreadySentException()

            transaction.set(
                feedbackRef,
                mapOf(
                    "eventId" to eventId,
                    "uid" to uid,
                    "organizerId" to organizerId,
                    "rating" to rating,
                    "comment" to comment,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.awaitResult()
    }
}
