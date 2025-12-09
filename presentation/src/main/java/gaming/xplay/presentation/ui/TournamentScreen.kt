package gaming.xplay.presentation.ui

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.RankingType
import gaming.xplay.data.model.Tournament
import gaming.xplay.data.model.TournamentRanking
import gaming.xplay.data.model.rankings
import gaming.xplay.presentation.model.PlayerSearchResult
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.JoinTournamentActionState
import gaming.xplay.presentation.viewmodel.StartTournamentActionState
import gaming.xplay.presentation.viewmodel.SubmitMatchResultActionState
import gaming.xplay.presentation.viewmodel.TournamentViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlin.collections.find

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

    val currentUser by authViewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSection by remember { mutableStateOf(TournamentSection.Participants) }
    var showSubmitResultDialog by remember { mutableStateOf<Fixture?>(null) }

    // --- Action feedback ---
    LaunchedEffect(Unit) {
        tournamentViewModel.joinTournamentActionState.collectLatest {
            when (it) {
                is JoinTournamentActionState.Success ->
                    snackbarHostState.showSnackbar("Joined tournament")
                is JoinTournamentActionState.Error ->
                    snackbarHostState.showSnackbar(it.message)
                else -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        tournamentViewModel.startTournamentActionState.collectLatest {
            when (it) {
                is StartTournamentActionState.Success ->
                    snackbarHostState.showSnackbar("Tournament started")
                is StartTournamentActionState.Error ->
                    snackbarHostState.showSnackbar(it.message)
                else -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        tournamentViewModel.submitMatchResultActionState.collectLatest {
            when (it) {
                is SubmitMatchResultActionState.Success -> {
                    snackbarHostState.showSnackbar("Result submitted")
                    showSubmitResultDialog = null
                }
                is SubmitMatchResultActionState.Error ->
                    snackbarHostState.showSnackbar(it.message)
                else -> Unit
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    TournamentContent(
                        tournament = tournament,
                        membersState = membersState,
                        fixturesState = fixturesState,
                        rankingsState = tournamentRankingsState,
                        globalRankingsState = globalRankingsState,
                        currentUserId = currentUser?.uid,
                        selectedSection = selectedSection,
                        onSectionChange = { selectedSection = it },
                        onStartTournament = tournamentViewModel::startTournament,
                        onJoinTournament = {
                            currentUser?.uid?.let { tournamentViewModel.joinTournament(it) }
                        },
                        onSubmitResult = { showSubmitResultDialog = it }
                    )

                    if (showSubmitResultDialog != null) {
                        SubmitResultDialog(
                            fixture = showSubmitResultDialog!!,
                            tournament = tournament,
                            members = (membersState as? UiState.Success)?.data ?: emptyList(),
                            onConfirm = tournamentViewModel::submitTournamentMatchResult,
                            onDismiss = { showSubmitResultDialog = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentContent(
    tournament: Tournament,
    membersState: UiState<List<Player>>,
    fixturesState: UiState<List<Fixture>>,
    rankingsState: UiState<List<TournamentRanking>>,
    globalRankingsState: UiState<List<rankings>>,
    currentUserId: String?,
    selectedSection: TournamentSection,
    onSectionChange: (TournamentSection) -> Unit,
    onStartTournament: () -> Unit,
    onJoinTournament: () -> Unit,
    onSubmitResult: (Fixture) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {

        TournamentHeader(tournament)

        Spacer(Modifier.height(16.dp))

        TournamentActions(
            isAdmin = tournament.adminId == currentUserId,
            isMember = tournament.members.contains(currentUserId),
            status = tournament.status,
            onStart = onStartTournament,
            onJoin = onJoinTournament
        )

        Spacer(Modifier.height(16.dp))

        val sections = buildList {
            add(TournamentSection.Participants)
            if (tournament.status != "upcoming") {
                add(TournamentSection.Fixtures)
                if (tournament.rankingType == RankingType.LOCAL)
                    add(TournamentSection.Rankings)
            }
        }

        TournamentTabs(sections, selectedSection, onSectionChange)

        Spacer(Modifier.height(8.dp))

        when (selectedSection) {
            TournamentSection.Participants ->
                ParticipantsSection(membersState, globalRankingsState, currentUserId)

            TournamentSection.Fixtures ->
                FixturesSection(fixturesState, membersState, tournament, currentUserId, onSubmitResult)

            TournamentSection.Rankings ->
                RankingsSection(rankingsState, membersState, currentUserId)
        }
    }
}

@Composable
private fun TournamentHeader(tournament: Tournament) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
        Column(Modifier.padding(16.dp)) {
            Text(
                tournament.name ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(tournament.status) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Schedule, null)
                    }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(tournament.rankingType.name) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Leaderboard, null)
                    }
                )
            }
        }
    }
}

@Composable
private fun TournamentTabs(
    sections: List<TournamentSection>,
    selected: TournamentSection,
    onSelected: (TournamentSection) -> Unit
) {
    TabRow(selectedTabIndex = sections.indexOf(selected)) {
        sections.forEach {
            Tab(
                selected = it == selected,
                onClick = { onSelected(it) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (it) {
                                TournamentSection.Participants -> Icons.Outlined.Groups
                                TournamentSection.Fixtures -> Icons.Outlined.EmojiEvents
                                TournamentSection.Rankings -> Icons.Outlined.Leaderboard
                            },
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(it.name)
                    }
                }
            )
        }
    }
}

@Composable
private fun TournamentActions(
    isAdmin: Boolean,
    isMember: Boolean,
    status: String,
    onStart: () -> Unit,
    onJoin: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        if (isAdmin && status == "upcoming") {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Start Tournament")
            }
        }

        Button(
            onClick = onJoin,
            enabled = !isMember && status == "upcoming",
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (isMember) Icons.Outlined.Check else Icons.Outlined.PersonAdd,
                null
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isMember) "Joined" else "Join Tournament")
        }
    }
}

