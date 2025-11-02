package gaming.xplay.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import gaming.xplay.datamodel.NotificationRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    /**
     * Sends FCM notification and waits for boolean feedback
     * This is the main function you'll call from your ViewModel
     */
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

            // Call the Cloud Function
            val data = hashMapOf(
                "targetUserId" to request.targetUserId,
                "title" to request.title,
                "body" to request.body,
                "requestId" to request.requestId
            )

            functions
                .getHttpsCallable("sendNotification")
                .call(data)
                .await()

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

    /**
     * Send feedback response (called by receiver user)
     */
    suspend fun sendFeedback(requestId: String, response: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                firestore
                    .collection("notification_responses")
                    .document(requestId)
                    .update(
                        mapOf(
                            "status" to "completed",
                            "response" to response,
                            "respondedAt" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()
            } catch (e: Exception) {
                println("Error sending feedback: ${e.message}")
            }
        }
    }
}
