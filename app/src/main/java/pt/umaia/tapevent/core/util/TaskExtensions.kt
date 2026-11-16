package pt.umaia.tapevent.core.util

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Suspends until a Firebase Task finishes, so repositories can be written with coroutines. */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
    addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
    addOnCanceledListener { cont.cancel() }
}