@Composable
private fun ParticipantsSection(
    membersState: UiState<List<Player>>,
    globalRankingsState: UiState<List<rankings>>,
    currentUserId: String?
) {
    when (membersState) {
        is UiState.Loading -> {
            CircularProgressIndicator()
        }

        is UiState.Error -> {
            Text(membersState.message, color = MaterialTheme.colorScheme.error)
        }

        is UiState.Success -> {
            if (membersState.data.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Groups,
                    text = "No participants yet"
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(membersState.data) { member ->
                        val ranking =
                            (globalRankingsState as? UiState.Success)
                                ?.data
                                ?.find { it.playerid == member.uid }

                        PlayerCard(
                            name = if (member.uid == currentUserId) "You" else member.name,
                            xp = ranking?.XPpoints ?: 0,
                            wins = ranking?.wins ?: 0,
                            losses = ranking?.losses ?: 0,
                            isCurrentUser = member.uid == currentUserId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FixturesSection(
    fixturesState: UiState<List<Fixture>>,
    membersState: UiState<List<Player>>,
    tournament: Tournament,
    currentUserId: String?,
    onSubmitResult: (Fixture) -> Unit
) {
    when (fixturesState) {
        is UiState.Loading -> CircularProgressIndicator()

        is UiState.Error ->
            Text(fixturesState.message, color = MaterialTheme.colorScheme.error)

        is UiState.Success -> {
            if (fixturesState.data.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.EmojiEvents,
                    text = "Fixtures not generated yet"
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(fixturesState.data) { fixture ->
                        val members =
                            (membersState as? UiState.Success)?.data ?: return@items

                        val p1 = members.find { it.uid == fixture.player1Id }
                        val p2 = members.find { it.uid == fixture.player2Id }

                        FixtureCard(
                            player1 = p1?.name ?: "",
                            player2 = p2?.name ?: "",
                            status = fixture.status,
                            isAdmin = tournament.adminId == currentUserId,
                            onSubmit = { onSubmitResult(fixture) },
                            winner =
                                members.find { it.uid == fixture.winnerId }?.name
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingsSection(
    rankingsState: UiState<List<TournamentRanking>>,
    membersState: UiState<List<Player>>,
    currentUserId: String?
) {
    when (rankingsState) {
        is UiState.Loading -> CircularProgressIndicator()

        is UiState.Error ->
            Text(rankingsState.message, color = MaterialTheme.colorScheme.error)

        is UiState.Success -> {
            if (rankingsState.data.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.MilitaryTech,
                    text = "No rankings yet"
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rankingsState.data) { ranking ->
                        val player =
                            (membersState as? UiState.Success)
                                ?.data
                                ?.find { it.uid == ranking.playerId }

                        PlayerRankCard(
                            name = if (player?.uid == currentUserId) "You" else player?.name,
                            points = ranking.points,
                            wins = ranking.wins,
                            draws = ranking.draws,
                            losses = ranking.losses
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(
    name: String?,
    xp: Int,
    wins: Int,
    losses: Int,
    isCurrentUser: Boolean
) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    name ?: "Player",
                    fontWeight = FontWeight.Medium,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text("XP: $xp", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$wins W • $losses L")
        }
    }
}

@Composable
private fun PlayerRankCard(
    name: String?,
    points: Int,
    wins: Int,
    draws: Int,
    losses: Int
) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name ?: "Player", fontWeight = FontWeight.Medium)
            Text("Pts: $points  W:$wins D:$draws L:$losses")
        }
    }
}

@Composable
private fun FixtureCard(
    player1: String,
    player2: String,
    status: String,
    isAdmin: Boolean,
    onSubmit: () -> Unit,
    winner: String?
) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp)) {
            Text("$player1 vs $player2", fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(8.dp))

            when {
                status == "pending" && isAdmin -> {
                    Button(onClick = onSubmit) {
                        Icon(Icons.Outlined.Edit, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Submit Result")
                    }
                }
                status == "played" -> {
                    Text(
                        "Winner: ${winner ?: "Draw"}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SubmitResultDialog(
    fixture: Fixture,
    tournament: Tournament,
    members: List<Player>,
    onConfirm: (Fixture, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val player1 = members.find { it.uid == fixture.player1Id }
    val player2 = members.find { it.uid == fixture.player2Id }

    var selectedWinnerId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Match Result") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "${player1?.name ?: "Player 1"} vs ${player2?.name ?: "Player 2"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Divider()

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedWinnerId = player1?.uid }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selectedWinnerId == player1?.uid,
                            onClick = { selectedWinnerId = player1?.uid }
                        )
                        Text(player1?.name ?: "")
                    }

                    if (tournament.rankingType == RankingType.LOCAL) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedWinnerId = null }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedWinnerId == null,
                                onClick = { selectedWinnerId = null }
                            )
                            Text("Draw")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedWinnerId = player2?.uid }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selectedWinnerId == player2?.uid,
                            onClick = { selectedWinnerId = player2?.uid }
                        )
                        Text(player2?.name ?: "")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(fixture, selectedWinnerId)
                }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
