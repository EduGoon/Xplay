package gaming.xplay.ui.presentation

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import gaming.xplay.datamodel.Challenge
import gaming.xplay.datamodel.UiState
import gaming.xplay.viewmodel.AuthViewModel
import gaming.xplay.viewmodel.GameViewModel

@Composable
fun ChallengesScreen(
    gameViewModel: GameViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Incoming", "Outgoing", "Active")

    // Fetch all challenges when the screen is first launched
    LaunchedEffect(Unit) {
        gameViewModel.fetchChallengesForCurrentUser()
    }

    val currentUserId = authViewModel.checkCurrentUserUid()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> {
                val incomingState by gameViewModel.incomingChallenges.collectAsState()
                ChallengeList(uiState = incomingState) { challenge ->
                    IncomingChallengeCard(challenge, gameViewModel)
                }
            }
            1 -> {
                val outgoingState by gameViewModel.outgoingChallenges.collectAsState()
                ChallengeList(uiState = outgoingState) { challenge ->
                    OutgoingChallengeCard(challenge)
                }
            }
            2 -> {
                val acceptedState by gameViewModel.acceptedChallenges.collectAsState()
                ChallengeList(uiState = acceptedState) { challenge ->
                    if (currentUserId != null) {
                        ActiveChallengeCard(challenge, currentUserId, gameViewModel)
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
                CircularProgressIndicator()
            }
        }
        is UiState.Success -> {
            if (uiState.data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No challenges here.")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(uiState.data) { item ->
                        itemContent(item)
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
fun IncomingChallengeCard(challenge: Challenge, viewModel: GameViewModel) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Challenge from: ${challenge.player1Id}") // You might want to resolve player names
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { viewModel.acceptChallenge(challenge) }) {
                    Text("Accept")
                }
                Button(onClick = { viewModel.rejectChallenge(challenge) }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
fun OutgoingChallengeCard(challenge: Challenge) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Challenge to: ${challenge.player2Id}")
            Text("Status: ${challenge.status.replaceFirstChar { it.uppercase() }}")
        }
    }
}

@Composable
fun ActiveChallengeCard(challenge: Challenge, currentUserId: String, viewModel: GameViewModel) {
    val opponentId = if (currentUserId == challenge.player1Id) challenge.player2Id else challenge.player1Id
    val myResult = if (currentUserId == challenge.player1Id) challenge.player1Result else challenge.player2Result

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Match against: $opponentId")
            Spacer(modifier = Modifier.height(16.dp))

            if (challenge.status == "disputed") {
                Text("Match Disputed", color = MaterialTheme.colorScheme.error)
            } else if (myResult == null) {
                // Player has not submitted their result yet
                Text("Match played? Submit your result:")
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { viewModel.submitMatchResult(challenge.challengeId, "win") }) {
                        Text("I Won")
                    }
                    Button(onClick = { viewModel.submitMatchResult(challenge.challengeId, "loss") }) {
                        Text("I Lost")
                    }
                }
            } else {
                // Player has submitted, waiting for opponent
                Text("Your result: ${myResult.replaceFirstChar { it.uppercase() }}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Waiting for opponent...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
