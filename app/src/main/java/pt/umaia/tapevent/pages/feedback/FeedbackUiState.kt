package pt.umaia.tapevent.pages.feedback

import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.Feedback

data class FeedbackUiState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    /** 0 = no star picked yet. */
    val rating: Int = 0,
    val comment: String = "",
    /** Feedback already sent earlier: the screen shows it read-only. */
    val existingFeedback: Feedback? = null,
    val isSending: Boolean = false,
    /** True after sending; the screen goes back. */
    val sent: Boolean = false,
    val errorMessage: String? = null
)
