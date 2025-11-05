package gaming.xplay.ui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gaming.xplay.datamodel.UiState
import gaming.xplay.viewmodel.AuthViewModel
import gaming.xplay.viewmodel.NotificationViewModel

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val notificationViewModel: NotificationViewModel = hiltViewModel()

    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoggedOut by authViewModel.isLoggedOut.collectAsState()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable("username_setup") {
            UsernameSetupScreen(
                authViewModel = authViewModel
            )
        }
        composable("home") {
            MainApp(
                authViewModel = authViewModel,
                notificationViewModel = notificationViewModel
            )
        }
    }

    LaunchedEffect(currentUser, isLoggedOut) {
        when (val userState = currentUser) {
            is UiState.Success -> {
                if (userState.data.name.isNullOrBlank()) {
                    navController.navigate("username_setup") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            is UiState.Error -> {
                if (isLoggedOut) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                    authViewModel.onLoggedOut()
                }
            }
            UiState.Loading -> Unit // Do nothing, maybe show a splash screen
        }
    }
}