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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import gaming.xplay.data.model.Fixture
import gaming.xplay.data.model.RankingType
import gaming.xplay.data.model.Tournament
import gaming.xplay.presentation.model.PlayerSearchResult
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.JoinTournamentActionState
import gaming.xplay.presentation.viewmodel.StartTournamentActionState
import gaming.xplay.presentation.viewmodel.SubmitMatchResultActionState
import gaming.xplay.presentation.viewmodel.TournamentViewModel
import kotlinx.coroutines.flow.collectLatest

private enum class TournamentSection {
    Participants, Fixtures, Rankings
}

@Composable
fun TournamentScreen(
    tournamentViewModel: TournamentViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val tournamentState by tournamentViewModel.tournament.collectAsState()
    val membersState by tournamentViewModel.members.collectAsState()
    val fixturesState by tournamentViewModel.fixtures.collectAsState()
    val tournamentRankingsState by tournamentViewModel.tournamentRankings.collectAsState()
    val globalRankingsState by tournamentViewModel.globalRankings.collectAsState()
    val joinTournamentState by tournamentViewModel.joinTournamentActionState.collectAsState(initial = JoinTournamentActionState.Idle)
    val startTournamentState by tournamentViewModel.startTournamentActionState.collectAsState(initial = StartTournamentActionState.Idle)
    val submitMatchResultState by tournamentViewModel.submitMatchResultActionState.collectAsState(initial = SubmitMatchResultActionState.Idle)
    val currentUser by authViewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSubmitResultDialog by remember { mutableStateOf<Fixture?>(null) }
    var selectedSection by remember { mutableStateOf(TournamentSection.Participants) }

    LaunchedEffect(Unit) {
        tournamentViewModel.joinTournamentActionState.collectLatest { state ->
            when (state) {
                is JoinTournamentActionState.Success -> {
                    snackbarHostState.showSnackbar("Joined tournament successfully!")
                }

                is JoinTournamentActionState.Error -> {
                    snackbarHostState.showSnackbar(state.message)
                }

                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        tournamentViewModel.startTournamentActionState.collectLatest { state ->
            when (state) {
                is StartTournamentActionState.Success -> {
                    snackbarHostState.showSnackbar("Tournament started successfully!")
                }

                is StartTournamentActionState.Error -> {
                    snackbarHostState.showSnackbar(state.message)
                }

                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        tournamentViewModel.submitMatchResultActionState.collectLatest { state ->
            when (state) {
                is SubmitMatchResultActionState.Success -> {
                    snackbarHostState.showSnackbar("Match result submitted successfully!")
                    showSubmitResultDialog = null
                }

                is SubmitMatchResultActionState.Error -> {
                    snackbarHostState.showSnackbar(state.message)
                }

                else -> {}
            }
        }
    }

    if (showSubmitResultDialog != null && tournamentState is UiState.Success) {
        SubmitResultDialog(
            fixture = showSubmitResultDialog!!,
            tournament = (tournamentState as UiState.Success<Tournament>).data,
            members = (membersState as? UiState.Success)?.data ?: emptyList(),
            onConfirm = { fixture, winnerId ->
                tournamentViewModel.submitTournamentMatchResult(fixture, winnerId)
            },
            onDismiss = { showSubmitResultDialog = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            when (val state = tournamentState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Success -> {
                    val tournament = state.data
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = tournament.name ?: "", style = MaterialTheme.typography.headlineMedium)
                        Text(text = "Status: ${tournament.status}", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Ranking: ${tournament.rankingType}", style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.height(16.dp))

                        if (tournament.adminId == currentUser?.uid && tournament.status == "upcoming") {
                            Button(
                                onClick = { tournamentViewModel.startTournament() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start Tournament")
                            }
                        }

                        val isMember = tournament.members.contains(currentUser?.uid)
                        Button(
                            onClick = {
                                currentUser?.uid?.let { userId ->
                                    tournamentViewModel.joinTournament(userId)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = currentUser != null && !isMember && tournament.status == "upcoming"
                        ) {
                            Text(if (isMember) "You are a member" else "Join Tournament")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val sections = mutableListOf(TournamentSection.Participants)
                        if (tournament.status == "in-progress" || tournament.status == "completed") {
                            sections.add(TournamentSection.Fixtures)
                            if (tournament.rankingType == RankingType.LOCAL) {
                                sections.add(TournamentSection.Rankings)
                            }
                        }

                        TabRow(selectedTabIndex = sections.indexOf(selectedSection)) {
                            sections.forEach { section ->
                                Tab(
                                    selected = selectedSection == section,
                                    onClick = { selectedSection = section },
                                    text = { Text(section.name) }
                                )
                            }
                        }

                        when (selectedSection) {
                            TournamentSection.Participants -> {
                                when (val membersUiState = membersState) {
                                    is UiState.Loading -> {
                                        CircularProgressIndicator()
                                    }
                                    is UiState.Error -> {
                                        Text(text = membersUiState.message, color = MaterialTheme.colorScheme.error)
                                    }
                                    is UiState.Success -> {
                                        if (membersUiState.data.isEmpty()) {
                                            EmptyState(
                                                icon = Icons.Outlined.Groups,
                                                text = "No participants have joined yet."
                                            )
                                        } else {
                                            LazyColumn {
                                                items(membersUiState.data) { member ->
                                                    val ranking = (globalRankingsState as? UiState.Success)?.data?.find { it.playerid == member.uid }
                                                    TournamentPlayerRow(
                                                        result = PlayerSearchResult(member, ranking),
                                                        isCurrentUser = currentUser?.uid == member.uid
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            TournamentSection.Fixtures -> {
                                when (val fixturesUiState = fixturesState) {
                                    is UiState.Loading -> {
                                        CircularProgressIndicator()
                                    }
                                    is UiState.Error -> {
                                        Text(text = fixturesUiState.message, color = MaterialTheme.colorScheme.error)
                                    }
                                    is UiState.Success -> {
                                        if (fixturesUiState.data.isEmpty()) {
                                            EmptyState(
                                                icon = Icons.Outlined.EmojiEvents,
                                                text = "Fixtures have not been created yet."
                                            )
                                        } else {
                                            LazyColumn {
                                                items(fixturesUiState.data) { fixture ->
                                                    val player1 = (membersState as? UiState.Success)?.data?.find { it.uid == fixture.player1Id }
                                                    val player2 = (membersState as? UiState.Success)?.data?.find { it.uid == fixture.player2Id }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            val p1isCurrent = player1?.uid == currentUser?.uid
                                                            Text(text = if (p1isCurrent) "You" else player1?.name ?: "", color = if (p1isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified)
                                                            Text(text = " vs ")
                                                            val p2isCurrent = player2?.uid == currentUser?.uid
                                                            Text(text = if (p2isCurrent) "You" else player2?.name ?: "", color = if (p2isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified)
                                                        }

                                                        if (fixture.status == "pending" && currentUser?.uid == tournament.adminId) {
                                                            Button(onClick = { showSubmitResultDialog = fixture }) {
                                                                Text(text = "Submit Result")
                                                            }
                                                        } else if (fixture.status == "played") {
                                                            val winner = (membersState as? UiState.Success)?.data?.find { it.uid == fixture.winnerId }?.name
                                                            Text(text = "Winner: ${winner ?: "Draw"}")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            TournamentSection.Rankings -> {
                                when (val rankingsUiState = tournamentRankingsState) {
                                    is UiState.Loading -> {
                                        CircularProgressIndicator()
                                    }
                                    is UiState.Error -> {
                                        Text(text = rankingsUiState.message, color = MaterialTheme.colorScheme.error)
                                    }
                                    is UiState.Success -> {
                                        if (rankingsUiState.data.isEmpty()) {
                                            EmptyState(
                                                icon = Icons.Outlined.MilitaryTech,
                                                text = "No rankings yet. Play some matches to see your rank!"
                                            )
                                        } else {
                                            LazyColumn {
                                                items(rankingsUiState.data) { ranking ->
                                                    val player = (membersState as? UiState.Success)?.data?.find { it.uid == ranking.playerId }
                                                    Row(modifier = Modifier.fillMaxWidth()) {
                                                        val isCurrentUser = player?.uid == currentUser?.uid
                                                        Text(text = if (isCurrentUser) "You" else player?.name ?: "", color = if (isCurrentUser) MaterialTheme.colorScheme.primary else Color.Unspecified)
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        Text(text = "Pts: ${ranking.points}, W: ${ranking.wins}, D: ${ranking.draws}, L: ${ranking.losses}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentPlayerRow(
    result: PlayerSearchResult,
    isCurrentUser: Boolean
) {
    val player = result.player
    val ranking = result.ranking

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCurrentUser) "You" else player.name ?: "Player...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "XP: ${ranking?.XPpoints ?: 0}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "${ranking?.wins ?: 0} Wins • ${ranking?.losses ?: 0} Losses",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 100.dp)
        )
    }
}

@Composable
fun SubmitResultDialog(
    fixture: Fixture,
    tournament: Tournament,
    members: List<gaming.xplay.data.model.Player>,
    onConfirm: (Fixture, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val player1 = members.find { it.uid == fixture.player1Id }
    val player2 = members.find { it.uid == fixture.player2Id }
    var selectedWinner by remember { mutableStateOf<String?>(null) }
    var hasSelection by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Match Result") },
        text = {
            Column {
                Text("Select the winner:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { 
                            selectedWinner = fixture.player1Id 
                            hasSelection = true 
                        },
                        enabled = selectedWinner != fixture.player1Id
                    ) {
                        Text(player1?.name ?: "")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { 
                            selectedWinner = null
                            hasSelection = true
                        },
                        enabled = tournament.rankingType == RankingType.LOCAL
                    ) {
                        Text("Draw")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { 
                            selectedWinner = fixture.player2Id
                            hasSelection = true
                        },
                        enabled = selectedWinner != fixture.player2Id
                    ) {
                        Text(player2?.name ?: "")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(fixture, selectedWinner)
                },
                enabled = hasSelection
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
