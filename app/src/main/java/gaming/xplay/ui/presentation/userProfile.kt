package gaming.xplay.ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import gaming.xplay.datamodel.UiState
import gaming.xplay.viewmodel.AuthViewModel

@Composable
fun UserProfileScreen(
    mainNavController: NavHostController,
    authViewModel: AuthViewModel
) {
    val userState by authViewModel.currentUser.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = userState) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text(
                text = "Error: ${state.message}", 
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                val player = state.data
                if (player != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Welcome, ${player.name}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { authViewModel.signOut() }) {
                            Text("Sign Out")
                        }
                    }
                } else {
                    // This case should ideally not be reached if Navigation.kt is correct
                    Text("Not logged in.")
                }
            }
        }
    }
}