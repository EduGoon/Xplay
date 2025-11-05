package gaming.xplay.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import gaming.xplay.datamodel.Player
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user
        if (user != null) {
            saveUserToFirestore(user)
        } else {
            throw Exception("Google sign-in succeeded but user data is null.")
        }
    }

    private suspend fun saveUserToFirestore(user: FirebaseUser) {
        val player = Player(
            uid = user.uid,
            email = user.email,
            profilePictureUrl = user.photoUrl?.toString()
        )
        // Use set with merge to create or update user data without overwriting
        firestore.collection("players").document(user.uid)
            .set(player, SetOptions.merge()).await()
        Log.d("AuthRepo", "User saved to Firestore: ${user.uid}")
    }

    suspend fun updateUsername(userId: String, newUsername: String) {
        firestore.collection("players").document(userId)
            .update("name", newUsername)
            .await()
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun fetchCurrentUser(): Player? {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            try {
                firestore.collection("players").document(firebaseUser.uid).get().await()
                    .toObject(Player::class.java)
            } catch (e: Exception) {
                // Handle exception
                null
            }
        } else {
            null
        }
    }
}
