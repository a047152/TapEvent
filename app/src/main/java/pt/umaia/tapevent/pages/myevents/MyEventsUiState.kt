package pt.umaia.tapevent.pages.myevents

import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.Registration
import pt.umaia.tapevent.core.model.RegistrationStatus

/** One row of "Os meus eventos": the event plus this student's registration. */
data class MyEventItem(
    val event: Event,
    val registration: Registration,
    val hasStarted: Boolean
) {
    val isNotifyRequest: Boolean get() = registration.status == RegistrationStatus.NOTIFY
    val attended: Boolean get() = registration.status == RegistrationStatus.ATTENDED

    /** "Confirmar presença" shows from the start time while the status is still "inscrito". */
    val canConfirmAttendance: Boolean
        get() = hasStarted && registration.status == RegistrationStatus.REGISTERED
}

data class MyEventsUiState(
    val isLoading: Boolean = true,
    /** Not started yet (seats and "notify me" requests), soonest first. */
    val upcoming: List<MyEventItem> = emptyList(),
    /** Already started (only with a seat), most recent first. */
    val past: List<MyEventItem> = emptyList(),
    /** Events whose "Confirmar presença" is being saved. */
    val confirmingIds: Set<String> = emptySet(),
    val message: String? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean get() = upcoming.isEmpty() && past.isEmpty()
}
