package gaming.xplay.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import gaming.xplay.data.model.Player
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {

    suspend fun signInWithGoogle(idToken: String): Player {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        val data = hashMapOf("idToken" to idToken)

        val result = functions
            .getHttpsCallable("signIn")
            .call(data)
            .await()

        val resultMap = result.data as HashMap<String, Any>

        return Player(
            uid = resultMap["uid"] as String?,
            name = resultMap["name"] as String?,
            email = resultMap["email"] as String?,
            profilePictureUrl = resultMap["profilePictureUrl"] as String?,
            isFirstTime = resultMap["isFirstTime"] as Boolean
        )
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

    fun checkCurrentUserUid() :String? {
        val firebaseUser = auth.currentUser
        val uid = firebaseUser?.uid
        return uid
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

    suspend fun updateFCMToken(token: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("players").document(userId)
                .update("fcmToken", token)
                .await()
            Log.d("AuthRepo", "FCM token updated for user: $userId")
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error updating FCM token for user: $userId", e)
            // Depending on your error handling strategy, you might want to log this to a service like Crashlytics.
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
