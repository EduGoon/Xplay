package gaming.xplay.data.network

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val auth: FirebaseAuth
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val user = auth.currentUser
        if (user == null) {
            Log.w("AuthInterceptor", "Firebase user is null. Proceeding without auth header.")
            return chain.proceed(originalRequest)
        }

        return try {
            // Block the thread to synchronously get the token. This is safe to do in an interceptor.
            val tokenResult = Tasks.await(user.getIdToken(true))
            val idToken = tokenResult.token

            if (idToken == null) {
                Log.w("AuthInterceptor", "Firebase ID token is null. Proceeding without auth header.")
                chain.proceed(originalRequest)
            } else {
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $idToken")
                    .build()
                chain.proceed(newRequest)
            }
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "Error getting Firebase ID token.", e)
            // On error, proceed with the original request
            chain.proceed(originalRequest)
        }
    }
}
