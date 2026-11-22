package pt.umaia.tapevent.pages.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.MAX_COMMENT_LENGTH
import pt.umaia.tapevent.core.model.MAX_RATING
import pt.umaia.tapevent.core.model.MIN_RATING
import pt.umaia.tapevent.core.repository.AttendanceRequiredException
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.EventRepository
import pt.umaia.tapevent.core.repository.FeedbackAlreadySentException
import pt.umaia.tapevent.core.repository.FeedbackRepository

class FeedbackViewModel(
    private val eventId: String,
    authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository(),
    private val feedbackRepository: FeedbackRepository = FeedbackRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    private val uid: String? = authRepository.currentUid

    init {
        load()
    }

    private fun load() {
        val uid = uid ?: return
        viewModelScope.launch {
            try {
                val event = eventRepository.observeEvent(eventId).first()
                val existing = feedbackRepository.getFeedback(eventId, uid)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        event = event,
                        existingFeedback = existing,
                        rating = existing?.rating ?: 0,
                        comment = existing?.comment.orEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Não foi possível carregar o evento.") }
            }
        }
    }

    fun onRatingChange(value: Int) =
        _uiState.update { it.copy(rating = value.coerceIn(MIN_RATING, MAX_RATING), errorMessage = null) }

    fun onCommentChange(value: String) =
        _uiState.update { it.copy(comment = value.take(MAX_COMMENT_LENGTH)) }

    fun submit() {
        val uid = uid ?: return
        val state = _uiState.value
        val event = state.event ?: return
        if (state.rating !in MIN_RATING..MAX_RATING) {
            _uiState.update { it.copy(errorMessage = "Escolhe uma nota de 1 a 5.") }
            return
        }

        _uiState.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                feedbackRepository.submitFeedback(
                    eventId = eventId,
                    uid = uid,
                    organizerId = event.organizerId,
                    rating = state.rating,
                    comment = state.comment.trim()
                )
                _uiState.update { it.copy(isSending = false, sent = true) }
            } catch (e: Exception) {
                val text = when (e.cause ?: e) {
                    is AttendanceRequiredException -> "Só podes dar feedback depois de confirmares a presença."
                    is FeedbackAlreadySentException -> "Já deste feedback a este evento."
                    else -> "Algo correu mal. Tenta outra vez."
                }
                _uiState.update { it.copy(isSending = false, errorMessage = text) }
            }
        }
    }

    companion object {
        fun factory(eventId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { FeedbackViewModel(eventId) }
        }
    }
}
