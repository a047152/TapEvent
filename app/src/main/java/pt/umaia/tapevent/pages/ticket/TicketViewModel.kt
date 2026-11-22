package pt.umaia.tapevent.pages.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.umaia.tapevent.core.model.RegistrationStatus
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.CannotCancelException
import pt.umaia.tapevent.core.repository.EventRepository
import pt.umaia.tapevent.core.repository.RegistrationRepository
import pt.umaia.tapevent.core.util.QrCode

class TicketViewModel(
    private val eventId: String,
    authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository(),
    private val registrationRepository: RegistrationRepository = RegistrationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketUiState())
    val uiState: StateFlow<TicketUiState> = _uiState.asStateFlow()

    private val uid: String? = authRepository.currentUid

    init {
        observe()
    }

    private fun observe() {
        val uid = uid ?: return
        viewModelScope.launch {
            combine(
                eventRepository.observeEvent(eventId),
                registrationRepository.observeRegistration(eventId, uid)
            ) { event, registration -> event to registration }
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { (event, registration) ->
                    val started = event?.startAt?.let { it <= Timestamp.now() } ?: true
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            event = event,
                            registration = registration,
                            canCancel = registration?.status == RegistrationStatus.REGISTERED && !started
                        )
                    }
                    // The QR code only depends on the registration id, so it is generated once
                    if (registration != null && _uiState.value.qrCode == null) {
                        val qr = withContext(Dispatchers.Default) {
                            QrCode.generate(QrCode.ticketContent(registration.id))
                        }
                        _uiState.update { it.copy(qrCode = qr) }
                    }
                }
        }
    }

    fun askToCancel() = _uiState.update { it.copy(showCancelDialog = true) }
    fun dismissCancel() = _uiState.update { it.copy(showCancelDialog = false) }
    fun messageShown() = _uiState.update { it.copy(message = null) }

    fun confirmCancel() {
        val uid = uid ?: return
        _uiState.update { it.copy(showCancelDialog = false, isCancelling = true) }
        viewModelScope.launch {
            try {
                registrationRepository.cancelRegistration(eventId, uid)
                _uiState.update {
                    it.copy(isCancelling = false, cancelled = true, message = "Inscrição cancelada. A vaga ficou livre.")
                }
            } catch (e: Exception) {
                val text = if ((e.cause ?: e) is CannotCancelException)
                    "Já não é possível cancelar esta inscrição."
                else
                    "Algo correu mal. Tenta outra vez."
                _uiState.update { it.copy(isCancelling = false, message = text) }
            }
        }
    }

    companion object {
        fun factory(eventId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { TicketViewModel(eventId) }
        }
    }
}
