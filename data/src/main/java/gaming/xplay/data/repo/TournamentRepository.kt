package gaming.xplay.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import gaming.xplay.data.model.Challenge
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

                    var challengeId: String? = null
                    if (tournament.rankingType == gaming.xplay.data.model.RankingType.GLOBAL) {
                        val challenge = Challenge(
                            player1Id = members[i].uid,
                            player2Id = members[j].uid,
                            gameId = "FIFA", // Assuming a default game for now
                            status = "accepted"
                        )
                        challengeId = gameRepository.createChallenge(challenge).let { (it as Result.Success).data }
                    }

                    val fixture = Fixture(
                        fixtureId = fixtureRef.id,
                        tournamentId = tournament.tournamentId,
                        player1Id = members[i].uid,
                        player2Id = members[j].uid,
                        challengeId = challengeId
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
        winnerId: String?
    ): Result<Unit> {
        return try {
            if (tournament.rankingType == gaming.xplay.data.model.RankingType.GLOBAL) {
                if (winnerId != null) {
                    val loserId = if (winnerId == fixture.player1Id) fixture.player2Id else fixture.player1Id
                    fixture.challengeId?.let {
                        gameRepository.adminSubmitMatchResult(it, winnerId, loserId)
                    }
                }
            } else {
                val result = TournamentMatchResult(tournament.tournamentId, fixture.fixtureId, winnerId)
                firestore.collection("tournament_results").add(result).await()
                updateTournamentRankings(tournament.tournamentId, fixture.player1Id, fixture.player2Id, winnerId)
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

    private suspend fun updateTournamentRankings(tournamentId: String, player1Id: String, player2Id: String, winnerId: String?) {
        val player1RankingRef = firestore.collection("tournament_rankings").document("${tournamentId}_$player1Id")
        val player2RankingRef = firestore.collection("tournament_rankings").document("${tournamentId}_$player2Id")

        firestore.runTransaction {
            val player1Ranking = it.get(player1RankingRef).toObject(TournamentRanking::class.java) ?: TournamentRanking(tournamentId, player1Id)
            val player2Ranking = it.get(player2RankingRef).toObject(TournamentRanking::class.java) ?: TournamentRanking(tournamentId, player2Id)

            when (winnerId) {
                player1Id -> {
                    it.set(player1RankingRef, player1Ranking.copy(points = player1Ranking.points + 3, wins = player1Ranking.wins + 1))
                    it.set(player2RankingRef, player2Ranking.copy(losses = player2Ranking.losses + 1))
                }
                player2Id -> {
                    it.set(player2RankingRef, player2Ranking.copy(points = player2Ranking.points + 3, wins = player2Ranking.wins + 1))
                    it.set(player1RankingRef, player1Ranking.copy(losses = player1Ranking.losses + 1))
                }
                else -> { // Draw
                    it.set(player1RankingRef, player1Ranking.copy(points = player1Ranking.points + 1, draws = player1Ranking.draws + 1))
                    it.set(player2RankingRef, player2Ranking.copy(points = player2Ranking.points + 1, draws = player2Ranking.draws + 1))
                }
            }
        }.await()
    }

    suspend fun getTournamentRankings(tournamentId: String): Result<List<TournamentRanking>> {
        return try {
            val rankings = firestore.collection("tournament_rankings")
                .whereEqualTo("tournamentId", tournamentId)
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(TournamentRanking::class.java)
            Result.Success(rankings)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
