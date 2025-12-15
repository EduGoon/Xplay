package gaming.xplay.presentation.ui

import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import gaming.xplay.data.model.Match
import gaming.xplay.data.model.Player
import gaming.xplay.presentation.R
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ChallengeCreationState
import gaming.xplay.presentation.viewmodel.GameViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerProfile(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel(),
    userId: String,
    xpPoints: Int? = null,
    wins: Int? = null,
    losses: Int? = null
) {
    var player by remember { mutableStateOf<Player?>(null) }
    val challengeCreationState by gameViewModel.challengeCreationState.collectAsState()

    LaunchedEffect(userId) {
        player = authViewModel.getPlayerProfile(userId)
    }

    if (challengeCreationState is ChallengeCreationState.Success || challengeCreationState is ChallengeCreationState.Error) {
        ChallengeResultDialog(
            state = challengeCreationState,
            onDismiss = { gameViewModel.onChallengeCreationStatusConsumed() },
            onNavigateToChallenges = {
                gameViewModel.onChallengeCreationStatusConsumed()
                navController.navigate("challenges") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }


    val playerName = player?.name ?: "Loading..."
    val playerAvi = player?.profilePictureUrl
    val totalMatches = (wins ?: 0) + (losses ?: 0)
    val winRate = if (totalMatches > 0) ((wins ?: 0) * 100f / totalMatches).toInt() else 0

    Scaffold(
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

            XPBar(currentXP = xpPoints ?: 0)

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

            val isLoading = challengeCreationState is ChallengeCreationState.Loading

            Button(
                onClick = { gameViewModel.onChallengeCreate(userId, "FIFA") },
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Challenge", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(Modifier.height(40.dp))

            MatchHistory(gameViewModel, authViewModel, userId)
        }
    }
}


@Composable
fun ChallengeResultDialog(
    state: ChallengeCreationState,
    onDismiss: () -> Unit,
    onNavigateToChallenges: () -> Unit
) {
    val isSuccess = state is ChallengeCreationState.Success
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(
            if (isSuccess) R.raw.success else R.raw.error
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close dialog")
                }

                LottieAnimation(
                    composition = composition,
                    iterations = 1,
                    modifier = Modifier.size(120.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (isSuccess) "Challenge Created!" else "Challenge Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isSuccess) "Go to challenges tab to see your challenge" else "Challenge creation has failed",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                if (isSuccess) {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateToChallenges,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Go to Challenges")
                    }
                }
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
fun StatCard(label: String, value: Any, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
fun MatchHistory(
    gameViewModel: GameViewModel,
    authViewModel: AuthViewModel,
    userId: String
) {
    val matchHistoryState by gameViewModel.matchHistory.collectAsState()

    LaunchedEffect(userId) {
        gameViewModel.fetchMatchHistory(userId)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = matchHistoryState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Success -> {
                val matches = state.data
                if (matches.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.History,
                        text = "No match history found"
                    )
                } else {
                    val groupedMatches = matches.groupBy { getFormattedDate(it.playedAt) }
                    groupedMatches.forEach { (date, matches) ->
                        DateDivider(date = date)
                        matches.forEach { match ->
                            MatchCard(match = match, currentUserId = userId, authViewModel = authViewModel)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            is UiState.Error -> {
                Text(state.message)
            }
        }
    }
}

@Composable
fun DateDivider(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
    }
}

fun getFormattedDate(date: Date?): String {
    if (date == null) return "Unknown Date"

    return when {
        DateUtils.isToday(date.time) -> "Today"
        DateUtils.isToday(date.time + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
    }
}

@Composable
fun MatchCard(match: Match, currentUserId: String, authViewModel: AuthViewModel) {
    val opponentId = if (match.player1Id == currentUserId) match.player2Id else match.player1Id
    var opponent by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(opponentId) {
        opponent = authViewModel.getPlayerProfile(opponentId)
    }

    val won = match.winnerId == currentUserId
    val cardColor = if (won) Color(0xFF3DDC84).copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
    val borderColor = if (won) Color(0xFF3DDC84) else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = opponent?.profilePictureUrl ?: "",
                    contentDescription = "Opponent Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = opponent?.name ?: "Opponent",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Game: ${match.gameId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (won) "WIN" else "LOSS",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            )
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
