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
import pt.umaia.tapevent.core.model.Idea
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.EventRepository
import pt.umaia.tapevent.core.repository.IdeaRepository

class ApprovalsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository(),
    private val ideaRepository: IdeaRepository = IdeaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApprovalsUiState())
    val uiState: StateFlow<ApprovalsUiState> = _uiState.asStateFlow()

    init {
        observePendingEvents()
        observePendingIdeas()
    }

    private fun observePendingIdeas() {
        viewModelScope.launch {
            ideaRepository.observePendingIdeas()
                .catch { _uiState.update { it.copy(errorMessage = "Não foi possível carregar as ideias.") } }
                .collect { ideas -> _uiState.update { it.copy(pendingIdeas = ideas) } }
        }
    }

    /** "Aprovar votação" or "Rejeitar" on a student idea (no confirmation, as in the mockup). */
    fun reviewIdea(idea: Idea, approve: Boolean) {
        val adminUid = authRepository.currentUid ?: return
        _uiState.update { it.copy(processingIds = it.processingIds + idea.id) }
        viewModelScope.launch {
            val text = try {
                ideaRepository.reviewIdea(idea.id, approve, adminUid)
                if (approve) "Ideia aprovada. Os estudantes já podem votar." else "Ideia rejeitada."
            } catch (e: Exception) {
                "Não foi possível atualizar a ideia."
            }
            _uiState.update { it.copy(processingIds = it.processingIds - idea.id, message = text) }
        }
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
