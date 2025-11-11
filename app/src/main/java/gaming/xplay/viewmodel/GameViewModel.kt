package gaming.xplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.datamodel.Match
import gaming.xplay.datamodel.NotificationRequest
import gaming.xplay.datamodel.NotificationState
import gaming.xplay.datamodel.Player
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

    private val _notificationState = MutableStateFlow<NotificationState>(NotificationState.Idle)
    val notificationState: StateFlow<NotificationState> = _notificationState.asStateFlow()

    private val _matchHistory = MutableStateFlow<UiState<List<Match>>>(UiState.Loading)
    val matchHistory: StateFlow<UiState<List<Match>>> = _matchHistory.asStateFlow()

    private val _leaderboard = MutableStateFlow<UiState<List<rankings>>>(UiState.Loading)
    val leaderboard: StateFlow<UiState<List<rankings>>> = _leaderboard.asStateFlow()

    fun uploadMatchResult(
        gameId: String,
        player1Id: String, // The one uploading
        player2Id: String, // The opponent
        score: String,
        winnerId: String
    ) {
        viewModelScope.launch {
            // 1. Create the match document in 'pending' state
            val match = Match(
                gameId = gameId,
                player1Id = player1Id,
                player2Id = player2Id,
                score = score,
                winnerId = winnerId,
                status = "pending"
            )
            val matchId = gameRepository.createMatch(match)

            // 2. Send a notification to player 2 for confirmation
            _notificationState.value = NotificationState.Sending
            val request = NotificationRequest(
                targetUserId = player2Id,
                title = "Match Result Confirmation",
                body = "A player has submitted a match result. Please confirm.",
                requestId = matchId
            )

            val feedback = notificationRepository.sendNotificationAndAwaitFeedback(request)

            // 3. Process the feedback
            when (feedback) {
                true -> {
                    // Player 2 confirmed
                    gameRepository.confirmMatch(matchId, true)
                    _notificationState.value = NotificationState.Success(accepted = true)
                }
                false -> {
                    // Player 2 rejected
                    gameRepository.confirmMatch(matchId, false)

                    // Notify Player 1 of the rejection
                    val rejectionNotification = NotificationRequest(
                        targetUserId = player1Id,
                        title = "Match Result Rejected",
                        body = "Your recent match result was rejected by your opponent."
                    )
                    notificationRepository.sendOneWayNotification(rejectionNotification)

                    _notificationState.value = NotificationState.Success(accepted = false)
                }
                null -> {
                    // Timeout
                    _notificationState.value = NotificationState.Timeout
                }
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

    fun resetState() {
        _notificationState.value = NotificationState.Idle
    }
}
