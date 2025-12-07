package gaming.xplay.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import gaming.xplay.data.model.Club
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    clubViewModel: ClubViewModel = hiltViewModel()
) {
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
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(32.dp))
                FifaClubsSection(clubViewModel, authViewModel, navController)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                UpcomingGamesSection()
            }
        }
    }
}

@Composable
fun FifaClubsSection(clubViewModel: ClubViewModel, authViewModel: AuthViewModel, navController: NavController) {
    val clubsState by clubViewModel.clubs.collectAsState()
    var showCreateClubDialog by remember { mutableStateOf(false) }
    val currentUser by authViewModel.currentUser.collectAsState()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available FIFA clubs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = { showCreateClubDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Club", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Club")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (val state = clubsState) {
            is UiState.Loading -> {
                CircularProgressIndicator()
            }

            is UiState.Success -> {
                val clubs = state.data
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(clubs) { club ->
                        FifaClubCard(club = club) {
                            navController.navigate("clubdetails/${club.clubId}")
                        }
                    }
                }
            }

            is UiState.Error -> {
                Text(text = state.message)
            }
        }
    }

    if (showCreateClubDialog) {
        CreateClubDialog(
            onDismiss = { showCreateClubDialog = false },
            onCreateClub = {
                currentUser?.uid?.let { it1 ->
                    clubViewModel.createClub(
                        it,
                        it1
                    )
                }
                showCreateClubDialog = false
            }
        )
    }
}

@Composable
fun CreateClubDialog(onDismiss: () -> Unit, onCreateClub: (String) -> Unit) {
    var clubName by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Create a new club") },
        text = {
            TextField(
                value = clubName,
                onValueChange = { if (it.length <= 10) clubName = it },
                label = { Text("Club Name (Max 10 characters)") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onCreateClub(clubName) }) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FifaClubCard(club: Club, onCardClicked: () -> Unit) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(240.dp)
            .clickable { onCardClicked() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = club.imageUrl,
                contentDescription = club.clubName,
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
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 100f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = club.clubName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Test Location",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Members",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${club.members} members",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
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
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Multiplayer games with more that 2 possible outcomes coming soon",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
