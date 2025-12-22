package gaming.xplay.presentation.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import gaming.xplay.data.model.Badge
import gaming.xplay.data.model.Club
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.rankings
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.ClubViewModel
import gaming.xplay.presentation.viewmodel.GameViewModel
import gaming.xplay.presentation.viewmodel.UpdateProfileState
import kotlinx.coroutines.launch

// Helper function to map Badge enum to a Compose Color
@Composable
fun badgeColor(badge: Badge): Color {
    return when (badge) {
        Badge.BRONZE -> Color(0xFFA97142)
        Badge.SILVER -> Color(0xFFA8A8A8)
        Badge.GOLD -> Color(0xFFE5B533)
        Badge.DIAMOND -> Color(0xFF80DEEA)
    }
}

@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    clubViewModel: ClubViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val clubsState by clubViewModel.clubs.collectAsState()
    val createClubState by clubViewModel.createClubState.collectAsState()
    val leaderboardState by gameViewModel.leaderboard.collectAsState()
    val updateProfileState by authViewModel.updateProfileState.collectAsState()

    var selectedDrawerItem by remember { mutableStateOf("Profile") }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val hasConnection by authViewModel.hasConnection.collectAsState()
    val showOfflineError by authViewModel.showOfflineError.collectAsState()
    var badgesExpanded by remember { mutableStateOf(false) }

    val currentBadge = currentUser?.currentBadge?.let { badgeName ->
        Badge.values().find { it.name == badgeName }
    }
    val badgeMainColor = currentBadge?.let { badgeColor(it) }

    LaunchedEffect(showOfflineError) {
        if (showOfflineError) {
            snackbarHostState.showSnackbar("You're offline. Please check your connection.")
            authViewModel.dismissOfflineError()
        }
    }


    LaunchedEffect(Unit) {
        authViewModel.refreshCurrentUser()
        gameViewModel.fetchLeaderboard("FIFA")
    }

    LaunchedEffect(createClubState) {
        if (createClubState is UiState.Success) authViewModel.refreshCurrentUser()
    }

    LaunchedEffect(updateProfileState) {
        when (val state = updateProfileState) {
            is UpdateProfileState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Profile updated successfully!")
                }
                showEditProfileDialog = false
                authViewModel.resetUpdateProfileState()
            }

            is UpdateProfileState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
                authViewModel.resetUpdateProfileState()
            }

            else -> {}
        }
    }

    val userRanking = (leaderboardState as? UiState.Success)?.data
        ?.find { it.playerid == currentUser?.uid }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentUser = currentUser,
            updateProfileState = updateProfileState,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, imageUri ->
                if (hasConnection) {
                    authViewModel.updateUserProfile(name, imageUri)
                } else {
                    authViewModel.showOfflineError
                }
            }
        )
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                authViewModel.signOut()
                showLogoutDialog = false
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(12.dp))

                    val drawerColors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = badgeMainColor ?: MaterialTheme.colorScheme.primary,
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = selectedDrawerItem == "Profile",
                        onClick = {
                            selectedDrawerItem = "Profile"
                            scope.launch { drawerState.close() }
                        },
                        colors = drawerColors
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "Match History") },
                        label = { Text("Match History") },
                        selected = selectedDrawerItem == "Match History",
                        onClick = {
                            selectedDrawerItem = "Match History"
                            scope.launch { drawerState.close() }
                        },
                        colors = drawerColors
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Group, contentDescription = "My Clubs") },
                        label = { Text("My Clubs") },
                        selected = selectedDrawerItem == "My Clubs",
                        onClick = {
                            selectedDrawerItem = "My Clubs"
                            scope.launch { drawerState.close() }
                        },
                        colors = drawerColors
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = "Badges") },
                        label = { Text("Badges") },
                        badge = {
                            Icon(
                                imageVector = if (badgesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (badgesExpanded) "Collapse" else "Expand"
                            )
                        },
                        selected = false,
                        onClick = { badgesExpanded = !badgesExpanded }
                    )

                    AnimatedVisibility(badgesExpanded) {
                        Column(Modifier.padding(start = 32.dp)) {
                            Badge.values().forEach { badge ->
                                BadgeItem(
                                    name = badge.displayName,
                                    color = badgeColor(badge),
                                    isUnlocked = currentUser?.unlockedBadges?.contains(badge.name) == true
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    val logoutDrawerColors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = MaterialTheme.colorScheme.error,
                        unselectedIconColor = MaterialTheme.colorScheme.error
                    )

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Log Out"
                            )
                        },
                        label = { Text("Log Out") },
                        selected = false,
                        onClick = { showLogoutDialog = true },
                        colors = logoutDrawerColors
                    )
                }
            }
        }
    ) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
            val screenModifier = if (badgeMainColor != null) {
                Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                badgeMainColor.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            } else {
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            }
            Column(
                modifier = screenModifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {

                // ---------------- Header Panel ----------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            if (badgeMainColor != null) {
                                badgeMainColor.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        )
                ) {
                    SubcomposeAsyncImage(
                        model = currentUser?.profilePictureUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    ) {
                        when (painter.state) {
                            is coil.compose.AsyncImagePainter.State.Error,
                            is coil.compose.AsyncImagePainter.State.Empty -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (badgeMainColor != null) {
                                            badgeMainColor.copy(alpha = 0.7f)
                                        } else {
                                            Color(
                                                MaterialTheme.colorScheme.primary.red,
                                                MaterialTheme.colorScheme.primary.green,
                                                MaterialTheme.colorScheme.primary.blue,
                                                0.4f
                                            )
                                        }
                                    )
                            )

                            else -> SubcomposeAsyncImageContent()
                        }
                    }

                    AsyncImage(
                        model = currentUser?.profilePictureUrl ?: "",
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 16.dp, y = 60.dp)
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(
                                3.dp,
                                if (badgeMainColor != null) badgeMainColor else MaterialTheme.colorScheme.background,
                                CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )

                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }

                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(70.dp))
                currentUser?.let {
                    it.name?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                when (selectedDrawerItem) {
                    "Profile" -> {
                        ProfileSummary(userRanking = userRanking, currentUser = currentUser)
                    }

                    "Match History" -> {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            MatchHistory(gameViewModel, authViewModel, currentUser?.uid ?: "")
                        }
                    }

                    "My Clubs" -> {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            when (val state = clubsState) {
                                is UiState.Loading -> {
                                    Box(
                                        Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                is UiState.Success -> {
                                    val all = state.data
                                    val my = all.filter {
                                        it.memberIds.contains(currentUser?.uid) ||
                                                it.adminId == currentUser?.uid
                                    }

                                    if (my.isNotEmpty()) {
                                        my.forEach { club ->
                                            ClubCard(
                                                club = club,
                                                isAdmin = club.adminId == currentUser?.uid
                                            )
                                            Spacer(Modifier.height(16.dp))
                                        }
                                    } else {
                                        Text("You haven't joined any clubs yet.")
                                    }
                                }

                                is UiState.Error -> {
                                    Text(state.message)
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
fun ProfileSummary(
    userRanking: rankings?,
    currentUser: Player?
) {
    val xp = userRanking?.XPpoints ?: 0
    val wins = userRanking?.wins ?: 0
    val losses = userRanking?.losses ?: 0
    val currentBadge = currentUser?.currentBadge?.let { badgeName ->
        Badge.values().find { it.name == badgeName }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // ---------------- XP Panel ----------------
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val badgeName = currentBadge?.displayName ?: "No Badge"
                Text("$badgeName Tier", style = MaterialTheme.typography.titleMedium)
                XPBar(xp = xp, badge = currentBadge)
                val xpText = "${xp % 100} / 100 XP"
                Text(
                    xpText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---------------- Info Cards ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCard(title = "Wins", value = wins.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Losses", value = losses.toString(), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        WinLossDonutChart(
            wins = wins,
            losses = losses,
            modifier = Modifier
                .fillMaxWidth(),
            winColor = currentBadge?.let { badgeColor(it) } ?: MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun XPBar(xp: Int, badge: Badge?) {
    // Define colors from the theme here, within the composable context.
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val positiveColor = MaterialTheme.colorScheme.primary
    val negativeColor = MaterialTheme.colorScheme.error
    val barColor = if (badge != null) badgeColor(badge) else Color.Transparent

    val backgroundColor = Color(
        onSurfaceColor.red,
        onSurfaceColor.green,
        onSurfaceColor.blue,
        0.12f
    )
    val zeroMarkerColor = Color(
        onSurfaceColor.red,
        onSurfaceColor.green,
        onSurfaceColor.blue,
        0.5f
    )

    // State for Badged Player
    val badgeProgress = if (badge != null) (xp % 100) / 100f else 0f
    val animatedBadgeProgress by animateFloatAsState(targetValue = badgeProgress, label = "xp_badge_progress")

    // State for Regular Player
    val regularProgress = if (badge == null) xp.coerceIn(-100, 100) / 100f else 0f
    val animatedRegularProgress by animateFloatAsState(targetValue = regularProgress, label = "xp_regular_progress")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background track for all cases
            drawRoundRect(
                color = backgroundColor,
                size = size,
            )

            if (badge != null) {
                // Badged player: Left-to-right progress bar
                drawRoundRect(
                    color = barColor,
                    size = Size(width = size.width * animatedBadgeProgress, height = size.height),
                )
            } else {
                // Regular player: Centered progress bar for -100 to 100
                val center = size.width / 2

                // Draw progress
                if (animatedRegularProgress != 0f) {
                    val color = if (animatedRegularProgress > 0) positiveColor else negativeColor
                    val start = center
                    val end = center + center * animatedRegularProgress
                    drawLine(
                        color = color,
                        start = Offset(if (start < end) start else end, size.height / 2),
                        end = Offset(if (start < end) end else start, size.height / 2),
                        strokeWidth = size.height,
                        cap = StrokeCap.Butt
                    )
                }

                // Draw zero marker
                drawLine(
                    color = zeroMarkerColor,
                    start = Offset(center, 0f),
                    end = Offset(center, size.height),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Are you sure you want to log out?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("No")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Yes")
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentUser: Player?,
    updateProfileState: UpdateProfileState,
    onDismiss: () -> Unit,
    onSave: (String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val isNameInvalid = name.length > 10

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            imageUri = uri
        }
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Edit Profile", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))

                Box(modifier = Modifier.clickable { imagePicker.launch("image/*") }) {
                    AsyncImage(
                        model = imageUri ?: currentUser?.profilePictureUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Image",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isNameInvalid,
                    supportingText = {
                        Text(
                            text = "${name.length} / 10",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    }
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, imageUri) },
                        enabled = updateProfileState !is UpdateProfileState.Loading && !isNameInvalid && name.isNotEmpty()
                    ) {
                        if (updateProfileState is UpdateProfileState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}


@Composable
fun ClubCard(club: Club, isAdmin: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(
                text = club.clubName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(Modifier.height(8.dp))

            // Clean Member Row + Green “Admin” Tag
            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(
                    text = "Members: ${club.members}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isAdmin) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x403DDC84))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Admin",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1B8A5A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WinLossDonutChart(
    wins: Int,
    losses: Int,
    modifier: Modifier = Modifier,
    winColor: Color = MaterialTheme.colorScheme.primary
) {
    val totalGames = wins + losses
    if (totalGames == 0) {
        Box(
            modifier = modifier
                .height(150.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("Play a match to see your stats! ", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val winPercentage = wins.toFloat() / totalGames.toFloat()
    val winAngle = 360 * winPercentage

    val animatedSweepAngle by animateFloatAsState(
        targetValue = winAngle,
        animationSpec = tween(durationMillis = 1000, delayMillis = 250),
        label = "winLossDonut"
    )

    val winRate = (winPercentage * 100).toInt()

    val lossColor = Color(
        MaterialTheme.colorScheme.error.red,
        MaterialTheme.colorScheme.error.green,
        MaterialTheme.colorScheme.error.blue,
        0.2f
    )

    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            // Background arc (losses)
            drawArc(
                color = lossColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
            // Foreground arc (wins)
            drawArc(
                color = winColor,
                startAngle = -90f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$winRate%",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = "Win Rate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BadgeItem(name: String, color: Color, isUnlocked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isUnlocked) Icons.Default.WorkspacePremium else Icons.Default.Lock,
            contentDescription = null,
            tint = if (isUnlocked) color else {
                val c = MaterialTheme.colorScheme.onSurface
                Color(c.red, c.green, c.blue, 0.6f)
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else {
                val c = MaterialTheme.colorScheme.onSurface
                Color(c.red, c.green, c.blue, 0.6f)
            }
        )
    }
}
