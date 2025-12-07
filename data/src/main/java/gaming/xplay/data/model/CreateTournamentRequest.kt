package gaming.xplay.data.model

data class CreateTournamentRequest(
    val clubId: String,
    val adminId: String,
    val tournamentName: String,
    val rankingType: RankingType
)
