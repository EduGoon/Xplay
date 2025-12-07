package gaming.xplay.data.model

data class CreateClubRequest(
    val clubName: String,
    val adminId: String,
    val imageUrl: String? = null
)
