package pt.umaia.tapevent.core.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pt.umaia.tapevent.core.model.Friendship
import pt.umaia.tapevent.core.model.FriendshipStatus
import pt.umaia.tapevent.core.model.Roles
import pt.umaia.tapevent.core.util.awaitResult

class StudentNotFoundException : Exception("No account with this email")
class NotAStudentException : Exception("Friends are only between students")
class SelfFriendException : Exception("Cannot add yourself")
class AlreadyFriendsException : Exception("Already friends")
class RequestAlreadySentException : Exception("Request already sent")

/** What happened when a request was sent. */
enum class FriendRequestResult {
    /** A new request is waiting for the other student. */
    SENT,
    /** The other student had already sent a request, so both are friends now. */
    ACCEPTED
}

class FriendRepository {

    private val db = Firebase.firestore
    private val users = db.collection("users")
    private val friendships = db.collection("friendships")

    /** Every friendship and request that includes this student. */
    fun observeFriendships(uid: String): Flow<List<Friendship>> = callbackFlow {
        val registration = friendships.whereArrayContains("users", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map(Friendship::fromDocument))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Sends a request to the student with this email. Only students can be added:
     * accounts with another role (organizers, administration) are refused.
     */
    suspend fun sendRequest(myUid: String, myName: String, myEmail: String, email: String): FriendRequestResult {
        val found = users.whereEqualTo("email", email.lowercase()).limit(1).get().awaitResult()
            .documents.firstOrNull() ?: throw StudentNotFoundException()

        if (found.id == myUid) throw SelfFriendException()
        if (found.getString("role") != Roles.STUDENT) throw NotAStudentException()

        val otherUid = found.id
        val ref = friendships.document(Friendship.idFor(myUid, otherUid))

        return db.runTransaction { transaction ->
            val existing = transaction.get(ref)
            if (existing.exists()) {
                val status = existing.getString("status")
                val requester = existing.getString("requesterId")
                when {
                    status == FriendshipStatus.ACCEPTED -> throw AlreadyFriendsException()
                    requester == myUid -> throw RequestAlreadySentException()
                    else -> {
                        // They had asked me already: sending back means accepting
                        transaction.update(
                            ref,
                            mapOf("status" to FriendshipStatus.ACCEPTED, "acceptedAt" to FieldValue.serverTimestamp())
                        )
                        return@runTransaction FriendRequestResult.ACCEPTED
                    }
                }
            }
            transaction.set(
                ref,
                mapOf(
                    "users" to listOf(myUid, otherUid),
                    "requesterId" to myUid,
                    "status" to FriendshipStatus.PENDING,
                    "names" to mapOf(myUid to myName, otherUid to found.getString("name").orEmpty()),
                    "emails" to mapOf(myUid to myEmail, otherUid to found.getString("email").orEmpty()),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            FriendRequestResult.SENT
        }.awaitResult()
    }

    suspend fun accept(friendshipId: String) {
        friendships.document(friendshipId).update(
            mapOf("status" to FriendshipStatus.ACCEPTED, "acceptedAt" to FieldValue.serverTimestamp())
        ).awaitResult()
    }

    /** Declining a request, cancelling a sent one and removing a friend all delete the document. */
    suspend fun remove(friendshipId: String) {
        friendships.document(friendshipId).delete().awaitResult()
    }
}
