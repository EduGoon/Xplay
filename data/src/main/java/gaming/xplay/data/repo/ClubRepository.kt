package gaming.xplay.data.repo

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import gaming.xplay.data.model.Club
import gaming.xplay.data.model.CreateClubRequest
import gaming.xplay.data.model.JoinClubRequest
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ClubRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun createClub(request: CreateClubRequest): Result<Club> {
        return try {
            val newClubRef = firestore.collection("clubs").document()
            val playerRef = firestore.collection("players").document(request.adminId)

            val newClub = Club(
                clubId = newClubRef.id,
                clubName = request.clubName,
                adminId = request.adminId,
                members = 1,
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/xplay-e8751.appspot.com/o/club_images%2Fdefault_club_image.png?alt=media&token=e3a9c782-7d29-4f31-8e5f-1c5c4e7e6d3d",
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

    suspend fun joinClub(request: JoinClubRequest): Result<Unit> {
        return try {
            val clubRef = firestore.collection("clubs").document(request.clubId)
            val playerRef = firestore.collection("players").document(request.playerId)

            firestore.runTransaction {
                val clubSnapshot = it.get(clubRef)
                val club = clubSnapshot.toObject(Club::class.java)!!

                it.update(clubRef, "members", club.members + 1)
                it.update(clubRef, "memberIds", FieldValue.arrayUnion(request.playerId))
                it.update(playerRef, "clubs", FieldValue.arrayUnion(request.clubId))
            }.await()
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
}
