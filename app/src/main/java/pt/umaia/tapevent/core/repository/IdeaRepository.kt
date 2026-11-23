package pt.umaia.tapevent.core.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import pt.umaia.tapevent.core.model.Idea
import pt.umaia.tapevent.core.model.IdeaStatus
import pt.umaia.tapevent.core.util.awaitResult

class VotingClosedException : Exception("Idea is not open for voting")

class IdeaRepository {

    private val db = Firebase.firestore
    private val ideas = db.collection("ideas")
    /** Top-level collection (not a subcollection), so "my votes" is a simple query. */
    private val votes = db.collection("ideaVotes")

    /** Ideas open for voting, most voted first. */
    fun observeApprovedIdeas(): Flow<List<Idea>> =
        observeIdeas(ideas.whereEqualTo("status", IdeaStatus.APPROVED))
            .map { list -> list.sortedByDescending { it.votes } }

    /** Ideas waiting for the administration, oldest first. */
    fun observePendingIdeas(): Flow<List<Idea>> =
        observeIdeas(ideas.whereEqualTo("status", IdeaStatus.PENDING))
            .map { list -> list.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE } }

    /** This student's own ideas (every status), newest first. */
    fun observeMyIdeas(uid: String): Flow<List<Idea>> =
        observeIdeas(ideas.whereEqualTo("authorId", uid))
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE } }

    /** Ids of the ideas this student voted for. */
    fun observeMyVotedIdeaIds(uid: String): Flow<Set<String>> = callbackFlow {
        val registration = votes.whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.getString("ideaId") }.toSet())
            }
        awaitClose { registration.remove() }
    }

    /** New ideas always start as "pendente". */
    suspend fun proposeIdea(title: String, theme: String, authorId: String, authorName: String) {
        ideas.add(
            mapOf(
                "title" to title,
                "theme" to theme,
                "authorId" to authorId,
                "authorName" to authorName,
                "status" to IdeaStatus.PENDING,
                "votes" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).awaitResult()
    }

    /**
     * Adds the vote if the student has not voted, removes it otherwise.
     * Runs in a transaction so the "votes" counter always matches the vote documents.
     */
    suspend fun toggleVote(ideaId: String, uid: String) {
        val ideaRef = ideas.document(ideaId)
        val voteRef = votes.document(Idea.voteIdFor(ideaId, uid))

        db.runTransaction { transaction ->
            val idea = transaction.get(ideaRef)
            val vote = transaction.get(voteRef)
            if (idea.getString("status") != IdeaStatus.APPROVED) throw VotingClosedException()

            if (vote.exists()) {
                transaction.delete(voteRef)
                transaction.update(ideaRef, "votes", FieldValue.increment(-1))
            } else {
                transaction.set(
                    voteRef,
                    mapOf("ideaId" to ideaId, "uid" to uid, "createdAt" to FieldValue.serverTimestamp())
                )
                transaction.update(ideaRef, "votes", FieldValue.increment(1))
            }
        }.awaitResult()
    }

    /** Admin decision: "aprovada" opens the voting, "rejeitada" closes the idea. */
    suspend fun reviewIdea(ideaId: String, approve: Boolean, adminUid: String) {
        ideas.document(ideaId).update(
            mapOf(
                "status" to if (approve) IdeaStatus.APPROVED else IdeaStatus.REJECTED,
                "reviewedBy" to adminUid,
                "reviewedAt" to FieldValue.serverTimestamp()
            )
        ).awaitResult()
    }

    private fun observeIdeas(query: Query): Flow<List<Idea>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().map(Idea::fromDocument))
        }
        awaitClose { registration.remove() }
    }
}
