package gaming.xplay.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import gaming.xplay.repo.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val matchId = intent.getStringExtra("requestId") ?: return
        val confirmed = when (intent.action) {
            "ACTION_YES" -> true
            "ACTION_NO" -> false
            else -> return
        }

        coroutineScope.launch {
            GameRepository().confirmMatch(matchId, confirmed)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.cancel(matchId.hashCode())
    }
}
