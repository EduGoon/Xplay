package gaming.xplay.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import gaming.xplay.data.model.Fixture
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.model.Tournament
import gaming.xplay.data.model.TournamentMatchResult
import gaming.xplay.data.model.TournamentRanking
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TournamentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val gameRepository: GameRepository
) {

    suspend fun createFixtures(tournament: Tournament, members: List<Player>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            for (i in 0 until members.size - 1) {
                for (j in i + 1 until members.size) {
                    val fixtureRef = firestore.collection("fixtures").document()
                    val fixture = Fixture(
                        fixtureId = fixtureRef.id,
                        tournamentId = tournament.tournamentId,
                        player1Id = members[i].uid,
                        player2Id = members[j].uid
                    )
                    batch.set(fixtureRef, fixture)
                }
            }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getFixtures(tournamentId: String): Result<List<Fixture>> {
        return try {
            val fixtures = firestore.collection("fixtures")
                .whereEqualTo("tournamentId", tournamentId)
                .get()
                .await()
                .toObjects(Fixture::class.java)
            Result.Success(fixtures)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun startTournament(tournamentId: String): Result<Unit> {
        return try {
            firestore.collection("tournaments").document(tournamentId)
                .update("status", "in-progress").await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun submitTournamentMatchResult(
        tournament: Tournament,
        fixture: Fixture,
        winnerId: String
    ): Result<Unit> {
        return try {
            if (tournament.rankingType == gaming.xplay.data.model.RankingType.GLOBAL) {
                val challenge = gaming.xplay.data.model.Challenge(
                    player1Id = fixture.player1Id,
                    player2Id = fixture.player2Id,
                    gameId = "FIFA",
                )
                val challengeId = gameRepository.createChallenge(challenge).let { (it as Result.Success).data }
                gameRepository.submitMatchResult(challengeId, winnerId)
            } else {
                val result = TournamentMatchResult(tournament.tournamentId, fixture.fixtureId, winnerId)
                firestore.collection("tournament_results").add(result).await()
                val loserId = if (winnerId == fixture.player1Id) fixture.player2Id else fixture.player1Id
                updateTournamentRankings(tournament.tournamentId, winnerId, loserId)
            }

            firestore.collection("fixtures").document(fixture.fixtureId)
                .update("winnerId", winnerId, "status", "played").await()

            checkTournamentCompletion(tournament.tournamentId)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun checkTournamentCompletion(tournamentId: String) {
        val fixtures = getFixtures(tournamentId).let { (it as Result.Success).data }
        if (fixtures.all { it.status == "played" }) {
            firestore.collection("tournaments").document(tournamentId)
                .update("status", "completed").await()
        }
    }

    private suspend fun updateTournamentRankings(tournamentId: String, winnerId: String, loserId: String) {
        val winnerRankingRef = firestore.collection("tournament_rankings").document("${tournamentId}_$winnerId")
        val loserRankingRef = firestore.collection("tournament_rankings").document("${tournamentId}_$loserId")

        firestore.runTransaction {
            val winnerRanking = it.get(winnerRankingRef).toObject(TournamentRanking::class.java) ?: TournamentRanking(tournamentId, winnerId)
            val loserRanking = it.get(loserRankingRef).toObject(TournamentRanking::class.java) ?: TournamentRanking(tournamentId, loserId)

            it.set(winnerRankingRef, winnerRanking.copy(wins = winnerRanking.wins + 1))
            it.set(loserRankingRef, loserRanking.copy(losses = loserRanking.losses + 1))
        }.await()
    }

    suspend fun getTournamentRankings(tournamentId: String): Result<List<TournamentRanking>> {
        return try {
            val rankings = firestore.collection("tournament_rankings")
                .whereEqualTo("tournamentId", tournamentId)
                .orderBy("wins", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(TournamentRanking::class.java)
            Result.Success(rankings)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
