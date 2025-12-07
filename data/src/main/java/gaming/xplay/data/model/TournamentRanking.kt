package gaming.xplay.data.model

data class TournamentRanking(
    val tournamentId: String = "",
    val playerId: String = "",
    val wins: Int = 0,
    val losses: Int = 0
)
