package gaming.xplay.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import gaming.xplay.presentation.model.PlayerSearchResult
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ClubDetailsViewModel

@Composable
fun ClubDetailsScreen(
    clubDetailsViewModel: ClubDetailsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val clubState by clubDetailsViewModel.club.collectAsState()
    val membersState by clubDetailsViewModel.members.collectAsState()
    val rankingsState by clubDetailsViewModel.rankings.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        when (val state = clubState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Success -> {
                val club = state.data
                Column(modifier = Modifier.fillMaxWidth()) {
                    SubcomposeAsyncImage(
                        model = club.imageUrl,
                        contentDescription = club.clubName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = club.clubName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "${club.members} members")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        when (val members = membersState) {
                            is UiState.Loading -> {
                                CircularProgressIndicator()
                            }
                            is UiState.Success -> {
                                val memberRankings = (rankingsState as? UiState.Success)?.data ?: emptyList()
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(members.data) { member ->
                                        val ranking = memberRankings.find { it.playerid == member.uid }
                                        val playerSearchResult = PlayerSearchResult(member, ranking)
                                        val isAdmin = club.adminId == member.uid
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            PlayerRow(result = playerSearchResult) {}
                                            if (isAdmin) {
                                                Text(
                                                    text = "Admin",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(start = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is UiState.Error -> {
                                Text(text = members.message)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { currentUser?.uid?.let { clubDetailsViewModel.joinClub(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = currentUser != null && club.adminId != currentUser?.uid && !club.memberIds.contains(currentUser?.uid)
                        ) {
                            Text("Join Club")
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message)
                }
            }
        }
    }
}
