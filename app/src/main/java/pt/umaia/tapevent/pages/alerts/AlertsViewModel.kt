package pt.umaia.tapevent.pages.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.AlertSettings
import pt.umaia.tapevent.core.model.EVENT_THEMES
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.PreferencesRepository

class AlertsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val preferencesRepository: PreferencesRepository = PreferencesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val uid: String? = authRepository.currentUid

    init {
        load()
    }

    private fun load() {
        val uid = uid ?: return
        viewModelScope.launch {
            val user = runCatching { authRepository.getUser(uid) }.getOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    interests = user?.interests.orEmpty().toSet(),
                    alerts = user?.alerts ?: AlertSettings(),
                    message = if (user == null) "Não foi possível carregar as tuas preferências." else null
                )
            }
        }
    }

    /** Changes are shown at once and saved in the background; on error the old value comes back. */
    fun toggleInterest(theme: String) {
        val uid = uid ?: return
        val before = _uiState.value.interests
        val after = if (theme in before) before - theme else before + theme
        _uiState.update { it.copy(interests = after) }

        viewModelScope.launch {
            try {
                // Keep the same order as the theme list
                preferencesRepository.saveInterests(uid, EVENT_THEMES.filter { it in after })
            } catch (e: Exception) {
                _uiState.update { it.copy(interests = before, message = "Não foi possível guardar. Tenta outra vez.") }
            }
        }
    }

    fun setAlert(key: String, enabled: Boolean) {
        val uid = uid ?: return
        val before = _uiState.value.alerts
        val after = when (key) {
            AlertSettings.KEY_NEW -> before.copy(newEvents = enabled)
            AlertSettings.KEY_DAY -> before.copy(oneDayBefore = enabled)
            AlertSettings.KEY_HOUR -> before.copy(oneHourBefore = enabled)
            AlertSettings.KEY_FEW -> before.copy(fewSeats = enabled)
            else -> return
        }
        _uiState.update { it.copy(alerts = after) }

        viewModelScope.launch {
            try {
                preferencesRepository.saveAlert(uid, key, enabled)
            } catch (e: Exception) {
                _uiState.update { it.copy(alerts = before, message = "Não foi possível guardar. Tenta outra vez.") }
            }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }
}
