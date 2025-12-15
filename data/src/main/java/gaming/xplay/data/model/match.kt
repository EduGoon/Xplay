package gaming.xplay.data.model

data class Match(
    val matchid: String = "",
    val gameId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val winnerId: String = "",
    val loserId: String = "",
    val status: String = ""
)
