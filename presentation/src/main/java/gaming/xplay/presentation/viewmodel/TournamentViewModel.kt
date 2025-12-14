package gaming.xplay.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Fixture
import gaming.xplay.data.model.JoinTournamentRequest
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.model.Tournament
import gaming.xplay.data.model.TournamentRanking
import gaming.xplay.data.model.rankings
import gaming.xplay.data.repo.ClubRepository
import gaming.xplay.data.repo.GameRepository
import gaming.xplay.data.repo.TournamentRepository
import gaming.xplay.presentation.ui.State.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class JoinTournamentActionState {
    object Idle : JoinTournamentActionState()
    object Success : JoinTournamentActionState()
    data class Error(val message: String) : JoinTournamentActionState()
}

sealed class StartTournamentActionState {
    object Idle : StartTournamentActionState()
    object Success : StartTournamentActionState()
    data class Error(val message: String) : StartTournamentActionState()
}

sealed class SubmitMatchResultActionState {
    object Idle : SubmitMatchResultActionState()
    object Loading : SubmitMatchResultActionState()
    object Success : SubmitMatchResultActionState()
    data class Error(val message: String) : SubmitMatchResultActionState()
}

@HiltViewModel
class TournamentViewModel @Inject constructor(
    private val clubRepository: ClubRepository,
    private val tournamentRepository: TournamentRepository,
    private val gameRepository: GameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tournamentId: String = savedStateHandle.get<String>("tournamentId")!!

    private val _tournament = MutableStateFlow<UiState<Tournament>>(UiState.Loading)
    val tournament: StateFlow<UiState<Tournament>> = _tournament

    private val _members = MutableStateFlow<UiState<List<Player>>>(UiState.Loading)
    val members: StateFlow<UiState<List<Player>>> = _members

    private val _fixtures = MutableStateFlow<UiState<List<Fixture>>>(UiState.Loading)
    val fixtures: StateFlow<UiState<List<Fixture>>> = _fixtures

    private val _tournamentRankings = MutableStateFlow<UiState<List<TournamentRanking>>>(UiState.Loading)
    val tournamentRankings: StateFlow<UiState<List<TournamentRanking>>> = _tournamentRankings

    private val _globalRankings = MutableStateFlow<UiState<List<rankings>>>(UiState.Loading)
    val globalRankings: StateFlow<UiState<List<rankings>>> = _globalRankings

    private val _joinTournamentActionState = MutableSharedFlow<JoinTournamentActionState>()
    val joinTournamentActionState: SharedFlow<JoinTournamentActionState> = _joinTournamentActionState

    private val _startTournamentActionState = MutableSharedFlow<StartTournamentActionState>()
    val startTournamentActionState: SharedFlow<StartTournamentActionState> = _startTournamentActionState

    private val _submitMatchResultActionState = MutableStateFlow<SubmitMatchResultActionState>(SubmitMatchResultActionState.Idle)
    val submitMatchResultActionState: StateFlow<SubmitMatchResultActionState> = _submitMatchResultActionState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        fetchTournamentDetails()
        fetchFixtures()
        fetchTournamentRankings()
    }

    fun refreshTournamentDetails() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchTournamentDetails()
            fetchFixtures()
            fetchTournamentRankings()
            delay(1000) // To ensure the refresh indicator is visible
            _isRefreshing.value = false
        }
    }

    private fun fetchTournamentDetails() {
        viewModelScope.launch {
            when (val result = clubRepository.getTournament(tournamentId)) {
                is Result.Success -> {
                    val tournamentData = result.data
                    if (tournamentData != null) {
                        _tournament.value = UiState.Success(tournamentData)
                        fetchTournamentMembers(tournamentData.members)
                    } else {
                        _tournament.value = UiState.Error("Tournament not found")
                    }
                }
                is Result.Error -> _tournament.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    private fun fetchTournamentMembers(memberIds: List<String>) {
        viewModelScope.launch {
            _members.value = UiState.Loading
            when (val result = clubRepository.getClubMembers(memberIds)) {
                is Result.Success -> {
                    _members.value = UiState.Success(result.data)
                    fetchMembersGlobalRankings(result.data)
                }
                is Result.Error -> _members.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    private fun fetchMembersGlobalRankings(members: List<Player>) {
        viewModelScope.launch {
            _globalRankings.value = UiState.Loading
            val rankingsList = members.map { member ->
                async {
                    when (val rankingResult = gameRepository.getPlayerRanking(member.uid, "FIFA")) { // Assuming "FIFA"
                        is Result.Success -> rankingResult.data
                        is Result.Error -> null
                    }
                }
            }.awaitAll().filterNotNull()
            _globalRankings.value = UiState.Success(rankingsList)
        }
    }

    private fun fetchFixtures() {
        viewModelScope.launch {
            when (val result = tournamentRepository.getFixtures(tournamentId)) {
                is Result.Success -> {
                    _fixtures.value = UiState.Success(result.data)
                }
                is Result.Error -> _fixtures.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    private fun fetchTournamentRankings() {
        viewModelScope.launch {
            when (val result = tournamentRepository.getTournamentRankings(tournamentId)) {
                is Result.Success -> {
                    _tournamentRankings.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _tournamentRankings.value = UiState.Error(result.exception.message ?: "An error occurred")
                }
            }
        }
    }

    fun joinTournament(playerId: String) {
        viewModelScope.launch {
            val request = JoinTournamentRequest(tournamentId, playerId)
            when (val result = clubRepository.joinTournament(request)) {
                is Result.Success -> {
                    _joinTournamentActionState.emit(JoinTournamentActionState.Success)
                    fetchTournamentDetails()
                }
                is Result.Error -> {
                    _joinTournamentActionState.emit(JoinTournamentActionState.Error(result.exception.message ?: "An unknown error occurred"))
                }
            }
        }
    }

    fun startTournament() {
        viewModelScope.launch {
            val tournamentData = (_tournament.value as? UiState.Success)?.data
            val membersData = (_members.value as? UiState.Success)?.data
            if (tournamentData != null && membersData != null) {
                when (tournamentRepository.createFixtures(tournamentData, membersData)) {
                    is Result.Success -> {
                        when (tournamentRepository.startTournament(tournamentId)) {
                            is Result.Success -> {
                                _startTournamentActionState.emit(StartTournamentActionState.Success)
                                fetchTournamentDetails()
                                fetchFixtures()
                            }
                            is Result.Error -> _startTournamentActionState.emit(StartTournamentActionState.Error("Failed to start tournament"))
                        }
                    }
                    is Result.Error -> _startTournamentActionState.emit(StartTournamentActionState.Error("Failed to create fixtures"))
                }
            } else {
                _startTournamentActionState.emit(StartTournamentActionState.Error("Could not get tournament or members data"))
            }
        }
    }

    fun submitTournamentMatchResult(fixture: Fixture, winnerId: String?) {
        viewModelScope.launch {
            _submitMatchResultActionState.value = SubmitMatchResultActionState.Loading
            val tournamentData = (_tournament.value as? UiState.Success)?.data
            if (tournamentData != null) {
                when (tournamentRepository.submitTournamentMatchResult(tournamentData, fixture, winnerId)) {
                    is Result.Success -> {
                        _submitMatchResultActionState.value = SubmitMatchResultActionState.Success
                        fetchFixtures()
                        fetchTournamentRankings()
                    }
                    is Result.Error -> _submitMatchResultActionState.value = SubmitMatchResultActionState.Error("Failed to submit result")
                }
            } else {
                _submitMatchResultActionState.value = SubmitMatchResultActionState.Error("Could not get tournament data")
            }
        }
    }
}
