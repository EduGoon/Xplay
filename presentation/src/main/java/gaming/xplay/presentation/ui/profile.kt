package gaming.xplay.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import gaming.xplay.data.model.Player
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ChallengeCreationState
import gaming.xplay.presentation.viewmodel.GameViewModel

@Composable
fun PlayerProfile(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    gameviewmodel: GameViewModel = hiltViewModel(),
    userId: String,
    XPpoints: Int? = null,
    wins: Int? = null,
    losses: Int? = null
) {
    var player by remember { mutableStateOf<Player?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val challengeCreationState by gameviewmodel.challengeCreationState.collectAsState()

    LaunchedEffect(userId) {
        player = authViewModel.getPlayerProfile(userId)
    }

    LaunchedEffect(challengeCreationState) {
        when (val state = challengeCreationState) {
            is ChallengeCreationState.Success -> {
                snackbarHostState.showSnackbar("Challenge created successfully!")
                gameviewmodel.onChallengeCreationStatusConsumed()
            }
            is ChallengeCreationState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                gameviewmodel.onChallengeCreationStatusConsumed()
            }
            ChallengeCreationState.Idle -> {
                // Do nothing
            }
        }
    }

    val playerName = player?.name ?: "Loading..."
    val playerAvi = player?.profilePictureUrl
    val totalMatches = (wins ?: 0) + (losses ?: 0)
    val winRate = if (totalMatches > 0) ((wins ?: 0) * 100f / totalMatches).toInt() else 0

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .verticalScroll(rememberScrollState()) // Make the page scrollable
                .padding(paddingValues)
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
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(24.dp)) // Increased spacing

            XPBar(currentXP = XPpoints ?: 0)

            Spacer(Modifier.height(32.dp)) // Increased spacing

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard("Wins", wins ?: 0, MaterialTheme.colorScheme.primary)
                StatCard("Losses", losses ?: 0, MaterialTheme.colorScheme.error)
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

            MatchHistory(gameviewmodel, userId)
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun StatCard(label: String, value: Any, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MatchHistory(gameviewmodel: GameViewModel, userId: String){

    LaunchedEffect(userId) {
        gameviewmodel.fetchMatchHistory(userId)
    }

    //Handle the List of matches from the MatchHistory state handler in gameviewmodel inside the column below
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Match History",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Coming Soon!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
