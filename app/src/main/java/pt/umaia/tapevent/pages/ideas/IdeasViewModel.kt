package pt.umaia.tapevent.pages.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.umaia.tapevent.core.model.Idea
import pt.umaia.tapevent.core.model.MAX_IDEA_TITLE_LENGTH
import pt.umaia.tapevent.core.repository.AuthRepository
import pt.umaia.tapevent.core.repository.IdeaRepository
import pt.umaia.tapevent.core.repository.VotingClosedException

class IdeasViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val ideaRepository: IdeaRepository = IdeaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdeasUiState())
    val uiState: StateFlow<IdeasUiState> = _uiState.asStateFlow()

    private val uid: String? = authRepository.currentUid

    init {
        observe()
    }

    private fun observe() {
        val uid = uid ?: return
        viewModelScope.launch {
            combine(
                ideaRepository.observeApprovedIdeas(),
                ideaRepository.observeMyVotedIdeaIds(uid),
                ideaRepository.observeMyIdeas(uid)
            ) { open, votes, mine -> Triple(open, votes, mine) }
                .catch { _uiState.update { it.copy(isLoading = false, errorMessage = "Não foi possível carregar as ideias.") } }
                .collect { (open, votes, mine) ->
                    _uiState.update {
                        it.copy(isLoading = false, openIdeas = open, myVotes = votes, myIdeas = mine, errorMessage = null)
                    }
                }
        }
    }

    fun toggleVote(idea: Idea) {
        val uid = uid ?: return
        if (idea.id in _uiState.value.votingIds) return
        _uiState.update { it.copy(votingIds = it.votingIds + idea.id) }
        viewModelScope.launch {
            val error = try {
                ideaRepository.toggleVote(idea.id, uid)
                null
            } catch (e: Exception) {
                if ((e.cause ?: e) is VotingClosedException) "A votação desta ideia já fechou."
                else "Algo correu mal. Tenta outra vez."
            }
            _uiState.update { it.copy(votingIds = it.votingIds - idea.id, message = error) }
        }
    }

    fun onTitleChange(value: String) =
        _uiState.update { it.copy(newTitle = value.take(MAX_IDEA_TITLE_LENGTH), formError = null) }

    fun onThemeSelect(value: String) = _uiState.update { it.copy(newTheme = value, formError = null) }

    fun propose() {
        val uid = uid ?: return
        val state = _uiState.value
        val error = when {
            state.newTitle.isBlank() -> "Escreve o nome do evento."
            state.newTheme == null -> "Escolhe um tema."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(formError = error) }
            return
        }

        _uiState.update { it.copy(isProposing = true, formError = null) }
        viewModelScope.launch {
            try {
                val name = runCatching { authRepository.getUser(uid)?.name }.getOrNull().orEmpty()
                ideaRepository.proposeIdea(state.newTitle.trim(), state.newTheme!!, uid, name)
                _uiState.update {
                    it.copy(
                        isProposing = false,
                        newTitle = "",
                        newTheme = null,
                        message = "Ideia enviada. Só abre a votação depois de a administração aprovar."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProposing = false, formError = "Não foi possível enviar a ideia.") }
            }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }
}
