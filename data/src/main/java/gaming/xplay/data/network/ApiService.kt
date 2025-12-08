package gaming.xplay.data.network

import gaming.xplay.data.model.AdminSubmitMatchResultRequest
import gaming.xplay.data.model.JoinClubRequest
import gaming.xplay.data.model.NotificationRequest
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.SubmitMatchResultRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("submit-match-result")
    suspend fun submitMatchResult(@Body request: SubmitMatchResultRequest)

    @Headers("Content-Type: application/json")
    @POST("admin/submit-match-result")
    suspend fun adminSubmitMatchResult(@Body request: AdminSubmitMatchResultRequest)

    @Headers("Content-Type: application/json")
    @POST("sign-in")
    suspend fun signIn(): Player

    @Headers("Content-Type: application/json")
    @POST("send-notification")
    suspend fun sendNotification(@Body request: NotificationRequest)

    @Headers("Content-Type: application/json")
    @POST("approve-join-club-request")
    suspend fun approveJoinRequest(@Body request: JoinClubRequest)
}
