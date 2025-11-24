package gaming.xplay.presentation.viewmodel

import android.util.Log
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

sealed class NavigationState {
    object Loading : NavigationState()
    object ToLogin : NavigationState()
    object ToOnboarding : NavigationState()
    object ToHome : NavigationState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Loading)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _signInState = MutableStateFlow<Boolean?>(null)
    val signInState: StateFlow<Boolean?> = _signInState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            when (val result = authRepository.fetchCurrentUserProfile()) {
                is Result.Success -> {
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

    fun checkCurrentUserUid() : String? {
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
        authRepository.signOut()
        _navigationState.value = NavigationState.ToLogin
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
