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
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import gaming.xplay.viewmodel.AuthViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp(authviewmodel: AuthViewModel = viewModel()) {

    val isSignedIn by authviewmodel.isUserSignedIn.collectAsState()
    val navController = rememberAnimatedNavController()

    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
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
        composable("login") { LoginScreen(navController) }
        composable("onboardingScreen") { OnboardingScreen(navController) }
        composable("home") { HomePage(navController) }
    }
}
