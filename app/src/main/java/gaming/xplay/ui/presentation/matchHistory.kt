package gaming.xplay.ui.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import gaming.xplay.datamodel.UiState
import gaming.xplay.ui.theme.LightText
import gaming.xplay.viewmodel.GameViewModel

@Composable
fun MatchHistory(gameViewModel: GameViewModel = viewModel()) {
    val matchHistoryState by gameViewModel.matchHistory.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { gameViewModel.fetchMatchHistory(it) }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (currentUser == null) {
            Text("Please sign in to view your match history.")
        } else {
            when (val state = matchHistoryState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(
                    text = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
                is UiState.Success -> {
                    val matchHistory = state.data
                    if (matchHistory.isEmpty()) {
                        Text("No matches played yet.", color = LightText)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Match History",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                ),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            LazyColumn {
                                items(matchHistory) { match ->
                                    Text("Match ID: ${match.gameId}")
                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}
