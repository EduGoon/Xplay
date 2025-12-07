package gaming.xplay.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ClubPost(
    val postId: String = "",
    val clubId: String = "",
    val authorId: String = "",
    val authorName: String? = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)
