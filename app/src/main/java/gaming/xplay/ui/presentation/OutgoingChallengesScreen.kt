package gaming.xplay.ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import gaming.xplay.datamodel.Challenge
import gaming.xplay.datamodel.UiState
import gaming.xplay.viewmodel.GameViewModel

@Composable
fun OutgoingChallengesScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    val outgoingChallengesState by viewModel.outgoingChallenges.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchChallengesForCurrentUser()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = outgoingChallengesState) {
            is UiState.Loading -> {
                CircularProgressIndicator()
            }
            is UiState.Success -> {
                val challenges = state.data
                if (challenges.isEmpty()) {
                    Text("No outgoing challenges.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(challenges) { challenge ->
                            OutgoingChallengeItem(challenge)
                        }
                    }
                }
            }
            is UiState.Error -> {
                Text(state.message)
            }
        }
    }
}

@Composable
fun OutgoingChallengeItem(challenge: Challenge) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Challenge to: ${challenge.player2Id}")
            Text("Status: ${challenge.status}")
        }
    }
}
