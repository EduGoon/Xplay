package gaming.xplay.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import gaming.xplay.data.model.NotificationRequest
import gaming.xplay.data.network.ApiService
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val apiService: ApiService
) {
    suspend fun sendNotificationAndAwaitFeedback(
        request: NotificationRequest,
        timeoutSeconds: Long = 30
    ): Boolean? = withContext(Dispatchers.IO) {
        try {
            // Create pending response document
            val responseRef = firestore
                .collection("notification_responses")
                .document(request.requestId)

            responseRef.set(mapOf(
                "status" to "pending",
                "createdAt" to com.google.firebase.Timestamp.now(),
                "targetUserId" to request.targetUserId
            )).await()

            // Call the api service
            apiService.sendNotification(request)

            // Wait for response with timeout
            withTimeoutOrNull(timeoutSeconds * 1000) {
                waitForResponse(request.requestId)
            }

        } catch (e: Exception) {
            println("Error sending notification: ${e.message}")
            null
        }
    }

    /**
     * Sends a one-way FCM notification without waiting for feedback.
     */
    suspend fun sendNotification(
        request: NotificationRequest,
        ) {
        try {
            apiService.sendNotification(request)
        } catch (e: Exception) {
            println("Error sending notification: ${e.message}")
        }
    }

    /**
     * Listen for response updates in Firestore
     */
    private suspend fun waitForResponse(requestId: String): Boolean? =
        suspendCancellableCoroutine { continuation ->
            val responseRef = firestore
                .collection("notification_responses")
                .document(requestId)

            val listener = responseRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    continuation.resumeWith(Result.success(null))
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val status = it.getString("status")
                    val response = it.getBoolean("response")

                    if (status == "completed" && response != null) {
                        continuation.resumeWith(Result.success(response))
                    }
                }
            }

            continuation.invokeOnCancellation {
                listener.remove()
            }
        }
    
}
