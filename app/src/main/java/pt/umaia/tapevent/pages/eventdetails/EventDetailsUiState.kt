package pt.umaia.tapevent.pages.eventdetails

import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.Registration

/** What the main button at the bottom of the screen does. */
enum class EventAction {
    /** Has a seat: open the ticket. */
    VIEW_TICKET,
    /** Free seats available: take one. */
    REGISTER,
    /** No seats: ask to be notified. */
    NOTIFY_ME,
    /** Already asked to be notified. */
    NOTIFY_REQUESTED,
    /** Event already started: nothing to do. */
    STARTED
}

data class EventDetailsUiState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val registration: Registration? = null,
    val action: EventAction = EventAction.REGISTER,
    val isWorking: Boolean = false,
    /** One-time message shown as a Toast. */
    val message: String? = null,
    val errorMessage: String? = null
)
