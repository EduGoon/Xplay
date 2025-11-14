package gaming.xplay.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import gaming.xplay.datamodel.Challenge
import gaming.xplay.datamodel.Match
import gaming.xplay.datamodel.rankings
import kotlinx.coroutines.tasks.await
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class GameRepository @Inject constructor(
    private val db: FirebaseFirestore
) {

    // --- Challenge Functions ---

    suspend fun createChallenge(challenge: Challenge): String {
        val challengeRef = db.collection("challenges").document()
        val newChallenge = challenge.copy(challengeId = challengeRef.id)
        challengeRef.set(newChallenge).await()
        return challengeRef.id
    }

    suspend fun getChallenge(challengeId: String): Challenge? {
        return db.collection("challenges").document(challengeId).get().await().toObject(Challenge::class.java)
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

    suspend fun updateChallengeStatus(challengeId: String, status: String) {
        db.collection("challenges").document(challengeId).update("status", status).await()
    }

    suspend fun submitMatchResult(challengeId: String, playerId: String, result: String) {
        val challengeRef = db.collection("challenges").document(challengeId)

        // The transaction ensures this whole block is atomic, preventing race conditions.
        val updatedChallenge = db.runTransaction { transaction ->
            val snapshot = transaction.get(challengeRef)
            val challenge = snapshot.toObject(Challenge::class.java)
                ?: throw IllegalStateException("Challenge not found!")

            // Pre-condition checks inside the transaction
            if (challenge.status != "accepted") throw IllegalStateException("This challenge is not active.")
            val playerResultField = if (playerId == challenge.player1Id) "player1Result" else "player2Result"
            if (snapshot.getString(playerResultField) != null) throw IllegalStateException("You have already submitted your result.")
            
            transaction.update(challengeRef, playerResultField, result)
            
            // Return a new copy of the challenge with the updated field for the next step.
            if (playerId == challenge.player1Id) {
                challenge.copy(player1Result = result)
            } else {
                challenge.copy(player2Result = result)
            }
        }.await()

        // Verification happens *after* the transaction is successful
        if (updatedChallenge.player1Result != null && updatedChallenge.player2Result != null) {
            verifyAndFinalizeMatch(updatedChallenge)
        }
    }

    private suspend fun verifyAndFinalizeMatch(challenge: Challenge) {
        val p1Result = challenge.player1Result!!
        val p2Result = challenge.player2Result!!

        val batch = db.batch() // A batch ensures all writes succeed or none do.

        // Handle Disputes
        if (p1Result == p2Result) {
            batch.update(db.collection("challenges").document(challenge.challengeId), "status", "disputed")
            batch.commit().await()
            return
        }

        val winnerId = if (p1Result == "win") challenge.player1Id else challenge.player2Id
        val loserId = if (winnerId == challenge.player1Id) challenge.player2Id else challenge.player1Id

        // 1. Create the official Match document
        val matchRef = db.collection("matches").document()
        val newMatch = Match(matchid = matchRef.id, gameId = challenge.gameId, player1Id = challenge.player1Id, player2Id = challenge.player2Id, winnerId = winnerId)
        batch.set(matchRef, newMatch)

        // 2. Update player stats
        updatePlayerStatsWithBatch(batch, winnerId, challenge.gameId, 3, 1, 0)
        updatePlayerStatsWithBatch(batch, loserId, challenge.gameId, -3, 0, 1)

        // 3. Mark challenge as completed
        batch.update(db.collection("challenges").document(challenge.challengeId), "status", "completed")

        batch.commit().await()
    }
    
    // This is a new helper function for batching stat updates.
    private suspend fun updatePlayerStatsWithBatch(batch: com.google.firebase.firestore.WriteBatch, playerId: String, gameId: String, xpChange: Long, winIncrement: Int, lossIncrement: Int) {
        val rankingsQuery = db.collection("rankings").whereEqualTo("playerid", playerId).whereEqualTo("gameid", gameId).limit(1)
        val snapshot = rankingsQuery.get().await()

        if (snapshot.isEmpty) {
            val newRankingRef = db.collection("rankings").document()
            val newRanking = rankings(id = newRankingRef.id, playerid = playerId, gameid = gameId, XPpoints = max(0, xpChange).toInt(), wins = winIncrement, losses = lossIncrement)
            batch.set(newRankingRef, newRanking)
        } else {
            val docRef = snapshot.documents[0].reference
            // When using a batch, we can't read then write. We must read *before* the batch starts.
            // We fetch the latest data from the snapshot we already have.
            val currentXp = snapshot.documents[0].getLong("XPpoints") ?: 0L
            val currentWins = snapshot.documents[0].getLong("wins") ?: 0L
            val currentLosses = snapshot.documents[0].getLong("losses") ?: 0L

            batch.update(docRef, mapOf(
                "XPpoints" to max(0L, currentXp + xpChange),
                "wins" to currentWins + winIncrement,
                "losses" to currentLosses + lossIncrement
            ))
        }
    }

    // --- Match and Ranking Functions (Originals, now for reference) ---

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
