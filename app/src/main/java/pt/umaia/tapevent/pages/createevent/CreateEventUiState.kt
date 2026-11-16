package pt.umaia.tapevent.pages.createevent

import java.time.LocalDate
import java.time.LocalTime

const val MIN_CAPACITY = 5
const val CAPACITY_STEP = 5

data class CreateEventUiState(
    val title: String = "",
    val format: String? = null,
    val theme: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val location: String = "",
    val description: String = "",
    val capacity: Int = 20,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** True after the event was saved; the screen goes back. */
    val submitted: Boolean = false
)