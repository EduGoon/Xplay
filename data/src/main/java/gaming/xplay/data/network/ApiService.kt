package gaming.xplay.data.network

import gaming.xplay.data.model.NotificationRequest
import gaming.xplay.data.model.Player
import gaming.xplay.data.model.SignInRequest
import gaming.xplay.data.model.SubmitMatchResultRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("submitMatchResult")
    suspend fun submitMatchResult(@Body request: SubmitMatchResultRequest)

    @POST("signIn")
    suspend fun signIn(@Body request: SignInRequest): Player

    @POST("sendNotification")
    suspend fun sendNotification(@Body request: NotificationRequest)
}
