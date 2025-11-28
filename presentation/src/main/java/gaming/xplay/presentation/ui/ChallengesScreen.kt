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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import gaming.xplay.data.model.Challenge
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.GameViewModel

@Composable
fun ChallengesScreen(
    gameViewModel: GameViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Incoming", "Outgoing", "Active")
    val snackbarHostState = remember { SnackbarHostState() }
    val errorState by gameViewModel.errorState.collectAsState()

    LaunchedEffect(Unit) {
        gameViewModel.fetchChallengesForCurrentUser()
    }

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
fun ChallengeCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun IncomingChallengeCard(challenge: Challenge, viewModel: GameViewModel) {
    ChallengeCard {
        Text("Challenge from: ${challenge.player1Id}", color = MaterialTheme.colorScheme.onSurface) // You might want to resolve player names
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.acceptChallenge(challenge) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Accept", color = MaterialTheme.colorScheme.onSecondary)
            }
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Button(
                onClick = { viewModel.rejectChallenge(challenge) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Reject", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@Composable
fun OutgoingChallengeCard(challenge: Challenge) {
    ChallengeCard {
        Text("Challenge to: ${challenge.player2Id}", color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Status: ${challenge.status.replaceFirstChar { it.uppercase() }}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActiveChallengeCard(challenge: Challenge, currentUserId: String, viewModel: GameViewModel) {
    val opponentId = if (currentUserId == challenge.player1Id) challenge.player2Id else challenge.player1Id
    val myResult = if (currentUserId == challenge.player1Id) challenge.player1Result else challenge.player2Result

    ChallengeCard {
        Text("Match against: $opponentId", color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        if (challenge.status == "disputed") {
            Text("Match Disputed", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        } else if (myResult == null) {
            Text("Match played? Submit your result:", color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = { viewModel.submitMatchResult(challenge.challengeId, "win") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("I Won", color = MaterialTheme.colorScheme.onSecondary)
                }
                Button(
                    onClick = { viewModel.submitMatchResult(challenge.challengeId, "loss") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("I Lost", color = MaterialTheme.colorScheme.onError)
                }
            }
        } else {
            Text("Your result: ${myResult.replaceFirstChar { it.uppercase() }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Waiting for opponent...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
