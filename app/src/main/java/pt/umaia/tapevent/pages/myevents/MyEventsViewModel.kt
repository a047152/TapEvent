package pt.umaia.tapevent.pages.myevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.Registration
import pt.umaia.tapevent.core.repository.AttendanceNotOpenException
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.EventRepository
import pt.umaia.tapevent.core.repository.RegistrationRepository

class MyEventsViewModel(
    authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository(),
    private val registrationRepository: RegistrationRepository = RegistrationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyEventsUiState())
    val uiState: StateFlow<MyEventsUiState> = _uiState.asStateFlow()

    private val uid: String? = authRepository.currentUid

    private var events: List<Event> = emptyList()
    private var registrations: List<Registration> = emptyList()

    init {
        observe()
        refreshEveryMinute()
    }

    /** Registrations of this student joined with the approved events, on the device. */
    private fun observe() {
        val uid = uid ?: return
        viewModelScope.launch {
            combine(
                registrationRepository.observeMyRegistrations(uid),
                eventRepository.observeApprovedEvents()
            ) { regs, evs -> regs to evs }
                .catch {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Não foi possível carregar os teus eventos.") }
                }
                .collect { (regs, evs) ->
                    registrations = regs
                    events = evs
                    rebuild()
                }
        }
    }

    /** An event can start while the screen is open: re-check every minute so "Confirmar presença" appears. */
    private fun refreshEveryMinute() {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                rebuild()
            }
        }
    }

    private fun rebuild() {
        val now = Timestamp.now()
        val eventsById = events.associateBy { it.id }
        val items = registrations.mapNotNull { registration ->
            val event = eventsById[registration.eventId] ?: return@mapNotNull null
            val started = event.startAt?.let { it <= now } ?: false
            MyEventItem(event, registration, started)
        }
        val upcoming = items.filter { !it.hasStarted }
            .sortedBy { it.event.startAt?.seconds ?: Long.MAX_VALUE }
        // A "notify me" request for an event that already started is no longer useful
        val past = items.filter { it.hasStarted && !it.isNotifyRequest }
            .sortedByDescending { it.event.startAt?.seconds ?: 0L }

        _uiState.update {
            it.copy(isLoading = false, upcoming = upcoming, past = past, errorMessage = null)
        }
    }

    fun confirmAttendance(item: MyEventItem) {
        val uid = uid ?: return
        val eventId = item.event.id
        _uiState.update { it.copy(confirmingIds = it.confirmingIds + eventId) }
        viewModelScope.launch {
            val text = try {
                registrationRepository.confirmAttendance(eventId, uid)
                "Presença confirmada. Obrigado!"
            } catch (e: Exception) {
                if ((e.cause ?: e) is AttendanceNotOpenException)
                    "Só podes confirmar a presença depois de o evento começar."
                else
                    "Algo correu mal. Tenta outra vez."
            }
            _uiState.update { it.copy(confirmingIds = it.confirmingIds - eventId, message = text) }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }
}
