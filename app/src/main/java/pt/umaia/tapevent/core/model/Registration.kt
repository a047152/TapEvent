package pt.umaia.tapevent.core.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/** Values of the "status" field in registrations/{eventId_uid} (kept in Portuguese, as in the plan). */
object RegistrationStatus {
    /** Has a free seat. */
    const val REGISTERED = "inscrito"
    /** No seats were left; wants to be notified if one opens. */
    const val NOTIFY = "aviso"
    /** Confirmed attendance after the event started. */
    const val ATTENDED = "presente"
}

data class Registration(
    val id: String = "",
    val eventId: String = "",
    val uid: String = "",
    val organizerId: String = "",
    val status: String = RegistrationStatus.REGISTERED,
    /** Copied from the event so "My events" can sort without reading every event. */
    val eventStartAt: Timestamp? = null,
    val createdAt: Timestamp? = null
) {
    /** True for "inscrito" and "presente": the student holds a seat. */
    val hasSeat: Boolean
        get() = status == RegistrationStatus.REGISTERED || status == RegistrationStatus.ATTENDED

    companion object {
        /** One registration per student per event: the id is "eventId_uid". */
        fun idFor(eventId: String, uid: String) = "${eventId}_$uid"

        fun fromDocument(doc: DocumentSnapshot): Registration = Registration(
            id = doc.id,
            eventId = doc.getString("eventId").orEmpty(),
            uid = doc.getString("uid").orEmpty(),
            organizerId = doc.getString("organizerId").orEmpty(),
            status = doc.getString("status") ?: RegistrationStatus.REGISTERED,
            eventStartAt = doc.getTimestamp("eventStartAt"),
            createdAt = doc.getTimestamp("createdAt")
        )
    }
}
