package gaming.xplay.presentation.ui

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.rankings
import gaming.xplay.presentation.model.PlayerSearchResult
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.AuthViewModel
import gaming.xplay.presentation.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun LeaderboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by authViewModel.searchResults.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val leaderboardState by gameViewModel.leaderboard.collectAsState()
    val leaderboardPlayerProfiles by gameViewModel.leaderboardPlayerProfiles.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val hasConnection by authViewModel.hasConnection.collectAsState()
    val showOfflineError by authViewModel.showOfflineError.collectAsState()

    LaunchedEffect(showOfflineError) {
        if (showOfflineError) {
            snackbarHostState.showSnackbar("You're offline. Please check your connection.")
            authViewModel.dismissOfflineError()
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            gameViewModel.fetchLeaderboard("FIFA")
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            SearchBar(
                searchQuery = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    if (hasConnection) {
                        authViewModel.searchPlayers(searchQuery)
                    } else {
                        authViewModel.showOfflineError
                    }
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (searchQuery.isBlank()) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LeaderboardSection(
                        leaderboardState = leaderboardState,
                        playerProfiles = leaderboardPlayerProfiles,
                        currentUser = currentUser,
                        onRefresh = {
                            if (hasConnection) {
                                gameViewModel.fetchLeaderboard("FIFA")
                            } else {
                                authViewModel.showOfflineError
                            }
                        },
                        navController = navController
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val leaderboardList =
                        (leaderboardState as? UiState.Success<List<rankings>>)?.data.orEmpty()

                    LeaderboardScatterChart(
                        rankings = leaderboardList,
                        playerProfiles = leaderboardPlayerProfiles,
                        currentUser = currentUser
                    )
                }
            } else {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    SearchResultsList(
                        results = searchResults,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(searchQuery: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = { Text("Search for players...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
        ),
        singleLine = true
    )
}

@Composable
fun SearchResultsList(
    results: List<PlayerSearchResult>,
    navController: NavController
) {
    if (results.isEmpty()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "No players found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(results) { result ->
                PlayerRow(
                    result = result,
                    onClick = {
                        val player = result.player
                        val ranking = result.ranking
                        val xp = ranking?.XPpoints ?: 0
                        val wins = ranking?.wins ?: 0
                        val losses = ranking?.losses ?: 0
                        navController.navigate("profile/${player.uid}/$xp/$wins/$losses")
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun PlayerRow(
    result: PlayerSearchResult,
    onClick: () -> Unit
) {
    val player = result.player
    val ranking = result.ranking

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // You can add a player avatar here later
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name ?: "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "XP: ${ranking?.XPpoints ?: "N/A"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "${ranking?.wins ?: "N/A"} Wins • ${ranking?.losses ?: "N/A"} Losses",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 100.dp)
        )
    }
}

@Composable
fun LeaderboardSection(
    leaderboardState: UiState<List<rankings>>,
    playerProfiles: Map<String, Player?>,
    currentUser: Player?,
    onRefresh: () -> Unit,
    navController: NavController
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🏆 Leaderboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = { onRefresh() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh leaderboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when (leaderboardState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is UiState.Success -> {
                if (leaderboardState.data.isEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No rankings yet. Be the first one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        leaderboardState.data.forEachIndexed { index, ranking ->
                            val player = playerProfiles[ranking.playerid]
                            RankingRow(
                                ranking = ranking,
                                rank = index + 1,
                                player = player,
                                isCurrentUser = currentUser?.uid == ranking.playerid,
                            ) { playerId, xpPoints, wins, losses ->
                                if (currentUser?.uid == playerId) {
                                    navController.navigate("myprofile")
                                } else {
                                    navController.navigate("profile/$playerId/$xpPoints/$wins/$losses")
                                }
                            }
                            if (index != leaderboardState.data.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = leaderboardState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun RankingRow(
    ranking: rankings,
    rank: Int,
    player: Player?,
    isCurrentUser: Boolean,
    onClick: (String, Int, Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(ranking.playerid, ranking.XPpoints, ranking.wins, ranking.losses) }
            .background(if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(32.dp)
        )
        // You can add player avatar here
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player?.name ?: "Loading...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "XP: ${ranking.XPpoints}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${ranking.wins} W / ${ranking.losses} L",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 80.dp)
        )
    }
}

@Composable
fun LeaderboardScatterChart(
    rankings: List<rankings>,
    playerProfiles: Map<String, Player?>,
    currentUser: Player?,
    modifier: Modifier = Modifier
) {
    if (rankings.isEmpty()) return

    // ---------- Data ----------
    val chartData = remember(rankings, currentUser) {
        rankings.mapNotNull { r ->
            val games = r.wins + r.losses
            if (games == 0) null else ChartPoint(
                x = r.wins.toFloat() / games,
                y = r.XPpoints.toFloat(),
                isCurrentUser = r.playerid == currentUser?.uid,
                playerId = r.playerid
            )
        }
    }
    if (chartData.isEmpty()) return

    // ---------- Axis ----------
    val xMin = 0f
    val xMax = 1f
    val yMinData = chartData.minOf { it.y }
    val yMaxData = chartData.maxOf { it.y }
    val yPad = (yMaxData - yMinData).takeIf { it > 0 }?.times(0.1f) ?: 10f
    val yMin = yMinData - yPad
    val yMax = yMaxData + yPad

    // ---------- Density calculation ----------
    val densityMap = remember(chartData) {
        chartData.associate { p ->
            val neighbors = chartData.count { o ->
                hypot(p.x - o.x, p.y - o.y) < 0.12f
            }
            p.playerId to neighbors.coerceAtLeast(1)
        }
    }

    // ---------- Colors ----------
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val axis = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    // ---------- Interaction state ----------
    var tappedPlayer by remember { mutableStateOf<String?>(null) }
    var tappedOffset by remember { mutableStateOf<Offset?>(null) }
    val avatarAlpha by animateFloatAsState(if (tappedPlayer != null) 1f else 0f, label = "")
    val avatarScale by animateFloatAsState(if (tappedPlayer != null) 1f else 0.6f, label = "")

    // Auto-dismiss avatar
    LaunchedEffect(tappedPlayer) {
        if (tappedPlayer != null) {
            delay(1400)
            tappedPlayer = null
            tappedOffset = null
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text("XP vs Win Rate", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)) {

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 70.dp, bottom = 60.dp, top = 20.dp, end = 20.dp)
                    .pointerInput(chartData) {
                        detectTapGestures { tap ->
                            val w = size.width
                            val h = size.height
                            fun nx(x: Float) = (x - xMin) / (xMax - xMin)
                            fun ny(y: Float) = (y - yMin) / (yMax - yMin)

                            chartData.firstOrNull { p ->
                                val px = nx(p.x) * w
                                val py = h - ny(p.y) * h
                                (Offset(px, py) - tap).getDistance() < 14.dp.toPx()
                            }?.let { hit ->
                                tappedPlayer = hit.playerId
                                tappedOffset = Offset(nx(hit.x) * w, h - ny(hit.y) * h)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                fun nx(x: Float) = (x - xMin) / (xMax - xMin)
                fun ny(y: Float) = (y - yMin) / (yMax - yMin)

                // ---------- Grid + Y labels (spacing fix) ----------
                val ticks = 5
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 32f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                for (i in 0..ticks) {
                    val y = h - i * h / ticks
                    drawLine(grid, Offset(0f, y), Offset(w, y), 1f)

                    val value = yMin + i * (yMax - yMin) / ticks
                    drawContext.canvas.nativeCanvas.drawText(
                        value.roundToInt().toString(),
                        -30f,   // ← FIX: extra spacing from axis
                        y + 10f,
                        paint
                    )
                }

                // Axes
                drawLine(axis, Offset(0f, h), Offset(w, h), 2f)
                drawLine(axis, Offset(0f, 0f), Offset(0f, h), 2f)

                // ---------- Points (density-aware) ----------
                chartData.forEach { p ->
                    val density = densityMap[p.playerId] ?: 1
                    val alpha = (1f / density).coerceIn(0.35f, 1f)
                    val radius = (10f / density).coerceIn(6f, 12f)

                    val x = nx(p.x) * w
                    val y = h - ny(p.y) * h

                    if (p.isCurrentUser) {
                        drawCircle(primary.copy(alpha = 0.25f), 18f, Offset(x, y))
                    }

                    drawCircle(
                        color = if (p.isCurrentUser) primary else secondary.copy(alpha = alpha),
                        radius = if (p.isCurrentUser) 12f else radius,
                        center = Offset(x, y)
                    )
                }
            }

            // ---------- Playful avatar pop ----------
            tappedPlayer?.let { id ->
                val player = playerProfiles[id]
                val pos = tappedOffset
                if (player != null && pos != null) {
                    AsyncImage(
                        model = player.profilePictureUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = avatarAlpha
                                scaleX = avatarScale
                                scaleY = avatarScale
                            }
                            .size(60.dp)
                            .offset {
                                IntOffset(pos.x.roundToInt() - 24, pos.y.roundToInt() - 24)
                            }
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

// Data classes
private data class ChartPoint(
    val x: Float,
    val y: Float,
    val isCurrentUser: Boolean,
    val playerId: String
)
