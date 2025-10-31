package gaming.xplay.repo

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import gaming.xplay.datamodel.Player
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val auth: FirebaseAuth
) {
    private var verificationId: String? = null
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Activity passed as parameter, not stored
    fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,  // ← Passed in, not stored
        onCodeSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)  // ← Used here only
            .setCallbacks(
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        signInAndSaveUser(credential, phoneNumber, onError)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        onError(e.message ?: "Verification failed")
                    }

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

    private fun signInAndSaveUser(
        credential: PhoneAuthCredential,
        phoneNumber: String,
        onError: (String) -> Unit,
        onSuccess: (() -> Unit)? = null
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val playerid = authResult.user?.uid ?: return@addOnSuccessListener

                // Create user with only phone number filled
                val player = Player(
                    playerid = playerid,
                    name = "",  // Will be updated later
                    phoneNumber = phoneNumber,
                    CountyOfResidence = "",  // Will be updated later
                )

                saveUserToFirestore(player) { success ->
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

    private fun saveUserToFirestore(player: Player, onComplete: (Boolean) -> Unit) {
        firestore.collection("players").document(player.playerid).set(player)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
