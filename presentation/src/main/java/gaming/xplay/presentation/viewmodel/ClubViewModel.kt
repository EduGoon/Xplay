package gaming.xplay.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Club
import gaming.xplay.data.model.CreateClubRequest
import gaming.xplay.data.model.Result
import gaming.xplay.data.repo.ClubRepository
import gaming.xplay.data.repo.StorageRepository
import gaming.xplay.presentation.ui.State.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class ClubNavigationState {
    data object Idle : ClubNavigationState()
    data class ToClubDetails(val clubId: String, val isNewAdmin: Boolean) : ClubNavigationState()
}

@HiltViewModel
class ClubViewModel @Inject constructor(
    private val clubRepository: ClubRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _clubs = MutableStateFlow<UiState<List<Club>>>(UiState.Loading)
    val clubs: StateFlow<UiState<List<Club>>> = _clubs

    private val _createClubState = MutableStateFlow<UiState<Club>>(UiState.Loading)
    val createClubState: StateFlow<UiState<Club>> = _createClubState

    private val _navigationState = MutableStateFlow<ClubNavigationState>(ClubNavigationState.Idle)
    val navigationState: StateFlow<ClubNavigationState> = _navigationState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchClubs()
    }

    fun fetchClubs(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) {
                _clubs.value = UiState.Loading
            }
            when (val result = clubRepository.getClubs()) {
                is Result.Success -> _clubs.value = UiState.Success(result.data)
                is Result.Error -> _clubs.value = UiState.Error(result.exception.message ?: "An error occurred")
            }
        }
    }

    fun refreshClubs() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchClubs(isRefresh = true)
            _isRefreshing.value = false
        }
    }

    fun createClub(clubName: String, adminId: String, imageUri: Uri?) {
        viewModelScope.launch {
            _createClubState.value = UiState.Loading
            val imageUrl = imageUri?.let {
                when (val result = storageRepository.uploadImage(it)) {
                    is Result.Success -> result.data
                    is Result.Error -> {
                        _createClubState.value = UiState.Error("Failed to upload image")
                        return@launch
                    }
                }
            }

            val request = CreateClubRequest(clubName, adminId, imageUrl)
            when (val result = clubRepository.createClub(request)) {
                is Result.Success -> {
                    val newClub = result.data
                    _createClubState.value = UiState.Success(newClub)
                    fetchClubs()

                    val userResult = clubRepository.getPlayer(adminId)
                    val isNewAdmin = if (userResult is Result.Success) {
                        !userResult.data?.isClubOwner!!
                    } else {
                        false
                    }
                    _navigationState.value = ClubNavigationState.ToClubDetails(newClub.clubId, isNewAdmin)
                }
                is Result.Error -> _createClubState.value = UiState.Error("Failed to create club")
            }
        }
    }

    fun onNavigationHandled() {
        _navigationState.value = ClubNavigationState.Idle
    }
}
