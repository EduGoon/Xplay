package gaming.xplay.datamodel

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object CodeSent : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

data class Player(
    val playerid: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val CountyOfResidence: String = ""
)

data class Game(
    val gameid: String,
    val name: String,
)

data class Match(
    val matchid: String,
    val player1: Player,
    val player2: Player,
    val score: String,
    val winner: Player,
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

sealed class NotificationState {
    object Idle : NotificationState()
    object Sending : NotificationState()
    data class Success(val accepted: Boolean) : NotificationState()
    data class Error(val message: String) : NotificationState()
    object Timeout : NotificationState()
}
