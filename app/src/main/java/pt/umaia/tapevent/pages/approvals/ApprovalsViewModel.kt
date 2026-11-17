package pt.umaia.tapevent.pages.approvals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.EventRepository

class ApprovalsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApprovalsUiState())
    val uiState: StateFlow<ApprovalsUiState> = _uiState.asStateFlow()

    init {
        observePendingEvents()
    }

    private fun observePendingEvents() {
        viewModelScope.launch {
            eventRepository.observePendingEvents()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Não foi possível carregar os eventos.")
                    }
                }
                .collect { events ->
                    _uiState.update { it.copy(isLoading = false, pendingEvents = events, errorMessage = null) }
                }
        }
    }

    fun approve(event: Event) = review(event, approve = true)

    /** Opens the confirmation dialog; the event is only rejected after confirmRejection(). */
    fun askToReject(event: Event) = _uiState.update { it.copy(eventToReject = event) }

    fun cancelRejection() = _uiState.update { it.copy(eventToReject = null) }

    fun confirmRejection() {
        val event = _uiState.value.eventToReject ?: return
        _uiState.update { it.copy(eventToReject = null) }
        review(event, approve = false)
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    private fun review(event: Event, approve: Boolean) {
        val adminUid = authRepository.currentUid ?: return
        _uiState.update { it.copy(processingIds = it.processingIds + event.id) }

        viewModelScope.launch {
            try {
                eventRepository.reviewEvent(event.id, approve, adminUid)
                val text = if (approve) "Evento aprovado. Já aparece em Descobrir." else "Evento rejeitado."
                // The live listener removes the event from the list automatically
                _uiState.update { it.copy(processingIds = it.processingIds - event.id, message = text) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingIds = it.processingIds - event.id,
                        message = "Não foi possível atualizar o evento."
                    )
                }
            }
        }
    }
}
