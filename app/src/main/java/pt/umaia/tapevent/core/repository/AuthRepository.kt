package pt.umaia.tapevent.core.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import pt.umaia.tapevent.core.util.awaitResult
import pt.umaia.tapevent.core.model.Roles
import pt.umaia.tapevent.core.model.User

class AuthRepository {

    private val auth = Firebase.auth
    private val users = Firebase.firestore.collection("users")

    val currentUid: String?
        get() = auth.currentUser?.uid

    suspend fun signIn(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).awaitResult()
        val uid = result.user?.uid ?: error("Sign-in returned no user")
        return getUser(uid) ?: throw ProfileNotFoundException()
    }

    /** Creates the Auth account and the users/{uid} profile. New accounts are always students. */
    suspend fun register(name: String, email: String, password: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).awaitResult()
        val uid = result.user?.uid ?: error("Registration returned no user")

        val profile = mapOf(
            "name" to name,
            "email" to email,
            "role" to Roles.STUDENT,
            "interests" to emptyList<String>(),
            "alerts" to mapOf(
                "novos" to true,
                "d1" to true,
                "h1" to true,
                "pouco" to false
            ),
            "createdAt" to FieldValue.serverTimestamp()
        )
        users.document(uid).set(profile).awaitResult()

        return User(uid = uid, name = name, email = email, role = Roles.STUDENT)
    }

    /**
     * Several profiles at once (e.g. the names on an attendee list).
     * Firestore "in" queries accept up to 30 ids, so the list is read in chunks.
     */
    suspend fun getUsers(uids: Collection<String>): Map<String, User> {
        if (uids.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, User>()
        uids.distinct().chunked(30).forEach { chunk ->
            val snapshot = users.whereIn(FieldPath.documentId(), chunk).get().awaitResult()
            snapshot.documents.forEach { doc ->
                result[doc.id] = User(
                    uid = doc.id,
                    name = doc.getString("name").orEmpty(),
                    email = doc.getString("email").orEmpty(),
                    role = doc.getString("role") ?: Roles.STUDENT
                )
            }
        }
        return result
    }

    suspend fun getUser(uid: String): User? {
        val doc = users.document(uid).get().awaitResult()
        if (!doc.exists()) return null
        @Suppress("UNCHECKED_CAST")
        return User(
            uid = doc.id,
            name = doc.getString("name").orEmpty(),
            email = doc.getString("email").orEmpty(),
            role = doc.getString("role") ?: Roles.STUDENT,
            interests = (doc.get("interests") as? List<String>).orEmpty()
        )
    }

    fun signOut() = auth.signOut()
}

class ProfileNotFoundException : Exception("User profile not found in Firestore")
