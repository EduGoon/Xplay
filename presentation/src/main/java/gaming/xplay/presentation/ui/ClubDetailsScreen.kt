package gaming.xplay.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter.State
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import gaming.xplay.data.model.ClubPost
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.RankingType
import gaming.xplay.presentation.model.PlayerSearchResult
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ClubDetailsViewModel
import gaming.xplay.presentation.viewmodel.CreatePostState
import gaming.xplay.presentation.viewmodel.CreateTournamentState
import gaming.xplay.presentation.viewmodel.JoinClubActionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

private enum class ClubSection {
    Members, Tournaments
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDetailsScreen(
    navController: NavController,
    clubDetailsViewModel: ClubDetailsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val clubState by clubDetailsViewModel.club.collectAsState()
    val membersState by clubDetailsViewModel.members.collectAsState()
    val rankingsState by clubDetailsViewModel.rankings.collectAsState()
    val tournamentsState by clubDetailsViewModel.tournaments.collectAsState()
    val clubPostsState by clubDetailsViewModel.clubPosts.collectAsState()
    val createPostState by clubDetailsViewModel.createPostState.collectAsState()
    val createTournamentState by clubDetailsViewModel.createTournamentState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf<ClubSection?>(null) }
    var postText by remember { mutableStateOf("") } // Hoisted state

    val isRefreshing by clubDetailsViewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val isNewAdmin = navController.currentBackStackEntry?.arguments?.getBoolean("isNewAdmin") ?: false
    var showNewAdminMessage by rememberSaveable { mutableStateOf(isNewAdmin) }


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

