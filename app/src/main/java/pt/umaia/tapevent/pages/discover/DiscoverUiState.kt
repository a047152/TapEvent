package pt.umaia.tapevent.pages.discover

import pt.umaia.tapevent.core.model.Event
import java.time.LocalDate

/** Filters chosen by the student. Empty sets mean "no filter". */
data class DiscoverFilters(
    val themes: Set<String> = emptySet(),
    /** Values from TimeSlot: "manha", "tarde", "noite". */
    val timeSlots: Set<String> = emptySet(),
    val weekendOnly: Boolean = false,
    /** Day picked in the week strip, or null for any day. */
    val selectedDay: LocalDate? = null
) {
    val isActive: Boolean
        get() = themes.isNotEmpty() || timeSlots.isNotEmpty() || weekendOnly || selectedDay != null
}

/** One day of the week strip at the top of Discover. */
data class WeekDay(
    val date: LocalDate,
    val isToday: Boolean,
    val eventCount: Int
)

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val today: LocalDate = LocalDate.now(),
    val weekDays: List<WeekDay> = emptyList(),
    val filters: DiscoverFilters = DiscoverFilters(),
    /** Upcoming events that match the filters, until the end of this week. */
    val thisWeek: List<Event> = emptyList(),
    /** Upcoming events that match the filters, after this week. */
    val later: List<Event> = emptyList(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = thisWeek.isEmpty() && later.isEmpty()
}
