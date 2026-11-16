package pt.umaia.tapevent.core.util

import android.util.Patterns

object EmailRules {
    /** Institutional domains accepted by the app.*/
    private const val INSTITUTIONAL_DOMAIN = "umaia.pt"

    fun isInstitutional(email: String): Boolean {
        val domain = email.substringAfterLast("@", missingDelimiterValue = "").lowercase()
        return domain == INSTITUTIONAL_DOMAIN || domain.endsWith(".$INSTITUTIONAL_DOMAIN")
    }

    fun isValidFormat(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
