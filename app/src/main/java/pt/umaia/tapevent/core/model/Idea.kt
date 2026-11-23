package pt.umaia.tapevent.core.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

const val MAX_IDEA_TITLE_LENGTH = 80

/** Values of the "status" field in ideas/{id} (kept in Portuguese, as in the plan). */
object IdeaStatus {
    const val PENDING = "pendente"
    /** Approved by the administration: open for voting. */
    const val APPROVED = "aprovada"
    const val REJECTED = "rejeitada"
}

/** An event idea proposed by a student. Voting opens only after the administration approves it. */
data class Idea(
    val id: String = "",
    val title: String = "",
    val theme: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val status: String = IdeaStatus.PENDING,
    val votes: Int = 0,
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Idea = Idea(
            id = doc.id,
            title = doc.getString("title").orEmpty(),
            theme = doc.getString("theme").orEmpty(),
            authorId = doc.getString("authorId").orEmpty(),
            authorName = doc.getString("authorName").orEmpty(),
            status = doc.getString("status") ?: IdeaStatus.PENDING,
            votes = doc.getLong("votes")?.toInt() ?: 0,
            createdAt = doc.getTimestamp("createdAt")
        )

        /** One vote per student per idea: ideaVotes/{ideaId_uid}. */
        fun voteIdFor(ideaId: String, uid: String) = "${ideaId}_$uid"
    }
}
