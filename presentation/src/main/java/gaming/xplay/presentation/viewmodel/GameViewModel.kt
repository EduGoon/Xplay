package gaming.xplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Challenge
import gaming.xplay.data.model.Match
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.model.rankings
import gaming.xplay.data.repo.AuthRepository
import gaming.xplay.data.repo.GameRepository
import gaming.xplay.data.repo.NotificationRepository
import gaming.xplay.presentation.session.UserSessionManager
import gaming.xplay.presentation.ui.State.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChallengeCreationState {
    object Idle : ChallengeCreationState()
    object Loading : ChallengeCreationState()
    object Success : ChallengeCreationState()
    data class Error(val message: String) : ChallengeCreationState()
}

sealed class MatchSubmissionState {
    object Idle : MatchSubmissionState()
    object Loading : MatchSubmissionState()
    data class Success(val message: String) : MatchSubmissionState()
    data class Error(val message: String) : MatchSubmissionState()
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _allChallenges = MutableStateFlow<UiState<List<Challenge>>>(UiState.Loading)
    private val _currentUser = MutableStateFlow<Result<Player?>>(Result.Error(Exception("Not logged in")))

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val incomingChallenges: StateFlow<UiState<List<Challenge>>> = combine(
        _allChallenges,
        _currentUser
    ) { allChallenges, currentUserResult ->
        when {
            allChallenges is UiState.Success && currentUserResult is Result.Success -> {
                val currentUserId = currentUserResult.data?.uid
                if (currentUserId != null) {
                    UiState.Success(allChallenges.data.filter { it.player2Id == currentUserId })
                } else {
                    UiState.Error("User not logged in")
                }
            }

            allChallenges is UiState.Error -> allChallenges
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val outgoingChallenges: StateFlow<UiState<List<Challenge>>> = combine(
        _allChallenges,
        _currentUser
    ) { allChallenges, currentUserResult ->
        when {
            allChallenges is UiState.Success && currentUserResult is Result.Success -> {
                val currentUserId = currentUserResult.data?.uid
                if (currentUserId != null) {
                    UiState.Success(allChallenges.data.filter { it.player1Id == currentUserId })
                } else {
                    UiState.Error("User not logged in")
                }
            }

            allChallenges is UiState.Error -> allChallenges
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val activeChallenges: StateFlow<UiState<List<Challenge>>> = _allChallenges.combine(
        _currentUser
    ) { allChallenges, _ ->
        when (allChallenges) {
            is UiState.Success -> {
                UiState.Success(allChallenges.data.filter {
                    it.status == "accepted" || it.status == "waiting verification"
                })
            }

            is UiState.Error -> allChallenges
            is UiState.Loading -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _matchHistory = MutableStateFlow<UiState<List<Match>>>(UiState.Loading)
    val matchHistory: StateFlow<UiState<List<Match>>> = _matchHistory.asStateFlow()

    private val _leaderboard = MutableStateFlow<UiState<List<rankings>>>(UiState.Loading)
    val leaderboard: StateFlow<UiState<List<rankings>>> = _leaderboard.asStateFlow()

    private val _leaderboardPlayerProfiles = MutableStateFlow<Map<String, Player?>>(emptyMap())
    val leaderboardPlayerProfiles: StateFlow<Map<String, Player?>> = _leaderboardPlayerProfiles.asStateFlow()


    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _challengeCreationState =
        MutableStateFlow<ChallengeCreationState>(ChallengeCreationState.Idle)
    val challengeCreationState: StateFlow<ChallengeCreationState> = _challengeCreationState.asStateFlow()

    private val _matchSubmissionState = MutableStateFlow<MatchSubmissionState>(MatchSubmissionState.Idle)
    val matchSubmissionState: StateFlow<MatchSubmissionState> = _matchSubmissionState.asStateFlow()

    init {
        userSessionManager.logoutEvents
            .onEach { clearAllData() }
            .launchIn(viewModelScope)
    }

    fun onChallengeCreationStatusConsumed() {
        _challengeCreationState.value = ChallengeCreationState.Idle
    }

    fun onChallengeCreate(player2Id: String, gameId: String) {
        if (_challengeCreationState.value is ChallengeCreationState.Loading || _challengeCreationState.value is ChallengeCreationState.Error) return
        createChallenge(player2Id, gameId)
    }

    private fun createChallenge(player2Id: String, gameId: String) {
        viewModelScope.launch {
            _challengeCreationState.value = ChallengeCreationState.Loading
            when (val currentUserResult = authRepository.fetchCurrentUserProfile()) {
                is Result.Success -> {
                    val currentUser = currentUserResult.data
                    if (currentUser != null) {
                        val challenge = Challenge(
                            player1Id = currentUser.uid,
                            player2Id = player2Id,
                            gameId = gameId
                        )
                        when (gameRepository.createChallenge(challenge)) {
                            is Result.Success -> {
                                _challengeCreationState.value = ChallengeCreationState.Success
                                fetchChallengesForCurrentUser()
                            }

                            is Result.Error -> _challengeCreationState.value =
                                ChallengeCreationState.Error("Failed to create challenge.")
                        }
                    } else {
                        _challengeCreationState.value =
                            ChallengeCreationState.Error("Could not identify current user.")
                    }
                }

                is Result.Error -> _challengeCreationState.value =
                    ChallengeCreationState.Error("Failed to fetch current user.")
            }
        }
    }

    fun acceptChallenge(challenge: Challenge) {
        viewModelScope.launch {
            when (gameRepository.updateChallengeStatus(challenge.challengeId, "accepted")) {
                is Result.Success -> {
                    fetchChallengesForCurrentUser()
                }

                is Result.Error -> _errorState.value = "Failed to accept challenge."
            }
        }
    }

    fun rejectChallenge(challenge: Challenge) {
        viewModelScope.launch {
            when (gameRepository.updateChallengeStatus(challenge.challengeId, "rejected")) {
                is Result.Success -> {
                    fetchChallengesForCurrentUser()
                }

                is Result.Error -> _errorState.value = "Failed to reject challenge."
            }
        }
    }

    fun refreshAllChallenges() {
        viewModelScope.launch {
            fetchChallengesForCurrentUser()
        }
    }

    private fun fetchChallengesForCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = authRepository.fetchCurrentUserProfile()
            when (val currentUserResult = _currentUser.value) {
                is Result.Success -> {
                    val currentUser = currentUserResult.data
                    if (currentUser != null) {
                        if (_allChallenges.value !is UiState.Success) {
                            _allChallenges.value = UiState.Loading
                        }
                        when (val result = gameRepository.getAllChallenges(currentUser.uid)) {
                            is Result.Success -> _allChallenges.value = UiState.Success(result.data)
                            is Result.Error -> _allChallenges.value =
                                UiState.Error(result.exception.message ?: "An error occurred")
                        }
                    } else {
                        _allChallenges.value = UiState.Error("User not logged in")
                    }
                }

                is Result.Error -> {
                    _allChallenges.value = UiState.Error("Failed to fetch user")
                }
            }
        }
    }

    fun refreshOutgoingChallenges() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchChallengesForCurrentUser()
            _isRefreshing.value = false
        }
    }

    fun refreshIncomingChallenges() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchChallengesForCurrentUser()
            _isRefreshing.value = false
        }
    }

