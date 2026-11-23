package pt.umaia.tapevent.pages.approvals

import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.Idea

data class ApprovalsUiState(
    val isLoading: Boolean = true,
    val pendingEvents: List<Event> = emptyList(),
    /** Student ideas waiting for approval before voting opens. */
    val pendingIdeas: List<Idea> = emptyList(),
    /** Events and ideas being approved/rejected right now (their buttons are disabled). */
    val processingIds: Set<String> = emptySet(),
    /** Event waiting for the "reject?" confirmation dialog. */
    val eventToReject: Event? = null,
    /** One-time message shown as a Toast (e.g. "Evento aprovado."). */
    val message: String? = null,
    val errorMessage: String? = null
)
