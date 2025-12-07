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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClubViewModel @Inject constructor(
    private val clubRepository: ClubRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _clubs = MutableStateFlow<UiState<List<Club>>>(UiState.Loading)
    val clubs: StateFlow<UiState<List<Club>>> = _clubs

    private val _createClubState = MutableStateFlow<UiState<Club>>(UiState.Loading)
    val createClubState: StateFlow<UiState<Club>> = _createClubState

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
                    _createClubState.value = UiState.Success(result.data)
                    fetchClubs()
                }
                is Result.Error -> _createClubState.value = UiState.Error("Failed to create club")
            }
        }
    }
}
