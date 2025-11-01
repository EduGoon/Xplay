package gaming.xplay.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import gaming.xplay.datamodel.AuthState
import gaming.xplay.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun sendVerificationCode(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading

        repository.sendVerificationCode(
            phoneNumber = phoneNumber,
            activity = activity,  // ← ViewModel passes it
            onCodeSent = {
                _authState.value = AuthState.CodeSent
            },
            onError = { error ->
                _authState.value = AuthState.Error(error)
            }
        )
    }

    fun verifyCode(code: String, phoneNumber: String) {
        _authState.value = AuthState.Loading

        repository.verifyCode(
            code = code,
            phoneNumber = phoneNumber,
            onSuccess = {
                _authState.value = AuthState.Success
            },
            onError = { error ->
                _authState.value = AuthState.Error(error)
            }
        )
    }
}
