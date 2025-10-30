package gaming.xplay.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import gaming.xplay.datamodel.User
import gaming.xplay.viewmodel.usersVM.HiltViewModel
import java.util.concurrent.TimeUnit

@HiltViewModel
class usersVM @Inject constructor() : ViewModel() {
    annotation class HiltViewModel

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Store verification ID when code is sent
    private var verificationId: String? = null

    // Step 1: Send verification code
    fun sendVerificationCode(
        phoneNumber: String,  // Should be String like "+254712345678"
        activity: Activity,
        onCodeSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    //Auto verification of vID sent to user and sign in user only for android
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInAndSaveUser(credential, phoneNumber, onError)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onError(e.message ?: "Verification failed")
                }

                    //This is after vID sent to user and user needs to enter vID
                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = vId
                    onCodeSent()
                }
            }
            )
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // Step 2: Verify the code user entered manually
    fun verifyCode(
        code: String,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (verificationId == null) {
            onError("Verification ID is null. Please resend code.")
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInAndSaveUser(credential, phoneNumber, onError, onSuccess)
    }
    
    // Step 3: Sign in and save to Firestore
    private fun signInAndSaveUser(
        credential: PhoneAuthCredential,
        phoneNumber: String,
        onError: (String) -> Unit,
        onSuccess: (() -> Unit)? = null
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

                // Create user with only phone number filled
                val user = User(
                    uid = uid,
                    name = "",  // Will be updated later
                    phoneNumber = phoneNumber,
                    CountyOfResidence = "",  // Will be updated later
                    XplayPoints = 0,
                    ranking = 0,
                    wins = 0,
                    draws = 0,
                    losses = 0
                )

                saveUserToFirestore(user) { success ->
                    if (success) {
                        onSuccess?.invoke()
                    } else {
                        onError("Failed to save user to Firestore")
                    }
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Sign in failed")
            }
    }

    private fun saveUserToFirestore(user: User, onComplete: (Boolean) -> Unit) {
        firestore.collection("users").document(user.uid).set(user)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}

annotation class Inject
