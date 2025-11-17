package gaming.xplay.data.model

data class Challenge(
    val challengeId: String = "",
    val gameId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val status: String = "pending", // pending, accepted, completed, disputed
    val player1Result: String? = null, // "win" or "loss"
    val player2Result: String? = null
)
