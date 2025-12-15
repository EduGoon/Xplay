package gaming.xplay.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.rankings
import gaming.xplay.presentation.model.PlayerSearchResult
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.GameViewModel

@Composable
fun LeaderboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by authViewModel.searchResults.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val leaderboardState by gameViewModel.leaderboard.collectAsState()
    val leaderboardPlayerProfiles by gameViewModel.leaderboardPlayerProfiles.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            gameViewModel.fetchLeaderboard("FIFA")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SearchBar(
            searchQuery = searchQuery,
            onQueryChange = {
                searchQuery = it
                authViewModel.searchPlayers(searchQuery)
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (searchQuery.isBlank()) {
            LeaderboardSection(
                leaderboardState = leaderboardState,
                playerProfiles = leaderboardPlayerProfiles,
                currentUser = currentUser,
                onRefresh = {
                    gameViewModel.fetchLeaderboard("FIFA")
                },
                navController = navController
            )
        } else {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                SearchResultsList(
                    results = searchResults,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun SearchBar(searchQuery: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = { Text("Search for players...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
        ),
        singleLine = true
    )
}

@Composable
fun SearchResultsList(
    results: List<PlayerSearchResult>,
    navController: NavController
) {
    if (results.isEmpty()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "No players found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(results) { result ->
                PlayerRow(
                    result = result,
                    onClick = {
                        val player = result.player
                        val ranking = result.ranking
                        val xp = ranking?.XPpoints ?: 0
                        val wins = ranking?.wins ?: 0
                        val losses = ranking?.losses ?: 0
                        navController.navigate("profile/${player.uid}/$xp/$wins/$losses")
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun PlayerRow(
    result: PlayerSearchResult,
    onClick: () -> Unit
) {
    val player = result.player
    val ranking = result.ranking

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // You can add a player avatar here later
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name ?: "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "XP: ${ranking?.XPpoints ?: "N/A"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "${ranking?.wins ?: "N/A"} Wins • ${ranking?.losses ?: "N/A"} Losses",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 100.dp)
        )
    }
}

@Composable
fun LeaderboardSection(
    leaderboardState: UiState<List<rankings>>,
    playerProfiles: Map<String, Player?>,
    currentUser: Player?,
    onRefresh: () -> Unit,
    navController: NavController
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🏆 Leaderboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = { onRefresh() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh leaderboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when (leaderboardState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is UiState.Success -> {
                if (leaderboardState.data.isEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No rankings yet. Be the first one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        leaderboardState.data.forEachIndexed { index, ranking ->
                            val player = playerProfiles[ranking.playerid]
                            RankingRow(
                                ranking = ranking,
                                rank = index + 1,
                                player = player,
                                isCurrentUser = currentUser?.uid == ranking.playerid,
                            ) { playerId, xpPoints, wins, losses ->
                                if (currentUser?.uid == playerId) {
                                    navController.navigate("myprofile")
                                } else {
                                    navController.navigate("profile/$playerId/$xpPoints/$wins/$losses")
                                }
                            }
                            if (index != leaderboardState.data.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = leaderboardState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun RankingRow(
    ranking: rankings,
    rank: Int,
    player: Player?,
    isCurrentUser: Boolean,
    onClick: (String, Int, Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(ranking.playerid, ranking.XPpoints, ranking.wins, ranking.losses) }
            .background(if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(32.dp)
        )
        // You can add player avatar here
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player?.name ?: "Loading...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "XP: ${ranking.XPpoints}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${ranking.wins} W / ${ranking.losses} L",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 80.dp)
        )
    }
}
