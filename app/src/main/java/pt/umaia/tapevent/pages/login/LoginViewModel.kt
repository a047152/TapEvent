package pt.umaia.tapevent.pages.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.util.EmailRules
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.ProfileNotFoundException

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value.trim(), errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

    fun toggleMode() = _uiState.update {
        it.copy(isRegisterMode = !it.isRegisterMode, errorMessage = null)
    }

    fun submit() {
        val state = _uiState.value
        validate(state)?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val user = if (state.isRegisterMode) {
                    repository.register(state.name.trim(), state.email, state.password)
                } else {
                    repository.signIn(state.email, state.password)
                }
                _uiState.update { it.copy(isLoading = false, signedInRole = user.role) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    /** If the user is already signed in (app reopened), skip the form. */
    private fun restoreSession() {
        val uid = repository.currentUid ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val user = repository.getUser(uid)
                if (user != null) {
                    _uiState.update { it.copy(isLoading = false, signedInRole = user.role) }
                } else {
                    repository.signOut()
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    private fun validate(state: LoginUiState): String? = when {
        state.isRegisterMode && state.name.isBlank() -> "Escreve o teu nome."
        !EmailRules.isValidFormat(state.email) -> "Escreve um email válido."
        !EmailRules.isInstitutional(state.email) -> "Usa o teu email institucional UMAIA."
        state.password.length < 8 -> "A password tem de ter pelo menos 8 caracteres."
        else -> null
    }

    private fun Exception.toUserMessage(): String = when (this) {
        is FirebaseAuthWeakPasswordException -> "A password é demasiado fraca."
        is FirebaseAuthInvalidCredentialsException -> "Email ou password incorretos."
        is FirebaseAuthInvalidUserException -> "Não existe nenhuma conta com este email."
        is FirebaseAuthUserCollisionException -> "Já existe uma conta com este email."
        is FirebaseNetworkException -> "Sem ligação. Verifica a tua internet."
        is ProfileNotFoundException -> "O teu perfil não foi encontrado. Contacta a administração."
        else -> "Algo correu mal. Tenta outra vez."
    }
}
