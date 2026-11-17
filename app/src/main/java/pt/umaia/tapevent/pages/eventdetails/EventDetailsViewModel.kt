package pt.umaia.tapevent.pages.eventdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.Registration
import pt.umaia.tapevent.core.model.RegistrationStatus
import pt.umaia.tapevent.core.repository.AlreadyRegisteredException
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.EventNotAvailableException
import pt.umaia.tapevent.core.repository.EventRepository
import pt.umaia.tapevent.core.repository.NoSeatsLeftException
import pt.umaia.tapevent.core.repository.RegistrationRepository

class EventDetailsViewModel(
    private val eventId: String,
    private val authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository(),
    private val registrationRepository: RegistrationRepository = RegistrationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailsUiState())
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    private val uid: String? = authRepository.currentUid

    init {
        observe()
    }

    /** Listens to the event and to this student's registration at the same time. */
    private fun observe() {
        val uid = uid ?: return
        viewModelScope.launch {
            combine(
                eventRepository.observeEvent(eventId),
                registrationRepository.observeRegistration(eventId, uid)
            ) { event, registration -> event to registration }
                .catch {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Não foi possível carregar o evento.") }
                }
                .collect { (event, registration) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            event = event,
                            registration = registration,
                            action = actionFor(event, registration),
                            errorMessage = if (event == null) "Este evento já não existe." else null
                        )
                    }
                }
        }
    }

    /** Called by the main button. */
    fun onMainAction() {
        when (_uiState.value.action) {
            EventAction.REGISTER -> register()
            EventAction.NOTIFY_ME -> requestNotification()
            EventAction.NOTIFY_REQUESTED -> cancelNotification()
            EventAction.VIEW_TICKET, EventAction.STARTED -> Unit // handled by the screen
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    private fun register() = runAction(successMessage = "Vaga garantida!") { uid ->
        registrationRepository.register(eventId, uid)
    }

    private fun requestNotification() = runAction(successMessage = "Vais ser avisado se abrir uma vaga.") { uid ->
        val event = _uiState.value.event ?: return@runAction
        registrationRepository.requestNotification(eventId, uid, event.organizerId, event.startAt)
    }

    private fun cancelNotification() = runAction(successMessage = "Já não vais ser avisado.") { uid ->
        registrationRepository.cancelNotification(eventId, uid)
    }

    private fun runAction(successMessage: String, block: suspend (uid: String) -> Unit) {
        val uid = uid ?: return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val message = try {
                block(uid)
                successMessage
            } catch (e: Exception) {
                // A transaction wraps our exceptions, so look at the cause too
                when (e.cause ?: e) {
                    is NoSeatsLeftException -> "As vagas acabaram de esgotar."
                    is AlreadyRegisteredException -> "Já tens vaga neste evento."
                    is EventNotAvailableException -> "Este evento já não aceita inscrições."
                    else -> "Algo correu mal. Tenta outra vez."
                }
            }
            _uiState.update { it.copy(isWorking = false, message = message) }
        }
    }

    private fun actionFor(event: Event?, registration: Registration?): EventAction {
        val started = event?.startAt?.let { it <= Timestamp.now() } ?: false
        return when {
            registration?.hasSeat == true -> EventAction.VIEW_TICKET
            started -> EventAction.STARTED
            (event?.freeSeats ?: 0) > 0 -> EventAction.REGISTER
            registration?.status == RegistrationStatus.NOTIFY -> EventAction.NOTIFY_REQUESTED
            else -> EventAction.NOTIFY_ME
        }
    }

    companion object {
        /** Lets the screen create this ViewModel with the eventId from the route. */
        fun factory(eventId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { EventDetailsViewModel(eventId) }
        }
    }
}
