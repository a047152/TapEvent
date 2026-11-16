package pt.umaia.tapevent.pages.createevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.repository.APP_ZONE
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.EventRepository
import pt.umaia.tapevent.core.repository.NewEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class CreateEventViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun onFormatSelect(value: String) = _uiState.update { it.copy(format = value, errorMessage = null) }
    fun onThemeSelect(value: String) = _uiState.update { it.copy(theme = value, errorMessage = null) }
    fun onDateSelect(value: LocalDate) = _uiState.update { it.copy(date = value, errorMessage = null) }
    fun onTimeSelect(value: LocalTime) = _uiState.update { it.copy(time = value, errorMessage = null) }
    fun onLocationChange(value: String) = _uiState.update { it.copy(location = value, errorMessage = null) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value, errorMessage = null) }

    fun increaseCapacity() = _uiState.update { it.copy(capacity = it.capacity + CAPACITY_STEP) }
    fun decreaseCapacity() = _uiState.update {
        it.copy(capacity = (it.capacity - CAPACITY_STEP).coerceAtLeast(MIN_CAPACITY))
    }

    fun submit() {
        val state = _uiState.value
        validate(state)?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
            return
        }

        val uid = authRepository.currentUid ?: run {
            _uiState.update { it.copy(errorMessage = "Your session expired. Please sign in again.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val organizer = authRepository.getUser(uid)
                val newEvent = NewEvent(
                    title = state.title.trim(),
                    description = state.description.trim(),
                    format = state.format!!,
                    theme = state.theme!!,
                    start = startDateTime(state)!!,
                    location = state.location.trim(),
                    capacity = state.capacity
                )
                eventRepository.createEvent(newEvent, uid, organizer?.name.orEmpty())
                _uiState.update { it.copy(isSaving = false, submitted = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Could not save the event.")
                }
            }
        }
    }

    private fun startDateTime(state: CreateEventUiState): ZonedDateTime? {
        val date = state.date ?: return null
        val time = state.time ?: return null
        return ZonedDateTime.of(date, time, APP_ZONE)
    }

    private fun validate(state: CreateEventUiState): String? {
        val start = startDateTime(state)
        return when {
            state.title.isBlank() -> "Give the event a title."
            state.format == null -> "Choose a format."
            state.theme == null -> "Choose a theme."
            state.date == null -> "Choose a date."
            state.time == null -> "Choose a time."
            start != null && start.isBefore(ZonedDateTime.now(APP_ZONE)) -> "The event must be in the future."
            state.location.isBlank() -> "Enter a location."
            state.description.isBlank() -> "Write the event description."
            else -> null
        }
    }
}
