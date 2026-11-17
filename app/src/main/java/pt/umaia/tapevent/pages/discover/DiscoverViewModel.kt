package pt.umaia.tapevent.pages.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.repository.APP_ZONE
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.EventRepository
import pt.umaia.tapevent.core.util.DateFormats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

class DiscoverViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    /** Every approved event, as received from Firestore. Filters are applied on top of it. */
    private var allEvents: List<Event> = emptyList()

    init {
        loadUserName()
        observeEvents()
    }

    private fun loadUserName() {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            runCatching { authRepository.getUser(uid) }
                .getOrNull()
                ?.let { user -> _uiState.update { it.copy(userName = user.name) } }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            eventRepository.observeApprovedEvents()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Não foi possível carregar os eventos.")
                    }
                }
                .collect { events ->
                    allEvents = events
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    refresh()
                }
        }
    }

    fun toggleTheme(theme: String) = updateFilters { f ->
        f.copy(themes = if (theme in f.themes) f.themes - theme else f.themes + theme)
    }

    fun toggleTimeSlot(slot: String) = updateFilters { f ->
        f.copy(timeSlots = if (slot in f.timeSlots) f.timeSlots - slot else f.timeSlots + slot)
    }

    fun toggleWeekend() = updateFilters { f -> f.copy(weekendOnly = !f.weekendOnly) }

    /** Tapping the selected day again removes the day filter. */
    fun selectDay(day: LocalDate) = updateFilters { f ->
        f.copy(selectedDay = if (f.selectedDay == day) null else day)
    }

    fun clearFilters() = updateFilters { DiscoverFilters() }

    private fun updateFilters(change: (DiscoverFilters) -> DiscoverFilters) {
        _uiState.update { it.copy(filters = change(it.filters)) }
        refresh()
    }

    /** Recomputes the week strip and the two lists from allEvents + filters. */
    private fun refresh() {
        val now = ZonedDateTime.now(APP_ZONE)
        val today = now.toLocalDate()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val filters = _uiState.value.filters

        val upcoming = allEvents.filter { event ->
            val start = event.startAt?.let(DateFormats::toZoned) ?: return@filter false
            start.isAfter(now)
        }

        val countByDay = upcoming.groupingBy { DateFormats.toZoned(it.startAt!!).toLocalDate() }.eachCount()
        val weekDays = (0L..6L).map { offset ->
            val date = monday.plusDays(offset)
            WeekDay(date = date, isToday = date == today, eventCount = countByDay[date] ?: 0)
        }

        val visible = upcoming.filter { it.matches(filters) }
        val (thisWeek, later) = visible.partition {
            !DateFormats.toZoned(it.startAt!!).toLocalDate().isAfter(sunday)
        }

        _uiState.update {
            it.copy(today = today, weekDays = weekDays, thisWeek = thisWeek, later = later)
        }
    }

    private fun Event.matches(filters: DiscoverFilters): Boolean {
        val day = DateFormats.toZoned(startAt!!).toLocalDate()
        return (filters.themes.isEmpty() || theme in filters.themes) &&
                (filters.timeSlots.isEmpty() || timeSlot in filters.timeSlots) &&
                (!filters.weekendOnly || isWeekend) &&
                (filters.selectedDay == null || day == filters.selectedDay)
    }
}
