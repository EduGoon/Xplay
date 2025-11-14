package gaming.xplay.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import gaming.xplay.R
import gaming.xplay.receiver.NotificationActionReceiver
import gaming.xplay.repo.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FCMMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onNewToken(token: String) {
        Log.d("FCM", "Refreshed token: $token")
        serviceScope.launch {
            try {
                authRepository.updateFCMToken(token)
            } catch (e: Exception) {
                Log.e("FCM", "Error updating FCM token in repository", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val title = data["title"]
        val body = data["body"]
        val requestId = data["requestId"] // This will be our challengeId

        if (title.isNullOrBlank() || body.isNullOrBlank()) {
            Log.w("FCM", "Received a message with missing title or body.")
            return
        }

        if (requestId != null) {
            showNotificationWithActions(title, body, requestId)
        } else {
            showSimpleNotification(title, body)
        }
    }

    private fun showNotificationWithActions(title: String, body: String, requestId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "challenge_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Challenges", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val acceptIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_YES"
            putExtra("requestId", requestId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            this, requestId.hashCode(), acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_NO"
            putExtra("requestId", requestId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this, requestId.hashCode() + 1, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "Accept", acceptPendingIntent) 
            .addAction(R.drawable.ic_launcher_foreground, "Reject", rejectPendingIntent)

        notificationManager.notify(requestId.hashCode(), notificationBuilder.build())
    }

    private fun showSimpleNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "default_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "General", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
