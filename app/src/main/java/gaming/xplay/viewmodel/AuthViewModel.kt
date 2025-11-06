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

    private val _currentUser = MutableStateFlow<UiState<Player?>>(UiState.Success(null))
    val currentUser: StateFlow<UiState<Player?>> = _currentUser.asStateFlow()

    private val _isUserSignedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _currentUser.value = UiState.Loading
            try {
                authRepository.signInWithGoogle(idToken)
                _isUserSignedIn.value = true
                _currentUser.value = UiState.Success(null)
            } catch (e: Exception) {
                _currentUser.value = UiState.Error(e.message ?: "An unknown error occurred")
                _isUserSignedIn.value = false
            }
        }
    }

    fun fetchPlayerProfile() {
        viewModelScope.launch {
            _currentUser.value = UiState.Loading
            try {
                val player = authRepository.fetchCurrentUser()
                _currentUser.value = UiState.Success(player)
            } catch (e: Exception) {
                _currentUser.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _isUserSignedIn.value = false
        _currentUser.value = UiState.Success(null)
    }
}
