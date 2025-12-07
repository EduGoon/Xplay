package gaming.xplay.data.model

data class Fixture(
    val fixtureId: String = "",
    val tournamentId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val winnerId: String? = null,
    val status: String = "pending",
    val challengeId: String? = null
)
