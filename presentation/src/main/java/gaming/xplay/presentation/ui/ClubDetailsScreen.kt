package gaming.xplay.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
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

    when (val state = clubState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val club = state.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                // Header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        SubcomposeAsyncImage(
                            model = club.imageUrl ?: "https://via.placeholder.com/400x200",
                            contentDescription = club.clubName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when (painter.state) {
                                is coil.compose.AsyncImagePainter.State.Error,
                                is coil.compose.AsyncImagePainter.State.Empty -> {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                        )
                        Text(
                            text = club.clubName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${club.members} members",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Club icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Members list
                items((membersState as? UiState.Success)?.data ?: emptyList()) { member ->
                    val ranking = ((rankingsState as? UiState.Success)?.data ?: emptyList())
                        .find { it.playerid == member.uid }
                    val playerSearchResult = PlayerSearchResult(member, ranking)
                    val isAdmin = club.adminId == member.uid
                    val isCurrentUser = currentUser?.uid == member.uid

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .then(
                                if (isCurrentUser)
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                else Modifier
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                                PlayerRow(result = playerSearchResult) {}
                                if (isAdmin) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AdminBadge()
                                }
                        }
                    }
                }

                // Spacer at bottom
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Join Button
                item {
                    Button(
                        onClick = { currentUser?.uid?.let { clubDetailsViewModel.joinClub(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = currentUser != null &&
                                club.adminId != currentUser?.uid &&
                                !club.memberIds.contains(currentUser?.uid)
                    ) {
                        Text("Join Club")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Admin",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}
