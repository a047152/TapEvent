package pt.umaia.tapevent.core.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/** Values of the "status" field in friendships/{uidA_uidB} (kept in Portuguese, as in the plan). */
object FriendshipStatus {
    const val PENDING = "pendente"
    const val ACCEPTED = "aceite"
}

/**
 * A friendship between two students. Names and emails of both are copied into the
 * document, so the Friends list needs no extra reads of users/{uid}.
 */
data class Friendship(
    val id: String = "",
    val users: List<String> = emptyList(),
    val requesterId: String = "",
    val status: String = FriendshipStatus.PENDING,
    val names: Map<String, String> = emptyMap(),
    val emails: Map<String, String> = emptyMap(),
    val createdAt: Timestamp? = null
) {
    fun otherUid(myUid: String): String = users.firstOrNull { it != myUid }.orEmpty()
    fun otherName(myUid: String): String = names[otherUid(myUid)].orEmpty().ifBlank { "Aluno" }
    fun otherEmail(myUid: String): String = emails[otherUid(myUid)].orEmpty()

    companion object {
        /** Same id whoever sends the request: the two uids in alphabetical order. */
        fun idFor(uidA: String, uidB: String): String =
            if (uidA < uidB) "${uidA}_$uidB" else "${uidB}_$uidA"

        @Suppress("UNCHECKED_CAST")
        fun fromDocument(doc: DocumentSnapshot): Friendship = Friendship(
            id = doc.id,
            users = (doc.get("users") as? List<String>).orEmpty(),
            requesterId = doc.getString("requesterId").orEmpty(),
            status = doc.getString("status") ?: FriendshipStatus.PENDING,
            names = (doc.get("names") as? Map<String, String>).orEmpty(),
            emails = (doc.get("emails") as? Map<String, String>).orEmpty(),
            createdAt = doc.getTimestamp("createdAt")
        )
    }
}
