package gaming.xplay.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import gaming.xplay.data.model.Challenge
import gaming.xplay.data.model.Match
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

    suspend fun createChallenge(challenge: Challenge): String {
        val challengeRef = db.collection("challenges").document()
        val newChallenge = challenge.copy(challengeId = challengeRef.id)
        challengeRef.set(newChallenge).await()
        return challengeRef.id
    }

    suspend fun updateChallengeStatus(challengeId: String, status: String) {
        db.collection("challenges").document(challengeId).update("status", status).await()
    }

    suspend fun getIncomingChallenges(playerId: String): List<Challenge> {
        return db.collection("challenges")
            .whereEqualTo("player2Id", playerId)
            .whereEqualTo("status", "pending")
            .get()
            .await()
            .toObjects(Challenge::class.java)
    }

    suspend fun getOutgoingChallenges(playerId: String): List<Challenge> {
        return db.collection("challenges")
            .whereEqualTo("player1Id", playerId)
            .get()
            .await()
            .toObjects(Challenge::class.java)
    }

    suspend fun getAcceptedChallenges(playerId: String): List<Challenge> {
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

        return player1Accepted + player2Accepted
    }

    suspend fun submitMatchResult(challengeId: String, result: String) {
        val request = SubmitMatchResultRequest(challengeId, result)
        apiService.submitMatchResult(request)
    }

    // --- Match and Ranking Functions ---

    suspend fun getMatchHistory(playerId: String): List<Match> {
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
            .distinctBy { it.matchid } // Avoid duplicates if a player plays against themselves

        return allMatches
    }

    suspend fun getLeaderboard(gameId: String): List<rankings> {
        return db.collection("rankings")
            .whereEqualTo("gameid", gameId)
            .orderBy("XPpoints", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(rankings::class.java)
    }
}
