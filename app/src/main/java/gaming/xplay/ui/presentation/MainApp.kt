package gaming.xplay.ui.presentation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import gaming.xplay.viewmodel.AuthViewModel
import gaming.xplay.viewmodel.GameViewModel
import gaming.xplay.viewmodel.NavigationState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp(authViewModel: AuthViewModel = viewModel(), gameViewModel: GameViewModel = viewModel()) {
    val navController = rememberAnimatedNavController()
    val navigationState by authViewModel.navigationState.collectAsState()

    // This handles the initial navigation from Splash to Login/Home
    LaunchedEffect(navigationState, navController) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        when (navigationState) {
            is NavigationState.ToLogin -> {
                if (currentRoute != "login") navController.navigate("login") { popUpTo("splash") { inclusive = true } }
            }
            is NavigationState.ToOnboarding -> {
                 if (currentRoute != "onboardingScreen") navController.navigate("onboardingScreen") { popUpTo("login") { inclusive = true } }
            }
            is NavigationState.ToHome -> {
                 if (currentRoute != "home") navController.navigate("home") { popUpTo(navController.graph.startDestinationId) { inclusive = true } }
            }
            else -> Unit // Handle other states or stay on splash
        }
    }

    // Determine if the bottom bar should be shown
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomBarRoutes = setOf("home", "challenges")
    val showBottomBar = currentDestination?.route in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomAppBar {
                    // Home Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                        onClick = { navController.navigate("home") { launchSingleTop = true } }
                    )
                    // Challenges Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.List, contentDescription = "Challenges") },
                        label = { Text("Challenges") },
                        selected = currentDestination?.hierarchy?.any { it.route == "challenges" } == true,
                        onClick = { navController.navigate("challenges") { launchSingleTop = true } }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedNavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -it }) }
        ) {
            composable("splash") { SplashScreen() }
            composable("login") { LoginScreen(authViewModel) }
            composable("onboardingScreen") { OnboardingScreen(authViewModel) }
            composable("home") { HomePage(navController, authViewModel) }
            composable("challenges") { ChallengesScreen(gameViewModel, authViewModel) }
            composable(
                "profile/{playerId}/{XPpoints}/{wins}/{losses}",
                arguments = listOf(
                    navArgument("playerId") { type = NavType.StringType },
                    navArgument("XPpoints") { type = NavType.IntType },
                    navArgument("wins") { type = NavType.IntType },
                    navArgument("losses") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val playerId = backStackEntry.arguments?.getString("playerId")!!
                val xpPoints = backStackEntry.arguments?.getInt("XPpoints")!!
                val wins = backStackEntry.arguments?.getInt("wins")!!
                val losses = backStackEntry.arguments?.getInt("losses")!!
                PlayerProfile(navController, authViewModel, gameViewModel, playerId, xpPoints, wins, losses)
            }
        }
    }
}
