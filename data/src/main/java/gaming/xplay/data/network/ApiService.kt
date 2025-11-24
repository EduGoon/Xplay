package gaming.xplay.data.network

import gaming.xplay.data.model.NotificationRequest
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.SignInRequest
import gaming.xplay.data.model.SubmitMatchResultRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("submit-match-result")
    suspend fun submitMatchResult(@Body request: SubmitMatchResultRequest)

    @Headers("Content-Type: application/json")
    @POST("sign-in")
    suspend fun signIn(@Body request: SignInRequest): Player

    @Headers("Content-Type: application/json")
    @POST("send-notification")
    suspend fun sendNotification(@Body request: NotificationRequest)
}
