package gaming.xplay.datamodel

data class Player(
    val uid: String = "",
    val name: String? = null,
    val email: String? = null,
    val profilePictureUrl: String? = null
)

data class Game(
    val gameid: String,
    val name: String,
)

data class Match(
    val matchid: String = "",
    val gameId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val score: String = "",
    val winnerId: String = "",
    val status: String = "pending" // can be 'pending', 'confirmed', or 'rejected'
)

data class rankings(
    val id: String,
    val playerid: String,
    val gameid: String,
    val XPpoints: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
)

data class NotificationRequest(
    val targetUserId: String,
    val title: String,
    val body: String,
    val requestId: String = java.util.UUID.randomUUID().toString()
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class NotificationState {
    object Idle : NotificationState()
    object Sending : NotificationState()
    data class Success(val accepted: Boolean) : NotificationState()
    data class Error(val message: String) : NotificationState()
    object Timeout : NotificationState()
}
