package pt.umaia.tapevent.pages.dashboard

import pt.umaia.tapevent.core.model.Event

/** A published (approved) event with what the Dashboard shows about it. */
data class PublishedEvent(
    val event: Event,
    val hasStarted: Boolean,
    /** Seats taken = capacity - freeSeats. */
    val registeredCount: Int,
    /** Average rating, or null when nobody gave feedback yet. */
    val averageRating: Double?,
    val feedbackCount: Int
) {
    /** 0f..1f, for the occupancy bar. */
    val occupancy: Float
        get() = if (event.capacity > 0) registeredCount.toFloat() / event.capacity else 0f
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val organizerName: String = "",
    /** Waiting for the administration. */
    val pending: List<Event> = emptyList(),
    /** Approved: upcoming first, then the ones already held. */
    val published: List<PublishedEvent> = emptyList(),
    val rejected: List<Event> = emptyList(),
    val errorMessage: String? = null
) {
    val publishedCount: Int get() = published.size
    val totalRegistered: Int get() = published.sumOf { it.registeredCount }
}