    LaunchedEffect(createPostState) {
        when (val state = createPostState) {
            is CreatePostState.Success -> {
                postText = "" // Clear text field on success
                snackbarHostState.showSnackbar("Post created successfully!")
                clubDetailsViewModel.resetCreatePostState()
            }

            is CreatePostState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                clubDetailsViewModel.resetCreatePostState()
            }

            else -> {}
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (selectedSection == null && clubState is UiState.Success && (clubState as UiState.Success).data.memberIds.contains(currentUser?.uid)) {
                Surface(shadowElevation = 8.dp) {
                    val isPosting = createPostState is CreatePostState.Loading
                    CreatePostInput(
                        text = postText,
                        onTextChange = { postText = it },
                        isLoading = isPosting
                    ) {
                        if (postText.isNotBlank()) {
                            currentUser?.let { user ->
                                clubDetailsViewModel.createClubPost(postText, user.uid, user.name)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { clubDetailsViewModel.refreshClubDetails() },
            state = pullToRefreshState,
            modifier = Modifier.padding(paddingValues)
        ) {
            when (val state = clubState) {
                is UiState.Loading -> {
                    if (!isRefreshing) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is UiState.Success -> {
                    val club = state.data
                    val isMember = club.memberIds.contains(currentUser?.uid)

                    if (showCreateTournamentDialog) {
                        CreateTournamentDialog(
                            createTournamentState = createTournamentState,
                            onConfirm = { tournamentName, rankingType ->
                                currentUser?.uid?.let { adminId ->
                                    clubDetailsViewModel.createTournament(tournamentName, adminId, rankingType)
                                }
                            },
                            onDismiss = { showCreateTournamentDialog = false }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                                        is State.Error,
                                        is State.Empty -> {
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

                        // Main content area for posts
                        when (selectedSection) {
                            null -> {
                                if (showNewAdminMessage) {
                                    item {
                                        NewAdminMessage { showNewAdminMessage = false }
                                    }
                                }
                                // Show posts when no section is selected
                                when (val postsState = clubPostsState) {
                                    is UiState.Loading -> {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                    }

                                    is UiState.Error -> {
                                        item {
                                            Text(
                                                text = postsState.message,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }

                                    is UiState.Success -> {
                                        if (postsState.data.isEmpty()) {
                                            item {
                                                EmptyState(
                                                    icon = Icons.Outlined.Info,
                                                    text = "No posts for this club"
                                                )
                                            }
                                        } else {
                                            items(postsState.data) { post ->
                                                ClubPostItem(post, authViewModel)
                                                HorizontalDivider(thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }

                            ClubSection.Members -> {
                                // Show members
                                if ((membersState as? UiState.Success)?.data?.isEmpty() == true) {
                                    item {
                                        EmptyState(
                                            icon = Icons.Outlined.Groups,
                                            text = "No members yet. Be the first to join!"
                                        )
                                    }
                                } else {
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
                                                .then(
                                                    if (isCurrentUser) Modifier.border(
                                                        2.dp,
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    else Modifier
                                                ),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
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
                                }

                                // Join Button
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
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

                            ClubSection.Tournaments -> {
                                // Show tournaments
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
                                                EmptyState(
                                                    icon = Icons.Outlined.EmojiEvents,
                                                    text = "No tournaments yet!"
                                                )
                                            }
                                        } else {
                                            items(tournamentUiState.data) { tournament ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                                        .clickable { navController.navigate("tournamentscreen/${tournament.tournamentId}") },
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    )
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
    }
}

@Composable
fun NewAdminMessage(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Welcome, Club Admin!", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "You can now manage your club. Here are a few tips to get you started:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text("• Go to the 'Tournaments' tab to create new tournaments.")
                Text("• As an admin, you can start tournaments and submit match results.")
                Text("• You can also manage your club members.")
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
fun ClubPostItem(post: ClubPost, authViewModel: AuthViewModel) {
    var author by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(post.authorId) {
        author = authViewModel.getPlayerProfile(post.authorId)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = author?.profilePictureUrl ?: "",
            contentDescription = "Author Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.authorName ?: "Unknown",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                post.timestamp?.let {
                    Text(
                        text = getFormattedDateTime(it.time / 1000),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(post.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun getFormattedDateTime(epochSeconds: Long): String {
    val date = Date(epochSeconds * 1000)
    val dayFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return "${dayFormat.format(date)} • ${timeFormat.format(date)}"
}

@Composable
fun CreatePostInput(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    onPost: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextField(
                value = text,
                onValueChange = { if (it.length <= 150) onTextChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                enabled = !isLoading,
                placeholder = {
                    Text("Write a post…")
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null
                    )
                },
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(12.dp))

            FilledIconButton(
                onClick = onPost,
                enabled = text.isNotBlank() && !isLoading,
                shape = CircleShape
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Outlined.Send,
                        contentDescription = "Post"
                    )
                }
            }
        }
    }
}

@Composable
fun CreateTournamentDialog(
    createTournamentState: CreateTournamentState,
    onConfirm: (String, RankingType) -> Unit,
    onDismiss: () -> Unit
) {
    var tournamentName by remember { mutableStateOf("") }
    var isRankedGlobally by remember { mutableStateOf(true) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLoading = createTournamentState is CreateTournamentState.Loading

    LaunchedEffect(createTournamentState) {
        if (createTournamentState is CreateTournamentState.Success) {
            keyboardController?.hide()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Create Tournament",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Configure tournament visibility and ranking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // Tournament name section
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Tournament Info",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tournamentName,
                        onValueChange = { tournamentName = it },
                        label = { Text("Tournament name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }

                // Ranking type section
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Ranking Type",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RankingOptionCard(
                        selected = isRankedGlobally,
                        icon = Icons.Default.Public,
                        title = "Global Ranking",
                        description = "Results affect the global leaderboard",
                        onClick = { isRankedGlobally = true },
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    RankingOptionCard(
                        selected = !isRankedGlobally,
                        icon = Icons.Default.Groups,
                        title = "Local Ranking",
                        description = "Results are limited to this tournament",
                        onClick = { isRankedGlobally = false },
                        enabled = !isLoading
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = tournamentName.isNotBlank() && !isLoading,
                onClick = {
                    val rankingType =
                        if (isRankedGlobally) RankingType.GLOBAL else RankingType.LOCAL
                    onConfirm(tournamentName.trim(), rankingType)
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RankingOptionCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val borderColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline

    val backgroundColor = if (selected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    else
        MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, enabled = enabled)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
