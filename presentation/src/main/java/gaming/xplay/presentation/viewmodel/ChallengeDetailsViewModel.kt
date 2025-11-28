package gaming.xplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengeDetailsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _playerProfiles = MutableStateFlow<Map<String, Player?>>(emptyMap())
    val playerProfiles: StateFlow<Map<String, Player?>> = _playerProfiles.asStateFlow()

    fun fetchPlayerProfile(playerId: String) {
        if (_playerProfiles.value.containsKey(playerId)) return

        viewModelScope.launch {
            when (val result = authRepository.getPlayerProfile(playerId)) {
                is Result.Success -> {
                    _playerProfiles.value = _playerProfiles.value + (playerId to result.data)
                }
                is Result.Error -> {
                    // Handle error, maybe log it or expose it to the UI
                }
            }
        }
    }
}
