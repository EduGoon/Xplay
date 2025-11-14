package gaming.xplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.datamodel.Player
import gaming.xplay.repo.AuthRepository
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

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val player = authRepository.fetchCurrentUserProfile()
            if (player != null) {
                _navigationState.value = NavigationState.ToHome
            } else {
                _navigationState.value = NavigationState.ToLogin
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _signInState.value = null // Reset before sign-in
            try {
                val player = authRepository.signInWithGoogle(idToken)
                if (player.isFirstTime) {
                    _navigationState.value = NavigationState.ToOnboarding
                } else {
                    _navigationState.value = NavigationState.ToHome
                }
                _signInState.value = true
            } catch (e: Exception) {
                _signInState.value = false
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                authRepository.completeOnboarding()
                _navigationState.value = NavigationState.ToHome
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun checkCurrentUserUid() : String? {
        val userId = authRepository.checkCurrentUserUid()
        return userId
    }

    suspend fun getPlayerProfile(playerId: String): Player? {
        return authRepository.getPlayerProfile(playerId)
    }

    fun signOut() {
        authRepository.signOut()
        _navigationState.value = NavigationState.ToLogin
    }
}
