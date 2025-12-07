package gaming.xplay.data.model

import java.util.Date

data class Tournament(
    val tournamentId: String = "",
    val clubId: String = "",
    val name: String = "",
    val adminId: String = "",
    val members: List<String> = emptyList(),
    val startDate: Date? = null,
    val endDate: Date? = null,
    val status: String = "upcoming",
    val rankingType: RankingType = RankingType.GLOBAL
)
