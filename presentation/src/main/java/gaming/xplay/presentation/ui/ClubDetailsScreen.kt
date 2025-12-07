package gaming.xplay.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import gaming.xplay.presentation.model.PlayerSearchResult
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ClubDetailsViewModel
import gaming.xplay.presentation.viewmodel.CreateTournamentState
import gaming.xplay.presentation.viewmodel.JoinClubActionState
import kotlinx.coroutines.flow.collectLatest

private enum class ClubSection {
    Members, Tournaments
}

@Composable
fun ClubDetailsScreen(
    clubDetailsViewModel: ClubDetailsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val clubState by clubDetailsViewModel.club.collectAsState()
    val membersState by clubDetailsViewModel.members.collectAsState()
    val rankingsState by clubDetailsViewModel.rankings.collectAsState()
    val tournamentsState by clubDetailsViewModel.tournaments.collectAsState()
    val createTournamentState by clubDetailsViewModel.createTournamentState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf<ClubSection?>(null) }

    LaunchedEffect(Unit) {
        clubDetailsViewModel.joinClubActionState.collectLatest { state ->
            when (state) {
                is JoinClubActionState.Success -> {
                    snackbarHostState.showSnackbar("Request sent successfully!")
                }

                is JoinClubActionState.Error -> {
                    snackbarHostState.showSnackbar(state.message)
                }

                else -> {}
            }
        }
    }

    LaunchedEffect(createTournamentState) {
        when (val state = createTournamentState) {
            is CreateTournamentState.Success -> {
                snackbarHostState.showSnackbar("Tournament created successfully!")
                showCreateTournamentDialog = false
            }

            is CreateTournamentState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }

            else -> {}
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = clubState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UiState.Success -> {
                val club = state.data

                if (showCreateTournamentDialog) {
                    CreateTournamentDialog(
                        onConfirm = { tournamentName ->
                            currentUser?.uid?.let { adminId ->
                                clubDetailsViewModel.createTournament(tournamentName, adminId)
                            }
                        },
                        onDismiss = { showCreateTournamentDialog = false }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {

                    // Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = club.imageUrl,
                                contentDescription = club.clubName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                when (painter.state) {
                                    is AsyncImagePainter.State.Error,
                                    is AsyncImagePainter.State.Empty -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                        )
                                                    )
                                                )
                                        )
                                    }

                                    else -> SubcomposeAsyncImageContent()
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.35f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = club.clubName,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { selectedSection = if (selectedSection == ClubSection.Members) null else ClubSection.Members },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedSection == ClubSection.Members) Color.White else Color.White.copy(alpha = 0.2f),
                                            contentColor = if (selectedSection == ClubSection.Members) MaterialTheme.colorScheme.primary else Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("Members (${club.members})")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { selectedSection = if (selectedSection == ClubSection.Tournaments) null else ClubSection.Tournaments },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedSection == ClubSection.Tournaments) Color.White else Color.White.copy(alpha = 0.2f),
                                            contentColor = if (selectedSection == ClubSection.Tournaments) MaterialTheme.colorScheme.primary else Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("Tournaments")
                                    }
                                }
                            }
                        }
                    }

                    if (selectedSection == ClubSection.Members) {
                        items((membersState as? UiState.Success)?.data ?: emptyList()) { member ->
                            val ranking = ((rankingsState as? UiState.Success)?.data ?: emptyList())
                                .find { ranking -> ranking.playerid == member.uid }
                            val playerSearchResult = PlayerSearchResult(member, ranking)
                            val isAdmin = club.adminId == member.uid
                            val isCurrentUser = currentUser?.uid == member.uid

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .then(
                                        if (isCurrentUser)
                                            Modifier.border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                        else Modifier
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    PlayerRow(
                                        result = playerSearchResult,
                                        onClick = {}
                                    )

                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        AdminBadge()
                                    }
                                }
                            }
                        }
                        // Join Button
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            val isMember = club.memberIds.contains(currentUser?.uid)
                            val isPending = club.pendingMemberIds.contains(currentUser?.uid)
                            Button(
                                onClick = {
                                    currentUser?.uid?.let { userId ->
                                        clubDetailsViewModel.joinClub(
                                            userId,
                                            club,
                                            currentUser?.name ?: "A player"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                enabled = currentUser != null && club.adminId != currentUser?.uid && !isMember && !isPending
                            ) {
                                when {
                                    isMember -> Text("You are a member")
                                    isPending -> Text("Request Sent")
                                    else -> Text("Join Club")
                                }
                            }
                        }
                    }

                    if (selectedSection == ClubSection.Tournaments) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tournaments",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (club.adminId == currentUser?.uid) {
                                    Button(
                                        onClick = { showCreateTournamentDialog = true },
                                        shape = RoundedCornerShape(50),
                                    ) {
                                        Text("Create")
                                    }
                                }
                            }
                        }

                        // Tournaments List
                        when (val tournamentUiState = tournamentsState) {
                            is UiState.Loading -> {
                                item {
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                }
                            }

                            is UiState.Error -> {
                                item {
                                    Text(
                                        text = tournamentUiState.message,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }

                            is UiState.Success -> {
                                if (tournamentUiState.data.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No tournaments yet.",
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                } else {
                                    items(tournamentUiState.data) { tournament ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = tournament.name,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = tournament.status,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateTournamentDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tournamentName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tournament") },
        text = {
            OutlinedTextField(
                value = tournamentName,
                onValueChange = { newName -> tournamentName = newName },
                label = { Text("Tournament Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tournamentName.isNotBlank()) {
                        onConfirm(tournamentName)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Admin",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}
