package gaming.xplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import gaming.xplay.datamodel.Player
import gaming.xplay.datamodel.UiState
import gaming.xplay.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UiState<Player>>(UiState.Loading)
    val currentUser: StateFlow<UiState<Player>> = _currentUser.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUsernameSet = MutableStateFlow(false)
    val isUsernameSet: StateFlow<Boolean> = _isUsernameSet.asStateFlow()

    init {
        viewModelScope.launch {
            if (firebaseAuth.currentUser != null) {
                fetchPlayerProfile()
            } else {
                _isLoggedOut.value = true
            }
        }
    }

    private fun fetchPlayerProfile() {
        viewModelScope.launch {
            _currentUser.value = UiState.Loading
            try {
                val player = authRepository.fetchCurrentUser()
                if (player != null) {
                    _currentUser.value = UiState.Success(player)
                    if (!player.name.isNullOrBlank()) {
                        _loginSuccess.value = true
                    }
                } else {
                    _currentUser.value = UiState.Error("Couldn't fetch user profile.")
                    signOut() // Signing out to be safe
                }
            } catch (e: Exception) {
                _currentUser.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _currentUser.value = UiState.Loading
            try {
                authRepository.signInWithGoogle(idToken)
                fetchPlayerProfile()
            } catch (e: Exception) {
                _currentUser.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun updateUsername(username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = firebaseAuth.currentUser?.uid
                if (userId != null) {
                    authRepository.updateUsername(userId, username)
                    fetchPlayerProfile() // Re-fetch profile to get updated username
                    _isUsernameSet.value = true
                } else {
                    // Handle user not logged in case
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _currentUser.value = UiState.Error("Signed out")
        _loginSuccess.value = false
        _isLoggedOut.value = true
    }

    fun onLoggedOut() {
        _isLoggedOut.value = false
    }
}