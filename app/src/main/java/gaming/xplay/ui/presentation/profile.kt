package gaming.xplay.ui.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()) // Make the page scrollable
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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

        Spacer(Modifier.height(24.dp)) // Increased spacing

        XPBar(currentXP = XPpoints ?: 0)

        Spacer(Modifier.height(32.dp)) // Increased spacing

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
            onClick = { gameviewmodel.createChallenge(userId, "FIFA") },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Text("Challenge", color = MaterialTheme.colorScheme.onPrimary)
        }

        Spacer(Modifier.height(40.dp))

        // Placeholder for Match History
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Match History",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Coming Soon!",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun XPBar(currentXP: Int) {
    val level = (currentXP / 1000) + 1
    val xpForLevel = currentXP % 1000
    val progress = xpForLevel / 1000f

    // Animate the progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, delayMillis = 200), label = ""
    )

    Column(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Text showing Level and XP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Level $level",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "$xpForLevel / 1000 XP",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        // Custom progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp) // Thicker bar
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
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
