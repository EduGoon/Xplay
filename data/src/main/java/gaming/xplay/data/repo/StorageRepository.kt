package gaming.xplay.data.repo

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import gaming.xplay.data.model.Result
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {

    suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference
            val imageRef = storageRef.child("club_images/${UUID.randomUUID()}")
            val uploadTask = imageRef.putFile(imageUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
