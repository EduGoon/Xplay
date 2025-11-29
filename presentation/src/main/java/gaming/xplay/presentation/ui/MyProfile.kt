package gaming.xplay.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import gaming.xplay.data.model.Club
import gaming.xplay.data.model.Player
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
    val leaderboardState by gameViewModel.leaderboard.collectAsState()

    val userRanking = when (leaderboardState) {
        is UiState.Success -> {
            (leaderboardState as UiState.Success<List<gaming.xplay.data.model.rankings>>).data.find { it.playerid == currentUser?.uid }
        }
        else -> null
    }

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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = currentUser?.profilePictureUrl ?: "https://via.placeholder.com/150",
            contentDescription = "Player Avatar",
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = currentUser?.name ?: "My Profile",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(24.dp))

        XPBar(currentXP = userRanking?.XPpoints ?: 0)

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard("Wins", userRanking?.wins ?: 0, MaterialTheme.colorScheme.primary)
            StatCard("Losses", userRanking?.losses ?: 0, MaterialTheme.colorScheme.error)
            val totalMatches = (userRanking?.wins ?: 0) + (userRanking?.losses ?: 0)
            val winRate = if (totalMatches > 0) (((userRanking?.wins ?: 0) * 100f) / totalMatches).toInt() else 0
            StatCard("Winrate", "$winRate%", MaterialTheme.colorScheme.tertiary)
        }

        Spacer(Modifier.height(40.dp))

        when (val state = clubsState) {
            is UiState.Loading -> {
                CircularProgressIndicator()
            }
            is UiState.Success -> {
                val allClubs = state.data
                val myClubs = allClubs.filter { currentUser?.clubs?.contains(it.clubId) == true }

                if (myClubs.isNotEmpty()) {
                    Text(
                        text = "My Clubs (${myClubs.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    myClubs.forEach {
                        ClubItem(club = it, isAdmin = it.adminId == currentUser?.uid)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    Text("You haven't joined any clubs yet.")
                }
            }
            is UiState.Error -> {
                Text(text = state.message)
            }
        }
    }
}

@Composable
fun ClubItem(club: Club, isAdmin: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = club.clubName, style = MaterialTheme.typography.bodyLarge)
            if (isAdmin) {
                Text(
                    text = "Admin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
