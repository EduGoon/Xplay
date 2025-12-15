package gaming.xplay.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.repo.AuthRepository
import gaming.xplay.data.repo.GameRepository
import gaming.xplay.presentation.model.PlayerSearchResult
import gaming.xplay.presentation.session.UserSessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NavigationState {
    object Loading : NavigationState()
    object ToLogin : NavigationState()
    object ToOnboarding : NavigationState()
    object ToHome : NavigationState()
}

sealed class UpdateProfileState {
    object Idle : UpdateProfileState()
    object Loading : UpdateProfileState()
    data class Success(val player: Player) : UpdateProfileState()
    data class Error(val message: String) : UpdateProfileState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Loading)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _signInState = MutableStateFlow<Boolean?>(null)
    val signInState: StateFlow<Boolean?> = _signInState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PlayerSearchResult>>(emptyList())
    val searchResults: StateFlow<List<PlayerSearchResult>> = _searchResults.asStateFlow()

    private val _currentUser = MutableStateFlow<Player?>(null)
    val currentUser: StateFlow<Player?> = _currentUser.asStateFlow()

    private val _updateProfileState = MutableStateFlow<UpdateProfileState>(UpdateProfileState.Idle)
    val updateProfileState: StateFlow<UpdateProfileState> = _updateProfileState.asStateFlow()

    init {
        checkCurrentUser()
        userSessionManager.logoutEvents
            .onEach { clearData() }
            .launchIn(viewModelScope)
    }

    private fun clearData() {
        _signInState.value = null
        _errorState.value = null
        _searchResults.value = emptyList()
        _currentUser.value = null
        _updateProfileState.value = UpdateProfileState.Idle
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            when (val result = authRepository.fetchCurrentUserProfile()) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    if (result.data != null) {
                        _navigationState.value = NavigationState.ToHome
                    } else {
                        _navigationState.value = NavigationState.ToLogin
                    }
                }
                is Result.Error -> {
                    _errorState.value = "Failed to fetch user profile. Please check your connection."
                    _navigationState.value = NavigationState.ToLogin
                }
            }
        }
    }

    fun resetUpdateProfileState() {
        _updateProfileState.value = UpdateProfileState.Idle
    }

    fun refreshCurrentUser() {
        checkCurrentUser()
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    val player = result.data
                    if (player.isFirstTime) {
                        _navigationState.value = NavigationState.ToOnboarding
                    } else {
                        _navigationState.value = NavigationState.ToHome
                    }
                    _signInState.value = true
                    refreshCurrentUser()
                }
                is Result.Error -> {
                    Log.e(TAG, "signInWithGoogle: failed", result.exception)
                    _errorState.value = "Sign-in failed. Please try again."
                    _signInState.value = false
                }
            }
            _isLoading.value = false
        }
    }

    fun updateUserProfile(name: String, profilePictureUri: Uri?) {
        viewModelScope.launch {
            _updateProfileState.value = UpdateProfileState.Loading
            val uid = checkCurrentUserUid()
            if (uid != null) {
                when (val result = authRepository.updateUserProfile(uid, name, profilePictureUri)) {
                    is Result.Success -> {
                        _updateProfileState.value = UpdateProfileState.Success(result.data)
                        refreshCurrentUser()
                    }
                    is Result.Error -> {
                        _updateProfileState.value = UpdateProfileState.Error("Failed to update profile.")
                    }
                }
            } else {
                _updateProfileState.value = UpdateProfileState.Error("User not logged in.")
            }
        }
    }

    fun searchPlayers(query: String) {
        viewModelScope.launch {
            if (query.length < 2) {
                _searchResults.value = emptyList()
                return@launch
            }
            _isLoading.value = true
            when (val playersResult = authRepository.searchPlayers(query)) {
                is Result.Success -> {
                    val players = playersResult.data
                    val searchResultsList = players.map { player ->
                        async {
                            val ranking = when (val rankingResult = gameRepository.getPlayerRanking(player.uid, "FIFA")) {
                                is Result.Success -> rankingResult.data
                                is Result.Error -> {
                                    Log.e(TAG, "Failed to get ranking for ${player.uid}", rankingResult.exception)
                                    null
                                }
                            }
                            PlayerSearchResult(player, ranking)
                        }
                    }.awaitAll()
                    _searchResults.value = searchResultsList
                }
                is Result.Error -> {
                    Log.e(TAG, "searchPlayers failed", playersResult.exception)
                    _errorState.value = "Failed to search for players."
                    _searchResults.value = emptyList()
                }
            }
            _isLoading.value = false
        }
    }

    fun resetSignInState() {
        _signInState.value = null
    }

    fun dismissError() {
        _errorState.value = null
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            when (val result = authRepository.completeOnboarding()) {
                is Result.Success -> {
                    _navigationState.value = NavigationState.ToHome
                }
                is Result.Error -> {
                    Log.e(TAG, "completeOnboarding: failed", result.exception)
                    _errorState.value = "Failed to complete onboarding. Please try again."
                }
            }
        }
    }

    fun checkCurrentUserUid(): String? {
        return authRepository.checkCurrentUserUid()
    }

    suspend fun getPlayerProfile(playerId: String): Player? {
        return when (val result = authRepository.getPlayerProfile(playerId)) {
            is Result.Success -> result.data
            is Result.Error -> {
                Log.e(TAG, "getPlayerProfile failed", result.exception)
                null
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            userSessionManager.onLogout()
            _navigationState.value = NavigationState.ToLogin
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
