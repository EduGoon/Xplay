package gaming.xplay.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import gaming.xplay.receiver.NotificationActionReceiver


//This class is for handling the receiving end of the notification
class FCMMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        println("From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            println("Message data payload: " + remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            println("Message Notification Body: ${it.body}")
        }

        val requiresFeedback = remoteMessage.data["requiresFeedback"] == "true"
        val requestId = remoteMessage.data["requestId"]

        if (requiresFeedback && requestId != null) {
            showNotificationWithActions(
                title = remoteMessage.notification?.title ?: "New Request",
                body = remoteMessage.notification?.body ?: "",
                requestId = requestId
            )
        }
    }

    override fun onNewToken(token: String) {
        println("Refreshed token: $token")
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
    }

    private fun showNotificationWithActions(
        title: String,
        body: String,
        requestId: String
    ) {
        val yesIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_YES"
            putExtra("requestId", requestId)
        }

        val noIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_NO"
            putExtra("requestId", requestId)
        }

        val yesPendingIntent = PendingIntent.getBroadcast(
            this, 0, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val noPendingIntent = PendingIntent.getBroadcast(
            this, 1, noIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "default_channel")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(android.R.drawable.ic_input_add, "Yes", yesPendingIntent)
            .addAction(android.R.drawable.ic_delete, "No", noPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(requestId.hashCode(), notification)
    }
}
