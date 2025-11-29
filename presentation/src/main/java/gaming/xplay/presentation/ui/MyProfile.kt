package gaming.xplay.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    LaunchedEffect(Unit) {
        authViewModel.refreshCurrentUser()
        gameViewModel.fetchLeaderboard("FIFA")
    }

    LaunchedEffect(createClubState) {
        if (createClubState is UiState.Success) {
            authViewModel.refreshCurrentUser()
        }
    }

    val userRanking = when (leaderboardState) {
        is UiState.Success -> {
            (leaderboardState as UiState.Success<List<gaming.xplay.data.model.rankings>>).data
                .find { it.playerid == currentUser?.uid }
        }
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            SubcomposeAsyncImage(
                model = currentUser?.profilePictureUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) {
                when (painter.state) {
                    is coil.compose.AsyncImagePainter.State.Error,
                    is coil.compose.AsyncImagePainter.State.Empty -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        )
                    }
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

        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = currentUser?.name ?: "My Profile",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(12.dp))
            XPBar(currentXP = userRanking?.XPpoints ?: 0)

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard("Wins", userRanking?.wins ?: 0, MaterialTheme.colorScheme.primary)
                StatCard("Losses", userRanking?.losses ?: 0, MaterialTheme.colorScheme.error)
                val total = (userRanking?.wins ?: 0) + (userRanking?.losses ?: 0)
                val winRate = if (total > 0) ((userRanking?.wins ?: 0) * 100) / total else 0
                StatCard("Winrate", "$winRate%", MaterialTheme.colorScheme.tertiary)
            }

            Spacer(Modifier.height(36.dp))


            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("My Clubs", "Match History")

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
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
                                    ClubCard(club = club, isAdmin = club.adminId == currentUser?.uid)
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
                    MatchHistory(gameViewModel, currentUser?.uid ?: "")
                }
            }
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
