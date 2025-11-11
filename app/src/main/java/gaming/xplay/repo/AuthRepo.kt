package gaming.xplay.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import gaming.xplay.datamodel.Player
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun signInWithGoogle(idToken: String): Player {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user ?: throw Exception("Google sign-in succeeded but user data is null.")

        val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
        val userDocRef = firestore.collection("players").document(user.uid)

        if (isNewUser) {
            Log.d("AuthRepo", "New user detected, creating profile...")
            val newPlayer = Player(
                uid = user.uid,
                name = user.displayName,
                email = user.email,
                profilePictureUrl = user.photoUrl?.toString(),
                isFirstTime = true
            )
            // This operation might fail due to Firestore rules
            userDocRef.set(newPlayer).await()
            Log.d("AuthRepo", "New user profile created in Firestore: ${user.uid}")
            return newPlayer
        } else {
            Log.d("AuthRepo", "Returning user detected, fetching profile...")
            val snapshot = userDocRef.get().await()
            val player = snapshot.toObject(Player::class.java)
            if (player != null) {
                return player
            } else {
                // This case handles a returning user that is missing a firestore document.
                // This can happen if the document was manually deleted or the initial write failed
                Log.w("AuthRepo", "User exists in Auth, but not in Firestore. Creating new profile.")
                 val fallbackPlayer = Player(
                    uid = user.uid,
                    name = user.displayName,
                    email = user.email,
                    profilePictureUrl = user.photoUrl?.toString(),
                    isFirstTime = false // Not a new user, but needs a profile
                )
                userDocRef.set(fallbackPlayer).await()
                return fallbackPlayer
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun completeOnboarding() {
        val firebaseUser = auth.currentUser ?: throw Exception("User not authenticated")
        firestore.collection("players").document(firebaseUser.uid)
            .update("isFirstTime", false)
            .await()
    }

    suspend fun fetchCurrentUserProfile(): Player? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            firestore.collection("players").document(firebaseUser.uid).get().await()
                .toObject(Player::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error fetching user profile", e)
            null
        }
    }

    suspend fun getPlayerProfile(playerId: String): Player? {
        return try {
            firestore.collection("players").document(playerId).get().await()
                .toObject(Player::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error fetching player profile for ID: $playerId", e)
            null
        }
    }
}
