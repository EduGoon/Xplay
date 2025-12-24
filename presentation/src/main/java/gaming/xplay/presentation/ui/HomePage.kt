package gaming.xplay.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.rankings
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.GameViewModel
import gaming.xplay.presentation.viewmodel.NotificationViewModel
import gaming.xplay.presentation.theme.LocalGraffiti

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController, notificationViewModel: NotificationViewModel = hiltViewModel(), gameViewModel: GameViewModel = hiltViewModel()) {
    val pendingMembersState by notificationViewModel.pendingMembers.collectAsState()
    val notificationCount = (pendingMembersState as? UiState.Success)?.data?.values?.sumOf { it.size } ?: 0
    val graffiti = LocalGraffiti.current

    val suggestedMatchUpsState by gameViewModel.suggestedMatchUps.collectAsState()
    val suggestedMatchUpsPlayerProfiles by gameViewModel.suggestedMatchUpsPlayerProfiles.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Xplay",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        BadgedBox(badge = {
                            if (notificationCount > 0) {
                                Badge { Text("$notificationCount") }
                            }
                        }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate("myprofile") }) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = graffiti.background),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(graffiti.overlay)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val suggestions = suggestedMatchUpsState
                    if (suggestions is UiState.Success && suggestions.data.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        SuggestedMatchUpsSection(
                            suggestions = suggestions.data,
                            playerProfiles = suggestedMatchUpsPlayerProfiles,
                            navController = navController
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    UpcomingGamesSection()
                }
            }
        }
    }
}

data class Game(
    val title: String,
    val imageUrl: String
)

@Composable
fun SuggestedMatchUpsSection(
    suggestions: List<rankings>,
    playerProfiles: Map<String, Player?>,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
            )
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Suggested Match Ups",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Challenge players with similar skill levels.",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(suggestions) { ranking ->
                val player = playerProfiles[ranking.playerid]
                if (player != null) {
                    PlayerCard(player = player, ranking = ranking, onClick = {
                        navController.navigate("profile/${player.uid}/${ranking.XPpoints}/${ranking.wins}/${ranking.losses}")
                    })
                }
            }
        }
    }
}

@Composable
fun PlayerCard(player: Player, ranking: rankings, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SubcomposeAsyncImage(
                model = player.profilePictureUrl,
                contentDescription = player.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                loading = {
                    CircularProgressIndicator()
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default Profile Icon",
                        modifier = Modifier.size(80.dp)
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = player.name ?: "Unknown",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "XP: ${ranking.XPpoints}",
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun UpcomingGamesSection() {
    val games = listOf(
        Game("Street Fighter", "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1172540/ss_ae8ae2947e0789d322ffc1cdddf0671888336da8.1920x1080.jpg?t=1684260292"),
        Game("Tekken 7", "https://w0.peakpx.com/wallpaper/856/677/HD-wallpaper-tekken-7-fighters.jpg"),
        Game("PUBG", "https://i.ebayimg.com/images/g/ajAAAOSwbn1eNMu6/s-l1200.jpg"),
        Game("Fortnite", "https://m.media-amazon.com/images/M/MV5BMTZlMmIxM2EtN2Y4Zi00M2ZhLTk3NzgtNjJmZTU0MTQ3YjcwXkEyXkFqcGc%40._V1_FMjpg_UX1000_.jpg"),
        Game("Mortal Kombat 11", "https://upload.wikimedia.org/wikipedia/en/7/7e/Mortal_Kombat_11_cover_art.png"),
        Game("Guilty Gear Strive", "https://upload.wikimedia.org/wikipedia/en/7/7d/Guilty_Gear_Strive.jpg")
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { // Fill at least 90% width
        Text(
            text = "Upcoming Games",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Multiplayer games coming soon",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f),
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Vertical list instead of LazyRow
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            games.forEach { game ->
                GameCard(game, isMyGame = true)
            }
        }
    }
}

@Composable
fun GameCard(game: Game, isMyGame: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f).padding(horizontal = 8.dp) // Ensure each card fills 90% width
            .height(if (isMyGame) 200.dp else 140.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = game.imageUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Error loading image"
                        )
                    }
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black,
                            ),
                            startY = 0.1f

                        )
                    )
            )
            Text(
                text = game.title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (isMyGame) 16.sp else 20.sp
            )
        }
    }
}
