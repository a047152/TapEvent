package pt.umaia.tapevent.core.model

/** Values stored in the "role" field of users/{uid} (kept in Portuguese, as in the plan). */
object Roles {
    const val STUDENT = "aluno"
    const val ORGANIZER = "organizador"
    const val ADMIN = "admin"
}

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = Roles.STUDENT,
    val interests: List<String> = emptyList()
)
