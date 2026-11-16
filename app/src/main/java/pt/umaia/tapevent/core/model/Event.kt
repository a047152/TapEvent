package pt.umaia.tapevent.core.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/** Event formats, same as the mockup (Workshop, Palestra, Festa, ...). */
val EVENT_FORMATS = listOf("Workshop", "Palestra", "Festa", "Desporto", "Visita de estudo", "Voluntariado")

/** Event themes, used by the "filter by theme" in Discover. */
val EVENT_THEMES = listOf(
    "Tecnologia", "Filosofia", "Desporto", "Cultura e lazer",
    "Saúde e bem-estar", "Carreira", "Sociedade"
)

/** Values of the "status" field in events/{id} (kept in Portuguese, as in the plan). */
object EventStatus {
    const val PENDING = "pendente"
    const val APPROVED = "aprovado"
    const val REJECTED = "rejeitado"
}

/** Values of the "timeSlot" field, used by the "Estou livre" filter. */
object TimeSlot {
    const val MORNING = "manha"
    const val AFTERNOON = "tarde"
    const val NIGHT = "noite"
}

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val format: String = "",
    val theme: String = "",
    val startAt: Timestamp? = null,
    val location: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val capacity: Int = 0,
    val freeSeats: Int = 0,
    val status: String = EventStatus.PENDING,
    val timeSlot: String = "",
    val isWeekend: Boolean = false
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Event = Event(
            id = doc.id,
            title = doc.getString("title").orEmpty(),
            description = doc.getString("description").orEmpty(),
            format = doc.getString("format").orEmpty(),
            theme = doc.getString("theme").orEmpty(),
            startAt = doc.getTimestamp("startAt"),
            location = doc.getString("location").orEmpty(),
            organizerId = doc.getString("organizerId").orEmpty(),
            organizerName = doc.getString("organizerName").orEmpty(),
            capacity = doc.getLong("capacity")?.toInt() ?: 0,
            freeSeats = doc.getLong("freeSeats")?.toInt() ?: 0,
            status = doc.getString("status") ?: EventStatus.PENDING,
            timeSlot = doc.getString("timeSlot").orEmpty(),
            isWeekend = doc.getBoolean("isWeekend") ?: false
        )
    }
}