    fun refreshActiveChallenges() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchChallengesForCurrentUser()
            _isRefreshing.value = false
        }
    }

    fun submitMatchResult(challenge: Challenge, result: String) {
        viewModelScope.launch {
            _matchSubmissionState.value = MatchSubmissionState.Loading
            val submitResult = gameRepository.submitMatchResult(challenge.challengeId, result)
            when (submitResult) {
                is Result.Success -> {
                    val successMessage = if (challenge.status == "accepted") {
                        "Result submitted, waiting for opponent"
                    } else {
                        "Result submitted. Match Completed, see match in Match History"
                    }
                    _matchSubmissionState.value = MatchSubmissionState.Success(successMessage)
                    fetchChallengesForCurrentUser()
                }
                is Result.Error -> _matchSubmissionState.value =
                    MatchSubmissionState.Error("Failed to submit match result.")
            }
        }
    }

    fun onMatchSubmissionStatusConsumed() {
        _matchSubmissionState.value = MatchSubmissionState.Idle
    }

    fun fetchMatchHistory(playerId: String) {
        viewModelScope.launch {
            _matchHistory.value = UiState.Loading
            when (val result = gameRepository.getMatchHistory(playerId)) {
                is Result.Success -> _matchHistory.value = UiState.Success(result.data)
                is Result.Error -> _matchHistory.value =
                    UiState.Error(result.exception.message ?: "An unknown error occurred")
            }
        }
    }

    fun fetchLeaderboard(gameId: String) {
        viewModelScope.launch {
            _leaderboard.value = UiState.Loading
            when (val result = gameRepository.getLeaderboard(gameId)) {
                is Result.Success -> {
                    _leaderboard.value = UiState.Success(result.data)
                    val playerIds = result.data.map { it.playerid }
                    fetchPlayerProfilesForLeaderboard(playerIds)
                }

                is Result.Error -> _leaderboard.value =
                    UiState.Error(result.exception.message ?: "An unknown error occurred")
            }
        }
    }

    private fun fetchPlayerProfilesForLeaderboard(playerIds: List<String>) {
        viewModelScope.launch {
            val profileDeferreds = playerIds.map {
                async {
                    when (val profileResult = authRepository.getPlayerProfile(it)) {
                        is Result.Success -> it to profileResult.data
                        is Result.Error -> it to null
                    }
                }
            }
            _leaderboardPlayerProfiles.value = profileDeferreds.awaitAll().toMap()
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

    fun clearAllData() {
        _allChallenges.value = UiState.Loading
        _matchHistory.value = UiState.Loading
        _leaderboard.value = UiState.Loading
        _currentUser.value = Result.Error(Exception("Not logged in"))
        _leaderboardPlayerProfiles.value = emptyMap()
    }
}
