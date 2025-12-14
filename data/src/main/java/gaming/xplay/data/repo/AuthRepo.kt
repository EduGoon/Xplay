package gaming.xplay.data.repo

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.network.ApiService
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val apiService: ApiService,
    private val storage: FirebaseStorage
) {

    suspend fun signInWithGoogle(googleIdToken: String): Result<Player> {
        return try {
            // 1. Convert Google ID token → Firebase credential
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)

            // 2. Sign in to Firebase
            auth.signInWithCredential(credential).await()

            val firebaseIdToken = auth.currentUser?.getIdToken(true)?.await()?.token
                ?: throw Exception("Failed to get Firebase ID token")

            val player = apiService.signIn()

            Result.Success(player)

        } catch (e: Exception) {
            Log.e("AuthRepo", "Error in signInWithGoogle", e)
            Result.Error(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun completeOnboarding(): Result<Unit> {
        return try {
            val firebaseUser = auth.currentUser ?: throw Exception("User not authenticated")
            firestore.collection("players").document(firebaseUser.uid)
                .update("isFirstTime", false)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error in completeOnboarding", e)
            Result.Error(e)
        }
    }

    fun checkCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }

    suspend fun fetchCurrentUserProfile(): Result<Player?> {
        return try {
            val firebaseUser = auth.currentUser ?: return Result.Success(null)
            val player = firestore.collection("players").document(firebaseUser.uid).get().await()
                .toObject(Player::class.java)
            Result.Success(player)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error fetching user profile", e)
            Result.Error(e)
        }
    }

    suspend fun updateUserProfile(uid: String, name: String, profilePictureUri: Uri?): Result<Player> {
        return try {
            val user = firestore.collection("players").document(uid).get().await().toObject(Player::class.java)
                ?: throw Exception("Player not found")

            var newProfilePictureUrl = user.profilePictureUrl
            if (profilePictureUri != null) {
                newProfilePictureUrl = uploadImage(profilePictureUri)
            }

            val updatedPlayer = user.copy(
                name = name,
                profilePictureUrl = newProfilePictureUrl
            )

            firestore.collection("players").document(uid).set(updatedPlayer).await()

            Result.Success(updatedPlayer)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun uploadImage(imageUri: Uri): String {
        val storageRef = storage.reference.child("profile_pictures/${UUID.randomUUID()}")
        val uploadTask = storageRef.putFile(imageUri).await()
        return uploadTask.storage.downloadUrl.await().toString()
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
        }
    }

    suspend fun getPlayerProfile(playerId: String): Result<Player?> {
        return try {
            val player = firestore.collection("players").document(playerId).get().await()
                .toObject(Player::class.java)
            Result.Success(player)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error fetching player profile for ID: $playerId", e)
            Result.Error(e)
        }
    }
    
    suspend fun searchPlayers(query: String): Result<List<Player>> {
        return try {
            if (query.isBlank()) {
                return Result.Success(emptyList())
            }
            val players = firestore.collection("players")
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + '\uf8ff')
                .limit(10)
                .get()
                .await()
                .toObjects(Player::class.java)
            Result.Success(players)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error searching players", e)
            Result.Error(e)
        }
    }
}