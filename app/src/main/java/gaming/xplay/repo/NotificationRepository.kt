
package gaming.xplay.repo

import com.google.firebase.firestore.FirebaseFirestore
import gaming.xplay.datamodel.NotificationRequest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun sendNotificationAndAwaitFeedback(
        request: NotificationRequest,
        timeoutSeconds: Long
    ): Boolean? {
        val requestId = request.requestId

        return withTimeoutOrNull(timeoutSeconds * 1000) {
            // Send the notification request to Firestore
            db.collection("notifications").document(requestId).set(request).await()

            // Listen for feedback in a separate document
            val feedbackDoc = db.collection("feedback").document(requestId)

            // This is a simplified listener. In a real app, you might use a more robust
            // solution like a snapshot listener that can be properly removed.
            var feedback: Boolean? = null
            while (feedback == null) {
                val snapshot = feedbackDoc.get().await()
                if (snapshot.exists()) {
                    feedback = snapshot.getBoolean("accepted")
                }
            }
            feedback
        }
    }
}
