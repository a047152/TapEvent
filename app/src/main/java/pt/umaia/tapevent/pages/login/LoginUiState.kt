package pt.umaia.tapevent.pages.login

data class LoginUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Set after a successful sign-in; the screen uses it to navigate. */
    val signedInRole: String? = null
)
