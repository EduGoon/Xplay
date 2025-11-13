package gaming.xplay.ui.presentation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import gaming.xplay.viewmodel.AuthViewModel
import gaming.xplay.viewmodel.GameViewModel
import gaming.xplay.viewmodel.NavigationState
import androidx.navigation.NavType

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp(authViewModel: AuthViewModel = viewModel(), gameViewModel: GameViewModel = viewModel()) {
    val navController = rememberAnimatedNavController()
    val navigationState by authViewModel.navigationState.collectAsState()

    LaunchedEffect(navigationState, navController) {
        when (navigationState) {
            is NavigationState.ToLogin -> {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            is NavigationState.ToOnboarding -> {
                navController.navigate("onboardingScreen") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is NavigationState.ToHome -> {
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            else -> Unit // Keep showing splash or handle other states
        }
    }

    AnimatedNavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { it })
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        composable("splash") {
            SplashScreen()
        }
        composable("login") { LoginScreen(authViewModel) }
        composable("onboardingScreen") { OnboardingScreen(authViewModel) }
        composable("home") { HomePage(navController, authViewModel) }
        composable(
            "profile/{playerId}",
            arguments = listOf(
                navArgument("playerId") {
                    type = NavType.StringType
                }
            )
            ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getString("playerId")

            if(playerId != null) {
            playerProfile(navController, authViewModel, gameViewModel, userId = playerId) }
            else{
                Text("Error: Player ID not found")
            }
            }
    }
}
