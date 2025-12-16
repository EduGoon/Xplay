package gaming.xplay.data.repo

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import gaming.xplay.data.model.AdminSubmitMatchResultRequest
import gaming.xplay.data.model.Challenge
import gaming.xplay.data.model.Match
import gaming.xplay.data.model.Result
import gaming.xplay.data.model.SubmitMatchResultRequest
import gaming.xplay.data.model.rankings
import gaming.xplay.data.network.ApiService
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val apiService: ApiService
) {

    suspend fun createChallenge(challenge: Challenge): Result<String> {
        return try {
            val challengeRef = db.collection("challenges").document()
            val newChallenge = challenge.copy(
                challengeId = challengeRef.id
            )
            challengeRef.set(newChallenge).await()
            Result.Success(challengeRef.id)
        } catch (e: Exception) {
            Log.e("GameRepository", "error creating challenge", e)
            Result.Error(e)
        }
    }

    suspend fun updateChallengeStatus(challengeId: String, status: String): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>("status" to status)
            if (status == "accepted") {
                updates["acceptedAt"] = FieldValue.serverTimestamp()
            }
            db.collection("challenges").document(challengeId).update(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAllChallenges(playerId: String): Result<List<Challenge>> {
        return try {
            val player1Challenges = db.collection("challenges")
                .whereEqualTo("player1Id", playerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Challenge::class.java)

            val player2Challenges = db.collection("challenges")
                .whereEqualTo("player2Id", playerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Challenge::class.java)

            Result.Success((player1Challenges + player2Challenges).sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun submitMatchResult(challengeId: String, result: String): Result<Unit> {
        return try {
            val request = SubmitMatchResultRequest(challengeId, result)
            apiService.submitMatchResult(request)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun adminSubmitMatchResult(challengeId: String, winnerId: String, loserId: String): Result<Unit> {
        return try {
            val request = AdminSubmitMatchResultRequest(challengeId, winnerId, loserId)
            apiService.adminSubmitMatchResult(request)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getMatchHistory(playerId: String): Result<List<Match>> {
        return try {
            val player1Matches = db.collection("matches")
                .whereEqualTo("player1Id", playerId)
                .orderBy("playedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val player2Matches = db.collection("matches")
                .whereEqualTo("player2Id", playerId)
                .orderBy("playedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val allMatches = (player1Matches.toObjects(Match::class.java) +
                    player2Matches.toObjects(Match::class.java))
                .distinctBy { it.matchid }
                .sortedByDescending { it.playedAt }

            Result.Success(allMatches)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getLeaderboard(gameId: String): Result<List<rankings>> {
        return try {
            val board = db.collection("rankings")
                .whereEqualTo("gameid", gameId)
                .orderBy("XPpoints", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(rankings::class.java)
            Result.Success(board)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getPlayerRanking(playerId: String, gameId: String): Result<rankings?> {
        return try {
            val ranking = db.collection("rankings")
                .whereEqualTo("playerid", playerId)
                .whereEqualTo("gameid", gameId)
                .get()
                .await()
                .toObjects(rankings::class.java)
                .firstOrNull()
            Result.Success(ranking)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
