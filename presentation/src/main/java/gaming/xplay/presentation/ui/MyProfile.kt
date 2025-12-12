package gaming.xplay.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import gaming.xplay.data.model.Club
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ClubViewModel
import gaming.xplay.presentation.viewmodel.GameViewModel

@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    clubViewModel: ClubViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val clubsState by clubViewModel.clubs.collectAsState()
    val createClubState by clubViewModel.createClubState.collectAsState()
    val leaderboardState by gameViewModel.leaderboard.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("My Clubs", "Match History")

    LaunchedEffect(Unit) {
        authViewModel.refreshCurrentUser()
        gameViewModel.fetchLeaderboard("FIFA")
    }

    LaunchedEffect(createClubState) {
        if (createClubState is UiState.Success) authViewModel.refreshCurrentUser()
    }

    val userRanking = (leaderboardState as? UiState.Success)?.data
        ?.find { it.playerid == currentUser?.uid }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {

        // ---------------- Header Panel ----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            SubcomposeAsyncImage(
                model = currentUser?.profilePictureUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) {
                when (painter.state) {
                    is coil.compose.AsyncImagePainter.State.Error,
                    is coil.compose.AsyncImagePainter.State.Empty -> Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    )

                    else -> SubcomposeAsyncImageContent()
                }
            }

            AsyncImage(
                model = currentUser?.profilePictureUrl ?: "",
                contentDescription = "Avatar",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 16.dp, y = 60.dp)
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(70.dp))

        // ---------------- XP Panel ----------------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            val xp = userRanking?.XPpoints ?: 0
            val targetXP = ((xp / 100) + 1) * 100
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("XP Progress", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = xp.toFloat() / targetXP.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
                Text("$xp / $targetXP XP", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---------------- Info Cards ----------------
        val wins = userRanking?.wins ?: 0
        val losses = userRanking?.losses ?: 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCard(title = "Wins", value = wins.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Losses", value = losses.toString(), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        WinLossDonutChart(
            wins = wins,
            losses = losses,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )


        Spacer(Modifier.height(28.dp))


        // ---------------- Section Panels ----------------
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = title) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            when (selectedTab) {
                0 -> { // My Clubs
                    when (val state = clubsState) {
                        is UiState.Loading -> {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Success -> {
                            val all = state.data
                            val my = all.filter {
                                currentUser?.clubs?.contains(it.clubId) == true ||
                                        it.adminId == currentUser?.uid
                            }

                            if (my.isNotEmpty()) {
                                my.forEach { club ->
                                    ClubCard(
                                        club = club,
                                        isAdmin = club.adminId == currentUser?.uid
                                    )
                                    Spacer(Modifier.height(16.dp))
                                }
                            } else {
                                Text("You haven't joined any clubs yet.")
                            }
                        }

                        is UiState.Error -> {
                            Text(state.message)
                        }
                    }
                }

                1 -> { // Match History
                    MatchHistory(gameViewModel, authViewModel, currentUser?.uid ?: "")
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}


@Composable
fun ClubCard(club: Club, isAdmin: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(
                text = club.clubName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(Modifier.height(8.dp))

            // Clean Member Row + Green “Admin” Tag
            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(
                    text = "Members: ${club.members}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isAdmin) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF3DDC84).copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Admin",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1B8A5A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WinLossDonutChart(
    wins: Int,
    losses: Int,
    modifier: Modifier = Modifier
) {
    val totalGames = wins + losses
    if (totalGames == 0) {
        Box(
            modifier = modifier
                .height(150.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("Play a match to see your stats!", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val winPercentage = wins.toFloat() / totalGames.toFloat()
    val winAngle = 360 * winPercentage

    val animatedSweepAngle by animateFloatAsState(
        targetValue = winAngle,
        animationSpec = tween(durationMillis = 1000, delayMillis = 250),
        label = "winLossDonut"
    )

    val winRate = (winPercentage * 100).toInt()

    val lossColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
    val winColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            // Background arc (losses)
            drawArc(
                color = lossColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
            // Foreground arc (wins)
            drawArc(
                color = winColor,
                startAngle = -90f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$winRate%",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = "Win Rate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
