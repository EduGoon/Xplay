package gaming.xplay.data.repo

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import gaming.xplay.data.model.Club
import gaming.xplay.data.model.ClubPost
import gaming.xplay.data.model.CreateClubRequest
import gaming.xplay.data.model.CreateTournamentRequest
import gaming.xplay.data.model.JoinClubRequest
import gaming.xplay.data.model.JoinTournamentRequest
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import gaming.xplay.data.model.Tournament
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ClubRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storageRepository: StorageRepository
) {

    suspend fun createClub(request: CreateClubRequest): Result<Club> {
        return try {
            val newClubRef = firestore.collection("clubs").document()
            val playerRef = firestore.collection("players").document(request.adminId)

            val imageUrl = request.imageUrl ?: "https://firebasestorage.googleapis.com/v0/b/xplay-e8751.appspot.com/o/club_images%2Fdefault_club_image.png?alt=media&token=e3a9c782-7d29-4f31-8e5f-1c5c4e7e6d3d"

            val newClub = Club(
                clubId = newClubRef.id,
                clubName = request.clubName,
                adminId = request.adminId,
                members = 1,
                imageUrl = imageUrl,
                memberIds = listOf(request.adminId)
            )

            firestore.runBatch {
                it.set(newClubRef, newClub)
                it.update(playerRef, "isClubOwner", true)
                it.update(playerRef, "clubs", FieldValue.arrayUnion(newClubRef.id))
            }.await()

            Result.Success(newClub)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createTournament(request: CreateTournamentRequest): Result<Tournament> {
        return try {
            val newTournamentRef = firestore.collection("tournaments").document()
            val clubRef = firestore.collection("clubs").document(request.clubId)

            val newTournament = Tournament(
                tournamentId = newTournamentRef.id,
                clubId = request.clubId,
                name = request.tournamentName,
                adminId = request.adminId,
                rankingType = request.rankingType
            )

            firestore.runBatch {
                it.set(newTournamentRef, newTournament)
                it.update(clubRef, "tournaments", FieldValue.arrayUnion(newTournamentRef.id))
            }.await()

            Result.Success(newTournament)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getTournament(tournamentId: String): Result<Tournament?> {
        return try {
            val tournament = firestore.collection("tournaments").document(tournamentId).get().await().toObject(Tournament::class.java)
            Result.Success(tournament)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun joinTournament(request: JoinTournamentRequest): Result<Unit> {
        return try {
            firestore.collection("tournaments").document(request.tournamentId)
                .update("members", FieldValue.arrayUnion(request.playerId)).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getTournamentsForClub(clubId: String): Result<List<Tournament>> {
        return try {
            val tournaments = firestore.collection("tournaments").whereEqualTo("clubId", clubId).get().await().toObjects(Tournament::class.java)
            Result.Success(tournaments)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun requestToJoinClub(request: JoinClubRequest): Result<Unit> {
        return try {
            firestore.collection("clubs").document(request.clubId)
                .update("pendingMemberIds", FieldValue.arrayUnion(request.playerId)).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun approveJoinRequest(request: JoinClubRequest): Result<Unit> {
        return try {
            val clubRef = firestore.collection("clubs").document(request.clubId)
            val playerRef = firestore.collection("players").document(request.playerId)

            firestore.runTransaction {
                val clubSnapshot = it.get(clubRef)
                val club = clubSnapshot.toObject(Club::class.java)!!

                it.update(clubRef, "members", club.members + 1)
                it.update(clubRef, "memberIds", FieldValue.arrayUnion(request.playerId))
                it.update(clubRef, "pendingMemberIds", FieldValue.arrayRemove(request.playerId))
                it.update(playerRef, "clubs", FieldValue.arrayUnion(request.clubId))
            }.await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun declineJoinRequest(request: JoinClubRequest): Result<Unit> {
        return try {
            firestore.collection("clubs").document(request.clubId)
                .update("pendingMemberIds", FieldValue.arrayRemove(request.playerId))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getClubs(): Result<List<Club>> {
        return try {
            val clubs = firestore.collection("clubs").get().await().toObjects(Club::class.java)
            Result.Success(clubs)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAdminClubs(adminId: String): Result<List<Club>> {
        return try {
            val clubs = firestore.collection("clubs").whereEqualTo("adminId", adminId).get().await().toObjects(Club::class.java)
            Result.Success(clubs)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getClub(clubId: String): Result<Club?> {
        return try {
            val club = firestore.collection("clubs").document(clubId).get().await().toObject(Club::class.java)
            Result.Success(club)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getClubMembers(memberIds: List<String>): Result<List<Player>> {
        return try {
            if (memberIds.isEmpty()) {
                return Result.Success(emptyList())
            }
            val members = firestore.collection("players").whereIn("uid", memberIds).get().await().toObjects(Player::class.java)
            Result.Success(members)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createClubPost(clubId: String, authorId: String, authorName: String?, text: String): Result<Unit> {
        return try {
            val newPostRef = firestore.collection("clubs").document(clubId).collection("posts").document()
            val post = ClubPost(
                postId = newPostRef.id,
                clubId = clubId,
                authorId = authorId,
                authorName = authorName,
                text = text
            )
            newPostRef.set(post).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getClubPosts(clubId: String): Result<List<ClubPost>> {
        return try {
            val posts = firestore.collection("clubs").document(clubId).collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(ClubPost::class.java)
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}