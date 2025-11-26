package gaming.xplay.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import gaming.xplay.data.model.Challenge
import gaming.xplay.data.model.Match
import gaming.xplay.data.model.Result
import gaming.xplay.data.model.SubmitMatchResultRequest
import gaming.xplay.data.model.rankings
import gaming.xplay.data.network.ApiService
import kotlinx.coroutines.tasks.await
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
            val newChallenge = challenge.copy(challengeId = challengeRef.id)
            challengeRef.set(newChallenge).await()
            Result.Success(challengeRef.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateChallengeStatus(challengeId: String, status: String): Result<Unit> {
        return try {
            db.collection("challenges").document(challengeId).update("status", status).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getIncomingChallenges(playerId: String): Result<List<Challenge>> {
        return try {
            val challenges = db.collection("challenges")
                .whereEqualTo("player2Id", playerId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
                .toObjects(Challenge::class.java)
            Result.Success(challenges)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getOutgoingChallenges(playerId: String): Result<List<Challenge>> {
        return try {
            val challenges = db.collection("challenges")
                .whereEqualTo("player1Id", playerId)
                .get()
                .await()
                .toObjects(Challenge::class.java)
            Result.Success(challenges)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAcceptedChallenges(playerId: String): Result<List<Challenge>> {
        return try {
            val player1Accepted = db.collection("challenges")
                .whereEqualTo("player1Id", playerId)
                .whereEqualTo("status", "accepted")
                .get()
                .await()
                .toObjects(Challenge::class.java)

            val player2Accepted = db.collection("challenges")
                .whereEqualTo("player2Id", playerId)
                .whereEqualTo("status", "accepted")
                .get()
                .await()
                .toObjects(Challenge::class.java)

            Result.Success(player1Accepted + player2Accepted)
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

    suspend fun getMatchHistory(playerId: String): Result<List<Match>> {
        return try {
            val player1Matches = db.collection("matches")
                .whereEqualTo("player1Id", playerId)
                .get()
                .await()

            val player2Matches = db.collection("matches")
                .whereEqualTo("player2Id", playerId)
                .get()
                .await()

            val allMatches = (player1Matches.toObjects(Match::class.java) +
                    player2Matches.toObjects(Match::class.java))
                .distinctBy { it.matchid }

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
