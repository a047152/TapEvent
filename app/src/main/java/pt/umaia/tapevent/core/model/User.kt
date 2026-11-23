package pt.umaia.tapevent.core.model

/** Values stored in the "role" field of users/{uid} (kept in Portuguese, as in the plan). */
object Roles {
    const val STUDENT = "aluno"
    const val ORGANIZER = "organizador"
    const val ADMIN = "admin"
}

/**
 * The "alerts" map in users/{uid}. Keys kept as in the plan: novos, d1, h1, pouco.
 * The notifications themselves are sent in Phase 2 (Cloud Functions + FCM).
 */
data class AlertSettings(
    /** New events about one of my interests. */
    val newEvents: Boolean = true,
    /** Reminder 1 day before an event I registered for. */
    val oneDayBefore: Boolean = true,
    /** Reminder 1 hour before an event I registered for. */
    val oneHourBefore: Boolean = true,
    /** Last seats left in an event I asked to be notified about. */
    val fewSeats: Boolean = false
) {
    companion object {
        const val KEY_NEW = "novos"
        const val KEY_DAY = "d1"
        const val KEY_HOUR = "h1"
        const val KEY_FEW = "pouco"

        fun fromMap(map: Map<*, *>?): AlertSettings = AlertSettings(
            newEvents = map?.get(KEY_NEW) as? Boolean ?: true,
            oneDayBefore = map?.get(KEY_DAY) as? Boolean ?: true,
            oneHourBefore = map?.get(KEY_HOUR) as? Boolean ?: true,
            fewSeats = map?.get(KEY_FEW) as? Boolean ?: false
        )
    }
}

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = Roles.STUDENT,
    val interests: List<String> = emptyList(),
    val alerts: AlertSettings = AlertSettings()
)
