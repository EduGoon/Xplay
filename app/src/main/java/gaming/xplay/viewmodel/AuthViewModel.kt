
package gaming.xplay.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import gaming.xplay.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository(FirebaseAuth.getInstance())

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _verificationCode = MutableStateFlow("")
    val verificationCode: StateFlow<String> = _verificationCode.asStateFlow()

    private val _isCodeSent = MutableStateFlow(false)
    val isCodeSent: StateFlow<Boolean> = _isCodeSent.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    fun onPhoneNumberChanged(phone: String) {
        _phoneNumber.value = phone
    }

    fun onVerificationCodeChanged(code: String) {
        _verificationCode.value = code
    }

    fun sendVerificationCode(activity: Activity) {
        viewModelScope.launch {
            authRepository.sendVerificationCode(
                phoneNumber = _phoneNumber.value,
                activity = activity,
                onCodeSent = { _isCodeSent.value = true },
                onError = { _error.value = it }
            )
        }
    }

    fun verifyCode() {
        viewModelScope.launch {
            authRepository.verifyCode(
                code = _verificationCode.value,
                phoneNumber = _phoneNumber.value,
                onSuccess = { _loginSuccess.value = true },
                onError = { _error.value = it }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _isLoggedOut.value = true
    }

    fun onLoggedOut() {
        _isLoggedOut.value = false
        _loginSuccess.value = false
    }
}
