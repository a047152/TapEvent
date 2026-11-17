package pt.umaia.tapevent.pages.approvals

import pt.umaia.tapevent.core.model.Event

data class ApprovalsUiState(
    val isLoading: Boolean = true,
    val pendingEvents: List<Event> = emptyList(),
    /** Events being approved/rejected right now (their buttons are disabled). */
    val processingIds: Set<String> = emptySet(),
    /** Event waiting for the "reject?" confirmation dialog. */
    val eventToReject: Event? = null,
    /** One-time message shown as a Toast (e.g. "Event approved."). */
    val message: String? = null,
    val errorMessage: String? = null
)
