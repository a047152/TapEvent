package pt.umaia.tapevent.pages.alerts

import pt.umaia.tapevent.core.model.AlertSettings

data class AlertsUiState(
    val isLoading: Boolean = true,
    val interests: Set<String> = emptySet(),
    val alerts: AlertSettings = AlertSettings(),
    /** One-time message shown as a Toast (only for errors). */
    val message: String? = null
)
