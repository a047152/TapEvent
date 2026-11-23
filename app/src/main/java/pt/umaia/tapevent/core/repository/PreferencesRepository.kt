package pt.umaia.tapevent.core.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import pt.umaia.tapevent.core.util.awaitResult

/** Saves the student's interests and alert switches in users/{uid}. */
class PreferencesRepository {

    private val users = Firebase.firestore.collection("users")

    suspend fun saveInterests(uid: String, interests: List<String>) {
        users.document(uid).update("interests", interests).awaitResult()
    }

    /** Updates one switch only, e.g. key "d1" → "alerts.d1". */
    suspend fun saveAlert(uid: String, key: String, enabled: Boolean) {
        users.document(uid).update("alerts.$key", enabled).awaitResult()
    }
}
