package gaming.xplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Club
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.network.ConnectivityRepository
import gaming.xplay.data.repo.AuthRepository
import gaming.xplay.data.repo.ClubRepository
import gaming.xplay.data.repo.NotificationRepository
import gaming.xplay.presentation.ui.State.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val clubRepository: ClubRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val connectivityRepository: ConnectivityRepository
) : ViewModel() {

    private val _adminClubs = MutableStateFlow<UiState<List<Club>>>(UiState.Loading)
    val adminClubs: StateFlow<UiState<List<Club>>> = _adminClubs

    private val _pendingMembers = MutableStateFlow<UiState<Map<String, List<Player>>>>(UiState.Loading)
    val pendingMembers: StateFlow<UiState<Map<String, List<Player>>>> = _pendingMembers

    private val _joinRequestState = MutableStateFlow<Map<String, UiState<Unit>>>(emptyMap())
    val joinRequestState: StateFlow<Map<String, UiState<Unit>>> = _joinRequestState

    val hasConnection = connectivityRepository.hasConnection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        fetchAdminClubs()
    }

    fun fetchAdminClubs() {
        if (!hasConnection.value) {
            _adminClubs.value = UiState.Error("You're offline. Please check your connection.")
            return
        }
        viewModelScope.launch {
            _adminClubs.value = UiState.Loading
            when (val result = authRepository.fetchCurrentUserProfile()) {
                is Result.Success -> {
                    val currentUser = result.data
                    if (currentUser != null) {
                        when (val clubsResult = clubRepository.getAdminClubs(currentUser.uid)) {
                            is Result.Success -> {
                                val clubs = clubsResult.data
                                _adminClubs.value = UiState.Success(clubs)
                                fetchPendingMembersForClubs(clubs)
                            }
                            is Result.Error -> _adminClubs.value = UiState.Error(clubsResult.exception.message ?: "An error occurred")
                        }
                    } else {
                        _adminClubs.value = UiState.Error("User not logged in")
                    }
                }
                is Result.Error -> _adminClubs.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    private fun fetchPendingMembersForClubs(clubs: List<Club>) {
        if (!hasConnection.value) {
            _pendingMembers.value = UiState.Error("You're offline. Please check your connection.")
            return
        }
        viewModelScope.launch {
            _pendingMembers.value = UiState.Loading
            val pendingMembersMap = mutableMapOf<String, List<Player>>()
            clubs.forEach { club ->
                if (club.pendingMemberIds.isNotEmpty()) {
                    when (val result = clubRepository.getClubMembers(club.pendingMemberIds)) {
                        is Result.Success -> pendingMembersMap[club.clubId] = result.data
                        is Result.Error -> {
                            _pendingMembers.value = UiState.Error(result.exception.message ?: "Could not load pending members")
                            return@launch
                        }
                    }
                }
            }
            _pendingMembers.value = UiState.Success(pendingMembersMap)
        }
    }
    fun approveJoinRequest(clubId: String, playerId: String, clubName: String) {
        if (!hasConnection.value) {
            val requestId = "$clubId-$playerId"
            _joinRequestState.update { it + (requestId to UiState.Error("You're offline. Please check your connection.")) }
            return
        }
        val requestId = "$clubId-$playerId"
        viewModelScope.launch {
            _joinRequestState.update { it + (requestId to UiState.Loading) }
            try {
                val request = gaming.xplay.data.model.JoinClubRequest(clubId, playerId)
                when (val result = clubRepository.approveJoinRequest(request)) {
                    is Result.Success -> {
                        sendApprovalNotification(playerId, clubName)
                        fetchAdminClubs() // Refresh the list
                        _joinRequestState.update { it + (requestId to UiState.Success(Unit)) }
                    }

                    is Result.Error -> _joinRequestState.update { it + (requestId to UiState.Error(result.exception.message ?: "An error occurred")) }
                }
            } catch (e: Exception) {
                _joinRequestState.update { it + (requestId to UiState.Error(e.message ?: "An error occurred")) }
            }
        }
    }

    fun declineJoinRequest(clubId: String, playerId: String, clubName: String) {
        if (!hasConnection.value) {
            val requestId = "$clubId-$playerId"
            _joinRequestState.update { it + (requestId to UiState.Error("You're offline. Please check your connection.")) }
            return
        }
        val requestId = "$clubId-$playerId"
        viewModelScope.launch {
            _joinRequestState.update { it + (requestId to UiState.Loading) }
            try {
                val request = gaming.xplay.data.model.JoinClubRequest(clubId, playerId)
                when (val result = clubRepository.declineJoinRequest(request)) {
                    is Result.Success -> {
                        // sendRejectionNotification(playerId, clubName)
                        fetchAdminClubs() // Refresh the list
                        _joinRequestState.update { it + (requestId to UiState.Success(Unit)) }
                    }

                    is Result.Error -> _joinRequestState.update { it + (requestId to UiState.Error(result.exception.message ?: "An error occurred")) }
                }
            } catch (e: Exception) {
                _joinRequestState.update { it + (requestId to UiState.Error(e.message ?: "An error occurred")) }
            }
        }
    }
    
    fun clearJoinRequestStatus(clubId: String, playerId: String) {
        val requestId = "$clubId-$playerId"
        viewModelScope.launch {
            _joinRequestState.update { it - requestId }
        }
    }

    private fun sendApprovalNotification(playerId: String, clubName: String) {
        if (!hasConnection.value) {
            return
        }
        viewModelScope.launch {
            /*
            notificationRepository.sendNotification(
                gaming.xplay.data.model.NotificationRequest(
                    targetUserId = playerId,
                    title = "You'''re in!",
                    body = "Your request to join $clubName has been approved."
                )
            )
             */
        }
    }

    private fun sendRejectionNotification(playerId: String, clubName: String) {
        if (!hasConnection.value) {
            return
        }
        viewModelScope.launch {
            /*
            notificationRepository.sendNotification(
                gaming.xplay.data.model.NotificationRequest(
                    targetUserId = playerId,
                    title = "Request Declined",
                    body = "Your request to join $clubName has been declined."
                )
            )
             */
        }
    }
}