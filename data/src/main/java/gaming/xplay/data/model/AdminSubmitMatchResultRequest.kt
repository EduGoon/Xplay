package gaming.xplay.data.model

data class AdminSubmitMatchResultRequest(
    val challengeId: String,
    val winnerId: String,
    val loserId: String
)
