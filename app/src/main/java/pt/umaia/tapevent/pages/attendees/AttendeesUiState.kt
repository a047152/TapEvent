package pt.umaia.tapevent.pages.attendees

import pt.umaia.tapevent.core.model.Event

/** One student on the list. */
data class Attendee(
    val uid: String,
    val name: String,
    val email: String,
    val attended: Boolean
)

data class AttendeesUiState(
    val isLoading: Boolean = true,
    /** The organizer's approved events, for the chips at the top. */
    val events: List<Event> = emptyList(),
    val selectedEventId: String? = null,
    /** Students holding a seat ("inscrito" or "presente"), sorted by name. */
    val attendees: List<Attendee> = emptyList(),
    /** Students who asked "avisar-me se abrirem vagas". */
    val waitingCount: Int = 0,
    val hasStarted: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedEvent: Event? get() = events.firstOrNull { it.id == selectedEventId }
    val attendedCount: Int get() = attendees.count { it.attended }
}
