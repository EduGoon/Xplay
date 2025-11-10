package gaming.xplay.ui.presentation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import gaming.xplay.viewmodel.AuthViewModel
import gaming.xplay.viewmodel.NavigationState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberAnimatedNavController()
    val navigationState by authViewModel.navigationState.collectAsState()

    LaunchedEffect(navigationState) {
        when (navigationState) {
            is NavigationState.ToLogin -> navController.navigateAndPop("login", "splash")
            is NavigationState.ToOnboarding -> navController.navigateAndPop("onboardingScreen", "splash")
            is NavigationState.ToHome -> navController.navigateAndPop("home", "splash")
            else -> Unit // Keep showing splash
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
        composable("splash") { SplashScreen() }
        composable("login") { LoginScreen(authViewModel) }
        composable("onboardingScreen") { OnboardingScreen(navController) }
        composable("home") { HomePage(navController) }
    }
}

fun NavController.navigateAndPop(route: String, popUpToRoute: String) {
    navigate(route) {
        popUpTo(popUpToRoute) { inclusive = true }
    }
}
