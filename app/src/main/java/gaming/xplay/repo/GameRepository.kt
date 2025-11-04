package gaming.xplay.repo

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import gaming.xplay.datamodel.Match
import gaming.xplay.datamodel.rankings
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class GameRepository {

    private val db = Firebase.firestore

    suspend fun createMatch(match: Match): String {
        val matchRef = db.collection("matches").document()
        val newMatch = match.copy(matchid = matchRef.id)
        matchRef.set(newMatch).await()
        return matchRef.id
    }

    suspend fun confirmMatch(matchId: String, confirmed: Boolean) {
        val matchRef = db.collection("matches").document(matchId)
        val match = matchRef.get().await().toObject(Match::class.java) ?: return

        if (match.status != "pending") return // Already processed

        if (confirmed) {
            matchRef.update("status", "confirmed").await()
            val loserId = if (match.winnerId == match.player1Id) match.player2Id else match.player1Id
            val xpPoints = 3L

            // Update winner's stats
            updatePlayerStats(match.winnerId, match.gameId, xpPoints, 1, 0)
            // Update loser's stats
            updatePlayerStats(loserId, match.gameId, -xpPoints, 0, 1)

        } else {
            matchRef.update("status", "rejected").await()
        }
    }

    private suspend fun updatePlayerStats(playerId: String, gameId: String, xpChange: Long, winIncrement: Long, lossIncrement: Long) {
        val rankingsQuery = db.collection("rankings")
            .whereEqualTo("playerid", playerId)
            .whereEqualTo("gameid", gameId)
            .limit(1)

        val snapshot = rankingsQuery.get().await()

        if (snapshot.isEmpty) {
            // No ranking document yet for this game, create one
            val newRankingRef = db.collection("rankings").document()
            val newRanking = rankings(
                id = newRankingRef.id,
                playerid = playerId,
                gameid = gameId,
                XPpoints = max(0, xpChange).toInt(),
                wins = winIncrement.toInt(),
                losses = lossIncrement.toInt()
            )
            newRankingRef.set(newRanking).await()
        } else {
            // Document exists, update it using a transaction
            val docRef = snapshot.documents[0].reference
            db.runTransaction { transaction ->
                val rankingDoc = transaction.get(docRef)
                val currentXp = rankingDoc.getLong("XPpoints") ?: 0L
                val currentWins = rankingDoc.getLong("wins") ?: 0L
                val currentLosses = rankingDoc.getLong("losses") ?: 0L

                val newXp = max(0L, currentXp + xpChange)
                val newWins = currentWins + winIncrement
                val newLosses = currentLosses + lossIncrement

                transaction.update(docRef, mapOf(
                    "XPpoints" to newXp,
                    "wins" to newWins,
                    "losses" to newLosses
                ))
            }.await()
        }
    }

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
