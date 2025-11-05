package gaming.xplay.ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gaming.xplay.ui.theme.LightText

data class Notification(val title: String, val body: String)

val dummyNotifications = listOf(
    Notification("New Challenge!", "@ace_gamer has challenged you to a match!"),
    Notification("Challenge Accepted!", "@blaze_runner has accepted your challenge!"),
    Notification("New Follower", "@cyber_ninja is now following you."),
    Notification("New Message", "You have a new message from @dragon_slayer.")
)

@Composable
fun NotificationsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = LightText
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn {
            items(dummyNotifications) { notification ->
                NotificationItem(notification)
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = notification.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = LightText
            )
        )
        Text(
            text = notification.body,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = LightText.copy(alpha = 0.8f)
            )
        )
    }
}
