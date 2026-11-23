package pt.umaia.tapevent.pages.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.Friendship
import pt.umaia.tapevent.core.model.FriendshipStatus
import pt.umaia.tapevent.core.repository.AlreadyFriendsException
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.FriendRepository
import pt.umaia.tapevent.core.repository.FriendRequestResult
import pt.umaia.tapevent.core.repository.NotAStudentException
import pt.umaia.tapevent.core.repository.RequestAlreadySentException
import pt.umaia.tapevent.core.repository.SelfFriendException
import pt.umaia.tapevent.core.repository.StudentNotFoundException
import pt.umaia.tapevent.core.util.EmailRules

class FriendsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val friendRepository: FriendRepository = FriendRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val uid: String? = authRepository.currentUid

    init {
        observe()
    }

    private fun observe() {
        val uid = uid ?: return
        viewModelScope.launch {
            friendRepository.observeFriendships(uid)
                .catch { _uiState.update { it.copy(isLoading = false, errorMessage = "Não foi possível carregar os amigos.") } }
                .collect { list ->
                    fun Friendship.toItem() = FriendItem(id, otherName(uid), otherEmail(uid))
                    val pending = list.filter { it.status == FriendshipStatus.PENDING }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            received = pending.filter { f -> f.requesterId != uid }.map { f -> f.toItem() },
                            sent = pending.filter { f -> f.requesterId == uid }.map { f -> f.toItem() },
                            friends = list.filter { f -> f.status == FriendshipStatus.ACCEPTED }
                                .map { f -> f.toItem() }
                                .sortedBy { f -> f.name.lowercase() },
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(emailInput = value.trim(), formError = null) }

    fun sendRequest() {
        val uid = uid ?: return
        val email = _uiState.value.emailInput.lowercase()
        val error = when {
            !EmailRules.isValidFormat(email) -> "Escreve um email válido."
            !EmailRules.isInstitutional(email) -> "Usa o email institucional do teu colega."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(formError = error) }
            return
        }

        _uiState.update { it.copy(isSending = true, formError = null) }
        viewModelScope.launch {
            try {
                val me = authRepository.getUser(uid)
                val result = friendRepository.sendRequest(uid, me?.name.orEmpty(), me?.email.orEmpty(), email)
                val text = when (result) {
                    FriendRequestResult.SENT -> "Pedido enviado."
                    FriendRequestResult.ACCEPTED -> "Já tinhas um pedido desta pessoa. Agora são amigos!"
                }
                _uiState.update { it.copy(isSending = false, emailInput = "", message = text) }
            } catch (e: Exception) {
                val text = when (e.cause ?: e) {
                    is StudentNotFoundException -> "Não existe nenhum estudante com este email."
                    is NotAStudentException -> "Só podes adicionar estudantes."
                    is SelfFriendException -> "Não te podes adicionar a ti próprio."
                    is AlreadyFriendsException -> "Já são amigos."
                    is RequestAlreadySentException -> "Já enviaste um pedido a esta pessoa."
                    else -> "Algo correu mal. Tenta outra vez."
                }
                _uiState.update { it.copy(isSending = false, formError = text) }
            }
        }
    }

    fun accept(item: FriendItem) = run(item, "Pedido aceite.") { friendRepository.accept(item.friendshipId) }
    fun decline(item: FriendItem) = run(item, "Pedido recusado.") { friendRepository.remove(item.friendshipId) }
    fun cancelSent(item: FriendItem) = run(item, "Pedido cancelado.") { friendRepository.remove(item.friendshipId) }

    fun askToRemove(item: FriendItem) = _uiState.update { it.copy(friendToRemove = item) }
    fun dismissRemove() = _uiState.update { it.copy(friendToRemove = null) }
    fun confirmRemove() {
        val item = _uiState.value.friendToRemove ?: return
        _uiState.update { it.copy(friendToRemove = null) }
        run(item, "Amigo removido.") { friendRepository.remove(item.friendshipId) }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    private fun run(item: FriendItem, successMessage: String, block: suspend () -> Unit) {
        _uiState.update { it.copy(processingIds = it.processingIds + item.friendshipId) }
        viewModelScope.launch {
            val text = try {
                block()
                successMessage
            } catch (e: Exception) {
                "Algo correu mal. Tenta outra vez."
            }
            _uiState.update { it.copy(processingIds = it.processingIds - item.friendshipId, message = text) }
        }
    }
}
