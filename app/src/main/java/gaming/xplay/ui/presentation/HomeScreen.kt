package gaming.xplay.ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import gaming.xplay.datamodel.NotificationState
import gaming.xplay.ui.theme.LightText
import gaming.xplay.ui.theme.VibrantRed
import gaming.xplay.viewmodel.AuthViewModel
import gaming.xplay.viewmodel.NotificationViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val isLoggedOut by authViewModel.isLoggedOut.collectAsStateWithLifecycle()
    val notificationState by notificationViewModel.notificationState.collectAsStateWithLifecycle()
    var targetUserId by remember { mutableStateOf("") }
    var notificationStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
            authViewModel.onLoggedOut() // Reset the state
        }
    }

    LaunchedEffect(notificationState) {
        notificationStatus = when (val state = notificationState) {
            is NotificationState.Idle -> null
            is NotificationState.Sending -> "Sending challenge..."
            is NotificationState.Success -> if (state.accepted) "Challenge Accepted!" else "Challenge Declined."
            is NotificationState.Timeout -> "Challenge timed out."
            is NotificationState.Error -> "Error: ${state.message}"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Xplay!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = LightText,
                    fontSize = 32.sp
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Your gaming journey begins here.",
                style = MaterialTheme.typography.bodyLarge.copy(color = LightText.copy(alpha = 0.8f))
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Challenge Section
            Text(
                text = "Challenge a Player",
                style = MaterialTheme.typography.headlineSmall.copy(color = VibrantRed, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = targetUserId,
                onValueChange = { targetUserId = it },
                label = { Text("Opponent's User ID") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = VibrantRed,
                        unfocusedIndicatorColor = LightText.copy(alpha = 0.5f),
                        cursorColor = VibrantRed,
                        focusedLabelColor = VibrantRed,
                        unfocusedLabelColor = LightText.copy(alpha = 0.7f)
                    )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    notificationViewModel.sendNotificationToUser(
                        targetUserId = targetUserId,
                        title = "New Challenge!",
                        body = "A player has challenged you to a match!"
                    )
                },
                enabled = targetUserId.isNotBlank() && notificationState is NotificationState.Idle,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantRed)
            ) {
                Text("SEND CHALLENGE", color = LightText)
            }
            notificationStatus?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = LightText)
            }


            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { authViewModel.signOut() },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantRed)
            ) {
                Text("Logout", color = LightText)
            }
        }
    }
}
