package gaming.xplay.presentation.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import gaming.xplay.presentation.ui.State.UiState
import gaming.xplay.presentation.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val adminClubsState by notificationViewModel.adminClubs.collectAsState()
    val pendingMembersState by notificationViewModel.pendingMembers.collectAsState()
    val joinRequestState by notificationViewModel.joinRequestState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(joinRequestState) {
        when (val state = joinRequestState) {
            is UiState.Success -> scope.launch { snackbarHostState.showSnackbar("Operation successful") }
            is UiState.Error -> scope.launch { snackbarHostState.showSnackbar(state.message) }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Join Club Requests",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                adminClubsState is UiState.Loading || pendingMembersState is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                adminClubsState is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = (adminClubsState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                pendingMembersState is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = (pendingMembersState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                adminClubsState is UiState.Success && pendingMembersState is UiState.Success -> {
                    val clubs = (adminClubsState as UiState.Success).data
                    val pendingMembers = (pendingMembersState as UiState.Success).data

                    if (clubs.isEmpty() || pendingMembers.values.all { it.isEmpty() }) {
                        EmptyState(
                            icon = Icons.Outlined.Notifications,
                            text = "No new join requests"
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {                            clubs.forEach { club ->
                                val members = pendingMembers[club.clubId] ?: emptyList()
                                if (members.isNotEmpty()) {
                                    items(members) { member ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = CardDefaults.cardElevation(4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Text(
                                                    text = club.clubName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Person,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(28.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        member.name?.let {
                                                            Text(
                                                                text = it,
                                                                fontWeight = FontWeight.Medium,
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                        }
                                                        Text(
                                                            text = "Wants to join your club",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    val isLoading = joinRequestState is UiState.Loading
                                                    Button(
                                                        onClick = {
                                                            notificationViewModel.declineJoinRequest(
                                                                club.clubId,
                                                                member.uid,
                                                                club.clubName
                                                            )
                                                        },
                                                        enabled = !isLoading,
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                    ) {
                                                        Text("Decline", color = MaterialTheme.colorScheme.onError)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            notificationViewModel.approveJoinRequest(
                                                                club.clubId,
                                                                member.uid,
                                                                club.clubName
                                                            )
                                                        },
                                                        enabled = !isLoading
                                                    ) {
                                                        Text("Accept")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (joinRequestState is UiState.Loading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
