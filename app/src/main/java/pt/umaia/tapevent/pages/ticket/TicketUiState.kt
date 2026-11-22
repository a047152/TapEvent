package pt.umaia.tapevent.pages.ticket

import androidx.compose.ui.graphics.ImageBitmap
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.Registration

data class TicketUiState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val registration: Registration? = null,
    val qrCode: ImageBitmap? = null,
    /** Cancelling is allowed only before the event starts. */
    val canCancel: Boolean = false,
    val showCancelDialog: Boolean = false,
    val isCancelling: Boolean = false,
    /** True after the seat was given back; the screen goes back. */
    val cancelled: Boolean = false,
    /** One-time message shown as a Toast. */
    val message: String? = null
) {
    /** The student holds a seat (inscrito or presente). */
    val hasTicket: Boolean
        get() = event != null && registration?.hasSeat == true
}
