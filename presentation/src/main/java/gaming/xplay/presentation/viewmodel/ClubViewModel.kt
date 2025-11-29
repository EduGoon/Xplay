package gaming.xplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Club
import gaming.xplay.data.model.CreateClubRequest
import gaming.xplay.data.model.JoinClubRequest
import gaming.xplay.data.model.Result
import gaming.xplay.data.repo.ClubRepository
import gaming.xplay.presentation.ui.State.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClubViewModel @Inject constructor(
    private val clubRepository: ClubRepository
) : ViewModel() {

    private val _clubs = MutableStateFlow<UiState<List<Club>>>(UiState.Loading)
    val clubs: StateFlow<UiState<List<Club>>> = _clubs

    init {
        fetchClubs()
    }

    fun fetchClubs() {
        viewModelScope.launch {
            _clubs.value = UiState.Loading
            when (val result = clubRepository.getClubs()) {
                is Result.Success -> _clubs.value = UiState.Success(result.data)
                is Result.Error -> _clubs.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    fun createClub(clubName: String, adminId: String) {
        viewModelScope.launch {
            val request = CreateClubRequest(clubName, adminId)
            when (clubRepository.createClub(request)) {
                is Result.Success -> fetchClubs()
                is Result.Error -> _clubs.value = UiState.Error("Failed to create club")
            }
        }
    }

    fun joinClub(clubId: String, playerId: String) {
        viewModelScope.launch {
            val request = JoinClubRequest(clubId, playerId)
            when (clubRepository.joinClub(request)) {
                is Result.Success -> fetchClubs()
                is Result.Error -> _clubs.value = UiState.Error("Failed to join club")
            }
        }
    }
}
