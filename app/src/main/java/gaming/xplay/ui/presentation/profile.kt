package gaming.xplay.ui.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import gaming.xplay.datamodel.Player
import gaming.xplay.viewmodel.AuthViewModel
import gaming.xplay.viewmodel.GameViewModel

@Composable
fun PlayerProfile(
    navController: NavController,
    authViewModel: AuthViewModel,
    gameviewmodel: GameViewModel,
    userId: String,
    XPpoints: Int? = null,
    wins: Int? = null,
    losses: Int? = null
) {
    var player by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(userId) {
        player = authViewModel.getPlayerProfile(userId)
    }

    val playerName = player?.name ?: "Loading..."
    val playerAvi = player?.profilePictureUrl
    val totalMatches = (wins ?: 0) + (losses ?: 0)
    val winRate = if (totalMatches > 0) ((wins ?: 0) * 100f / totalMatches).toInt() else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                AsyncImage(
                    model = playerAvi ?: "https://via.placeholder.com/150",
                    contentDescription = "Player Avatar",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = playerName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(Modifier.height(8.dp))

                XPBar(currentXP = XPpoints ?: 0)

                Spacer(Modifier.height(16.dp))

                // <-- CORRECTED: use theme primary/secondary for wins/losses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("Wins", wins ?: 0, MaterialTheme.colorScheme.primary)
                    StatCard("Losses", losses ?: 0, MaterialTheme.colorScheme.secondary)
                    StatCard("Winrate", "$winRate%", MaterialTheme.colorScheme.tertiary)
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { navController.navigate("leaderboard") },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                ) {
                    Text("View Leaderboard", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun XPBar(currentXP: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "XP: $currentXP",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = (currentXP % 1000) / 1000f,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun StatCard(label: String, value: Any, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )
    }
}