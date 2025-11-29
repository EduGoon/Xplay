package gaming.xplay.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Club
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.model.rankings
import gaming.xplay.data.repo.ClubRepository
import gaming.xplay.data.repo.GameRepository
import gaming.xplay.presentation.ui.State.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClubDetailsViewModel @Inject constructor(
    private val clubRepository: ClubRepository,
    private val gameRepository: GameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val clubId: String = savedStateHandle.get<String>("clubId")!!

    private val _club = MutableStateFlow<UiState<Club>>(UiState.Loading)
    val club: StateFlow<UiState<Club>> = _club

    private val _members = MutableStateFlow<UiState<List<Player>>>(UiState.Loading)
    val members: StateFlow<UiState<List<Player>>> = _members

    private val _rankings = MutableStateFlow<UiState<List<rankings>>>(UiState.Loading)
    val rankings: StateFlow<UiState<List<rankings>>> = _rankings

    init {
        fetchClubDetails()
    }

    private fun fetchClubDetails() {
        viewModelScope.launch {
            when (val result = clubRepository.getClub(clubId)) {
                is Result.Success -> {
                    val clubData = result.data
                    if (clubData != null) {
                        _club.value = UiState.Success(clubData)
                        fetchClubMembers(clubData.memberIds)
                    } else {
                        _club.value = UiState.Error("Club not found")
                    }
                }
                is Result.Error -> _club.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    private fun fetchClubMembers(memberIds: List<String>) {
        viewModelScope.launch {
            when (val result = clubRepository.getClubMembers(memberIds)) {
                is Result.Success -> {
                    _members.value = UiState.Success(result.data)
                    fetchMemberRankings(result.data)
                }
                is Result.Error -> _members.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    private fun fetchMemberRankings(members: List<Player>) {
        viewModelScope.launch {
            val rankingsList = members.map { member ->
                async {
                    when (val rankingResult = gameRepository.getPlayerRanking(member.uid, "FIFA")) {
                        is Result.Success -> rankingResult.data
                        is Result.Error -> null
                    }
                }
            }.awaitAll().filterNotNull()
            _rankings.value = UiState.Success(rankingsList)
        }
    }

    fun joinClub(playerId: String) {
        viewModelScope.launch {
            val request = gaming.xplay.data.model.JoinClubRequest(clubId, playerId)
            when (clubRepository.joinClub(request)) {
                is Result.Success -> fetchClubDetails()
                is Result.Error -> { /* Handle error */ }
            }
        }
    }
}
