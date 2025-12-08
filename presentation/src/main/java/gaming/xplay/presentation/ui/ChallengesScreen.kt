package gaming.xplay.presentation.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import gaming.xplay.data.model.Challenge
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ChallengeDetailsViewModel
import gaming.xplay.presentation.viewmodel.GameViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChallengesScreen(
    gameViewModel: GameViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    challengeDetailsViewModel: ChallengeDetailsViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Outgoing", "Incoming", "Active")
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId = authViewModel.checkCurrentUserUid()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(it)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    val outgoingState by gameViewModel.outgoingChallenges.collectAsState()
                    ChallengeList(uiState = outgoingState) { challenge ->
                        OutgoingChallengeCard(challenge, challengeDetailsViewModel)
                    }
                }
                1 -> {
                    val incomingState by gameViewModel.incomingChallenges.collectAsState()
                    ChallengeList(uiState = incomingState) { challenge ->
                        IncomingChallengeCard(challenge, gameViewModel, challengeDetailsViewModel)
                    }
                }
                2 -> {
                    val activeState by gameViewModel.activeChallenges.collectAsState()
                    ChallengeList(uiState = activeState) { challenge ->
                        if (currentUserId != null) {
                            ActiveChallengeCard(challenge, currentUserId, gameViewModel, challengeDetailsViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> ChallengeList(uiState: UiState<List<T>>, itemContent: @Composable (T) -> Unit) {
    when (uiState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Success -> {
            if (uiState.data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No challenges here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(items = uiState.data) { item ->
                        itemContent(item)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: Challenge,
    player1Name: String?,
    player2Name: String?,
    timestamp: Date?,
    timestampLabel: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                timestamp?.let {
                    Text(
                        text = "$timestampLabel ${SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = challenge.status.lowercase(),
                    color = Color(0xFF00C853),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(player1Name ?: "Player 1", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("VS", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                Text(player2Name ?: "Player 2", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Game: ${challenge.gameId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun IncomingChallengeCard(
    challenge: Challenge,
    viewModel: GameViewModel,
    detailsViewModel: ChallengeDetailsViewModel
) {
    LaunchedEffect(challenge.player1Id) {
        detailsViewModel.fetchPlayerProfile(challenge.player1Id)
    }
    val playerProfiles by detailsViewModel.playerProfiles.collectAsState()
    val player1Name = playerProfiles[challenge.player1Id]?.name

    ChallengeCard(challenge, player1Name, "You", challenge.createdAt, "Received on:") {
        if (challenge.status == "pending") {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { viewModel.acceptChallenge(challenge) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x1A00C853)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Accept Challenge", tint = Color.Green)
                }
                Spacer(modifier = Modifier.padding(start = 16.dp))
                IconButton(
                    onClick = { viewModel.rejectChallenge(challenge) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x1AFF0000)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject Challenge", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun OutgoingChallengeCard(challenge: Challenge, detailsViewModel: ChallengeDetailsViewModel) {
    LaunchedEffect(challenge.player2Id) {
        detailsViewModel.fetchPlayerProfile(challenge.player2Id)
    }
    val playerProfiles by detailsViewModel.playerProfiles.collectAsState()
    val player2Name = playerProfiles[challenge.player2Id]?.name

    ChallengeCard(challenge, "You", player2Name, challenge.createdAt, "Sent on:") {}
}

@Composable
fun ActiveChallengeCard(
    challenge: Challenge,
    currentUserId: String,
    viewModel: GameViewModel,
    detailsViewModel: ChallengeDetailsViewModel
) {
    val opponentId = if (currentUserId == challenge.player1Id) challenge.player2Id else challenge.player1Id
    LaunchedEffect(opponentId) {
        detailsViewModel.fetchPlayerProfile(opponentId)
    }

    val playerProfiles by detailsViewModel.playerProfiles.collectAsState()
    val opponentName = playerProfiles[opponentId]?.name

    val player1Name = if (currentUserId == challenge.player1Id) "You" else opponentName
    val player2Name = if (currentUserId == challenge.player2Id) "You" else opponentName

    val myResult = if (currentUserId == challenge.player1Id) challenge.player1Result else challenge.player2Result

    ChallengeCard(challenge, player1Name, player2Name, challenge.acceptedAt, "Accepted on:") {
        if (challenge.status == "disputed") {
            Text("Match Disputed", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        } else if (myResult == null) {
            Text("Match played? Submit your result:", color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = { viewModel.submitMatchResult(challenge.challengeId, "win") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("I Won", color = MaterialTheme.colorScheme.onSecondary)
                }
                Button(
                    onClick = { viewModel.submitMatchResult(challenge.challengeId, "loss") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("I Lost", color = MaterialTheme.colorScheme.onError)
                }
            }
        } else if (challenge.status == "waiting verification"){
            Text("Your result: ${myResult.replaceFirstChar { it.uppercase() }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Waiting for opponent...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }else if(challenge.status == "completed"){
             /*TODO : Create something like a pop up dialog box that shows match completed with a tick*/
        }
    }
}
