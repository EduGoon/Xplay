package gaming.xplay.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import gaming.xplay.repo.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationActionReceiverEntryPoint {
        fun gameRepository(): GameRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val challengeId = intent.getStringExtra("requestId") ?: return
        val confirmed = when (intent.action) {
            "ACTION_YES" -> true
            "ACTION_NO" -> false
            else -> return
        }

        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationActionReceiverEntryPoint::class.java
        )
        val gameRepository = hiltEntryPoint.gameRepository()

        coroutineScope.launch {
            if (confirmed) {
                // The only action is to update the status to "accepted".
                // The match itself is created later, after both players submit results.
                gameRepository.updateChallengeStatus(challengeId, "accepted")
            } else {
                gameRepository.updateChallengeStatus(challengeId, "rejected")
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.cancel(challengeId.hashCode())
    }
}
