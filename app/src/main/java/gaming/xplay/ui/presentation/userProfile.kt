package gaming.xplay.ui.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import gaming.xplay.datamodel.UiState
import gaming.xplay.ui.theme.LightText
import gaming.xplay.viewmodel.AuthViewModel

@Composable
fun UserProfileScreen(authViewModel: AuthViewModel = viewModel()) {
    val currentUserState by authViewModel.currentUser.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = currentUserState) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            is UiState.Success -> {
                val currentUser = state.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentUser.name ?: "",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MatchHistory()
                }
            }
        }
    }
}
