package gaming.xplay.ui.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import gaming.xplay.viewmodel.AuthViewModel
import gaming.xplay.viewmodel.GameViewModel
import gaming.xplay.viewmodel.NotificationViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Leaderboard : Screen("leaderboard", "Leaderboard", Icons.Filled.Leaderboard)
    object MatchHistory : Screen("match_history", "History", Icons.Filled.History)
    object Notifications : Screen("notifications", "Notifications", Icons.Filled.Notifications)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person)
}

@Composable
fun MainApp(authViewModel: AuthViewModel, notificationViewModel: NotificationViewModel, gameViewModel: GameViewModel = viewModel()) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Leaderboard,
        Screen.MatchHistory,
        Screen.Notifications,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Home.route, 
            modifier = Modifier.padding(innerPadding)
        ) {            composable(Screen.Home.route) { HomepageScreen(authViewModel, notificationViewModel) }
            composable(Screen.Leaderboard.route) { Leaderboard(gameId = "valorant", gameViewModel = gameViewModel) }
            composable(Screen.MatchHistory.route) { MatchHistory(gameViewModel) }
            composable(Screen.Notifications.route) { NotificationsScreen() }
            composable(Screen.Profile.route) { UserProfileScreen() }
        }
    }
}