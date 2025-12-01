package gaming.xplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Club
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.repo.AuthRepository
import gaming.xplay.data.repo.ClubRepository
import gaming.xplay.data.repo.NotificationRepository
import gaming.xplay.presentation.ui.State.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val clubRepository: ClubRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _adminClubs = MutableStateFlow<UiState<List<Club>>>(UiState.Loading)
    val adminClubs: StateFlow<UiState<List<Club>>> = _adminClubs

    private val _pendingMembers = MutableStateFlow<UiState<Map<String, List<Player>>>>(UiState.Loading)
    val pendingMembers: StateFlow<UiState<Map<String, List<Player>>>> = _pendingMembers

    fun fetchAdminClubs() {
        viewModelScope.launch {
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
        viewModelScope.launch {
            val pendingMembersMap = mutableMapOf<String, List<Player>>()
            for (club in clubs) {
                if (club.pendingMemberIds.isNotEmpty()) {
                    when (val result = clubRepository.getClubMembers(club.pendingMemberIds)) {
                        is Result.Success -> pendingMembersMap[club.clubId] = result.data
                        is Result.Error -> { /* Handle error */ }
                    }
                }
            }
            _pendingMembers.value = UiState.Success(pendingMembersMap)
        }
    }

    fun approveJoinRequest(clubId: String, playerId: String, clubName: String) {
        viewModelScope.launch {
            val request = gaming.xplay.data.model.JoinClubRequest(clubId, playerId)
            val result = clubRepository.approveJoinRequest(request)
            if (result is Result.Success) {
                sendApprovalNotification(playerId, clubName)
                fetchAdminClubs() // Refresh the list
            }
        }
    }

    fun declineJoinRequest(clubId: String, playerId: String, clubName: String) {
        viewModelScope.launch {
            val request = gaming.xplay.data.model.JoinClubRequest(clubId, playerId)
            val result = clubRepository.declineJoinRequest(request)
            if (result is Result.Success) {
                sendRejectionNotification(playerId, clubName)
                fetchAdminClubs() // Refresh the list
            }
        }
    }

    private fun sendApprovalNotification(playerId: String, clubName: String) {
        viewModelScope.launch {
            /*
            notificationRepository.sendNotification(
                gaming.xplay.data.model.NotificationRequest(
                    targetUserId = playerId,
                    title = "You're in!",
                    body = "Your request to join $clubName has been approved."
                )
            )
             */
        }
    }

    private fun sendRejectionNotification(playerId: String, clubName: String) {
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
