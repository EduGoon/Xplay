package gaming.xplay.presentation.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ClubViewModel
import gaming.xplay.presentation.viewmodel.GameViewModel
import gaming.xplay.presentation.viewmodel.NavigationState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp(authViewModel: AuthViewModel = hiltViewModel(), gameViewModel: GameViewModel = hiltViewModel(), clubViewModel: ClubViewModel = hiltViewModel(), webClientId: String) {
    val navController = rememberAnimatedNavController()
    val navigationState by authViewModel.navigationState.collectAsState()
    val errorState by authViewModel.errorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorState) {
        errorState?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.dismissError()
        }
    }

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
    val bottomBarRoutes = setOf("home", "challenges", "leaderboard", "clubs")
    val showBottomBar = currentDestination?.route in bottomBarRoutes

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    // Home Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    // Challenges Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.List, contentDescription = "Challenges") },
                        label = { Text("Challenges") },
                        selected = currentDestination?.hierarchy?.any { it.route == "challenges" } == true,
                        onClick = {
                            navController.navigate("challenges") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    // Clubs Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.SportsEsports, contentDescription = "Clubs") },
                        label = { Text("Clubs") },
                        selected = currentDestination?.hierarchy?.any { it.route == "clubs" } == true,
                        onClick = {
                            navController.navigate("clubs") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    // Leaderboard Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Leaderboard, contentDescription = "Leaderboard") },
                        label = { Text("Leaderboard") },
                        selected = currentDestination?.hierarchy?.any { it.route == "leaderboard" } == true,
                        onClick = {
                            navController.navigate("leaderboard") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surface
                        )
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
            composable("login") { LoginScreen(authViewModel, webClientId) }
            composable("onboardingScreen") { OnboardingScreen(authViewModel) }
            composable("home") { HomePage(navController) }
            composable("challenges") { ChallengesScreen(gameViewModel, authViewModel) }
            composable("clubs") { ClubsScreen(clubViewModel, authViewModel, navController) }
            composable("leaderboard") { LeaderboardScreen(navController, authViewModel, gameViewModel) }
            composable("notifications") { NotificationsScreen() }
            composable("myprofile") { MyProfileScreen(authViewModel) }
            composable(
                "clubdetails/{clubId}?isNewAdmin={isNewAdmin}",
                arguments = listOf(
                    navArgument("clubId") { type = NavType.StringType },
                    navArgument("isNewAdmin") {
                        type = NavType.BoolType
                        defaultValue = false
                    })
            ) { ClubDetailsScreen(navController) }
            composable("tournamentscreen/{tournamentId}", arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })) { TournamentScreen() }
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
                PlayerProfile(
                    navController,
                    authViewModel,
                    gameViewModel,
                    playerId,
                    xpPoints,
                    wins,
                    losses
                )
            }
        }
    }
}
