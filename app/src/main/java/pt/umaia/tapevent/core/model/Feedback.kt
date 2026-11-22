package pt.umaia.tapevent.core.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

const val MIN_RATING = 1
const val MAX_RATING = 5
const val MAX_COMMENT_LENGTH = 500

/** A student's rating of an event they attended. Stored at feedback/{eventId_uid}. */
data class Feedback(
    val id: String = "",
    val eventId: String = "",
    val uid: String = "",
    val organizerId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Timestamp? = null
) {
    companion object {
        /** One feedback per student per event, like registrations. */
        fun idFor(eventId: String, uid: String) = "${eventId}_$uid"

        fun fromDocument(doc: DocumentSnapshot): Feedback = Feedback(
            id = doc.id,
            eventId = doc.getString("eventId").orEmpty(),
            uid = doc.getString("uid").orEmpty(),
            organizerId = doc.getString("organizerId").orEmpty(),
            rating = doc.getLong("rating")?.toInt() ?: 0,
            comment = doc.getString("comment").orEmpty(),
            createdAt = doc.getTimestamp("createdAt")
        )
    }
}
