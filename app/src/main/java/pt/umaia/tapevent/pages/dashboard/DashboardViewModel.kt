package pt.umaia.tapevent.pages.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.EventStatus
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.EventRepository
import pt.umaia.tapevent.core.repository.FeedbackRepository

class DashboardViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository(),
    private val feedbackRepository: FeedbackRepository = FeedbackRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val uid: String? = authRepository.currentUid

    init {
        loadName()
        observe()
    }

    private fun loadName() {
        val uid = uid ?: return
        viewModelScope.launch {
            runCatching { authRepository.getUser(uid) }.getOrNull()
                ?.let { user -> _uiState.update { it.copy(organizerName = user.name) } }
        }
    }

    private fun observe() {
        val uid = uid ?: return
        viewModelScope.launch {
            combine(
                eventRepository.observeOrganizerEvents(uid),
                feedbackRepository.observeOrganizerFeedback(uid)
            ) { events, feedback -> events to feedback }
                .catch {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Não foi possível carregar os teus eventos.") }
                }
                .collect { (events, feedback) ->
                    val now = Timestamp.now()
                    val ratingsByEvent = feedback.groupBy({ it.eventId }, { it.rating })

                    val published = events
                        .filter { it.status == EventStatus.APPROVED }
                        .map { event ->
                            val ratings = ratingsByEvent[event.id].orEmpty()
                            PublishedEvent(
                                event = event,
                                hasStarted = event.startAt?.let { it <= now } ?: false,
                                registeredCount = (event.capacity - event.freeSeats).coerceAtLeast(0),
                                averageRating = ratings.takeIf { it.isNotEmpty() }?.average(),
                                feedbackCount = ratings.size
                            )
                        }
                        // Upcoming (soonest first), then held (most recent first)
                        .sortedWith(
                            compareBy<PublishedEvent> { it.hasStarted }
                                .thenBy { if (it.hasStarted) -(it.event.startAt?.seconds ?: 0L) else it.event.startAt?.seconds ?: 0L }
                        )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pending = events.filter { e -> e.status == EventStatus.PENDING },
                            published = published,
                            rejected = events.filter { e -> e.status == EventStatus.REJECTED },
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
