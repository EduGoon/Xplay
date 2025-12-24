package gaming.xplay.presentation.ui

import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import gaming.xplay.data.model.Badge
import gaming.xplay.data.model.Match
import gaming.xplay.data.model.Player
import gaming.xplay.presentation.R
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ChallengeCreationState
import gaming.xplay.presentation.viewmodel.GameViewModel
import kotlinx.coroutines.launch
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
    val snackbarHostState = remember { SnackbarHostState() }
    val showOfflineError by authViewModel.showOfflineError.collectAsState()
    val hasConnection by authViewModel.hasConnection.collectAsState()

    LaunchedEffect(userId) {
        player = authViewModel.getPlayerProfile(userId)
    }

    val currentBadge = player?.currentBadge?.let { badgeName ->
        Badge.entries.find { it.name == badgeName }
    }

    val badgeMainColor = currentBadge?.let { badgeColor(it) }

    LaunchedEffect(showOfflineError) {
        if (showOfflineError) {
            snackbarHostState.showSnackbar("You're offline. Please check your connection.")
            authViewModel.dismissOfflineError()
        }
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

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (badgeMainColor != null) {
                        Brush.verticalGradient(
                            colors = listOf(
                                badgeMainColor.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    }
                )
                .verticalScroll(rememberScrollState())
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
                    .border(
                        3.dp,
                        badgeMainColor ?: MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
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

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val xp = xpPoints ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentBadge != null) "${currentBadge.displayName} Tier" else "XP Progress",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val xpText = if (currentBadge != null) {
                        "${xp % 100} / 100 XP"
                    } else {
                        if (xp >= 0) "$xp / 100 XP" else "$xp / -100 XP"
                    }
                    Text(
                        text = xpText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                XPBar(xp = xp, badge = currentBadge)

            }

            Spacer(Modifier.height(24.dp))

            val isLoading = challengeCreationState is ChallengeCreationState.Loading

            Button(
                onClick = {
                    if(hasConnection){
                        gameViewModel.onChallengeCreate(userId, "FIFA")
                    } else {
                        authViewModel.showOfflineError
                    }
                },
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

            MatchHistory(gameViewModel, authViewModel, userId, wins, losses)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MatchHistory(
    gameViewModel: GameViewModel,
    authViewModel: AuthViewModel,
    userId: String,
    wins: Int?,
    losses: Int?
) {
    val matchHistoryState by gameViewModel.matchHistory.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        gameViewModel.clearMatchHistory()
        gameViewModel.fetchMatchHistory(userId)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Match History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Stats") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Results") }
                )
            }

            val opponentCache = remember { mutableStateMapOf<String, Player?>() }

            HorizontalPager(state = pagerState, modifier = Modifier.height(300.dp)) { page ->
                when (page) {
                    0 -> StatsTab(matchHistoryState, userId, wins, losses, authViewModel, opponentCache)
                    1 -> ResultsTab(matchHistoryState, userId, authViewModel, opponentCache)
                }
            }
        }
    }
}

@Composable
fun StatsTab(
    matchHistoryState: UiState<List<Match>>,
    userId: String,
    wins: Int?,
    losses: Int?,
    authViewModel: AuthViewModel,
    opponentCache: MutableMap<String, Player?>
) {
    when (val state = matchHistoryState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Success -> {
            val matches = state.data
            val totalGames = matches.size
            val winsCount = wins ?: matches.count { it.winnerId == userId }
            val lossesCount = losses ?: matches.count { it.winnerId != userId && it.winnerId.isNotEmpty() }
            val winRate = if (totalGames > 0) (winsCount * 100f / totalGames) else 0f
            val opponents = matches.map { if (it.player1Id == userId) it.player2Id else it.player1Id }
            val opponentCounts = opponents.groupingBy { it }.eachCount()
            val sortedOpponents = opponentCounts.entries.sortedByDescending { it.value }

            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                // Overall Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Played", totalGames.toString())
                    StatItem("Wins", winsCount.toString())
                    StatItem("Losses", lossesCount.toString())
                    StatItem("Winrate", "${String.format("%.1f", winRate)}%")
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Most Faced Opponents
                Text("Most Faced Opponents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                if (sortedOpponents.isNotEmpty()) {
                    sortedOpponents.forEach { (opponentId, count) ->
                        val opponent = opponentCache.getOrPut(opponentId) { null }
                        LaunchedEffect(opponentId) {
                            if (opponent == null) {
                                opponentCache[opponentId] = authViewModel.getPlayerProfile(opponentId)
                            }
                        }
                        val percentage = (count * 100f) / totalGames
                        OpponentStatCard(opponentCache[opponentId], count, percentage)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Text("No opponent data available.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is UiState.Error -> {
            Text(state.message, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OpponentStatCard(opponent: Player?, count: Int, percentage: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                Text(opponent?.name ?: "Opponent :", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Text("$count games (${String.format("%.1f", percentage)}%)", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ResultsTab(
    matchHistoryState: UiState<List<Match>>,
    userId: String,
    authViewModel: AuthViewModel,
    opponentCache: MutableMap<String, Player?>
) {
    when (val state = matchHistoryState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Success -> {
            val matches = state.data
            if (matches.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    text = "No match results found"
                )
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val groupedMatches =
                        matches.groupBy { getFormattedDate(it.playedAt) }
                    groupedMatches.forEach { (date, matches) ->
                        DateDivider(date = date)
                        matches.forEach { match ->
                            MatchCard(
                                match = match,
                                currentUserId = userId,
                                authViewModel = authViewModel,
                                opponentCache = opponentCache
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        is UiState.Error -> {
            Text(state.message, modifier = Modifier.padding(16.dp))
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
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(
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
fun MatchCard(
    match: Match,
    currentUserId: String,
    authViewModel: AuthViewModel,
    opponentCache: MutableMap<String, Player?>
) {
    val opponentId = if (match.player1Id == currentUserId) match.player2Id else match.player1Id
    val opponent = opponentCache.getOrPut(opponentId) { null }

    LaunchedEffect(opponentId) {
        if (opponent == null) {
            opponentCache[opponentId] = authViewModel.getPlayerProfile(opponentId)
        }
    }

    val won = match.winnerId == currentUserId
    val resultColor = if (won) Color(0xFF3DDC84) else MaterialTheme.colorScheme.error
    val resultText = if (won) "WIN" else "LOSS"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            // Result indicator bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(resultColor)
            )

            // Player and game info
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = opponentCache[opponentId]?.profilePictureUrl ?: "",
                    contentDescription = "Opponent Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = opponentCache[opponentId]?.name ?: "Opponent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Game: ${match.gameId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Result text
            Text(
                text = resultText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = resultColor,
                modifier = Modifier.padding(end = 16.dp)
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
