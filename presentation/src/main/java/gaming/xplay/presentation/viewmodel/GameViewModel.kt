package gaming.xplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Challenge
import gaming.xplay.data.model.Match
import gaming.xplay.data.model.NotificationRequest
import gaming.xplay.data.model.Result
import gaming.xplay.data.model.rankings
import gaming.xplay.data.repo.AuthRepository
import gaming.xplay.data.repo.GameRepository
import gaming.xplay.data.repo.NotificationRepository
import gaming.xplay.presentation.ui.State.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChallengeCreationState {
    object Idle : ChallengeCreationState()
    object Success : ChallengeCreationState()
    data class Error(val message: String) : ChallengeCreationState()
}

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

    private val _challengeCreationState = MutableStateFlow<ChallengeCreationState>(ChallengeCreationState.Idle)
    val challengeCreationState: StateFlow<ChallengeCreationState> = _challengeCreationState.asStateFlow()

    fun onChallengeCreationStatusConsumed() {
        _challengeCreationState.value = ChallengeCreationState.Idle
    }

    fun createChallenge(player2Id: String, gameId: String) {
        viewModelScope.launch {
            when (val currentUserResult = authRepository.fetchCurrentUserProfile()) {
                is Result.Success -> {
                    val currentUser = currentUserResult.data
                    if (currentUser != null) {
                        val challenge = Challenge(player1Id = currentUser.uid, player2Id = player2Id, gameId = gameId)
                        when (gameRepository.createChallenge(challenge)) {
                            is Result.Success -> {
                                _challengeCreationState.value = ChallengeCreationState.Success
                                /*
                                notificationRepository.sendNotification(
                                    NotificationRequest(
                                        targetUserId = player2Id,
                                        title = "New Challenge!",
                                        body = "You have a new match challenge from ${currentUser.name ?: "a player"}"
                                    )
                                )
                                 */
                                fetchOutgoingChallenges(currentUser.uid)
                            }
                            is Result.Error -> _challengeCreationState.value = ChallengeCreationState.Error("Failed to create challenge.")
                        }
                    } else {
                        _challengeCreationState.value = ChallengeCreationState.Error("Could not identify current user.")
                    }
                }
                is Result.Error -> _challengeCreationState.value = ChallengeCreationState.Error("Failed to fetch current user.")
            }
        }
    }

    fun acceptChallenge(challenge: Challenge) {
        viewModelScope.launch {
            when (gameRepository.updateChallengeStatus(challenge.challengeId, "accepted")) {
                is Result.Success -> { /* Handle success if needed, e.g., refresh lists */ }
                is Result.Error -> _errorState.value = "Failed to accept challenge."
            }
        }
    }

    fun rejectChallenge(challenge: Challenge) {
        viewModelScope.launch {
            when (gameRepository.updateChallengeStatus(challenge.challengeId, "rejected")) {
                is Result.Success -> { /* Handle success if needed, e.g., refresh lists */ }
                is Result.Error -> _errorState.value = "Failed to reject challenge."
            }
        }
    }

    fun fetchChallengesForCurrentUser() {
        viewModelScope.launch {
            when (val currentUserResult = authRepository.fetchCurrentUserProfile()) {
                is Result.Success -> {
                    val currentUser = currentUserResult.data
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
                is Result.Error -> {
                    _incomingChallenges.value = UiState.Error("Failed to fetch user")
                    _outgoingChallenges.value = UiState.Error("Failed to fetch user")
                    _acceptedChallenges.value = UiState.Error("Failed to fetch user")
                }
            }
        }
    }

    private fun fetchIncomingChallenges(playerId: String) {
        viewModelScope.launch {
            _incomingChallenges.value = UiState.Loading
            when (val result = gameRepository.getIncomingChallenges(playerId)) {
                is Result.Success -> _incomingChallenges.value = UiState.Success(result.data)
                is Result.Error -> _incomingChallenges.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    private fun fetchOutgoingChallenges(playerId: String) {
        viewModelScope.launch {
            _outgoingChallenges.value = UiState.Loading
            when (val result = gameRepository.getOutgoingChallenges(playerId)) {
                is Result.Success -> _outgoingChallenges.value = UiState.Success(result.data)
                is Result.Error -> _outgoingChallenges.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    private fun fetchAcceptedChallenges(playerId: String) {
        viewModelScope.launch {
            _acceptedChallenges.value = UiState.Loading
            when (val result = gameRepository.getAcceptedChallenges(playerId)) {
                is Result.Success -> _acceptedChallenges.value = UiState.Success(result.data)
                is Result.Error -> _acceptedChallenges.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    fun submitMatchResult(challengeId: String, result: String) {
        viewModelScope.launch {
            _errorState.value = null
            when (val submitResult = gameRepository.submitMatchResult(challengeId, result)) {
                is Result.Success -> fetchChallengesForCurrentUser()
                is Result.Error -> _errorState.value = submitResult.exception.message ?: "An unexpected error occurred."
            }
        }
    }

    fun fetchMatchHistory(playerId: String) {
        viewModelScope.launch {
            _matchHistory.value = UiState.Loading
            when (val result = gameRepository.getMatchHistory(playerId)) {
                is Result.Success -> _matchHistory.value = UiState.Success(result.data)
                is Result.Error -> _matchHistory.value = UiState.Error(result.exception.message ?: "An unknown error occurred")
            }
        }
    }

    fun fetchLeaderboard(gameId: String) {
        viewModelScope.launch {
            _leaderboard.value = UiState.Loading
            when (val result = gameRepository.getLeaderboard(gameId)) {
                is Result.Success -> _leaderboard.value = UiState.Success(result.data)
                is Result.Error -> _leaderboard.value = UiState.Error(result.exception.message ?: "An unknown error occurred")
            }
        }
    }

    suspend fun getPlayerRanking(playerId: String, gameId: String): rankings? {
        return when (val result = gameRepository.getPlayerRanking(playerId, gameId)) {
            is Result.Success -> result.data
            is Result.Error -> {
                _errorState.value = result.exception.message ?: "An unknown error occurred"
                null
            }
        }
    }
}
