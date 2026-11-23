package pt.umaia.tapevent.pages.attendees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.EventStatus
import pt.umaia.tapevent.core.model.RegistrationStatus
import pt.umaia.tapevent.core.model.User
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.EventRepository
import pt.umaia.tapevent.core.repository.RegistrationRepository

class AttendeesViewModel(
    initialEventId: String,
    private val authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository(),
    private val registrationRepository: RegistrationRepository = RegistrationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendeesUiState(selectedEventId = initialEventId))
    val uiState: StateFlow<AttendeesUiState> = _uiState.asStateFlow()

    /** Names already read from users/{uid}, so switching events does not read them again. */
    private val userCache = mutableMapOf<String, User>()
    private var registrationsJob: Job? = null

    init {
        observeEvents()
        observeRegistrations(initialEventId)
    }

    private fun observeEvents() {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            eventRepository.observeOrganizerEvents(uid)
                .catch { _uiState.update { it.copy(errorMessage = "Não foi possível carregar os teus eventos.") } }
                .collect { events ->
                    _uiState.update { state ->
                        val approved = events.filter { it.status == EventStatus.APPROVED }
                        val selected = approved.firstOrNull { it.id == state.selectedEventId }
                        state.copy(
                            events = approved,
                            hasStarted = selected?.startAt?.let { it <= Timestamp.now() } ?: false
                        )
                    }
                }
        }
    }

    fun selectEvent(eventId: String) {
        if (eventId == _uiState.value.selectedEventId) return
        val event = _uiState.value.events.firstOrNull { it.id == eventId }
        _uiState.update {
            it.copy(
                selectedEventId = eventId,
                isLoading = true,
                attendees = emptyList(),
                waitingCount = 0,
                hasStarted = event?.startAt?.let { start -> start <= Timestamp.now() } ?: false
            )
        }
        observeRegistrations(eventId)
    }

    /** Stops listening to the previous event and starts on the new one. */
    private fun observeRegistrations(eventId: String) {
        registrationsJob?.cancel()
        registrationsJob = viewModelScope.launch {
            registrationRepository.observeEventRegistrations(eventId)
                .catch {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Não foi possível carregar os inscritos.") }
                }
                .collect { registrations ->
                    val withSeat = registrations.filter { it.hasSeat }
                    val missing = withSeat.map { it.uid }.filter { it !in userCache }
                    if (missing.isNotEmpty()) {
                        runCatching { authRepository.getUsers(missing) }.getOrNull()?.let(userCache::putAll)
                    }
                    val attendees = withSeat.map { registration ->
                        val user = userCache[registration.uid]
                        Attendee(
                            uid = registration.uid,
                            name = user?.name?.ifBlank { null } ?: "Aluno",
                            email = user?.email.orEmpty(),
                            attended = registration.status == RegistrationStatus.ATTENDED
                        )
                    }.sortedBy { it.name.lowercase() }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            attendees = attendees,
                            waitingCount = registrations.count { r -> r.status == RegistrationStatus.NOTIFY },
                            errorMessage = null
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(eventId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { AttendeesViewModel(eventId) }
        }
    }
}
