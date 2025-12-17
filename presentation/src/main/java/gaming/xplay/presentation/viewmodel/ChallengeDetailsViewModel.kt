package gaming.xplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.network.ConnectivityRepository
import gaming.xplay.data.repo.AuthRepository
import gaming.xplay.presentation.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengeDetailsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userSessionManager: UserSessionManager,
    private val connectivityRepository: ConnectivityRepository
) : ViewModel() {

    private val _playerProfiles = MutableStateFlow<Map<String, Player?>>(emptyMap())
    val playerProfiles: StateFlow<Map<String, Player?>> = _playerProfiles.asStateFlow()

    val hasConnection = connectivityRepository.hasConnection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        userSessionManager.logoutEvents
            .onEach { clearData() }
            .launchIn(viewModelScope)
    }

    fun fetchPlayerProfile(playerId: String) {
        if (_playerProfiles.value.containsKey(playerId)) return

        if (!hasConnection.value) {
            _playerProfiles.value = _playerProfiles.value + (playerId to null)
            return
        }

        viewModelScope.launch {
            when (val result = authRepository.getPlayerProfile(playerId)) {
                is Result.Success -> {
                    _playerProfiles.value = _playerProfiles.value + (playerId to result.data)
                }
                is Result.Error -> {
                    _playerProfiles.value = _playerProfiles.value + (playerId to null)
                }
            }
        }
    }

    fun clearData() {
        _playerProfiles.value = emptyMap()
    }
}
