package pt.umaia.tapevent.pages.ideas

import pt.umaia.tapevent.core.model.Idea

data class IdeasUiState(
    val isLoading: Boolean = true,
    /** Approved ideas, most voted first. */
    val openIdeas: List<Idea> = emptyList(),
    val myVotes: Set<String> = emptySet(),
    val myIdeas: List<Idea> = emptyList(),
    /** Ideas whose vote is being saved. */
    val votingIds: Set<String> = emptySet(),
    // "Tens uma ideia?" form
    val newTitle: String = "",
    val newTheme: String? = null,
    val isProposing: Boolean = false,
    val formError: String? = null,
    val message: String? = null,
    val errorMessage: String? = null
) {
    /** Highest vote count, for the length of the bars. */
    val maxVotes: Int get() = openIdeas.maxOfOrNull { it.votes }?.coerceAtLeast(1) ?: 1
}
