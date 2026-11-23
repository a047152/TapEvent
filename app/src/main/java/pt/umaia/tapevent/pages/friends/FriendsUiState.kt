package pt.umaia.tapevent.pages.friends

/** One person on the Friends screen. */
data class FriendItem(
    val friendshipId: String,
    val name: String,
    val email: String
)

data class FriendsUiState(
    val isLoading: Boolean = true,
    val emailInput: String = "",
    val isSending: Boolean = false,
    val formError: String? = null,
    /** Requests other students sent me. */
    val received: List<FriendItem> = emptyList(),
    /** Requests I sent, still waiting. */
    val sent: List<FriendItem> = emptyList(),
    val friends: List<FriendItem> = emptyList(),
    /** Friendships being accepted/removed right now. */
    val processingIds: Set<String> = emptySet(),
    /** Friend waiting for the "remove?" confirmation dialog. */
    val friendToRemove: FriendItem? = null,
    val message: String? = null,
    val errorMessage: String? = null
)
