package gaming.xplay.ui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val userState by authViewModel.currentUser.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable("username_setup") {
            UsernameSetupScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable("home") {
            MainApp(
                mainNavController = navController,
                authViewModel = authViewModel,
                notificationViewModel = notificationViewModel
            )
        }
    }

    LaunchedEffect(userState) {
        when (val state = userState) {
            is UiState.Success -> {
                val player = state.data
                if (player == null) {
                    // User is signed out
                    if (navController.currentDestination?.route != "login") {
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                } else if (player.name.isNullOrBlank()) {
                    // User needs to set a username
                    if (navController.currentDestination?.route != "username_setup") {
                        navController.navigate("username_setup") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                } else {
                    // User is authenticated
                    if (navController.currentDestination?.route != "home") {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            }

            is UiState.Error -> {
                // Stay on the current screen. The screen itself should show the error.
            }

            is UiState.Loading -> {
                // Show a splash screen or do nothing.
            }
        }
    }
}