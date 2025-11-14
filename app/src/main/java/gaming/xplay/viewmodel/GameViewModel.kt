package gaming.xplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.datamodel.Challenge
import gaming.xplay.datamodel.Match
import gaming.xplay.datamodel.NotificationRequest
import gaming.xplay.datamodel.UiState
import gaming.xplay.datamodel.rankings
import gaming.xplay.repo.AuthRepository
import gaming.xplay.repo.GameRepository
import gaming.xplay.repo.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _incomingChallenges = MutableStateFlow<UiState<List<Challenge>>>(UiState.Loading)
    val incomingChallenges: StateFlow<UiState<List<Challenge>>> = _incomingChallenges.asStateFlow()

    private val _outgoingChallenges = MutableStateFlow<UiState<List<Challenge>>>(UiState.Loading)
    val outgoingChallenges: StateFlow<UiState<List<Challenge>>> = _outgoingChallenges.asStateFlow()

    private val _acceptedChallenges = MutableStateFlow<UiState<List<Challenge>>>(UiState.Loading)
    val acceptedChallenges: StateFlow<UiState<List<Challenge>>> = _acceptedChallenges.asStateFlow()

    private val _matchHistory = MutableStateFlow<UiState<List<Match>>>(UiState.Loading)
    val matchHistory: StateFlow<UiState<List<Match>>> = _matchHistory.asStateFlow()

    private val _leaderboard = MutableStateFlow<UiState<List<rankings>>>(UiState.Loading)
    val leaderboard: StateFlow<UiState<List<rankings>>> = _leaderboard.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    fun createChallenge(player2Id: String, gameId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.fetchCurrentUserProfile()
            if (currentUser != null) {
                val challenge = Challenge(player1Id = currentUser.uid, player2Id = player2Id, gameId = gameId)
                gameRepository.createChallenge(challenge)
                notificationRepository.sendNotification(
                    NotificationRequest(
                        targetUserId = player2Id,
                        title = "New Challenge!",
                        body = "You have a new match challenge from ${currentUser.name ?: "a player"}"
                    )
                )
                fetchOutgoingChallenges(currentUser.uid)
            }
        }
    }

    fun acceptChallenge(challenge: Challenge) {
        viewModelScope.launch {
            gameRepository.updateChallengeStatus(challenge.challengeId, "accepted")
            fetchChallengesForCurrentUser()
        }
    }

    fun rejectChallenge(challenge: Challenge) {
        viewModelScope.launch {
            gameRepository.updateChallengeStatus(challenge.challengeId, "rejected")
            fetchChallengesForCurrentUser()
        }
    }

    fun submitMatchResult(challengeId: String, result: String) {
        viewModelScope.launch {
            _errorState.value = null // Clear previous errors
            try {
                val currentUser = authRepository.fetchCurrentUserProfile()
                if (currentUser != null) {
                    gameRepository.submitMatchResult(challengeId, currentUser.uid, result)
                    fetchChallengesForCurrentUser() // Refresh the list
                } else {
                    _errorState.value = "You must be logged in to perform this action."
                }
            } catch (e: IllegalStateException) {
                _errorState.value = e.message // Display user-friendly error from the repository
            } catch (e: Exception) {
                _errorState.value = "An unexpected error occurred. Please try again."
            }
        }
    }

    fun fetchChallengesForCurrentUser() {
        viewModelScope.launch {
            val currentUser = authRepository.fetchCurrentUserProfile()
            if (currentUser != null) {
                fetchIncomingChallenges(currentUser.uid)
                fetchOutgoingChallenges(currentUser.uid)
                fetchAcceptedChallenges(currentUser.uid)
            } else {
                _incomingChallenges.value = UiState.Error("User not logged in")
                _outgoingChallenges.value = UiState.Error("User not logged in")
                _acceptedChallenges.value = UiState.Error("User not logged in")
            }
        }
    }

    private fun fetchIncomingChallenges(playerId: String) {
        viewModelScope.launch {
            _incomingChallenges.value = UiState.Loading
            try {
                val challenges = gameRepository.getIncomingChallenges(playerId)
                _incomingChallenges.value = UiState.Success(challenges)
            } catch (e: Exception) {
                _incomingChallenges.value = UiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private fun fetchOutgoingChallenges(playerId: String) {
        viewModelScope.launch {
            _outgoingChallenges.value = UiState.Loading
            try {
                val challenges = gameRepository.getOutgoingChallenges(playerId)
                _outgoingChallenges.value = UiState.Success(challenges)
            } catch (e: Exception) {
                _outgoingChallenges.value = UiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private fun fetchAcceptedChallenges(playerId: String) {
        viewModelScope.launch {
            _acceptedChallenges.value = UiState.Loading
            try {
                val challenges = gameRepository.getAcceptedChallenges(playerId)
                _acceptedChallenges.value = UiState.Success(challenges)
            } catch (e: Exception) {
                _acceptedChallenges.value = UiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun fetchMatchHistory(playerId: String) {
        viewModelScope.launch {
            _matchHistory.value = UiState.Loading
            try {
                val history = gameRepository.getMatchHistory(playerId)
                _matchHistory.value = UiState.Success(history)
            } catch (e: Exception) {
                _matchHistory.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun fetchLeaderboard(gameId: String) {
        viewModelScope.launch {
            _leaderboard.value = UiState.Loading
            try {
                val board = gameRepository.getLeaderboard(gameId)
                _leaderboard.value = UiState.Success(board)
            } catch (e: Exception) {
                _leaderboard.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
