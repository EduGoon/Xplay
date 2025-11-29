package gaming.xplay.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Challenge(
    val challengeId: String = "",
    val gameId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    var player1Name: String? = null,
    var player2Name: String? = null,
    val status: String = "pending", // pending, accepted, completed, disputed,waiting verification
    val player1Result: String? = null, // "win" or "loss"
    val player2Result: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    val acceptedAt: Date? = null
)
